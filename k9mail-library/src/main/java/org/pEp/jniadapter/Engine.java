package org.pEp.jniadapter;

import java.util.ArrayList;
import java.util.Vector;

final public class Engine extends AbstractEngine {
    public Engine() throws pEpException { }

    public native void encrypt_message(
            Message src,
            ArrayList<String> extra,
            Message dst,
            Message.EncFormat enc_format
        ) throws pEpException;

    public native void decrypt_message(
            Message src,
            Message dst,
            ArrayList<String> keylist,
            Color color
        ) throws pEpException;

    public native void outgoing_message_color(
            Message msg,
            Color color
        ) throws pEpException;
}
