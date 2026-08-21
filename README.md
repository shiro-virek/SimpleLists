# SimpleLists

Android app for managing tabbed lists: each tab is an independent list of items with a description, creation date, tags, and manual drag-to-reorder. Everything works **100% offline** with a local SQLite database.

## Features

- 🗂️ **Unlimited tabs** — add, rename, and delete tabs (long-press on a tab)
- 📝 **Items with a large title**, description, creation date, and tags
- ↔️ **Drag & drop reordering** within each list
- 🏷️ **User-defined tags** managed from Settings, with multi-tag filtering inside each list
- 💾 **Full backup**: export and import the database as a `.db` file through the system file picker (no storage permissions required)
- 🌗 Material You / Material 3 with dynamic color and dark mode support

## Tech stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose · Material 3 |
| Database | Room (SQLite) |
| Navigation | Navigation Compose |
| Drag & drop | [sh.calvin.reorderable](https://github.com/Calvin-LL/Reorderable) |

`minSdk` 26 (Android 8.0+) · `targetSdk`/`compileSdk` 36

## Project structure

```
app/src/main/java/com/simplelists/app/
├── MainActivity.kt            # Single activity + navigation
├── data/db/
│   ├── Entities.kt            # TabEntity, ItemEntity, TagEntity, ItemTagCrossRef
│   ├── Daos.kt                # DAOs + ItemWithTags relation
│   └── AppDatabase.kt         # Room + export/import logic
└── ui/
    ├── theme/Theme.kt
    ├── lists/                 # Main screen (tabs, list, item editor)
    └── settings/              # Settings (tags and backups)
```

### Data model

- `tabs` → main lists
- `items` → belong to a tab (cascade delete), keep an internal position field
- `tags` → defined globally, unique by name
- `item_tags` → N:M relation between items and tags

## Building

Requirements: JDK 17+ and the Android SDK (platform 36).

```bash
./gradlew assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

> This project uses the Gradle 9.5.1 wrapper (AGP 8.x is not compatible with Gradle ≥ 9.6).
> If your system JDK is too new, pin a compatible one in `~/.gradle/gradle.properties`:
>
> ```properties
> org.gradle.java.home=/path/to/jdk-21
> ```

## Installing on your phone

1. Copy `app-debug.apk` to the device (USB cable, Drive, etc.)
2. Open it and allow installation from unknown sources
3. Done — no internet connection or special permissions required

## Backups

From **Settings → Backup**:

- **Export** produces a `.db` file (a WAL checkpoint runs first, so the file is safe to archive or upload to the cloud)
- **Import** replaces the entire current database after confirmation; it validates that the file is a valid SQLite database before overwriting

## License

This project is licensed under the [MIT License](LICENSE).
