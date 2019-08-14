package com.fsck.k9.mailstore.migrations;


import android.database.sqlite.SQLiteDatabase;


class MigrationTo62 {
    public static void removeErrorsFolder(SQLiteDatabase db) {
        db.execSQL("DELETE FROM folders WHERE name = 'pEp-errors'");
    }
}
