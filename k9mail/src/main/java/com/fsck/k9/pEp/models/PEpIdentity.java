package com.fsck.k9.pEp.models;


import android.os.Parcel;
import android.os.Parcelable;

import org.pEp.jniadapter.Identity;
import org.pEp.jniadapter.Rating;

public class PEpIdentity extends Identity implements Parcelable {

    private Rating rating;

    public Rating getRating() {
        return rating;
    }

    public void setRating(Rating rating) {
        this.rating = rating;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(address);
        parcel.writeString(fpr);
        parcel.writeString(user_id);
        parcel.writeString(username);
        parcel.writeString(lang);
        parcel.writeInt(flags);
        parcel.writeInt(rating.value);
    }
}
