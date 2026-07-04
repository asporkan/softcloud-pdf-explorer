# AGENTS.md — PDF Explorer Technical Constitution

This document defines how AI agents (Cursor and other assistants) must work in this repository.
It is the long-term technical constitution for the project. When in doubt, follow this file before improvising.

**Last updated:** 2026-07-04  
**Primary reference plan:** `.cursor/plans/pdf_explorer_master_plan_33f27232.plan.md`

---

## Project Overview

### What this project is

Professional Android PDF application built with **Kotlin**, **Jetpack Compose**, and **Material 3**.
It replaces the existing Play Store app **`com.softcloud.pdfexplorer`** as an **in-place update** (same signing key, users keep the app installed).

### Current vs target state

| Property | Current (repo) | Target (Play Store) |
|----------|----------------|---------------------|
| `applicationId` | `com.rejowan.pdfreaderpro` | `com.softcloud.pdfexplorer` |
| `versionCode` | `7` | `4` (Play last = 3) |
| `versionName` | `2.2.0` | `0.4` |
| Product lineage | PdfReaderPro upstream fork | SoftCloud GPL-based fork |

**Do not change package name, namespace, branding, or remove the update system unless a dedicated task explicitly requests it.**

### Development direction (team decision — not legal advice)

The team intends **GPL-compatible open-source development** for this repository.
This is a **product direction**, not a substitute for legal license review before release.

> **verification required before release:** Final distribution model (Play Store, F-Droid, sideload), source-offer process, and combined-work license obligations must be reviewed and signed off before any public release.

### Core capabilities

- PDF viewing (PDF.js + WebView)
- 12 PDF tools (merge, split, compress, rotate, reorder, lock/unlock, watermark, page numbers, image↔PDF, remove pages)
- Recent, favorites, bookmarks
- Themes: Light, Dark, Black (AMOLED), System
- Offline-first; no ads, no tracking

### Toolchain (version catalog is source of truth)

- **Gradle:** 9.4.x · **AGP:** 9.1.0 · **Kotlin:** 2.3.20 · **Java:** 21
- **compileSdk / targetSdk:** 36 · **minSdk:** 24
- **Single module:** `:app`
- Dependencies: `gradle/libs.versions.toml` — never hardcode versions in `build.gradle.kts`

---

## License & Compliance

This section lists **only verified facts** from repository files, Gradle resolution, and Maven POM/JAR metadata.
Agents must **not** assume license compatibility, Play Store clearance, or compliance obligations beyond what is documented here.

### Verified — project license

| Source | Finding |
|--------|---------|
| [`LICENSE`](LICENSE) | Full text of **GNU General Public License, Version 3** |
| [`fdroid/com.rejowan.pdfreaderpro.yml`](fdroid/com.rejowan.pdfreaderpro.yml) | `License: GPL-3.0-only` |
| [`app/src/main/res/values/strings.xml`](app/src/main/res/values/strings.xml) | App strings reference GPL v3.0 |

### Verified — direct Gradle dependencies (from `gradle/libs.versions.toml` + Maven POM)

| Coordinate | Version | License in POM / artifact | Verification source |
|------------|---------|---------------------------|---------------------|
| `com.itextpdf:itext-core` | 9.6.0 | **GNU Affero General Public License v3** | `itext-core-9.6.0.pom`, `root-9.6.0.pom` |
| `com.itextpdf:bouncy-castle-adapter` | 9.6.0 | *(not repeated in child POM)* | Parent: `com.itextpdf:root:9.6.0` → AGPL v3 |
| `org.bouncycastle:bcprov-jdk18on` | 1.83 | **Bouncy Castle Licence** | `bcprov-jdk18on-1.83.pom` |
| `org.bouncycastle:bcpkix-jdk18on` | 1.83 | **Bouncy Castle Licence** | `bcpkix-jdk18on-1.83.pom` |
| AndroidX / Compose BOM and libraries | per catalog | **Apache License 2.0** | Respective POMs (e.g. `core-ktx`, `room-runtime`, `compose-bom`) |
| `io.insert-koin:koin-android` | 4.2.0 | **Apache License 2.0** | POM |
| `io.coil-kt:coil-compose` | 2.7.0 | **Apache License 2.0** | POM |
| `com.jakewharton.timber:timber` | 5.0.1 | **Apache License 2.0** | POM |
| `com.airbnb.android:lottie-compose` | 6.7.1 | **Apache-2.0** | POM |
| `sh.calvin.reorderable:reorderable` | 3.0.0 | **Apache License 2.0** | POM |
| `com.rejowan:licensy-compose` | 1.1.0 | **Apache License 2.0** | POM |
| `io.ktor:ktor-client-android` | 3.4.2 | **Apache License 2.0** | POM |
| `org.jetbrains.kotlinx:kotlinx-serialization-json` | 1.10.0 | **Apache-2.0** | POM |

