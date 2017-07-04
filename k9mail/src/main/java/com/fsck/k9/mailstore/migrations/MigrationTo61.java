package com.fsck.k9.mailstore.migrations;


import android.database.sqlite.SQLiteDatabase;


class MigrationTo61 {
    static void renameK9Tables(SQLiteDatabase db) {
        addMessageCompositeIndex(db);
    }

    private static void addMessageCompositeIndex(SQLiteDatabase db) {
        db.execSQL("UPDATE folders SET name = replace(name, 'K9mail-errors', 'pEp-errors' ) WHERE name LIKE 'K9mail-errors';\n");
        db.execSQL("UPDATE folders SET name = replace(name, 'K9MAIL_INTERNAL_OUTBOX', 'PEP_INTERNAL_OUTBOX' ) WHERE name LIKE 'K9MAIL_INTERNAL_OUTBOX';\n");
    }
}
