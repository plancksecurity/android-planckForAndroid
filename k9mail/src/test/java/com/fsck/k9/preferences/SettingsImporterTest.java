package com.fsck.k9.preferences;


import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.fsck.k9.*;
import com.fsck.k9.mail.AuthType;

import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.PEpUtils;
import com.fsck.k9.pEp.ui.keys.FakeAndroidKeyStore;
import org.apache.tools.ant.filters.StringInputStream;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.anyInt;


@RunWith(AndroidJUnit4.class)
@Config(manifest = Config.NONE)
public class SettingsImporterTest {


    @Before
    public void before() {
        deletePreExistingAccounts();
    }

    private void deletePreExistingAccounts() {
        Preferences preferences = Preferences.getPreferences(ApplicationProvider.getApplicationContext());
        for (Account account : preferences.getAccounts()) {
            preferences.deleteAccount(account);
        }
    }

    @Test(expected = SettingsImportExportException.class)
    public void importSettings_throwsExceptionOnBlankFile() throws SettingsImportExportException {
        InputStream inputStream = new StringInputStream("");
        List<String> accountUuids = new ArrayList<>();

        SettingsImporter.importSettings(ApplicationProvider.getApplicationContext(), inputStream, true, accountUuids, true);
    }

    @Test(expected = SettingsImportExportException.class)
    public void importSettings_throwsExceptionOnMissingFormat() throws SettingsImportExportException {
        InputStream inputStream = new StringInputStream("<k9settings version=\"1\"></k9settings>");
        List<String> accountUuids = new ArrayList<>();

        SettingsImporter.importSettings(ApplicationProvider.getApplicationContext(), inputStream, true, accountUuids, true);
    }

    @Test(expected = SettingsImportExportException.class)
    public void importSettings_throwsExceptionOnInvalidFormat() throws SettingsImportExportException {
        InputStream inputStream = new StringInputStream("<k9settings version=\"1\" format=\"A\"></k9settings>");
        List<String> accountUuids = new ArrayList<>();

        SettingsImporter.importSettings(ApplicationProvider.getApplicationContext(), inputStream, true, accountUuids, true);
    }

    @Test(expected = SettingsImportExportException.class)
    public void importSettings_throwsExceptionOnNonPositiveFormat() throws SettingsImportExportException {
        InputStream inputStream = new StringInputStream("<k9settings version=\"1\" format=\"0\"></k9settings>");
        List<String> accountUuids = new ArrayList<>();

        SettingsImporter.importSettings(ApplicationProvider.getApplicationContext(), inputStream, true, accountUuids, true);
    }

    @Test(expected = SettingsImportExportException.class)
    public void importSettings_throwsExceptionOnMissingVersion() throws SettingsImportExportException {
        InputStream inputStream = new StringInputStream("<k9settings format=\"1\"></k9settings>");
        List<String> accountUuids = new ArrayList<>();

        SettingsImporter.importSettings(ApplicationProvider.getApplicationContext(), inputStream, true, accountUuids, true);
    }

    @Test(expected = SettingsImportExportException.class)
    public void importSettings_throwsExceptionOnInvalidVersion() throws SettingsImportExportException {
        InputStream inputStream = new StringInputStream("<k9settings format=\"1\" version=\"A\"></k9settings>");
        List<String> accountUuids = new ArrayList<>();

        SettingsImporter.importSettings(ApplicationProvider.getApplicationContext(), inputStream, true, accountUuids, true);
    }

    @Test(expected = SettingsImportExportException.class)
    public void importSettings_throwsExceptionOnNonPositiveVersion() throws SettingsImportExportException {
        InputStream inputStream = new StringInputStream("<k9settings format=\"1\" version=\"0\"></k9settings>");
        List<String> accountUuids = new ArrayList<>();

        SettingsImporter.importSettings(ApplicationProvider.getApplicationContext(), inputStream, true, accountUuids, true);
    }