### Verified — bundled assets (not Maven)

| Component | Version | License | Verification source |
|-----------|---------|---------|---------------------|
| **PDF.js** | 5.3.31 (`pdfjsVersion` in `app/src/main/assets/.../pdf.mjs`) | **Apache License 2.0** | License header in `pdf.mjs`, `pdf.worker.mjs`, viewer assets |

### Verified — iText runtime evidence

Gradle `releaseRuntimeClasspath` resolves `com.itextpdf:itext-core:9.6.0` and transitive modules at **9.6.0** (barcodes, kernel, layout, forms, sign, svg, etc.).

JAR inspection (`commons-9.6.0.jar`) contains:

- `com/itextpdf/commons/actions/processors/UnderAgplITextProductEventProcessor.class`
- `com/itextpdf/commons/actions/processors/UnderAgplProductProcessorFactory.class`

This confirms the resolved artifacts are **AGPL-labelled builds** per iText's own packaging — not an agent inference from README text.

### Verified — in-app license UI (may be inaccurate)

[`SettingsScreenContent.kt`](app/src/main/java/com/rejowan/pdfreaderpro/presentation/screens/settings/SettingsScreenContent.kt) `LicensesContent()`:

- iText → `Licenses.AGPL_3_0` *(matches Maven POM)*
- Bouncy Castle → `Licenses.MIT` *(**does not match** POM: "Bouncy Castle Licence")*

> **verification required before release:** Correct in-app license labels and complete third-party attribution list (including transitive dependencies such as Jackson, SLF4J, Apache Santuario xmlsec pulled in by iText).

### Verified — packaging concern

[`app/build.gradle.kts`](app/build.gradle.kts) `packaging.resources.excludes` removes:

- `/META-INF/LICENSE.md`
- `/META-INF/LICENSE-notice.md`

> **verification required before release:** Confirm release APK/AAB still satisfies all license notice requirements after packaging excludes.

### verification required before release — not yet verified in repo

The following **must not be assumed** by agents; each requires explicit review before Play Store or public distribution:

| Topic | Status |
|-------|--------|
| Legal compatibility of **GPL-3.0 (project)** with **AGPL-3.0 (iText)** as a combined work | **verification required before release** |
| Obligations for Play Store binary distribution (source offer, license texts, notice placement) | **verification required before release** |
| Full **transitive** dependency license audit on every release classpath | **verification required before release** |
| Whether current app meets **AGPL** attribution and source-access requirements | **verification required before release** |
| iText **commercial license** need (if team ever chooses non-copyleft distribution) | **verification required before release** — not documented in POM; POM lists AGPL v3 only |
| F-Droid / Play metadata license fields after rebrand to `com.softcloud.pdfexplorer` | **verification required before release** |
| [`README.md`](README.md) states "iText 7" while catalog pins **9.6.0** — documentation drift | Fix before release |

### Agent rules — dependencies & licenses

1. **Do not add** dependencies without checking POM/license metadata and recording the finding.
2. **Do not upgrade iText** without license re-verification on the new POM/JAR.
3. **Do not state** "license compatible" or "Play Store safe" in code, docs, or commits — use **verification required before release** until human review completes.
4. Prefer **`./gradlew :app:dependencies`** and POM inspection over README claims.
5. When adding a library, update the in-app licenses list if the library is user-visible or legally required to attribute.

---

## Architecture

### Pattern

**Clean Architecture + MVVM** with unidirectional data flow.

