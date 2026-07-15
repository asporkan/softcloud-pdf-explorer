# Release Report — PDF Explorer v0.6 (Production)

**Date:** 2026-07-15  
**Status:** **APPROVED FOR GOOGLE PLAY SUBMISSION**  
**Artifact:** Signed Android App Bundle (AAB)

---

## Build identity

| Field | Value |
|-------|--------|
| **versionCode** | `6` |
| **versionName** | `0.6` |
| **applicationId** | `com.softcloud.pdfexplorer` |
| **namespace** | `com.rejowan.pdfreaderpro` |
| **minSdk** | 24 |
| **targetSdk / compileSdk** | 36 |

---

## Artifact

| Field | Value |
|-------|--------|
| **File** | `app/build/outputs/bundle/release/app-release.aab` |
| **Full path** | `C:\Workspace\Android\pdf\pdf_explorer\app\build\outputs\bundle\release\app-release.aab` |
| **Size** | **21.87 MB** (22,931,936 bytes) |
| **Built** | 2026-07-15 15:14 (local) |
| **Command** | `./gradlew :app:bundleRelease` |
| **Source changes for this build** | None (release build only) |

---

## Signing

| Field | Value |
|-------|--------|
| **Signing status** | **Signed** (release signing config) |
| **Keystore** | Production `pdf_explorer.jks` |
| **Key alias** | `key0` |
| **Certificate CN** | `CN=pdf explorer` |
| **Certificate validity** | 2024-02-02 → 2049-01-26 |
| **jarsigner** | Verified — entries signed by production cert |

### Certificate fingerprints

| Algorithm | Fingerprint |
|-----------|-------------|
| **SHA-1** | `43:45:1D:B0:13:04:78:E4:7E:F8:C2:B4:A2:FE:B5:C8:17:0D:79:C0` |
| **SHA-256** | `70:82:F2:D3:A1:30:26:4B:19:11:54:CA:5C:C4:94:8E:30:36:F7:A0:DB:CA:8C:A6:6A:9D:59:E8:82:9A:6B:04` |

Use these fingerprints in Play Console / App signing verification as needed.

---

## Release notes (0.6) — summary

- In-app language selector (System / English / Turkish)
- Complete Save As / Overwrite via SAF (`CreateDocument` / `OpenDocumentTree`)
- Split / PDF→Images multi-file folder export fixed
- Password-protected PDF handling in editing tools
- Compress level UI without misleading size estimates
- Folder pull-to-refresh and List/Grid persistence
- Home Turkish tab layout + horizontal scroll
- Success Done / Open white-screen fix
- Large-PDF Reorder / Remove Pages fixes

Full history: `CHANGELOG.md` → `[0.6]`.

---

## Play Console checklist

- [x] Signed release AAB produced
- [x] `versionCode` 6 > previous Play baseline
- [x] `applicationId` = `com.softcloud.pdfexplorer`
- [ ] Upload AAB to Play Console (Internal / Closed / Production track as decided)
- [ ] Confirm Play App Signing shows matching upload key fingerprints if applicable
- [ ] Complete store listing / Data safety / content rating as required

---

## Notes

- No application source was modified for this production build.
- Upload path for Play:  
  `app/build/outputs/bundle/release/app-release.aab`