    @Test
    public void parseSettings_account() throws SettingsImportExportException {
        String validUUID = UUID.randomUUID().toString();
        InputStream inputStream = new StringInputStream("<k9settings format=\"1\" version=\"1\">" +
                "<accounts><account uuid=\"" + validUUID + "\"><name>Account</name></account></accounts></k9settings>");
        List<String> accountUuids = new ArrayList<>();
        accountUuids.add("1");

        SettingsImporter.Imported results = SettingsImporter.parseSettings(inputStream, true, accountUuids, true);

        assertEquals(1, results.accounts.size());
        assertEquals("Account", results.accounts.get(validUUID).name);
        assertEquals(validUUID, results.accounts.get(validUUID).uuid);
    }

    @Test
    public void parseSettings_account_identities() throws SettingsImportExportException {
        String validUUID = UUID.randomUUID().toString();
        InputStream inputStream = new StringInputStream("<k9settings format=\"1\" version=\"1\">" +
                "<accounts><account uuid=\"" + validUUID + "\"><name>Account</name>" +
                "<identities><identity><email>user@gmail.com</email></identity></identities>" +
                "</account></accounts></k9settings>");
        List<String> accountUuids = new ArrayList<>();
        accountUuids.add("1");

        SettingsImporter.Imported results = SettingsImporter.parseSettings(inputStream, true, accountUuids, true);

        assertEquals(1, results.accounts.size());
        assertEquals(validUUID, results.accounts.get(validUUID).uuid);
        assertEquals(1, results.accounts.get(validUUID).identities.size());
        assertEquals("user@gmail.com", results.accounts.get(validUUID).identities.get(0).email);
    }


    @Test
    public void parseSettings_account_cram_md5() throws SettingsImportExportException {
        String validUUID = UUID.randomUUID().toString();
        InputStream inputStream = new StringInputStream("<k9settings format=\"1\" version=\"1\">" +
                "<accounts><account uuid=\"" + validUUID + "\"><name>Account</name>" +
                "<incoming-server><authentication-type>CRAM_MD5</authentication-type></incoming-server>" +
                "</account></accounts></k9settings>");
        List<String> accountUuids = new ArrayList<>();
        accountUuids.add(validUUID);

        SettingsImporter.Imported results = SettingsImporter.parseSettings(inputStream, true, accountUuids, false);

        assertEquals("Account", results.accounts.get(validUUID).name);
        assertEquals(validUUID, results.accounts.get(validUUID).uuid);
        assertEquals(AuthType.CRAM_MD5, results.accounts.get(validUUID).incoming.authenticationType);
    }

    @Test
    public void importSettings_disablesAccountsNeedingPasswords() throws SettingsImportExportException {
        Mockito.mockStatic(PEpUtils.class);
        stubApplication();
        String validUUID = UUID.randomUUID().toString();
        InputStream inputStream = new StringInputStream("<k9settings format=\"1\" version=\"1\">" +
                "<accounts><account uuid=\"" + validUUID + "\"><name>Account</name>" +
                "<incoming-server type=\"IMAP\">" +
                "<connection-security>SSL_TLS_REQUIRED</connection-security>" +
                "<username>user@gmail.com</username>" +
                "<authentication-type>CRAM_MD5</authentication-type>" +
                "<host>googlemail.com</host>" +
                "</incoming-server>" +
                "<outgoing-server type=\"SMTP\">" +
                "<connection-security>SSL_TLS_REQUIRED</connection-security>" +
                "<username>user@googlemail.com</username>" +
                "<authentication-type>CRAM_MD5</authentication-type>" +
                "<host>googlemail.com</host>" +
                "</outgoing-server>" +
                "<settings><value key=\"a\">b</value></settings>" +
                "<identities><identity><email>user@gmail.com</email></identity></identities>" +
                "</account></accounts></k9settings>");
        List<String> accountUuids = new ArrayList<>();
        accountUuids.add(validUUID);

        SettingsImporter.ImportResults results = SettingsImporter.importSettings(
                ApplicationProvider.getApplicationContext(), inputStream, true, accountUuids, false);

        assertEquals(0, results.erroneousAccounts.size());
        assertEquals(1, results.importedAccounts.size());
        assertEquals("Account", results.importedAccounts.get(0).imported.name);
        assertEquals(validUUID, results.importedAccounts.get(0).imported.uuid);

        assertFalse(Preferences.getPreferences(ApplicationProvider.getApplicationContext())
                .getAccount(validUUID).isEnabled());
    }