```
app/src/main/java/com/rejowan/pdfreaderpro/
├── appClasses/          # Application entry (MyApplication)
├── data/                # Room, DataStore, repository implementations
│   ├── local/database/  # Entities, DAOs, PdfDatabase, migrations
│   ├── local/           # PasswordStorage, etc.
│   └── repository/      # *RepositoryImpl
├── domain/              # Models, repository interfaces (no Android deps)
│   ├── model/
│   └── repository/
├── presentation/        # Compose UI, ViewModels, navigation, theme
│   ├── components/      # Reusable UI + pdf/ WebView engine
│   ├── navigation/      # Routes.kt, NavGraph.kt
│   ├── screens/         # Feature screens (home, reader, tools, settings, …)
│   └── theme/
├── di/                  # Koin modules
└── util/                # Cross-cutting helpers
```

### Layer rules

| Layer | May depend on | Must not depend on |
|-------|---------------|-------------------|
| `domain` | Kotlin stdlib only | Android, Compose, Room, Koin |
| `data` | `domain`, Android SDK, libraries | `presentation` |
| `presentation` | `domain`, Compose, Koin | Direct DAO access (prefer repositories) |
| `di` | All layers for wiring | Business logic |

### Known architectural exceptions (do not extend)

- `ReaderViewModel` accesses `BookmarkDao` directly — legacy; new code must use repositories
- `Tools` and `Settings` exist both as **NavHost routes** and **Home bottom tabs** — understand both paths before changing navigation
- `AnnotationDao` is registered but unused in production — feature incomplete

### PDF subsystems

1. **Viewer:** `presentation/components/pdf/` + `pdfcompose/` — PDF.js in WebView via `WebViewAssetLoader`
2. **Tools:** `data/repository/PdfToolsRepositoryImpl.kt` — iText (`com.itextpdf:itext-core:9.6.0` per catalog)
3. **Thumbnails:** `util/PdfThumbnailManager.kt` — Android `PdfRenderer` + disk/memory cache

### Dependency injection

Five Koin modules loaded in `MyApplication` (order matters):

1. `databaseModule` → 2. `dataStoreModule` → 3. `networkModule` → 4. `repositoryModule` → 5. `viewModelModule`

---

## Coding Standards

### General principles

1. **Minimize scope** — smallest correct change; no drive-by refactors
2. **Match existing conventions** — read surrounding code before writing
3. **No over-engineering** — no abstractions for one-off use
4. **Self-explanatory code** — comments only for non-obvious business or platform constraints
5. **Version catalog** — all new dependencies go through `gradle/libs.versions.toml`

### File and package naming

- Kotlin files: `PascalCase.kt` matching primary type
- Packages: lowercase, feature-oriented under `com.rejowan.pdfreaderpro`
- Compose screens: `*Screen.kt` + `*ViewModel.kt` in same feature folder
- Tests mirror production package under `app/src/test/` and `app/src/androidTest/`

### Error handling

- Use `Result<T>` in repositories for expected failures
- Map errors to user-facing messages via `util/ErrorUtils.kt`
- Log with **Timber** in debug only (`BuildConfig.ENABLE_LOGGING`); never `Log.d` in new code
- Catch `OutOfMemoryError` around large PDF/bitmap operations

### Resources

- User-facing strings in `res/values/strings.xml` — no hardcoded UI text
- Dimensions/colors via Material theme — avoid magic numbers in composables when a token exists

---

## Kotlin Kuralları

- **100% Kotlin** for new code; do not add Java unless integrating an unavoidable SDK
- Target **JVM 21** (`jvmTarget = JVM_21`)
- Prefer **immutable** `data class` models in `domain/model/`
- Use **`suspend`** for IO; **`Flow`** for observable state; **`StateFlow`** in ViewModels
- Prefer **`val`** over `var`; avoid nullable types unless necessary
- Use **`require()` / `check()`** for programmer errors; **`Result.failure()`** for user-recoverable errors
- No blocking calls on Main thread — `Dispatchers.IO` or `viewModelScope`
- Serialization: `@Serializable` for navigation routes only (`presentation/navigation/Routes.kt`)
- Suppress warnings only with a one-line justification comment
- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)

---

## Jetpack Compose Kuralları

