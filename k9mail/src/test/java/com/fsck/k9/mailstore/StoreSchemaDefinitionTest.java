package com.fsck.k9.mailstore;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import androidx.test.core.app.ApplicationProvider;

import com.fsck.k9.Account;
import com.fsck.k9.BuildConfig;
import com.fsck.k9.GlobalsHelper;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.RobolectricTest;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.MessagingException;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.shadows.ShadowLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class StoreSchemaDefinitionTest extends RobolectricTest {
    private StoreSchemaDefinition storeSchemaDefinition;


    @Before
    public void setUp() throws MessagingException {
        ShadowLog.stream = System.out;

        Application application = ApplicationProvider.getApplicationContext();
        K9.app = application;
        GlobalsHelper.setContext(application);
        StorageManager.getInstance(application);

        storeSchemaDefinition = createStoreSchemaDefinition();
    }

    @Test
    public void getVersion_shouldReturnCurrentDatabaseVersion() {
        int version = storeSchemaDefinition.getVersion();

        assertEquals(LocalStore.DB_VERSION, version);
    }

    @Test
    public void doDbUpgrade_withEmptyDatabase_shouldSetsDatabaseVersion() {
        SQLiteDatabase database = SQLiteDatabase.create(null);

        storeSchemaDefinition.doDbUpgrade(database);

        assertEquals(LocalStore.DB_VERSION, database.getVersion());
    }

    @Test
    public void doDbUpgrade_withBadDatabase_shouldThrowInDebugBuild() {
        if (BuildConfig.DEBUG) {
            SQLiteDatabase database = SQLiteDatabase.create(null);
            database.setVersion(55);

            try {
                storeSchemaDefinition.doDbUpgrade(database);
                fail("Expected Error");
            } catch (Error e) {
                assertEquals("Exception while upgrading database", e.getMessage());
            }
        }
    }

    @Test
    public void doDbUpgrade_withV29_shouldUpgradeDatabaseToLatestVersion() {
        SQLiteDatabase database = createV55Database();

        storeSchemaDefinition.doDbUpgrade(database);

        assertEquals(LocalStore.DB_VERSION, database.getVersion());
    }

    @Test
    public void doDbUpgrade_withV29() {
        SQLiteDatabase database = createV55Database();
        insertMessageWithSubject(database, "Test Email");

        storeSchemaDefinition.doDbUpgrade(database);

        assertMessageWithSubjectExists(database, "Test Email");
    }

    @Test
    public void doDbUpgrade_fromV29_shouldResultInSameTables() {
        SQLiteDatabase newDatabase = createNewDatabase();
        SQLiteDatabase upgradedDatabase = createV55Database();

        storeSchemaDefinition.doDbUpgrade(upgradedDatabase);

        assertDatabaseTablesEquals(newDatabase, upgradedDatabase);
    }

    @Test
    public void doDbUpgrade_fromV29_shouldResultInSameTriggers() {
        SQLiteDatabase newDatabase = createNewDatabase();
        SQLiteDatabase upgradedDatabase = createV55Database();

        storeSchemaDefinition.doDbUpgrade(upgradedDatabase);

        assertDatabaseTriggersEquals(newDatabase, upgradedDatabase);
    }

    @Test
    public void doDbUpgrade_fromV29_shouldResultInSameIndexes() {
        SQLiteDatabase newDatabase = createNewDatabase();
        SQLiteDatabase upgradedDatabase = createV55Database();

        storeSchemaDefinition.doDbUpgrade(upgradedDatabase);

        assertDatabaseIndexesEquals(newDatabase, upgradedDatabase);
    }

    @Test
    public void justCreateDatabase() {
        SQLiteDatabase database = createV55Database();

        storeSchemaDefinition.doDbUpgrade(database);

        assertEquals(LocalStore.DB_VERSION, database.getVersion());
    }

    private SQLiteDatabase createV55Database() {
        SQLiteDatabase database = SQLiteDatabase.create(null);
        initV55Database(database);
        return database;
    }

    private void initV55Database(SQLiteDatabase db) {
        db.beginTransaction();

        db.execSQL("CREATE TABLE folders (" +
                "id INTEGER PRIMARY KEY," +
                "name TEXT, " +
                "last_updated INTEGER, " +
                "unread_count INTEGER, " +
                "visible_limit INTEGER, " +
                "status TEXT, " +
                "push_state TEXT, " +
                "last_pushed INTEGER, " +
                "flagged_count INTEGER default 0, " +
                "integrate INTEGER, " +
                "top_group INTEGER, " +
                "poll_class TEXT, " +
                "push_class TEXT, " +
                "display_class TEXT, " +
                "notify_class TEXT default '"+ Folder.FolderClass.INHERITED.name() + "', " +
                "more_messages TEXT default \"unknown\"" +
                ")");

        db.execSQL("CREATE INDEX IF NOT EXISTS folder_name ON folders (name)");

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
                "pep_color TEXT" +
                ")");

        db.execSQL("CREATE TABLE message_parts (" +
                "id INTEGER PRIMARY KEY, " +
                "type INTEGER NOT NULL, " +
                "root INTEGER, " +
                "parent INTEGER NOT NULL, " +
                "seq INTEGER NOT NULL, " +
                "mime_type TEXT, " +
                "decoded_body_size INTEGER, " +
                "display_name TEXT, " +
                "header TEXT, " +
                "encoding TEXT, " +
                "charset TEXT, " +
                "data_location INTEGER NOT NULL, " +
                "data BLOB, " +
                "preamble TEXT, " +
                "epilogue TEXT, " +
                "boundary TEXT, " +
                "content_id TEXT, " +
                "server_extra TEXT" +
                ")");

        db.execSQL("CREATE TRIGGER set_message_part_root " +
                "AFTER INSERT ON message_parts " +
                "BEGIN " +
                "UPDATE message_parts SET root=id WHERE root IS NULL AND ROWID = NEW.ROWID; " +
                "END");

        db.execSQL("CREATE TABLE threads (" +
                "id INTEGER PRIMARY KEY, " +
                "message_id INTEGER, " +
                "root INTEGER, " +
                "parent INTEGER" +
                ")");

        db.execSQL("CREATE INDEX threads_message_id ON threads (message_id)");

        db.execSQL("CREATE INDEX threads_root ON threads (root)");

        db.execSQL("CREATE INDEX threads_parent ON threads (parent)");

        db.execSQL("CREATE TRIGGER set_thread_root " +
                "AFTER INSERT ON threads " +
                "BEGIN " +
                "UPDATE threads SET root=id WHERE root IS NULL AND ROWID = NEW.ROWID; " +
                "END");

        db.execSQL("CREATE TABLE pending_commands (" +
                "id INTEGER PRIMARY KEY, " +
                "command TEXT, " +
                "arguments TEXT" +
                ")");

        db.execSQL("CREATE TRIGGER delete_folder BEFORE DELETE ON folders BEGIN DELETE FROM messages WHERE old.id = folder_id; END;");

        db.execSQL("CREATE VIRTUAL TABLE messages_fulltext USING fts4 (fulltext)");

        db.setVersion(55);

        db.setTransactionSuccessful();
        db.endTransaction();
    }

    private void assertMessageWithSubjectExists(SQLiteDatabase database, String subject) {
        Cursor cursor = database.query("messages", new String[]{"subject"}, null, null, null, null, null);
        try {
            assertTrue(cursor.moveToFirst());
            assertEquals(subject, cursor.getString(0));
        } finally {
            cursor.close();
        }
    }

    private void assertDatabaseTablesEquals(SQLiteDatabase expected, SQLiteDatabase actual) {
        List<String> tablesInNewDatabase = tablesInDatabase(expected);
        Collections.sort(tablesInNewDatabase);

        List<String> tablesInUpgradedDatabase = tablesInDatabase(actual);
        Collections.sort(tablesInUpgradedDatabase);

        assertEquals(tablesInNewDatabase, tablesInUpgradedDatabase);
    }

    private void assertDatabaseTriggersEquals(SQLiteDatabase expected, SQLiteDatabase actual) {
        List<String> triggersInNewDatabase = triggersInDatabase(expected);
        Collections.sort(triggersInNewDatabase);

        List<String> triggersInUpgradedDatabase = triggersInDatabase(actual);
        Collections.sort(triggersInUpgradedDatabase);

        assertEquals(triggersInNewDatabase, triggersInUpgradedDatabase);
    }

    private void assertDatabaseIndexesEquals(SQLiteDatabase expected, SQLiteDatabase actual) {
        List<String> indexesInNewDatabase = indexesInDatabase(expected);
        Collections.sort(indexesInNewDatabase);

        List<String> indexesInUpgradedDatabase = indexesInDatabase(actual);
        Collections.sort(indexesInUpgradedDatabase);

        assertEquals(indexesInNewDatabase, indexesInUpgradedDatabase);
    }

    private List<String> tablesInDatabase(SQLiteDatabase db) {
        return objectsInDatabase(db, "table");
    }

    private List<String> triggersInDatabase(SQLiteDatabase db) {
        return objectsInDatabase(db, "trigger");
    }

    private List<String> indexesInDatabase(SQLiteDatabase db) {
        return objectsInDatabase(db, "index");
    }

    private List<String> objectsInDatabase(SQLiteDatabase db, String type) {
        List<String> databaseObjects = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT sql FROM sqlite_master WHERE type = ? AND sql IS NOT NULL",
                new String[]{type});
        try {
            while (cursor.moveToNext()) {
                String sql = cursor.getString(cursor.getColumnIndex("sql"));
                String resortedSql = "table".equals(type) ? sortTableColumns(sql) : sql;
                databaseObjects.add(resortedSql);
            }
        } finally {
            cursor.close();
        }

        return databaseObjects;
    }

    private String sortTableColumns(String sql) {
        int positionOfColumnDefinitions = sql.indexOf('(');
        String columnDefinitionsSql = sql.substring(positionOfColumnDefinitions + 1, sql.length() - 1);
        String[] columnDefinitions = columnDefinitionsSql.split(" *, *(?![^(]*\\))");
        Arrays.sort(columnDefinitions);

        String sqlPrefix = sql.substring(0, positionOfColumnDefinitions + 1);
        String sortedColumnDefinitionsSql = TextUtils.join(", ", columnDefinitions);
        return sqlPrefix + sortedColumnDefinitionsSql + ")";
    }

    private void insertMessageWithSubject(SQLiteDatabase database, String subject) {
        ContentValues data = new ContentValues();
        data.put("subject", subject);
        long rowId = database.insert("messages", null, data);
        assertNotEquals(-1, rowId);
    }

    private StoreSchemaDefinition createStoreSchemaDefinition() throws MessagingException {
        Context context = createContext();
        Account account = createAccount();
        LockableDatabase lockableDatabase = createLockableDatabase();

        LocalStore localStore = mock(LocalStore.class);
        localStore.database = lockableDatabase;
        when(localStore.getContext()).thenReturn(context);
        when(localStore.getAccount()).thenReturn(account);

        return new StoreSchemaDefinition(localStore);
    }

    private Context createContext() {
        Context context = mock(Context.class);
        when(context.getString(R.string.special_mailbox_name_outbox)).thenReturn("Outbox");
        return context;
    }

    private LockableDatabase createLockableDatabase() throws MessagingException {
        LockableDatabase lockableDatabase = mock(LockableDatabase.class);
        when(lockableDatabase.execute(anyBoolean(), any(LockableDatabase.DbCallback.class))).thenReturn(false);
        return lockableDatabase;
    }

    private Account createAccount() {
        Account account = mock(Account.class);
        when(account.getInboxFolderName()).thenReturn("Inbox");
        when(account.getLocalStorageProviderId()).thenReturn(StorageManager.InternalStorageProvider.ID);
        return account;
    }

    private SQLiteDatabase createNewDatabase() {
        SQLiteDatabase database = SQLiteDatabase.create(null);
        storeSchemaDefinition.doDbUpgrade(database);
        return database;
    }
}
