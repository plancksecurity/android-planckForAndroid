package com.fsck.k9.mailstore.migrations;


import android.database.sqlite.SQLiteDatabase;


public class Migrations {
    @SuppressWarnings("fallthrough")
    public static void upgradeDatabase(SQLiteDatabase db, MigrationsHelper migrationsHelper) {
        switch (db.getVersion()) {
            case 55:
                MigrationTo56.renamepEpRatingColumn(db);
                MigrationTo56.cleanUpFtsTable(db);
            case 56:
                MigrationTo57.fixDataLocationForMultipartParts(db);
            case 57:
                MigrationTo58.cleanUpOrphanedData(db);
                MigrationTo58.createDeleteMessageTrigger(db);
            case 58:
                MigrationTo59.addMissingIndexes(db);
            case 59:
                MigrationTo60.migratePendingCommands(db);
            case 60:
                MigrationTo61.renameK9Tables(db);
            case 61:
                MigrationTo62.removeErrorsFolder(db);
            case 62:
                MigrationTo63.addAutoConsumeMessageColumn(db);
        }
    }
}
