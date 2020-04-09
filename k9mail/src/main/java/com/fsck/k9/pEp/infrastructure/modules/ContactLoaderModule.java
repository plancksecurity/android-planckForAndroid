package com.fsck.k9.pEp.infrastructure.modules;


import android.content.Context;

import com.fsck.k9.ui.contacts.ContactLetterBitmapConfig;
import com.fsck.k9.ui.contacts.ContactLetterBitmapCreator;
import com.fsck.k9.ui.contacts.ContactLetterExtractor;
import com.fsck.k9.ui.contacts.ContactPictureLoader;

import dagger.Module;
import dagger.Provides;

@Module
public class ContactLoaderModule {
    private final Context context;
    private final ContactLetterBitmapConfig contactLetterBitmapConfig;
    private final ContactLetterExtractor contactLetterExtractor;
    private final ContactLetterBitmapCreator contactLetterBitmapCreator;

    public ContactLoaderModule(Context context) {
        this.context = context;
        contactLetterExtractor = new ContactLetterExtractor();
        contactLetterBitmapConfig = new ContactLetterBitmapConfig(context);
        contactLetterBitmapCreator = new ContactLetterBitmapCreator(contactLetterExtractor, contactLetterBitmapConfig);
    }

    @Provides
    public ContactLetterExtractor provideContactLetterExtractor() {
        return contactLetterExtractor;
    }

    @Provides
    public ContactLetterBitmapConfig provideContactLetterBitmapConfig() {
        return contactLetterBitmapConfig;
    }

    @Provides
    public ContactLetterBitmapCreator provideContactLetterBitmapCreator() {
        return contactLetterBitmapCreator;
    }

    @Provides
    public ContactPictureLoader provideContactPictureLoader() {
        return new ContactPictureLoader(context, contactLetterBitmapCreator);
    }


}
