# LibreIntel Android - Project Structure & Git Policy

## Overview

This document describes the project structure of the LibreIntel Android app and the git commit policy that should be followed.

---

## Project Structure

```
libreintel-android/
├── .gitignore                 # Root gitignore (minimal - android/ has its own)
├── .opencode/                 # OpenCode agent configuration
├── PROJECT_STATUS.md         # Project history and status
├── android/                   # Android app source
│   ├── .gitignore            # Android-specific gitignore
│   ├── README.md             # Android-specific readme
│   ├── build.gradle          # Root build config
│   ├── settings.gradle       # Gradle settings
│   ├── gradle.properties     # Gradle properties
│   ├── gradlew               # Gradle wrapper script
│   ├── gradle/               # Gradle wrapper JAR
│   ├── local.properties      # SDK location (NOT committed)
│   ├── .gradle/              # Gradle cache (NOT committed)
│   ├── .idea/                # Android Studio files (NOT committed)
│   ├── *.iml                 # IntelliJ module files (NOT committed)
│   └── app/
│       ├── build.gradle      # App module build config
│       ├── proguard-rules.pro
│       ├── .gradle/          # Module gradle cache (NOT committed)
│       ├── build/            # Compiled outputs (NOT committed)
│       └── src/
│           └── main/
│               ├── AndroidManifest.xml
│               ├── java/com/libreintel/
│               │   ├── LibreIntelApp.kt
│               │   ├── data/              # Data layer
│               │   ├── domain/            # Domain layer
│               │   ├── di/                # Dependency injection
│               │   └── ui/                # UI layer
│               └── res/                   # Resources
│                   ├── drawable/
│                   ├── mipmap-*/
│                   └── values/
```

---

## Git Commit Policy

### What to COMMIT (Source Files)

| Category | Files |
|----------|-------|
| **Source Code** | `*.kt`, `*.java` |
| **Layouts/XML** | `*.xml` in `res/layout/`, `res/values/` |
| **Build Configs** | `build.gradle`, `settings.gradle`, `gradle.properties` |
| **Gradle Wrapper** | `gradlew`, `gradle/wrapper/*` |
| **App Icons** | `res/mipmap-*/*.png`, `res/drawable/*` |
| **Manifest** | `AndroidManifest.xml` |

### What to EXCLUDE (Never Commit)

| Category | Pattern | Reason |
|----------|---------|--------|
| **Compiled Outputs** | `app/build/`, `android/build/` | Regenerated on build |
| **Gradle Cache** | `.gradle/` | Generated, platform-specific |
| **IDE Files** | `.idea/`, `*.iml` | Personal/local settings |
| **SDK Config** | `local.properties` | Contains local SDK paths |
| **APK/DEX** | `*.apk`, `*.dex` | Binary files, huge size |
| **Logs** | `*.log` | Runtime generated |
| **OS Files** | `.DS_Store`, `Thumbs.db` | System artifacts |

### Why This Policy?

1. **Anyone can build** - Run `./gradlew assembleDebug` to generate APK
2. **Smaller repo** - No binary bloat, faster clones
3. **Cross-platform** - Build outputs are OS-specific
4. **Clean diffs** - Source changes are readable in git diff

---

## Standard Git Commands

### Clone & First Build
```bash
git clone git@github.com:FrankS-IntelLab/libreintel-android.git
cd libreintel-android/android
./gradlew assembleDebug
```

### Daily Development Workflow
```bash
# Make code changes in Android Studio
# Build & test in Android Studio (Run button)

# When ready to commit:
git add -A
git commit -m "Your commit message"
git push origin main
```

### Clean Build (if issues occur)
```bash
cd android
./gradlew clean
./gradlew assembleDebug
```

---

## Building the App

### Using Android Studio (Recommended)
1. Open `android/` folder in Android Studio
2. Wait for Gradle sync to complete
3. Click **Run** (green arrow) or **Build → Build APK**
4. APK will be at `android/app/build/outputs/apk/debug/app-debug.apk`

### Using Command Line
```bash
cd android
./gradlew assembleDebug    # Debug build
./gradlew assembleRelease  # Release build
```

**Note:** Compilation is handled by Android Studio or Gradle - no need to compile Kotlin manually.

---

## Best Practices

1. **Always use .gitignore** - Keep build outputs out of git
2. **Commit source, not builds** - Let others build from source
3. **Don't commit local.properties** - This contains your local SDK path
4. **Keep PROJECT_STATUS.md updated** - Document features and changes
5. **Use meaningful commit messages** - Describe what/why, not just what

---

## Common Issues

### "Build failed" after pulling
```bash
cd android
rm -rf .gradle build app/build
./gradlew clean assembleDebug
```

### "local.properties not found"
Create it with:
```
sdk.dir=/path/to/your/android/sdk
```

### Stuck with build cache issues
```bash
cd android
rm -rf .gradle
rm -rf ~/.gradle/caches/
./gradlew --refresh-dependencies
```

---

*Last updated: 2026-04-25*