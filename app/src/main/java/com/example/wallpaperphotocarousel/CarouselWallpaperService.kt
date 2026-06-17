package com.example.wallpaperphotocarousel

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CarouselWallpaperService : WallpaperService() {

  override fun onCreateEngine(): Engine {
    return CarouselEngine()
  }

  inner class CarouselEngine : Engine() {
    private var serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val prefs by lazy { getSharedPreferences("wallpaper_carousel_prefs", Context.MODE_PRIVATE) }
    
    private var currentBitmap: Bitmap? = null
    private var lastShownUri: String? = null
    private var surfaceWidth: Int = 1080
    private var surfaceHeight: Int = 1920
    private var transitionJob: Job? = null
    private var isReceiverRegistered = false

    private val screenReceiver = object : BroadcastReceiver() {
      override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_SCREEN_OFF) {
          loadNextWallpaper()
        }
      }
    }

    override fun onCreate(surfaceHolder: SurfaceHolder) {
      super.onCreate(surfaceHolder)
      registerScreenReceiver()
    }

    override fun onDestroy() {
      unregisterScreenReceiver()
      transitionJob?.cancel()
      serviceScope.cancel()
      currentBitmap?.recycle()
      super.onDestroy()
    }

    override fun onVisibilityChanged(visible: Boolean) {
      super.onVisibilityChanged(visible)
      if (visible) {
        // Redraw current state if visible
        redrawCurrent()
      }
    }

    override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
      super.onSurfaceChanged(holder, format, width, height)
      surfaceWidth = width
      surfaceHeight = height
      if (currentBitmap == null) {
        loadInitialWallpaper()
      } else {
        redrawCurrent()
      }
    }

    private fun registerScreenReceiver() {
      if (!isReceiverRegistered) {
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        registerReceiver(screenReceiver, filter, Context.RECEIVER_EXPORTED)
        isReceiverRegistered = true
      }
    }

    private fun unregisterScreenReceiver() {
      if (isReceiverRegistered) {
        try {
          unregisterReceiver(screenReceiver)
        } catch (e: Exception) {
          Log.e("CarouselWallpaper", "Error unregistering receiver", e)
        }
        isReceiverRegistered = false
      }
    }

    private fun loadInitialWallpaper() {
      serviceScope.launch {
        val uris = getSavedUris()
        if (uris.isEmpty()) {
          drawFallback()
          return@launch
        }
        val uriStr = uris.random()
        lastShownUri = uriStr
        val bitmap = loadBitmap(uriStr)
        if (bitmap != null) {
          currentBitmap = bitmap
          redrawCurrent()
        } else {
          drawFallback()
        }
      }
    }

    private fun loadNextWallpaper() {
      serviceScope.launch {
        val uris = getSavedUris()
        if (uris.isEmpty()) {
          drawFallback()
          return@launch
        }
        var nextUriStr = uris.random()
        if (uris.size > 1 && nextUriStr == lastShownUri) {
          val remaining = uris.filter { it != lastShownUri }
          if (remaining.isNotEmpty()) {
            nextUriStr = remaining.random()
          }
        }
        lastShownUri = nextUriStr
        val nextBitmap = loadBitmap(nextUriStr)
        if (nextBitmap != null) {
          startTransition(nextBitmap)
        }
      }
    }

    private fun startTransition(newBitmap: Bitmap) {
      transitionJob?.cancel()
      val oldBitmap = currentBitmap
      currentBitmap = newBitmap

      if (!isVisible) {
        if (oldBitmap != null && oldBitmap != newBitmap) {
          oldBitmap.recycle()
        }
        drawFrame(null, newBitmap, 1f)
        return
      }

      transitionJob = serviceScope.launch(Dispatchers.Main) {
        val startTime = SystemClock.uptimeMillis()
        val duration = 300L // 300ms transition for smooth cross-fade
        var progress = 0f

        while (progress < 1f) {
          val elapsed = SystemClock.uptimeMillis() - startTime
          progress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
          
          drawFrame(oldBitmap, newBitmap, progress)
          
          if (progress >= 1f) break
          delay(16) // ~60 FPS
        }

        if (oldBitmap != null && oldBitmap != newBitmap) {
          oldBitmap.recycle()
        }
        // Final redraw of the single new bitmap fully opaque
        drawFrame(null, newBitmap, 1f)
      }
    }

    private fun redrawCurrent() {
      val bitmap = currentBitmap
      if (bitmap != null) {
        drawFrame(null, bitmap, 1f)
      } else {
        drawFallback()
      }
    }

    private fun drawFrame(oldBitmap: Bitmap?, newBitmap: Bitmap, progress: Float) {
      val holder = surfaceHolder
      var canvas: Canvas? = null
      try {
        canvas = holder.lockCanvas()
        if (canvas != null) {
          canvas.drawColor(Color.BLACK)
          if (oldBitmap != null && progress < 1f) {
            drawBitmapCenterCrop(canvas, oldBitmap, 255)
            val alpha = (progress * 255).toInt()
            drawBitmapCenterCrop(canvas, newBitmap, alpha)
          } else {
            drawBitmapCenterCrop(canvas, newBitmap, 255)
          }
        }
      } catch (e: Exception) {
        Log.e("CarouselWallpaper", "Error drawing frame", e)
      } finally {
        if (canvas != null) {
          try {
            holder.unlockCanvasAndPost(canvas)
          } catch (e: Exception) {
            Log.e("CarouselWallpaper", "Error unlocking canvas", e)
          }
        }
      }
    }

    private fun drawFallback() {
      val holder = surfaceHolder
      var canvas: Canvas? = null
      try {
        canvas = holder.lockCanvas()
        if (canvas != null) {
          drawFallbackState(canvas)
        }
      } catch (e: Exception) {
        Log.e("CarouselWallpaper", "Error drawing fallback state", e)
      } finally {
        if (canvas != null) {
          try {
            holder.unlockCanvasAndPost(canvas)
          } catch (e: Exception) {
            Log.e("CarouselWallpaper", "Error unlocking canvas", e)
          }
        }
      }
    }

    private fun drawFallbackState(canvas: Canvas) {
      val width = canvas.width.toFloat()
      val height = canvas.height.toFloat()
      val gradient = LinearGradient(
        0f, 0f, 0f, height,
        intArrayOf(0xFF1E1E2C.toInt(), 0xFF110E18.toInt()),
        null, Shader.TileMode.CLAMP
      )
      val paint = Paint().apply {
        shader = gradient
      }
      canvas.drawRect(0f, 0f, width, height, paint)
      
      val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 64f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        alpha = 180
      }
      canvas.drawText("Photo Shuffle Carousel", width / 2, height / 2 - 40, textPaint)
      
      val subTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 38f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        alpha = 130
      }
      canvas.drawText("Open settings and select photos to start", width / 2, height / 2 + 40, subTextPaint)
    }

    private fun drawBitmapCenterCrop(canvas: Canvas, bitmap: Bitmap, alpha: Int) {
      val viewWidth = canvas.width.toFloat()
      val viewHeight = canvas.height.toFloat()
      val bitmapWidth = bitmap.width.toFloat()
      val bitmapHeight = bitmap.height.toFloat()
      
      val scale: Float
      var dx = 0f
      var dy = 0f
      
      if (bitmapWidth * viewHeight > viewWidth * bitmapHeight) {
        scale = viewHeight / bitmapHeight
        dx = (viewWidth - bitmapWidth * scale) * 0.5f
      } else {
        scale = viewWidth / bitmapWidth
        dy = (viewHeight - bitmapHeight * scale) * 0.5f
      }
      
      val matrix = Matrix().apply {
        setScale(scale, scale)
        postTranslate(dx, dy)
      }
      val paint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
        this.alpha = alpha
      }
      canvas.drawBitmap(bitmap, matrix, paint)
    }

    private suspend fun loadBitmap(uriStr: String): Bitmap? = withContext(Dispatchers.IO) {
      try {
        val uri = Uri.parse(uriStr)
        val options = BitmapFactory.Options().apply {
          inJustDecodeBounds = true
        }
        contentResolver.openInputStream(uri)?.use { stream ->
          BitmapFactory.decodeStream(stream, null, options)
        }

        val imageWidth = options.outWidth
        val imageHeight = options.outHeight
        if (imageWidth <= 0 || imageHeight <= 0) return@withContext null

        // Decode with inSampleSize matching target dimensions
        var inSampleSize = 1
        val reqWidth = surfaceWidth
        val reqHeight = surfaceHeight
        if (imageHeight > reqHeight || imageWidth > reqWidth) {
          val halfHeight = imageHeight / 2
          val halfWidth = imageWidth / 2
          while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
            inSampleSize *= 2
          }
        }

        val decodeOptions = BitmapFactory.Options().apply {
          this.inSampleSize = inSampleSize
          inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        contentResolver.openInputStream(uri)?.use { stream ->
          BitmapFactory.decodeStream(stream, null, decodeOptions)
        }
      } catch (e: Exception) {
        Log.e("CarouselWallpaper", "Failed to load bitmap from uri: $uriStr", e)
        null
      }
    }

    private fun getSavedUris(): List<String> {
      val serialized = prefs.getString("selected_uris", "") ?: ""
      return if (serialized.isEmpty()) emptyList() else serialized.split("\n")
    }
  }
}
