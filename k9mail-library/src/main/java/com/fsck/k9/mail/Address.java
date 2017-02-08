
package com.fsck.k9.mail;

import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.text.util.Rfc822Token;
import android.text.util.Rfc822Tokenizer;
import android.util.Log;

import com.fsck.k9.mail.filter.Base64;

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.codec.EncoderUtil;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.dom.address.MailboxList;
import org.apache.james.mime4j.field.address.AddressBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static com.fsck.k9.mail.K9MailLib.LOG_TAG;

public class Address implements Serializable {
    private static final Pattern ATOM = Pattern.compile("^(?:[a-zA-Z0-9!#$%&'*+\\-/=?^_`{|}~]|\\s)+$");

    /**
     * Immutable empty {@link Address} array
     */
    private static final Address[] EMPTY_ADDRESS_ARRAY = new Address[0];

    private String mAddress;

    private String mPersonal;

    private static final String UTF_8_Q = "=?utf-8?Q?";
    private static final String UTF_8_B = "=?utf-8?B?";
    private static final String UTF_ENDING = "?=";

    public Address(Address address) {
        mAddress = address.mAddress;
        mPersonal = address.mPersonal;
    }

    public Address(String address, String personal) {
        this(address, personal, true);
    }

    public Address(String address) {
        this(address, null, true);
    }

    private Address(String address, String personal, boolean parse) {
        if (parse) {
            Rfc822Token[] tokens =  Rfc822Tokenizer.tokenize(address);
            if (tokens.length > 0) {
                Rfc822Token token = tokens[0];
                mAddress = token.getAddress();
                String name = token.getName();
                if (!TextUtils.isEmpty(name)) {
                    /*
                     * Don't use the "personal" argument if "address" is of the form:
                     * James Bond <james.bond@mi6.uk>
                     *
                     * See issue 2920
                     */
                    mPersonal = name;
                } else {
                    mPersonal = (personal == null) ? null : personal.trim();
                }
            } else {
                // This should be an error
            }
        } else {
            mAddress = address;
            mPersonal = personal;
        }
        if (mPersonal != null && mPersonal.contains(UTF_8_Q)) {
            mPersonal = mPersonal.replace(UTF_8_Q, "").replace(UTF_ENDING, "");
        }
        if (mPersonal != null && mPersonal.contains(UTF_8_B)) {
            mPersonal = mPersonal.replace(UTF_8_B, "").replace(UTF_ENDING, "");
        }
    }

    public String getAddress() {
        return mAddress;
    }

    public String getHostname() {
        if (mAddress == null) {
            return null;
        }

        int hostIdx = mAddress.lastIndexOf("@");

        if (hostIdx == -1) {
            return null;
        }

        return mAddress.substring(hostIdx + 1);
    }

    public void setAddress(String address) {
        this.mAddress = address;
    }

    public String getPersonal() {
        return mPersonal;
    }

    public void setPersonal(String newPersonal) {
        String personal = newPersonal;
        if ("".equals(personal)) {
            personal = null;
        }
        if (personal != null) {
            personal = personal.trim();
        }
        this.mPersonal = personal;
    }

    /**
     * Parse a comma separated list of email addresses in human readable format and return an
     * array of Address objects, RFC-822 encoded.
     *
     * @param addressList
     * @return An array of 0 or more Addresses.
     */
    public static Address[] parseUnencoded(String addressList) {
        List<Address> addresses = new ArrayList<Address>();
        if (!TextUtils.isEmpty(addressList)) {
            Rfc822Token[] tokens = Rfc822Tokenizer.tokenize(addressList);
            for (Rfc822Token token : tokens) {
                String address = token.getAddress();
                if (!TextUtils.isEmpty(address)) {
                    addresses.add(new Address(token.getAddress(), token.getName(), false));
                }
            }
        }
        return addresses.toArray(EMPTY_ADDRESS_ARRAY);
    }

    /**
     * Parse a comma separated list of addresses in RFC-822 format and return an
     * array of Address objects.
     *
     * @param addressList
     * @return An array of 0 or more Addresses.
     */
    public static Address[] parse(String addressList) {
        if (TextUtils.isEmpty(addressList) || addressList.length() <= 5) {
            return EMPTY_ADDRESS_ARRAY;
        }
        List<Address> addresses = new ArrayList<Address>();
        try {
            MailboxList parsedList =  AddressBuilder.DEFAULT.parseAddressList(addressList).flatten();

            for (int i = 0, count = parsedList.size(); i < count; i++) {
                org.apache.james.mime4j.dom.address.Address address = parsedList.get(i);
                if (address instanceof Mailbox) {
                    Mailbox mailbox = (Mailbox) address;
                    addresses.add(new Address(mailbox.getLocalPart() + "@" + mailbox.getDomain(), mailbox.getName(), false));
                } else {
                    Log.e(LOG_TAG, "Unknown address type from Mime4J: "
                            + address.getClass().toString());
                }
            }
        } catch (MimeException pe) {
            Log.e(LOG_TAG, "MimeException in Address.parse()", pe);
            //but we do an silent failover : we just use the given string as name with empty address
            addresses.add(new Address(null, addressList, false));
        }
        return addresses.toArray(EMPTY_ADDRESS_ARRAY);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Address address = (Address) o;

        if (mAddress != null ? !mAddress.equals(address.mAddress) : address.mAddress != null) {
            return false;
        }

        return mPersonal != null ? mPersonal.equals(address.mPersonal) : address.mPersonal == null;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        if (mAddress != null) {
            hash += mAddress.hashCode();
        }
        if (mPersonal != null) {
            hash += 3 * mPersonal.hashCode();
        }
        return hash;
    }