- **100% Compose** for UI — no new XML layouts except manifest-linked resources (themes, drawables, xml configs)
- State hoisting: state down, events up
- ViewModels expose **`StateFlow`**; collect in UI with `collectAsState()` or `collectAsStateWithLifecycle()`
- Use **`remember`** for UI-local ephemeral state; **`rememberSaveable`** for config-surviving UI state
- Prefer **`LazyColumn` / `LazyVerticalGrid`** for lists; never scrollable `Column` for large PDF lists
- Extract reusable UI to `presentation/components/` when used in 2+ screens
- Pass **`NavController`** only when navigation is required; prefer callback lambdas for single-action events
- Use **`Modifier.testTag()`** for UI tests when adding test coverage
- Preview functions (`@Preview`) for isolated components when practical
- Do not call repositories or DAOs from composables — always go through ViewModel

### Compose + Koin

- ViewModels: `koinViewModel()` in composables
- Parameterized ViewModels (e.g. `ReaderViewModel`): use Koin `parametersOf()` as in `NavGraph.kt`

---

## Material 3 Kuralları

- Use **`MaterialTheme.colorScheme`** and **`MaterialTheme.typography`** — never hardcode colors that should follow theme
- App theme entry: `PdfReaderProTheme` in `presentation/theme/Theme.kt`
- Supported app themes: **Light, Dark, Black (AMOLED), System**
- Reader has separate reading themes (Light, Sepia, Dark, Black) via `ReadingTheme` in preferences
- **`dynamicColor`** infrastructure exists but defaults to `false` — enable via settings only when implementing that feature
- Prefer **`Material3` components** (`Button`, `ModalBottomSheet`, `TopAppBar`, `FilterChip`, etc.)
- Use **`Surface`**, **`Card`**, **`FilledTonalButton`** consistently with existing screens
- Respect existing corner radii and spacing patterns in `SettingsScreenContent` and `HomeScreen`

---

## MVVM Kuralları

### ViewModel

- One ViewModel per screen (or shared parent for tightly coupled tabs)
- Extend **`androidx.lifecycle.ViewModel`**
- Inject dependencies via **constructor injection (Koin)**
- Expose UI state as **`StateFlow`** (prefer) or **`Flow`**
- One-off events (snackbar, navigation): **`SharedFlow`** or callback to composable — avoid `StateFlow` for events
- Use **`viewModelScope`** for coroutines; cancel automatically on clear
- No Android `Context` in ViewModel except where already established (tool ViewModels) — prefer application context if needed

### View (Composable)

- Screen composable receives ViewModel (or state + callbacks)
- **No business logic** in composables — formatting/display logic only
- Collect flows in composable scope; use `LaunchedEffect` for one-shot actions

### Model

- Domain models in `domain/model/` — pure Kotlin
- UI-specific state wrappers in presentation layer (e.g. `ReaderState.kt`)

---

## Koin Kuralları

### Module placement

| Module | File | Scope |
|--------|------|-------|
| Database + DAOs | `di/DatabaseModule.kt` | `single` |
| DataStore | `di/DataStoreModule.kt` | `single` |
| HttpClient / Json | `di/NetworkModule.kt` | `single` |
| Repositories | `di/RepositoryModule.kt` | `single` |
| ViewModels | `di/ViewModelModule.kt` | `viewModel` / `viewModelOf` |

### Rules

- Register new repositories in **`repositoryModule`**
- Register new ViewModels in **`viewModelModule`**
- Use **`single { }`** for stateless services and repositories
- Use **`viewModelOf(::FooViewModel)`** when constructor matches Koin resolution
- Do not use **`GlobalContext.get()`** — inject dependencies
- Utilities (`PdfThumbnailManager`, `PasswordStorage`) are **not** Koin-managed today — inject `Context` or wrap in repository before expanding Koin surface
- New modules require explicit approval — prefer extending existing five modules

---

## Room Kuralları

### Database

- **Name:** `pdf_reader_db` · **Current version:** `7`
- Definition: `data/local/database/PdfDatabase.kt`
- Schema exports: `app/schemas/` (required — KSP arg `room.schemaLocation`)

### Entities (current)

| Table | Entity |
|-------|--------|
| `recent` | `RecentEntity` |
| `favorites` | `FavoriteEntity` |
| `bookmarks` | `BookmarkEntity` |
| `annotations` | `AnnotationEntity` |

### Migrations

- **Every schema change** requires:
  1. Increment `@Database(version = …)`
  2. New `Migration` object in `PdfDatabase.companion`
  3. Add to `PdfDatabase.migrations` array
  4. Exported JSON schema committed under `app/schemas/`
