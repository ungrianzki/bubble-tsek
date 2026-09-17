# Bubble Tsek – Android app

Bubble sheet checker for teachers. This folder is the full Android app project.
It works offline. The results Excel file can be shared to Google Drive, Messenger, Gmail and other apps.

## Get the APK (no coding needed, all free)

1. Sign in at https://github.com (or make a free account).
2. Click **+ → New repository**. Name it `bubble-tsek`, choose **Private**, and click **Create repository**.
3. On the new page, click **uploading an existing file**. Drag in **everything inside** this folder (not the folder itself), then click **Commit changes**.
4. Open the **Actions** tab.
   - If you see "Build Bubble Tsek APK" running, skip to step 5.
   - If not, click **set up a workflow yourself**. Delete the sample text, paste everything from `.github/workflows/build.yml`, and click **Commit changes**.
5. Wait about 5–8 minutes for the green check mark.
6. On your phone, open your repository on github.com (signed in) → **Releases** → **BubbleTsek.apk**. Tap it to download.
7. Open the downloaded file. If Android asks, allow **Install unknown apps** for your browser, then tap **Install**.

## Updating later
Upload changed files to the same repository. A new APK appears under **Releases**.
Install it over the old app, and your saved scores stay.

## Notes
- Keep the file `android/app/bubbletsek-release.jks`. Updates need it.
- On first scan, tap **Allow** when the app asks for the camera.
