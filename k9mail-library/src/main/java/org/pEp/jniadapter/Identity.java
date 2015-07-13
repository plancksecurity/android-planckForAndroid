package org.pEp.jniadapter;

public class Identity {
    public String address;
    public String fpr;
    public String user_id;
    public String username;
    CommType comm_type;
    public String lang;
    public boolean me;

    public Identity() {
        this.me = false;
    }

    public Identity(boolean me) {
        this.me = me;
    }
}

