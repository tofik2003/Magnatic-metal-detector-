# CI/CD GitHub Actions Setup Guide

To compile this project automatically into a downloadable `.apk` via GitHub Actions on GitHub, follow these simple steps:

### Step 1: Create the workflow file on GitHub
In your GitHub repository web interface:
1. Navigate to the **Actions** tab (or browse the repository code).
2. Click **New workflow** -> **set up a workflow yourself**.
3. Name the file `.github/workflows/android-build.yml`.
4. Paste the following configuration:

```yaml
name: Build Android APK & Run Tests

on:
  push:
    branches: [ "main", "arena/**" ]
  pull_request:
    branches: [ "main" ]
  workflow_dispatch:

jobs:
  build:
    name: Build & Test APK
    runs-on: ubuntu-latest

    steps:
      - name: Checkout Code
        uses: actions/checkout@v4

      - name: Validate Gradle Wrapper
        uses: gradle/actions/wrapper-validation@v3

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
          cache: 'gradle'

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      - name: Run Unit Tests
        run: ./gradlew testDebugUnitTest --no-daemon --stacktrace

      - name: Build Debug APK
        run: ./gradlew assembleDebug --no-daemon --stacktrace

      - name: Upload Debug APK Artifact
        uses: actions/upload-artifact@v4
        with:
          name: metal-detector-debug-apk
          path: app/build/outputs/apk/debug/app-debug.apk
          retention-days: 14
          if-no-files-found: error
```

### Step 2: Download your compiled APK
- Whenever you push code or trigger the workflow, GitHub will automatically:
  1. Boot a fresh Ubuntu runner.
  2. Set up Java 17 and Gradle 8.9.
  3. Run all unit tests for the detection algorithm.
  4. Compile `app-debug.apk`.
  5. Provide a download link under the **Artifacts** section of the run.
