package com.fsck.k9.preferences;

import android.app.Application;
import android.content.res.Resources;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.pEp.ui.keys.FakeAndroidKeyStore;
import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SettingsExporterTest {

    @Test
    public void exportPreferences_producesXML() throws Exception {
        Document document = exportPreferences(false, Collections.emptySet());

        assertEquals("k9settings", document.getRootElement().getName());
    }

    @Test
    public void exportPreferences_setsVersionToLatest() throws Exception {
        Document document = exportPreferences(false, Collections.emptySet());

        assertEquals(Integer.toString(Settings.VERSION), document.getRootElement().getAttributeValue("version"));
    }

    @Test
    public void exportPreferences_setsFormatTo1() throws Exception {
        Document document = exportPreferences(false, Collections.emptySet());

        assertEquals("1", document.getRootElement().getAttributeValue("format"));
    }

    @Test
    public void exportPreferences_exportsGlobalSettingsWhenRequested() throws Exception {
        stubApplication();
        Document document = exportPreferences(true, Collections.emptySet());

        assertNotNull(document.getRootElement().getChild("global"));
    }

    private void stubApplication() {
        Application mockApp = Mockito.mock(Application.class);
        Mockito.doReturn(mockApp).when(mockApp).getApplicationContext();
        Mockito.doReturn("org.robolectric.default").when(mockApp).getPackageName();
        Resources resources = Mockito.mock(Resources.class);
        Mockito.doReturn(new String[]{"af","id","ms","bm","ca"}).when(resources).getStringArray(R.array.language_values);
        Mockito.doReturn(resources).when(mockApp).getResources();
        K9.app = mockApp;
    }

    @Test
    public void exportPreferences_ignoresGlobalSettingsWhenRequested() throws Exception {
        Document document = exportPreferences(false, Collections.emptySet());

        assertNull(document.getRootElement().getChild("global"));
    }

    private Document exportPreferences(boolean globalSettings, Set<String> accounts) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        SettingsExporter.exportPreferences(ApplicationProvider.getApplicationContext(), outputStream,
                globalSettings, accounts);
        Document document = parseXML(outputStream.toByteArray());
        outputStream.close();
        return document;
    }

    private Document parseXML(byte[] xml) throws Exception {
        SAXBuilder builder = new SAXBuilder();
        InputStream stream = new ByteArrayInputStream(xml);
        return builder.build(stream);
    }

    @BeforeClass
    public static void beforeClass() {
        FakeAndroidKeyStore.setup();
    }
}
