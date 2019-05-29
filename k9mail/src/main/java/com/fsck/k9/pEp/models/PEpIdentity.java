package com.fsck.k9.pEp.models;


import foundation.pEp.jniadapter.Identity;
import foundation.pEp.jniadapter.Rating;

public class PEpIdentity extends Identity {

    private Rating rating;

    public Rating getRating() {
        return rating;
    }

    public void setRating(Rating rating) {
        this.rating = rating;
    }
}
