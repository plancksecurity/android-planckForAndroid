package com.fsck.k9.activity.compose;

import android.content.Context;
import android.net.Uri;
import android.provider.ContactsContract.Contacts;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fsck.k9.R;
import com.fsck.k9.mail.Address;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class Recipient implements Serializable {
    @Nullable // null means the address is not associated with a contact
    private final Long contactId;
    private final String contactLookupKey;

    @NonNull
    private Address address;

    private String addressLabel;

    @Nullable
    // null if the contact has no photo. transient because we serialize this manually, see below.
    private transient Uri photoThumbnailUri;

    @NonNull
    private RecipientSelectView.RecipientCryptoStatus cryptoStatus;

    public Recipient(@NonNull Address address) {
        this.address = address;
        this.contactId = null;
        this.cryptoStatus = RecipientSelectView.RecipientCryptoStatus.UNDEFINED;
        this.contactLookupKey = null;
    }

    public Recipient(String name, String email, String addressLabel, long contactId, String lookupKey) {
        this.address = new Address(email, name);
        this.contactId = contactId;
        this.addressLabel = addressLabel;
        this.cryptoStatus = RecipientSelectView.RecipientCryptoStatus.UNDEFINED;
        this.contactLookupKey = lookupKey;
    }

    @NonNull
    public Address getAddress() {
        return address;
    }

    public String getAddressLabel() {
        return addressLabel;
    }

    @Nullable
    public Uri getPhotoThumbnailUri() {
        return photoThumbnailUri;
    }

    public void setAddress(@NonNull Address address) {
        this.address = address;
    }

    void setAddressLabel(String addressLabel) {
        this.addressLabel = addressLabel;
    }

    void setPhotoThumbnailUri(@Nullable Uri photoThumbnailUri) {
        this.photoThumbnailUri = photoThumbnailUri;
    }

    String getDisplayNameOrAddress() {
        String displayName = getDisplayName();
        if (displayName != null) {
            return displayName;
        }

        return address.getAddress();
    }

    boolean isValidEmailAddress() {
        return (address.getAddress() != null);
    }

    String getDisplayNameOrUnknown(Context context) {
        String displayName = getDisplayName();
        if (displayName != null) {
            return displayName;
        }

        return context.getString(R.string.unknown_recipient);
    }

    public String getNameOrUnknown(Context context) {
        String name = address.getPersonal();
        if (name != null) {
            return name;
        }

        return context.getString(R.string.unknown_recipient);
    }

    private String getDisplayName() {
        if (TextUtils.isEmpty(address.getPersonal())) {
            return null;
        }

        String displayName = address.getPersonal();
        if (addressLabel != null) {
            displayName += " (" + addressLabel + ")";
        }

        return displayName;
    }

    @NonNull
    public RecipientSelectView.RecipientCryptoStatus getCryptoStatus() {
        return cryptoStatus;
    }

    public void setCryptoStatus(@NonNull RecipientSelectView.RecipientCryptoStatus cryptoStatus) {
        this.cryptoStatus = cryptoStatus;
    }

    @Nullable
    public Uri getContactLookupUri() {
        if (contactId == null) {
            return null;
        }

        return Contacts.getLookupUri(contactId, contactLookupKey);
    }

    @Override
    public boolean equals(Object o) {
        // Equality is entirely up to the address
        return o instanceof Recipient && address.equals(((Recipient) o).address);
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();

        // custom serialization, Android's Uri class is not serializable
        if (photoThumbnailUri != null) {
            oos.writeInt(1);
            oos.writeUTF(photoThumbnailUri.toString());
        } else {
            oos.writeInt(0);
        }
    }

    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ois.defaultReadObject();

        // custom deserialization, Android's Uri class is not serializable
        if (ois.readInt() != 0) {
            String uriString = ois.readUTF();
            photoThumbnailUri = Uri.parse(uriString);
        }
    }
}