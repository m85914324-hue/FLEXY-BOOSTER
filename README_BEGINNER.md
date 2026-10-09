# FLEXY: build it from your Android phone (step by step)

FLEXY = Game Booster dashboard + Game Launcher + Performance Monitor + Voice Changer.
Kotlin, Jetpack Compose, minimum Android 8.0 (API 26).

> Honest note: Android Studio does not run on Android phones, so the easiest
> phone-only route is to let GitHub build the APK for you in the cloud (free).

## Project structure
```
FLEXY/
├─ .github/workflows/build-apk.yml      <- GitHub builds your APK
├─ settings.gradle.kts, build.gradle.kts, gradle.properties
├─ gradle/wrapper/gradle-wrapper.properties
└─ app/
   ├─ build.gradle.kts                  <- dependencies, AdMob test ids
   └─ src/main/
      ├─ AndroidManifest.xml            <- permissions, AdMob app id
      ├─ res/ (values, xml, drawable, mipmap-anydpi-v26)
      └─ java/com/flexy/app/
         ├─ MainActivity.kt, FlexyApplication.kt
         ├─ navigation/  Route, FlexyRoot (sidebar+bars), FlexyDrawer
         ├─ ui/theme/    Theme.kt
         ├─ ui/components/ Components, Live, ActivityExt
         ├─ ui/screens/  Home, Boost, Booster, Games, Voice, Performance,
         │               Settings, InfoScreens (About + Privacy)
         ├─ data/        SettingsStore, GameRepository, SessionManager
         ├─ system/      DeviceInfo, SystemShortcuts, BoostAdvisor,
         │               SessionNotifier, Connectivity, Format
         ├─ audio/       VoiceRecorder, VoiceEffects, PcmPlayer, WavUtil,
         │               Recordings, VoiceViewModel
         └─ ads/         PlanManager, BillingManager, AdManager
```

## A. Build with only your phone (GitHub Actions)
1. Download **FLEXY.zip** to your phone.
2. In your phone browser open **github.com**, create a free account, tap **+ > New repository**, name it `flexy`, tap **Create repository**.
3. On the repository page tap **Code > Codespaces > Create codespace on main** (opens VS Code in the browser; "Desktop site" helps).
4. In the left Explorer, tap the **...** (or long-press empty space) > **Upload...** and pick `FLEXY.zip`.
5. Open the **Terminal** (menu > Terminal > New Terminal) and paste:
```
unzip -o FLEXY.zip
cp -r FLEXY/. .
rm -rf FLEXY FLEXY.zip
git add -A
git commit -m "Add FLEXY"
git push
```
6. Open the repo's **Actions** tab. "Build FLEXY APK" starts by itself (about 6-10 min the first time).
7. When it shows a green check, open the run and download **FLEXY-debug-apk** at the bottom.
8. Unzip it with your Files app, tap **app-debug.apk**, allow **Install unknown apps** when asked, then Install. (If Play Protect warns, tap **Install anyway**: it's your own app.)

## B. Build with Android Studio (laptop/PC)
1. Install Android Studio. **File > Open** the `FLEXY` folder. Wait for Gradle sync (it downloads Gradle 8.9 itself).
2. Plug in your phone (USB debugging on) and press **Run**. Or **Build > Build APK(s)**.

## Ads, FREE vs PLUS
* **FREE**: one full-screen Google AdMob ad when the app is opened (cold start, or coming back after 30+ minutes away). Never when switching screens, never more than once per 5 minutes, only if the app is on screen.
* **PLUS**: no consent request, no SDK start, no ad request. Completely ad-free, including offline (the plan is cached).
* **Offline**: no ad is requested or forced. Everything keeps working. For anything you gate behind an ad, call `AdManager.showRewarded(activity)`; on `AdGateResult.OFFLINE` show `AdManager.OFFLINE_MESSAGE` ("Internet connection required to load an advertisement.").
* **Consent**: Google's UMP form is shown where required; Settings gets an "Ad privacy options" button when Google requires it.
* **Test ads**: `gradle.properties` ships Google's official TEST ids, so you can't click real ads by accident. Before publishing, create your own AdMob app + ad units and replace `ADMOB_APP_ID`, `ADMOB_INTERSTITIAL_ID`, `ADMOB_REWARDED_ID`.
* **Try both plans**: debug builds have Settings > FLEXY plan > "Debug: simulate PLUS". Real purchases need a Play Console in-app product with id `flexy_plus` and FLEXY installed from Google Play (internal testing works). Sideloaded APKs can't buy.
* Test offline: turn on Airplane mode and reopen FLEXY (no ad, no errors).
* If your audience includes children, set the child-directed / under-age tags in AdMob and the consent request before publishing.

## Common build errors
| Error | Fix |
|---|---|
| `SDK location not found` | On GitHub this can't happen. In Android Studio: File > Project Structure > SDK. |
| `Unresolved reference ...` | Check the file is in the folder matching its `package` line. |
| `Could not resolve com.google...` | Internet/VPN issue; press Sync again. |
| `Duplicate class` | Delete `.gradle` and `build` folders, sync again. |
| `Unsupported class file major version` | Use JDK 17 (Android Studio's built-in JDK is fine). |
| Build fails in GitHub | Open the failed run > "Build debug APK" step, copy the red error text and send it to me. |

## Android limitations (also shown inside the app)
* No CPU/GPU boosting, FPS unlock or killing other apps (needs root).
* Brightness, Battery Saver and Game Mode can only be opened, not switched, by apps. Do Not Disturb works after you grant access.
* The voice changer processes recordings. It can't change your voice inside other games' voice chat.
* Exact play time in other apps needs "Usage access"; FLEXY doesn't ask for it.
