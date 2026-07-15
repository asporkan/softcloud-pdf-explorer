# Release Readiness Report — PDF Explorer RC6 (UX Consistency Pass)

**Date:** 2026-07-15  
**Version:** `versionCode` 6 / `versionName` 0.6  
**AAB:** **NOT GENERATED — awaiting approval**

---

## Verdict

### Conditional GO for RC6 device re-test · NO-GO for signed AAB

| Gate | Status |
|------|--------|
| Issue 1 Home tabs (original look + scroll) | **FIXED** |
| Issue 2 Folder view mode persistence | **FIXED** |
| Issue 3 Locked PDF messaging | **FIXED** |
| Issue 4 Incorrect password localization | **FIXED** |
| Issue 5 Password-protected tool audit | **FIXED** |
| `assembleDebug` | **PASS** |
| `assembleRelease` | **PASS** (unsigned without keystore) |
| Signed AAB | **Blocked** — awaiting approval + keystore |

---

## Issue 1 — Home tabs layout

Restored original `CompactTab` / `ViewModeIcon` padding, typography, spacing, and selection indicator.

**Only change:** tabs row is `weight(1f)` + `horizontalScroll` so long Turkish labels scroll; List/Grid toggle remains fixed on the right.

---

## Issue 2 — Folder view mode persistence

`FolderDetailViewModel.setViewMode` now calls `preferencesRepository.setDefaultViewMode(mode)` — same preference as Home. Reopening a folder restores the last List/Grid choice.

---

## Issues 3 & 5 — Password-protected PDFs

Shared gate: `passwordProtectedBlockMessage()` via `isPasswordProtected`.

| Tool | Behavior |
|------|----------|
| Reorder, Remove, Rotate, Compress, Split, Watermark, Page Numbers, Lock, PDF→Images | Detect encryption → dialog: unlock first → stay on picker (not silent empty) |
| Merge | Skips protected files; empty after skip → same unlock-first message |
| Unlock | Accepts protected PDFs (unchanged) |
| Image → PDF | N/A (images) |

UI: `ToolLoadErrorDialog` when load/blocked error and no source selected.

---

## Issue 4 — Incorrect password localization

| Key | EN | TR |
|-----|----|----|
| `incorrect_password` | Incorrect password. | Yanlış parola. |
| `incorrect_password_error` | Incorrect password. | Yanlış parola. |
| `error_wrong_password` | Incorrect password. | Yanlış parola. |
| `wrong_password` | Incorrect password. Please try again. | Yanlış parola. Lütfen tekrar deneyin. |

`PdfToolsRepositoryImpl.unlock` failures use `context.getString(R.string.incorrect_password)`.

---

## Build

| Command | Result |
|---------|--------|
| `./gradlew :app:assembleDebug` | **SUCCESS** |
| `./gradlew :app:assembleRelease` | **SUCCESS** (unsigned APK if no keystore) |
| `bundleRelease` | **Not run** |

---

## Manual re-test checklist

- [ ] Home Turkish tabs: original height/font; scroll if needed; List/Grid never overlaps
- [ ] Folder: set Grid → leave → reopen → still Grid
- [ ] Locked PDF in Rotate/Reorder/etc.: dialog “password protected / unlock first”, not silent Select PDF
- [ ] Unlock tool wrong password → “Yanlış parola.” in TR
- [ ] Merge with all protected files → unlock-first dialog

---

## AAB

**AAB NOT GENERATED — awaiting approval.**
