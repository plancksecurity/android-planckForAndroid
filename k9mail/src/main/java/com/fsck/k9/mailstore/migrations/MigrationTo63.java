package com.fsck.k9.mailstore.migrations;


import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;


class MigrationTo63 {
    static void addAutoConsumeMessageColumn(SQLiteDatabase db) {
        try {
            db.execSQL("ALTER TABLE messages ADD auto_consume INTEGER default 0");
        } catch (SQLiteException e) {
            if (!e.toString().startsWith("duplicate column name: auto_consume")) {
                throw e;
            }
        }
    }
}