    @Override
    public String toString() {
        if (!TextUtils.isEmpty(mPersonal)) {
            return quoteAtoms(mPersonal) + " <" + mAddress + ">";
        } else {
            return mAddress;
        }
    }

    public static String toString(Address[] addresses) {
        if (addresses == null) {
            return null;
        }
        return TextUtils.join(", ", addresses);
    }

    public String toEncodedString() {
        if (!TextUtils.isEmpty(mPersonal)) {
            mPersonal = convertFromUTF8(mPersonal);
            return EncoderUtil.encodeAddressDisplayName(mPersonal) + " <" + mAddress + ">";
        } else {
            return mAddress;
        }
    }

    public String convertFromUTF8(String string) {
        String out = null;
        out = string;
        out = out.replace("=?", "");
        out = out.replace("?=", "");
        String[] split = out.split("\\?");
        if (split != null && split.length > 2) {
            if (split[1].equals("B")) {
                byte[] bytesEncoded = Base64.encodeBase64(split[2].getBytes());
                byte[] valueDecoded= Base64.decodeBase64(bytesEncoded);
                return new String(valueDecoded);
            } else {
                return split[2];
            }
        }
        return out;
    }

    public static String toEncodedString(Address[] addresses) {
        if (addresses == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < addresses.length; i++) {
            sb.append(addresses[i].toEncodedString());
            if (i < addresses.length - 1) {
                sb.append(',');
            }
        }
        return sb.toString();
    }


    /**
     * Unpacks an address list previously packed with packAddressList()
     * @param addressList Packed address list.
     * @return Unpacked list.
     */
    public static Address[] unpack(String addressList) {
        if (addressList == null) {
            return new Address[] { };
        }
        List<Address> addresses = new ArrayList<Address>();
        int length = addressList.length();
        int pairStartIndex = 0;
        int pairEndIndex = 0;
        int addressEndIndex = 0;
        while (pairStartIndex < length) {
            pairEndIndex = addressList.indexOf(",\u0000", pairStartIndex);
            if (pairEndIndex == -1) {
                pairEndIndex = length;
            }
            addressEndIndex = addressList.indexOf(";\u0000", pairStartIndex);
            String address = null;
            String personal = null;
            if (addressEndIndex == -1 || addressEndIndex > pairEndIndex) {
                address = addressList.substring(pairStartIndex, pairEndIndex);
            } else {
                address = addressList.substring(pairStartIndex, addressEndIndex);
                personal = addressList.substring(addressEndIndex + 2, pairEndIndex);
            }
            addresses.add(new Address(address, personal, false));
            pairStartIndex = pairEndIndex + 2;
        }
        return addresses.toArray(new Address[addresses.size()]);
    }

    /**
     * Packs an address list into a String that is very quick to read
     * and parse. Packed lists can be unpacked with unpackAddressList()
     * The packed list is a ",\u0000" separated list of:
     * address;\u0000personal
     * @param addresses Array of addresses to pack.
     * @return Packed addresses.
     */
    public static String pack(Address[] addresses) {
        if (addresses == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0, count = addresses.length; i < count; i++) {
            Address address = addresses[i];
            sb.append(address.getAddress());
            String personal = address.getPersonal();
            if (personal != null) {
                sb.append(";\u0000");
                // Escape quotes in the address part on the way in
                personal = personal.replaceAll("\"", "\\\"");
                sb.append(personal);
            }
            if (i < count - 1) {
                sb.append(",\u0000");
            }
        }
        return sb.toString();
    }

    /**
     * Quote a string, if necessary, based upon the definition of an "atom," as defined by RFC2822
     * (http://tools.ietf.org/html/rfc2822#section-3.2.4). Strings that consist purely of atoms are
     * left unquoted; anything else is returned as a quoted string.
     * @param text String to quote.
     * @return Possibly quoted string.
     */
    public static String quoteAtoms(final String text) {
        if (ATOM.matcher(text).matches()) {
            return text;
        } else {
            return quoteString(text);
        }
    }

    /**
     * Ensures that the given string starts and ends with the double quote character.
     * The string is not modified in any way except to add the double quote character to start
     * and end if it's not already there.
     * sample -> "sample"
     * "sample" -> "sample"
     * ""sample"" -> ""sample""
     * "sample"" -> "sample"
     * sa"mp"le -> "sa"mp"le"
     * "sa"mp"le" -> "sa"mp"le"
     * (empty string) -> ""
     * " -> """
     * @param s
     * @return
     */
    @VisibleForTesting
    static String quoteString(String s) {
        if (s == null) {
            return null;
        }
        if (!s.matches("^\".*\"$")) {
            return "\"" + s + "\"";
        } else {
            return s;
        }
    }
}
