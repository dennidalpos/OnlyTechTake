---
name: persistence-room-sqlcipher
description: Secure data persistence using Room and SQLCipher for OnlyTechTake.
---

# Secure Persistence (Room + SQLCipher)

OnlyTechTake uses an encrypted local database.

## Technical Requirements
1. **Encryption**: The database MUST be encrypted using **SQLCipher** (`net.zetetic:sqlcipher-android`).
2. **Room**: Use Room as the abstraction layer.
3. **Schema Management**: `DATABASE_VERSION` is locked. Do NOT modify without explicit orders.
4. **No Cloud**: Data is local-first. Avoid any cloud sync unless specifically requested.

## Implementation Patterns
- **Database Module**: The database is provided via Hilt.
- **Safe Initialization**: Ensure `SupportFactory` is used with the encryption key during Room database building.
- **Migrations**: Always provide a `Migration` object if the schema changes.

## References
- [Room Persistence Library](https://developer.android.com/training/data-storage/room)
- [SQLCipher for Android](https://www.zetetic.net/sqlcipher/sqlcipher-for-android/)
