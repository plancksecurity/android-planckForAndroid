package org.pEp.jniadapter;

import java.util.ArrayList;
import java.util.Vector;
import java.util.Date;
import java.util.HashMap;

public class Message implements AutoCloseable {
    private final long handle;

    native long init();
    native void release(long handle);

    public Message() {
        handle = init();
    }

    public final void close() {
        release(handle);
    }

    public enum TextFormat {
        Plain (0), 
        Html (1), 
        Other (255)
        ;
    
        public final int value;
        public static final HashMap<Integer, TextFormat> tag =
                new HashMap<Integer, TextFormat>();
    
        TextFormat(int value) {
            this.value = value;
            fill(value);
        }
    
        private void fill(int value) {
            tag.put(value, this);
        }
    }
    
    public enum Direction {
        Incoming (0), 
        Outgoing (1)
        ;
    
        public final int value;
        public static final HashMap<Integer, Direction> tag =
                new HashMap<Integer, Direction>();
    
        Direction(int value) {
            this.value = value;
            fill(value);
        }
    
        private void fill(int value) {
            tag.put(value, this);
        }
    }
    
    public enum EncFormat {
        None (0), 
        Pieces (1), 
        SMIME (2), 
        PGPMIME (3), 
        PEP (4)
        ;
    
        public final int value;
        public static final HashMap<Integer, EncFormat> tag =
                new HashMap<Integer, EncFormat>();
    
        EncFormat(int value) {
            this.value = value;
            fill(value);
        }
    
        private void fill(int value) {
            tag.put(value, this);
        }
    }
    
    public native Direction getDir();
    public native void setDir(Direction value);
    
    private native byte[] _getId();
    private native void _setId(byte[] value);
    public String getId() {
        return AbstractEngine.toUTF16(_getId());
    }
    public void setId(String value) {
        _setId(AbstractEngine.toUTF8(value));
    }
    
    private native byte[] _getShortmsg();
    private native void _setShortmsg(byte[] value);
    public String getShortmsg() {
        return AbstractEngine.toUTF16(_getShortmsg());
    }
    public void setShortmsg(String value) {
        _setShortmsg(AbstractEngine.toUTF8(value));
    }
    
    private native byte[] _getLongmsg();
    private native void _setLongmsg(byte[] value);
    public String getLongmsg() {
        return AbstractEngine.toUTF16(_getLongmsg());
    }
    public void setLongmsg(String value) {
        _setLongmsg(AbstractEngine.toUTF8(value));
    }
    
    private native byte[] _getLongmsgFormatted();
    private native void _setLongmsgFormatted(byte[] value);
    public String getLongmsgFormatted() {
        return AbstractEngine.toUTF16(_getLongmsgFormatted());
    }
    public void setLongmsgFormatted(String value) {
        _setLongmsgFormatted(AbstractEngine.toUTF8(value));
    }
    
    public native ArrayList<Blob> getAttachments();
    public native void setAttachments(ArrayList<Blob> value);
    
    public native Date getSent();
    public native void setSent(Date value);
    
    public native Date getRecv();
    public native void setRecv(Date value);
    
    public native Identity getFrom();
    public native void setFrom(Identity value);
    
    public native ArrayList<Identity> getTo();
    public native void setTo(ArrayList<Identity> value);
    
    public native Identity getRecvBy();
    public native void setRecvBy(Identity value);
    
    public native ArrayList<Identity> getCc();
    public native void setCc(ArrayList<Identity> value);
    
    public native ArrayList<Identity> getBcc();
    public native void setBcc(ArrayList<Identity> value);
    
    public native ArrayList<Identity> getReplyTo();
    public native void setReplyTo(ArrayList<Identity> value);
    
    private native ArrayList<byte[]> _getInReplyTo();
    private native void _setInReplyTo(ArrayList<byte[]> value);
    public ArrayList<String> getInReplyTo() {
        return AbstractEngine.toUTF16(_getInReplyTo());
    }
    public void setInReplyTo(ArrayList<String> value) {
        _setInReplyTo(AbstractEngine.toUTF8(value));
    }
    
    private native ArrayList<byte[]> _getReferences();
    private native void _setReferences(ArrayList<byte[]> value);
    public ArrayList<String> getReferences() {
        return AbstractEngine.toUTF16(_getReferences());
    }
    public void setReferences(ArrayList<String> value) {
        _setReferences(AbstractEngine.toUTF8(value));
    }
    
    private native ArrayList<byte[]> _getKeywords();
    private native void _setKeywords(ArrayList<byte[]> value);
    public ArrayList<String> getKeywords() {
        return AbstractEngine.toUTF16(_getKeywords());
    }
    public void setKeywords(ArrayList<String> value) {
        _setKeywords(AbstractEngine.toUTF8(value));
    }
    
    private native byte[] _getComments();
    private native void _setComments(byte[] value);
    public String getComments() {
        return AbstractEngine.toUTF16(_getComments());
    }
    public void setComments(String value) {
        _setComments(AbstractEngine.toUTF8(value));
    }
    
    private native Vector<Pair<byte[], byte[]>> _getOptFields();
    private native void _setOptFields(Vector<Pair<byte[], byte[]>> value);
    public Vector<Pair<String, String>> getOptFields() {
        return AbstractEngine.toUTF16(_getOptFields());
    }
    public void setOptFields(Vector<Pair<String, String>> value) {
        _setOptFields(AbstractEngine.toUTF8(value));
    }
    
    public native Message.EncFormat getEncFormat();
    public native void setEncFormat(Message.EncFormat value);
    
}