    private void stubApplication() {

        Application mockApp = Mockito.mock(Application.class);
        Mockito.doReturn(mockApp).when(mockApp).getApplicationContext();
        Mockito.doReturn("org.robolectric.default").when(mockApp).getPackageName();
        Resources resources = Mockito.mock(Resources.class);
        Mockito.doReturn(resources).when(mockApp).getResources();
        K9.app = mockApp;
        Mockito.doReturn(new String[]{"-1", "1",
        "5", "10", "15", "30", "60", "120", "180", "360", "720", "1440"}).when(resources)
                .getStringArray(R.array.check_frequency_values);
        Mockito.doReturn(new String[]{
                "10", "25", "50", "100", "250", "500", "1000", "2500",
                "5000", "10000", "0"
        }).when(resources).getStringArray(R.array.display_count_values);
        Mockito.doReturn(new String[]{
                "EXPUNGE_IMMEDIATELY", "EXPUNGE_ON_POLL", "EXPUNGE_MANUALLY"
        }).when(resources).getStringArray(R.array.expunge_policy_values);
        Mockito.doReturn(new String[]{
                "24", "36", "48", "60"
        }).when(resources).getStringArray(R.array.idle_refresh_period_values);
        Mockito.doReturn(new String[]{
                "24", "36", "48", "60"
        }).when(resources).getStringArray(R.array.autodownload_message_size_values);
        Mockito.doReturn(new String[]{
                "-1", "0", "1", "2", "7", "14", "21", "28", "56", "84", "168", "365"
        }).when(resources).getStringArray(R.array.message_age_values);
        Mockito.doReturn(new String[]{
                "0", "1", "2", "3", "4", "5"
        }).when(resources).getStringArray(R.array.vibrate_pattern_values);
        Mockito.doReturn(new String[]{
                "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10"
        }).when(resources).getStringArray(R.array.vibrate_times_label);
        Mockito.doReturn(new String[]{
                "10", "25", "50", "100", "250", "500", "1000", "0"
        }).when(resources).getStringArray(R.array.remote_search_num_results_values);
    }

    @Test
    public void getImportStreamContents_account() throws SettingsImportExportException {
        String validUUID = UUID.randomUUID().toString();
        InputStream inputStream = new StringInputStream("<k9settings format=\"1\" version=\"1\">" +
                "<accounts>" +
                "<account uuid=\"" + validUUID + "\">" +
                "<name>Account</name>" +
                "<identities>" +
                "<identity>" +
                "<email>user@gmail.com</email>" +
                "</identity>" +
                "</identities>" +
                "</account>" +
                "</accounts></k9settings>");

        SettingsImporter.ImportContents results = SettingsImporter.getImportStreamContents(inputStream);

        assertFalse(results.globalSettings);
        assertEquals(1, results.accounts.size());
        assertEquals("Account", results.accounts.get(0).name);
        assertEquals(validUUID, results.accounts.get(0).uuid);
    }

    @Test
    public void getImportStreamContents_alternativeName() throws SettingsImportExportException {
        String validUUID = UUID.randomUUID().toString();
        InputStream inputStream = new StringInputStream("<k9settings format=\"1\" version=\"1\">" +
                "<accounts>" +
                "<account uuid=\"" + validUUID + "\">" +
                "<name></name>" +
                "<identities>" +
                "<identity>" +
                "<email>user@gmail.com</email>" +
                "</identity>" +
                "</identities>" +
                "</account>" +
                "</accounts></k9settings>");

        SettingsImporter.ImportContents results = SettingsImporter.getImportStreamContents(inputStream);

        assertFalse(results.globalSettings);
        assertEquals(1, results.accounts.size());
        assertEquals("user@gmail.com", results.accounts.get(0).name);
        assertEquals(validUUID, results.accounts.get(0).uuid);
    }

    @BeforeClass
    public static void beforeClass() {
        FakeAndroidKeyStore.setup();
    }
}