- **Never** use `fallbackToDestructiveMigration()` for upgrades (downgrade destructive is configured — do not rely on it for users)
- Test migrations with `MigrationTestHelper` / `room-testing`
- Legacy migration reference: `MIGRATION_4_5` maps old `recent_table` / `favorite` — for same-app lineage only

### DAO access

- DAOs injected via Koin in `databaseModule`
- Production code should access data through **repositories**, not DAOs directly (except known legacy paths)

---

## Navigation Kuralları

### Type-safe routes

All routes defined as `@Serializable` types in **`presentation/navigation/Routes.kt`**.

Examples: `Home`, `Reader(path, page, fromIntent)`, `ToolMerge(selectedFiles)`, etc.

### NavHost

- Graph: **`presentation/navigation/NavGraph.kt`**
- Register new destinations with **`composable<YourRoute> { … }`**
- Access route args: **`navController.toRoute<YourRoute>()`** or typed lambda parameter

### Dual navigation pattern

- **Bottom tabs** inside `HomeScreen`: Home, Folders, Tools, Settings
- **NavHost routes** for deep navigation: Reader, Search, FolderDetail, Tool screens, Settings (standalone)
- Before adding a screen, decide: **tab-embedded** vs **NavHost route** — do not duplicate without reason

### Navigation helpers

Extension functions on `NavController` at bottom of `NavGraph.kt` (`navigateToReader`, `navigateToHome`, tool navigators, etc.) — use them for consistency.

### Incomplete routes (known debt)

- `composable<Tools>` and `composable<ToolResult>` are stubs — Tools works via Home tab embedding

---

## Performance Kuralları

### PDF viewing

- PDF.js renders in WebView — avoid loading entire file into memory for viewing (use existing `WebViewAssetLoader` path handlers)
- **Do not** load full PDF as Base64/`ByteArray` for viewing — known OOM path in `WebInterface.handleBase64Data()`
- Large PDFs (>500 pages, >100 MB): expect slower scroll; use page scrubber; document limits in `KNOWN_ISSUES.md`

### File scanning

- `PdfFileRepositoryImpl.refreshPdfs()` queries MediaStore — avoid calling `getPageCount()` for every file on every refresh (known bottleneck)
- Thumbnails: disk cache at `cache/pdf_thumbnails/` — regenerate only when stale

### PDF tools

- iText operations run on **`Dispatchers.IO`**
- Pre-flight storage checks via `ErrorUtils.hasEnoughStorage()` before merge/compress
- Tool UI previews: limit bitmap count in ViewModel state (Rotate ≤50, Reorder/RemovePages ≤100 pages)

### Compose

- Use **`derivedStateOf`** for expensive derived UI state
- Stable parameters (`@Stable`, `@Immutable`) for list item models where appropriate
- Avoid recomposition storms — pass lambdas stably, use `remember` for callbacks

### Build

- Release: R8 minify + shrink resources enabled — update **`proguard-rules.pro`** when adding reflection/serialization classes

---

## Memory Kuralları

### WebView (critical)

When modifying PDF viewer lifecycle, ensure:

- `webView.destroy()` on release
- `removeJavascriptInterface("JWI")` before destroy
- `loadUrl("about:blank")` before destroy
- Remove WebView from parent before destroy

Current gap: `pdfcompose/PdfViewer.kt` `onRelease` does not fully cleanup — fix when touching viewer code.

### Bitmaps

- `PdfThumbnailManager` LRU: count-based (50) — prefer recycling; do not hold large bitmap lists in ViewModel longer than needed
- Always **`bitmap.recycle()`** when bypassing cache (if not using Coil/cache manager)
- Close **`PdfRenderer`**, **`PdfRenderer.Page`**, **`ParcelFileDescriptor`** in `finally` blocks

### Streams

- Always **`.use { }`** on `InputStream` / `OutputStream` (see `FileOperations.kt` for correct pattern)
- Tool ViewModels' `copyUriToCache()` has known input-stream leak pattern — fix when editing those files

### Static caches

- Do not add new static bitmap caches without memory-size bounds
- `PdfThumbnailManager` is an `object` — treat as process-lifetime cache

---

## Accessibility

