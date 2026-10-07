# Backup and restore — OnlyTechTake

This describes the current source behavior. Full provides ZIP backup/restore and order XLSX export; Demo restricts these operations. Review this document before describing backup capabilities in a store listing.

## ZIP contents and protection

| Entry | Contents | Current protection |
|---|---|---|
| `metadata/backup.json` | Timestamp, database name and schema version | No ZIP encryption |
| `config/configuration.json` | Categories, message templates and app settings | No ZIP encryption |
| `database/` | SQLCipher database and matching journal/WAL files | SQLCipher at rest; optional ZIP password encryption |
| `files/app_media/` | Photos, videos, signatures and documents | No ZIP encryption |
| `files/datastore/` | Local preference files | No ZIP encryption |

`BackupRepository.exportCompleteBackup` applies the configured password only to database entries. The ZIP is not fully password protected, even when backup encryption is enabled. Private app storage does not encrypt media or preferences. Treat exported archives as sensitive and do not rely on the ZIP password to protect all contents. Full archive protection remains pending in the tracker.

The admin chooses an export destination through Android's document picker. A selected provider may store or synchronize the file in the cloud. The app records backup success/failure locally; this does not establish cloud retention or deletion guarantees.

## Restore behavior and limits

1. Extract supported archive entries into an app-cache staging directory and validate metadata/schema version.
2. Validate the SQLCipher database using the current installation's key.
3. Close the database and snapshot its files and the media directory.
4. Replace database, media and DataStore files; clear authentication/session state and attempt to record the restore event.
5. On failure during replacement, attempt database/media rollback; clean temporary snapshots and staging files.

This is not a guaranteed atomic restore: DataStore preferences are not included in rollback, and snapshots are deleted in finalization even if recovery fails. Do not use restore as a migration guarantee or the only copy of important data. Recovery failure coverage and preference rollback remain pending.

The SQLCipher key is protected by the installation's Android Keystore and stored separately from archive contents. Matching app/schema versions alone do not enable restore on a new device or after loss of the original key. The current key manager replaces stored key material after a decryption failure; preservation and explicit recovery handling remain pending.

## Other exports

Order XLSX export contains tabular order/customer data without media or preferences and cannot restore the application. The current writer is called without a password. Standalone PDF and configuration JSON exports also have no app-level password encryption. Copies exported or shared outside the app are not removed by a local reset.

## Schema changes

`RepairAppDatabase.DATABASE_VERSION` is checked during restore; the current path requires the matching schema version. Any supported format or preference-layout change must update validation and restoration together, with recovery verification before migration claims.
