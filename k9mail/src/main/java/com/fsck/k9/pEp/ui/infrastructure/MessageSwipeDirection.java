package com.fsck.k9.pEp.ui.infrastructure;

public class MessageSwipeDirection {
    public static final String BACKWARDS = "backwards";
    public static final String FORWARD = "forward";

    private final String directionToLoad;

    public MessageSwipeDirection(String directionToLoad) {
        this.directionToLoad = directionToLoad;
    }

    public String getDirectionToLoad() {
        return directionToLoad;
    }
}
