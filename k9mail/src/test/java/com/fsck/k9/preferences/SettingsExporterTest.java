package com.fsck.k9.preferences;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
        Document document = exportPreferences(true, Collections.emptySet());

        assertNotNull(document.getRootElement().getChild("global"));
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
}
