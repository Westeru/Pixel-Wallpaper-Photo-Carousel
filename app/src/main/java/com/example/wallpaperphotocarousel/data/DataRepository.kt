package com.example.wallpaperphotocarousel.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

interface DataRepository {
  val data: Flow<List<String>>
  fun addUris(context: Context, uris: List<Uri>)
  fun removeUri(context: Context, uriString: String)
  fun getUris(context: Context): List<String>
  fun clearAll(context: Context)
}

class DefaultDataRepository(context: Context) : DataRepository {
  private val prefs = context.applicationContext.getSharedPreferences("wallpaper_carousel_prefs", Context.MODE_PRIVATE)
  private val _data = MutableStateFlow<List<String>>(emptyList())
  override val data: Flow<List<String>> = _data.asStateFlow()

  init {
    loadUris()
  }

  private fun loadUris() {
    val serialized = prefs.getString("selected_uris", "") ?: ""
    val list = if (serialized.isEmpty()) emptyList() else serialized.split("\n")
    _data.value = list
  }

  override fun getUris(context: Context): List<String> {
    val serialized = prefs.getString("selected_uris", "") ?: ""
    return if (serialized.isEmpty()) emptyList() else serialized.split("\n")
  }

  override fun addUris(context: Context, uris: List<Uri>) {
    val currentList = _data.value.toMutableList()
    val contentResolver = context.contentResolver

    for (uri in uris) {
      val uriString = uri.toString()
      if (!currentList.contains(uriString)) {
        try {
          contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
          )
          currentList.add(uriString)
        } catch (e: SecurityException) {
          Log.e("DataRepository", "Failed to take persistable URI permission for $uri", e)
          currentList.add(uriString)
        }
      }
    }
    saveList(currentList)
  }

  override fun removeUri(context: Context, uriString: String) {
    val currentList = _data.value.toMutableList()
    if (currentList.remove(uriString)) {
      val contentResolver = context.contentResolver
      try {
        contentResolver.releasePersistableUriPermission(
          Uri.parse(uriString),
          Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
      } catch (e: Exception) {
        Log.e("DataRepository", "Failed to release persistable URI permission for $uriString", e)
      }
      saveList(currentList)
    }
  }

  override fun clearAll(context: Context) {
    val currentList = _data.value
    val contentResolver = context.contentResolver
    for (uriString in currentList) {
      try {
        contentResolver.releasePersistableUriPermission(
          Uri.parse(uriString),
          Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
      } catch (e: Exception) {
        Log.e("DataRepository", "Failed to release persistable URI permission for $uriString", e)
      }
    }
    saveList(emptyList())
  }

  private fun saveList(list: List<String>) {
    val serialized = list.joinToString("\n")
    prefs.edit().putString("selected_uris", serialized).apply()
    _data.value = list
  }
}