- Every interactive icon must have **`contentDescription`** (or `null` only if purely decorative with adjacent text)
- Known gap: some icons lack descriptions — fix when touching those components
- Touch targets ≥ **48dp**
- Reader controls must be reachable with TalkBack — label zoom, scroll mode, bookmark actions
- Color contrast: use Material roles (`onSurface`, `onBackground`) — do not rely on color alone for state
- Test with TalkBack when changing reader chrome or bottom navigation

---

## Security

### WebView / PDF.js

- Keep **`allowFileAccess = false`**, **`allowContentAccess = false`**
- Serve PDF bytes only via **`WebViewAssetLoader`** path handlers — never direct `file://` URLs
- **`@JavascriptInterface`** (`WebInterface.kt`): sanitize any string interpolated into JS (injection risk on `fileName`, URLs)
- Do not re-enable commented-out external URL handling without review
- Require WebView **Chromium ≥110** (see `WebViewSupport.kt`)

### Secrets

- PDF passwords: **`PasswordStorage`** — AES-GCM + Android Keystore; keys `pwd_{path.hashCode()}`
- Never log passwords, keystore aliases, or file paths containing user PII
- No secrets in git — use `keystore.properties` (gitignored) or CI env vars

### Storage

- **`allowBackup="false"`** — intentional; document if changing
- **`MANAGE_EXTERNAL_STORAGE`** currently required for full file browser — Play policy risk; migration to SAF/MediaStore planned (Phase 3)
- Validate paths from intents via **`FileOperations.resolveUriToPath()`**

### Network

- Update system (GitHub APK download) is **scheduled for removal** — do not expand network attack surface until removed
- When `INTERNET` remains, use HTTPS only (Ktor client)

### Dependencies

- Do not upgrade iText without re-checking POM/JAR license metadata
- Do not add dependencies without documenting license source in PR
- **verification required before release:** dependency allow-list and license compatibility sign-off

---

## Play Store Migration Rules

These are **planned** changes — execute only when the task explicitly authorizes the phase.

### Phase gates

| Gate | Action | Preconditions |
|------|--------|---------------|
| Package rename | `com.softcloud.pdfexplorer` | Migration spec ready, signing verified |
| Version | `versionCode=4`, `versionName=0.4` | With first Play release build |
| Legacy data | `LegacyDataImporter` | Old app DB/SP schema documented by team |
| Update removal | Remove GitHub update + `REQUEST_INSTALL_PACKAGES` | Before Play submission |
| Rebrand | App name, icon, strings, upstream refs | Coordinated with package rename |
| Storage refactor | Remove `MANAGE_EXTERNAL_STORAGE` | SAF/MediaStore implementation complete |
| **License review** | Compliance sign-off | See **License & Compliance** section |

### Update compatibility

- Same signing key + same `applicationId` = in-place update preserves `/data/data/<package>/`
- Old app (`com.softcloud.pdfexplorer`) is a **different codebase** — Room/DataStore schemas will **not** match automatically
- Implement explicit **`LegacyDataDetector`** on first launch after update
- Room migrations (`MIGRATION_4_5` etc.) apply to **this app's** DB lineage only — not old SoftCloud schema

### Play submission checklist (agent reference)

- [ ] License & Compliance section fully reviewed — no open **verification required before release** items
- [ ] In-app license texts match verified POM/asset licenses (incl. Bouncy Castle correction)
- [ ] Source availability process documented for recipients
- [ ] No sideload/update permission (`REQUEST_INSTALL_PACKAGES` removed)
- [ ] Privacy policy covers: files on device, optional network, password storage
- [ ] `versionCode` monotonically increases from Play baseline (3 → 4)

> **verification required before release:** Play Console policy compliance (permissions, data safety, storage access justification).

---

## Git Workflow

### Repository

This GitHub repository is the **single source of truth** for development.

### What not to commit

- `keystore.properties`, `*.keystore`, `*.jks`
- `local.properties`
- `.idea/` user-specific files (unless team agrees)
- Build outputs (`app/build/`, `.gradle/`)
- Secrets, API keys, Play Console credentials

### What to commit

- Source, resources, assets (including Room schemas, PDF.js bundles when updated)
- `gradle/libs.versions.toml` changes with dependency updates
- `app/schemas/**/*.json` on Room migration
- Documentation updates (`AGENTS.md`, `CHANGELOG.md`, `KNOWN_ISSUES.md`)

---

## Branch Strategy

### Main branches

