package com.fsck.k9.preferences;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import androidx.test.core.app.ApplicationProvider;

import com.fsck.k9.Account;
import com.fsck.k9.Preferences;
import com.fsck.k9.RobolectricTest;
import com.fsck.k9.mail.AuthType;
import com.fsck.k9.planck.PlanckUtils;
import com.fsck.k9.planck.ui.keys.FakeAndroidKeyStore;

import org.apache.tools.ant.filters.StringInputStream;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Ignore("Settings import not used nor maintained on enterprise variant")
public class SettingsImporterTest extends RobolectricTest {


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
        MockedStatic<PlanckUtils> pEpUtilsMock = Mockito.mockStatic(PlanckUtils.class);
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
        pEpUtilsMock.close();
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
