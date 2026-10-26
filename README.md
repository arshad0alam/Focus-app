# StayFocused

## APK kaise banaye

**Option A – GitHub (laptop ki zarurat nahi)**
1. github.com par new repo banao, is folder ki saari files upload karo (`.github` folder ke saath).
2. Actions tab -> "Build APK" -> Run workflow.
3. Run khatam hone par Artifacts se `stayfocused.apk` download karo.

**Option B – Android Studio**
Folder open karo -> Build > Build APK(s) -> `app/build/outputs/apk/debug/app-debug.apk` ko rename karke `stayfocused.apk`.

## Phone mein setup
1. APK install karo, app kholo.
2. Home par "Usage access" -> StayFocused ON.
3. "Accessibility" -> StayFocused ON. (Android 13+ mein greyed ho to: Settings > Apps > StayFocused > ⋮ > Allow restricted settings.)
4. Battery > StayFocused > Unrestricted (taaki service band na ho).