| Branch | Purpose |
|--------|---------|
| `main` | Production-ready; Play release candidates |
| `develop` | Integration branch (use if team adopts Git Flow) |

If only `main` exists today, feature branches merge to `main` via PR.

### Feature branches

```
feature/<short-description>     # New features
fix/<short-description>         # Bug fixes
refactor/<short-description>    # Internal restructuring
migration/<short-description>   # Play/data migration work
docs/<short-description>        # Documentation only
```

Examples:

- `feature/legacy-data-importer`
- `fix/webview-memory-leak`
- `migration/softcloud-package-rename`

### Rules

- Never force-push to `main`
- Keep branches short-lived (< 2 weeks ideal)
- Rebase or merge from `main` before opening PR

---

## Commit Rules

### Format (Conventional Commits)

```
<type>(<scope>): <subject>

[optional body]

[optional footer]
```

**Types:** `feat`, `fix`, `refactor`, `perf`, `test`, `docs`, `build`, `ci`, `chore`

**Scopes:** `reader`, `tools`, `home`, `settings`, `navigation`, `data`, `di`, `theme`, `migration`, `release`

### Examples

```
feat(reader): add snap-to-page for horizontal scroll
fix(tools): close input stream in merge URI copy
refactor(data): expose bookmarks via repository
docs(agents): add Play Store migration gates
migration(package): rename applicationId to com.softcloud.pdfexplorer
```

### Rules

- One logical change per commit when possible
- Subject line ≤ 72 characters, imperative mood
- Reference issue numbers in footer: `Closes #123`
- Do not commit broken builds — run `./gradlew test` at minimum
- **Never** commit unless the user explicitly requests a commit

---

## Pull Request Rules

### Before opening

- [ ] Branch up to date with target branch
- [ ] `./gradlew test` passes
- [ ] `./gradlew assembleRelease` passes for release-critical changes
- [ ] No unrelated file changes
- [ ] Room schema exported if DB changed
- [ ] `CHANGELOG.md` updated for user-visible changes
- [ ] New dependencies: POM license noted in PR description

### PR title

Same format as commit subject: `feat(scope): description`

### PR description template

```markdown
## Summary
- Bullet points of what changed and why

## Test plan
- [ ] Unit tests pass
- [ ] Manual test steps
- [ ] Large PDF / memory scenario (if touching viewer/tools)

## Screenshots
(if UI changed)

## Migration / Play impact
(if applicable)

## License impact
(if dependencies changed — POM reference)
```

### Review criteria

- Architecture layer boundaries respected
- License metadata documented for new dependencies
- Performance/memory sections considered for PDF/WebView changes
- Accessibility: content descriptions for new icons

---

## Testing Rules

### Structure

| Type | Location | Runner |
|------|----------|--------|
| Unit tests | `app/src/test/` | JUnit 4, MockK, Turbine, Coroutines Test |
| Instrumented | `app/src/androidTest/` | AndroidJUnit4, Room testing |

### Requirements

- **New repository logic** → unit test required
- **New ViewModel business rules** → unit test with MockK
- **Room migration** → instrumented migration test
- **Bug fix** → regression test when feasible

### Commands

```bash
./gradlew test                    # Unit tests
./gradlew connectedAndroidTest    # Instrumented (device/emulator)
./gradlew :app:testDebugUnitTest  # Single variant
```

### What to test

- Repository mapping and error paths
- ViewModel state transitions
- Migration SQL (up/down grade)
- PDF tool validation (`InputValidation`, password-protected merge rejection)

### What not to test (low value)

- Pure Compose layout without logic
- Generated Room/KSP code
- Third-party library internals

---

## Refactoring Rules

### When refactoring is allowed

- Dedicated `refactor/*` branch or explicit task
- Must preserve behavior unless task says otherwise
- Must include tests for touched critical paths

### Refactoring priorities (from technical debt)

1. WebView lifecycle cleanup (`pdfcompose/PdfViewer.kt`)
2. Repository boundary (DAO access from ViewModels)
3. `NavGraph` stub routes (`Tools`, `ToolResult`)
4. Stream leak fixes in tool ViewModels
5. `SettingsScreenContent.kt` / `PdfViewer.kt` god-file splits — only with dedicated task

### Refactoring prohibitions

- No package/namespace rename without migration task
- No dependency major-version bumps bundled with feature work
- No "cleanup" commits mixed into feature PRs
- No removal of update system until Phase 1 migration task

