package com.fsck.k9.pEp.ui.models;


import org.pEp.jniadapter.Identity;
import org.pEp.jniadapter.Rating;

public class PEpIdentity extends Identity {

    private Rating rating;

    public Rating getRating() {
        return rating;
    }

    public void setRating(Rating rating) {
        this.rating = rating;
    }
}
