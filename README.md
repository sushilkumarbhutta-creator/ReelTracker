# Reel Tracker (Android)

Counts Instagram Reels and YouTube Shorts you swipe through, shows the count in a floating bubble at the top of the screen (over the notch area), and opens a details screen when you tap the bubble. It also shows today's screen time for Instagram, YouTube, and all apps.

## 1. Create the project

1. In Android Studio: **New Project → Empty Views Activity** (Kotlin, minimum SDK 26).
2. Replace the generated files with the ones in this folder:
   - `app/src/main/java/com/example/reeltracker/*.kt`
   - `app/src/main/AndroidManifest.xml`
   - `app/src/main/res/xml/accessibility_service_config.xml`
3. In `app/build.gradle.kts`, make sure these dependencies exist:
   ```kotlin
   implementation("androidx.appcompat:appcompat:1.7.0")
   implementation("androidx.core:core-ktx:1.13.1")
   ```
4. Sync Gradle and run on your phone (enable Developer Options and USB debugging first).

## 2. Grant permissions (in this order)

1. **Accessibility service:** Settings → Accessibility → Installed apps → Reel Tracker → On. On Android 13+ sideloaded apps may first need Settings → Apps → Reel Tracker → ⋮ → *Allow restricted settings*.
2. **Usage access:** needed for screen time.
3. **Display over other apps:** needed for the bubble.
4. **Notifications:** allow them so the foreground-service notification appears.

Then tap **Show bubble**. The bubble stays on screen until you stop the service.

## 3. Find the real view IDs (required for accurate counting)

The detection looks for view IDs such as `clips_viewer` in `ReelTrackerService.kt`. These are placeholders. Instagram and YouTube change their IDs with updates, so check your installed versions:

1. Connect the phone and open Instagram's Reels tab (and YouTube Shorts).
2. Run:
   ```bash
   adb shell uiautomator dump /sdcard/ui.xml
   adb pull /sdcard/ui.xml
   ```
3. Open `ui.xml` and search for `resource-id="com.instagram.android:id/..."` (or `com.google.android.youtube:id/...`) on the reels/shorts screen. Pick an ID that appears only there.
4. Put that fragment in the `targets` map in `ReelTrackerService.kt`.

If an app has no readable IDs, a fallback is to match on a `className` such as `androidx.viewpager2.widget.ViewPager2` plus a `contentDescription` unique to the reels screen. That needs a small code change in `hasMarker()`.

## 4. Known limits

- **Accuracy:** each vertical swipe is counted as one item, with a 1-second debounce. Scrolling comments inside a reel may be counted too. Tune the debounce and markers after testing.
- **Google Play:** Play does not allow accessibility services for general tracking purposes, so this will most likely be sideloaded (installed as an APK), not published.
- **Battery optimization:** on Xiaomi, Samsung, OnePlus, and similar phones, set the app to "No restrictions" in battery settings so Android doesn't kill the bubble.
- **Start on boot:** not included yet. A `BOOT_COMPLETED` receiver can start `BubbleService` after reboot.
- **Privacy:** the accessibility service reads only view IDs and scroll events. It does not store any screen text.

## 5. Possible next steps

- Draggable bubble (save its position in SharedPreferences)
- History screen with a bar chart per day
- Daily limits that change the bubble color or send a reminder
- Start-on-boot receiver