---

## Build Rules

### Local build

```bash
./gradlew assembleDebug          # Debug APK
./gradlew assembleRelease        # Release (requires signing config)
./gradlew test                   # Unit tests
```

### Signing

- Release signing: `keystore.properties` (see `keystore.properties.template`)
- Env fallbacks: `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`
- Play release uses **existing Play signing key** — coordinate with team before changing signing config

### Version management

- **`versionCode` / `versionName`:** `app/build.gradle.kts` — Play target: 4 / 0.4
- **Library versions:** `gradle/libs.versions.toml` only

### ProGuard / R8

- Rules: `app/proguard-rules.pro`
- Keep rules exist for: WebInterface, Room, iText, Kotlin Serialization
- When adding `@Serializable` routes or JS interfaces, verify release build

### Known packaging note

`app/build.gradle.kts` excludes some `META-INF/LICENSE*` files — see **License & Compliance** → **verification required before release**

---

## Release Rules

### Versioning (Play Store)

- **`versionCode`:** monotonic integer; next Play release = **4**
- **`versionName`:** semantic string; next = **0.4**
- Never decrease `versionCode`

### Release branch process

1. Complete Phase checklist in Master Plan
2. Complete all **verification required before release** items in **License & Compliance**
3. Update `CHANGELOG.md` (Keep a Changelog format)
4. Update `KNOWN_ISSUES.md` if new limitations discovered
5. Run full test suite + manual smoke on physical device
6. Build signed release AAB/APK
7. Tag: `v0.4` (match versionName)
8. Upload to Play Console internal/closed track first

### Distribution obligations

> **verification required before release:** Exact obligations depend on final license review. Verified inputs:
>
> - Project [`LICENSE`](LICENSE) = GPL-3.0
> - iText POM = AGPL-3.0
> - Team direction = open-source development
>
> Before release, confirm: source access method, in-app notices, store listing license statements, and third-party attribution completeness.

### Post-release

- Merge release branch back to `main`
- Open follow-up issues for deferred items
- Update Master Plan phase status

---

## AI Decision Rules

Agents must follow these decision rules in every session:

1. **When uncertain, ask the user** — do not guess on architecture, scope, licenses, or migration strategy.
2. **Do not perform large refactors** — keep changes focused; use a dedicated `refactor/*` task if a broad rewrite is needed.
3. **If you will change more than 5 files, present a plan first** — wait for user approval before implementing.
4. **Get approval before adding new dependencies** — include POM/license metadata in the proposal.
5. **If the build breaks, find the root cause first** — do not randomly edit unrelated files to “fix” the build.
6. **Do not commit** — create commits only when the user explicitly requests it.

---

## Agent Quick Reference

### Always do

- Read this file and the Master Plan before large changes
- Use version catalog for dependencies
- Respect layer boundaries (domain → data → presentation)
- Run tests before declaring work complete
- Document POM/license source when adding dependencies
- Mark unresolved legal/compliance topics as **verification required before release**

### Never do (unless explicitly tasked)

- Change `applicationId` / namespace / branding
- Remove GitHub update system or Play permissions
- Assert "license compatible" or "Play Store approved" without human sign-off
- Force-push `main`
- Commit without user request
- Expand scope with drive-by refactors

### Key files

| Topic | File |
|-------|------|
| Master plan | `.cursor/plans/pdf_explorer_master_plan_33f27232.plan.md` |
| Dependencies | `gradle/libs.versions.toml` |
| App config | `app/build.gradle.kts` |
| Manifest | `app/src/main/AndroidManifest.xml` |
| Database | `app/.../data/local/database/PdfDatabase.kt` |
| Navigation | `app/.../presentation/navigation/Routes.kt`, `NavGraph.kt` |
| Koin | `app/.../di/*.kt` |
| PDF viewer | `app/.../presentation/components/pdf/PdfViewer.kt` |
| PDF tools | `app/.../data/repository/PdfToolsRepositoryImpl.kt` |
| Theme | `app/.../presentation/theme/Theme.kt` |
| Project license | `LICENSE` |
| In-app licenses UI | `app/.../settings/SettingsScreenContent.kt` |

---

*This document supersedes informal agent instructions for this repository. Update it when architecture, verified license facts, or Play migration phases change.*
