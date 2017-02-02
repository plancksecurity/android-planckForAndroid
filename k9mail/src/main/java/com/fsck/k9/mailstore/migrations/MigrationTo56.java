package com.fsck.k9.mailstore.migrations;


import android.database.sqlite.SQLiteDatabase;


class MigrationTo56 {
    public static void renamepEpRatingColumn(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE messages RENAME TO messages_orig;\n");

        db.execSQL("CREATE TABLE messages (" +
                "id INTEGER PRIMARY KEY, " +
                "deleted INTEGER default 0, " +
                "folder_id INTEGER, " +
                "uid TEXT, " +
                "subject TEXT, " +
                "date INTEGER, " +
                "flags TEXT, " +
                "sender_list TEXT, " +
                "to_list TEXT, " +
                "cc_list TEXT, " +
                "bcc_list TEXT, " +
                "reply_to_list TEXT, " +
                "attachment_count INTEGER, " +
                "internal_date INTEGER, " +
                "message_id TEXT, " +
                "preview_type TEXT default \"none\", " +
                "preview TEXT, " +
                "mime_type TEXT, "+
                "normalized_subject_hash INTEGER, " +
                "empty INTEGER default 0, " +
                "read INTEGER default 0, " +
                "flagged INTEGER default 0, " +
                "answered INTEGER default 0, " +
                "forwarded INTEGER default 0, " +
                "message_part_id INTEGER, " +
                "pep_rating TEXT" +
                ")");
        db.execSQL("INSERT INTO messages(" +
                                            "id, " +
                                            "deleted, " +
                                            "folder_id, " +
                                            "uid, " +
                                            "subject, " +
                                            "date, " +
                                            "flags, " +
                                            "sender_list, " +
                                            "to_list, " +
                                            "cc_list, " +
                                            "bcc_list, " +
                                            "reply_to_list, " +
                                            "attachment_count, " +
                                            "internal_date, " +
                                            "message_id, " +
                                            "preview_type, " +
                                            "preview, " +
                                            "mime_type, "+
                                            "normalized_subject_hash, " +
                                            "empty, " +
                                            "read, " +
                                            "flagged, " +
                                            "answered, " +
                                            "forwarded, " +
                                            "message_part_id, " +
                                            "pep_rating" + ") " +
                            "SELECT "+ "id, " +
                                "deleted, " +
                                "folder_id, " +
                                "uid, " +
                                "subject, " +
                                "date, " +
                                "flags, " +
                                "sender_list, " +
                                "to_list, " +
                                "cc_list, " +
                                "bcc_list, " +
                                "reply_to_list, " +
                                "attachment_count, " +
                                "internal_date, " +
                                "message_id, " +
                                "preview_type, " +
                                "preview, " +
                                "mime_type, "+
                                "normalized_subject_hash, " +
                                "empty, " +
                                "read, " +
                                "flagged, " +
                                "answered, " +
                                "forwarded, " +
                                "message_part_id, " +
                                "pep_color "+
                            "FROM messages_orig;");


        db.execSQL("DROP TABLE messages_orig;");
    }

    static void cleanUpFtsTable(SQLiteDatabase db) {
        db.execSQL("DELETE FROM messages_fulltext WHERE docid NOT IN (SELECT id FROM messages WHERE deleted = 0)");
    }
    
    public static void migratePendingCommands(SQLiteDatabase db) {
        // TODO actually migrate
        db.execSQL("DROP TABLE IF EXISTS pending_commands");
        db.execSQL("CREATE TABLE pending_commands " +
                "(id INTEGER PRIMARY KEY, command TEXT, data TEXT)");
    }
}
