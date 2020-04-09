package com.fsck.k9.helper;

import com.fsck.k9.DI;
import com.fsck.k9.ui.contacts.ContactPictureLoader;

public class ContactPicture {

    public static ContactPictureLoader getContactPictureLoader() {
        return DI.get(ContactPictureLoader.class);
    }
}
