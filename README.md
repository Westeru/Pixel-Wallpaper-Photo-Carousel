# Lockscreen Wallpaper Photo Carousel

A lightweight, premium Android live wallpaper application that automatically shuffles and displays backgrounds from a custom user collection every time the device screen turns off, ensuring a fresh image is already set when the screen unlocks.

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
   git clone https://github.com/yourusername/wallpaper-photo-carousel.git
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
"# Pixel-Wallpaper-Photo-Carousel" 
