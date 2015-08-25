package com.fsck.k9.pEp;

import android.util.Log;

import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.internet.MimeMultipart;
import com.fsck.k9.mailstore.BinaryMemoryBody;

import org.pEp.jniadapter.Identity;
import org.pEp.jniadapter.Message;

import java.util.ArrayList;
import java.util.Date;
import java.util.Vector;

/**
 * some helper stuff
 *
 * TODO: pEp: rename methods. Current naming of methods perhaps misleading
 */

class PEpUtils {
    static Vector<Identity> createFrom(Address[] adrs) {
        Vector<Identity> rv = new Vector<Identity>(adrs.length);
        for(Address adr : adrs)
            rv.add(createFrom(adr));
        return rv;
    }

    static Identity createFrom(Address adr) {
        Identity id = new Identity();
        id.address = adr.getAddress();
        id.username = adr.getPersonal();

        // TODO: do I have any kind of unique id for user_id?
        return id;
    }

    static Address createFrom(Identity id) {
        Address adr = new Address(id.address, id.username);
        // Address() parses the address, eventually not setting it, therefore just a little sanity...
        // TODO: pEp check what happens if id.address == null beforehand
        if(adr.getAddress() == null && id.address != null)
            throw new RuntimeException("Could not convert Identiy.address " + id.address + " to Address.");
        return adr;
    }

    static Message createFrom(MimeMessage mm) {
        Message m = new Message();
/*
    public native void setDir(Direction value);
    public void setId(String value) {
    public void setShortmsg(String value) {

    public void setLongmsg(String value) {
    public void setLongmsgFormatted(String value) {
    public native void setAttachments(ArrayList<Blob> value);

    public native void setSent(Date value);

    public native void setRecv(Date value);

    public native void setFrom(Identity value);

    public native void setTo(ArrayList<Identity> value);

    public native void setRecvBy(Identity value);

    public native void setCc(ArrayList<Identity> value);

    public native void setBcc(ArrayList<Identity> value);

    public native void setReplyTo(ArrayList<Identity> value);

    public void setInReplyTo(ArrayList<String> value) {
    public void setReferences(ArrayList<String> value) {
    public void setKeywords(ArrayList<String> value) {
    public void setComments(String value) {
    public void setOptFields(Vector<Pair<String, String>> value) {
    public native void setEncFormat(Message.EncFormat value);
  */
        return m;
    }

    static MimeMessage createFrom(Message m) {
        return null;
    }








    static public void dumpMimeMessage(MimeMessage mm) {
        String out = "Root:\n";

        try {
            for (String header:mm.getHeaderNames())
                out += header + ":" + mm.getHeader(header) + "\n";
            out += "\n";
            out += "Message-Id:" + mm.getMessageId().hashCode() +"\n";
            out += mangleBody((MimeMultipart)mm.getBody());
            out += "hasAttachments:" + mm.hasAttachments();

        } catch (Exception e) {
            out += "\n\n" + e.getMessage();
        }

        Log.d("MIMEMESSAGE", out);

    }

    static private String mangleBody(MimeMultipart body) throws Exception {
        String rv = "Body:\n";
        for(Part p: body.getBodyParts())
            rv += "     " + new String(((BinaryMemoryBody) p).getData()) +"\n";
        //rv+="  " + ((BinaryMemoryBody) p)(((LocalBodyPart) p).getBody())).getData().toString() +"\n";

        return rv;
    }

}
