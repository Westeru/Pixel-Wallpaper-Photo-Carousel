# Lockscreen Wallpaper Photo Carousel

A lightweight, premium Android live wallpaper application that automatically shuffles and displays backgrounds from a custom user collection every time the device screen turns off, ensuring a fresh image is already set when the screen unlocks.

*Keywords: Changing lockscreen photo, lockscreen photo shuffler, wallpaper shuffle on screen off, automated lockscreen wallpaper changer, custom photo carousel.*

## Direct Download (Pre-built APK)
If you do not want to compile the code yourself, you can download the pre-built, ready-to-install app package directly:
- **[Download APK (Lockscreen Wallpaper Photo Carousel.apk)](Lockscreen%20Wallpaper%20Photo%20Carousel.apk)**

> [!WARNING]
> **Play Protect Warning / Blocked App warning**:
> Because this APK is self-built and self-signed (not published on the Google Play Store), your Android phone may show a warning like **"Unsafe App Blocked"** or **"Blocked by Play Protect"** during installation.
> 
> **How to bypass this and install:**
> 1. When the pop-up appears, tap **"More details"** (or the small dropdown arrow).
> 2. Select **"Install anyway"**.
> 
> *The app runs completely offline, uses standard local storage permissions, and does not collect or send any user data.*

## Features
- **Smart Background Loading**: Wallpaper changes occur on screen-off events (`ACTION_SCREEN_OFF`), ensuring the new image is fully loaded and ready before the user wakes the screen.
- **Rendering & Resource Optimization**: 
  - Bypasses transition cross-fades and redundant render loops while the screen is off to conserve device battery and CPU.
  - Decodes images with optimal downsampling sizes matching device surface dimensions.
  - Uses `RGB_565` color profiles for memory-efficient thumbnail previews.
- **Dynamic Collection Dashboard**:
  - Launch photo picking to select up to 100 images from the gallery per session.
  - Instant thumbnail grid previewing selected images.
  - Delete individual images or tap **Delete All** to clear the entire collection instantly.
- **Premium App Branding**: Custom launcher icon featuring vibrant gradient backgrounds and clean vector stacked polaroid art.

## Getting Started

### Prerequisites
- Android Studio Koala / Ladybug or newer.
- Android SDK 36 (target API level 36).
- A physical Android device or emulator running API 24 (Nougat) or higher.

### Building & Running

1. **Clone the repository**:
   ```bash
   git clone https://github.com/Westeru/Pixel-Wallpaper-Photo-Carousel.git
   cd wallpaper-photo-carousel
   ```

2. **Compile Debug APK**:
   ```bash
   ./gradlew assembleDebug
   ```

3. **Install on Connected Device**:
   ```bash
   ./gradlew installDebug
   ```

4. **Build signed Release APK / App Bundle**:
   The release signing configurations are defined in `app/build.gradle.kts` and utilize a local `my-release-key.jks` keystore (excluded from version control). To build a signed release variant:
   ```bash
   ./gradlew assembleRelease
   ```
   Or to create an Android App Bundle (.aab) for store listing:
   ```bash
   ./gradlew bundleRelease
   ```

