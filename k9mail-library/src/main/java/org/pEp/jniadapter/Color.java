package org.pEp.jniadapter;

/**
 * Created by dietz on 07.07.15.
 */
public enum Color {
    pEpRatingUndefined (0),
    pEpRatingCannotDecrypt (1), // x
    pEpRatingHaveNoKey (2),
    pEpRatingUnencrypted (3), // x
    pEpRatingUnreliable (4), // x
    pEpRatingReliable (5), // x
    pEpRatingTrusted (6), // x
    pEpRatingTrustedAndAnonymized (7), // x
    pEpRatingFullyAnonymous (8),
    pEpRatingUnderAttack (-1), // x
    pEpRatingB0rken (-2);

    public final int value;

    Color(int value) {
        this.value = value;
        this.name();
    }
}
