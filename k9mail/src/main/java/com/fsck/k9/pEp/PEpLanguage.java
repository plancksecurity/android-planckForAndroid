package com.fsck.k9.pEp;

public class PEpLanguage {
    private String locale;
    private String language;
    private String title;

    public PEpLanguage(String locale, String language, String title) {
        this.locale = locale;
        this.language = language;
        this.title = title;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String toString() {
        return "PEpLanguage{" +
                "locale='" + locale + '\'' +
                ", language='" + language + '\'' +
                ", title='" + title + '\'' +
                '}';
    }
}
