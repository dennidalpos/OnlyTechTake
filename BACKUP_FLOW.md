# Backup and Restore Flow - OnlyTechTake

This document describes the structure and flow of the full backup and restore system as of July 2026.

## 1. Full Backup (Export ZIP)

The full backup generates an encrypted ZIP file (using Zip4j) containing all application data and settings.

### File Structure inside ZIP:
- `metadata/backup.json`: Contains backup metadata (timestamp, device info, app version, database version).
- `config/configuration.json`: Contains exported app configurations (categories, message templates, app settings).
- `database/`: Contains the SQLite database files (`repair_app.db` and its journals/wal).
- `files/app_media/`: Contains all media files (photos, signatures, documents).
- `files/datastore/`: Contains all Jetpack DataStore preference files (auth, security, owner, backup reminder).

### Process:
1. User triggers "Run Full Backup".
2. Optional password is retrieved from `SecurityPreferences`.
3. ZIP is created via SAF (`CreateDocument`).
4. Files are walked and added to the ZIP with the specified structure.
5. Success/Failure is recorded in `BackupReminderPreferences`.

## 2. Restore Flow

Restoration is an atomic operation that replaces the current app state with the one from the backup.

### Process:
1. User selects a ZIP file via SAF (`OpenDocument`).
2. ZIP is extracted into a staging directory in `cacheDir`.
3. **Validation**:
    - Metadata is checked for compatibility (database version must match).
    - Database file is validated.
4. **Snapshot for Rollback**:
    - Current database files and media directory are moved/copied to a temporary backup area in `cacheDir`.
5. **Replacement**:
    - Current database instance is closed.
    - Database files are replaced with the ones from the backup.
    - `files/app_media` and `files/datastore` are replaced.
6. **Finalization**:
    - `AuthPreferences` and `SessionStore` are cleared to force a clean state.
    - Database is reopened and an Audit Log entry is created.
    - If anything fails, a rollback is performed from the snapshots.

## 3. Order Export (XLSX)

This is a partial export of order data only, meant for external processing (Excel/Sheets).
- It does NOT include media or preferences.
- It is NOT used for restoration.

## 4. Technical Notes for Future Schema Changes

If the database schema or preference structure changes:
- Update `RepairAppDatabase.DATABASE_VERSION`.
- The restore flow checks for this version in `metadata/backup.json`.
- Migration logic might be needed if cross-version restore is supported (currently it requires an exact match).
- If preference file names change in `DataStore`, update `BackupArchive.restoreAppFiles`.
