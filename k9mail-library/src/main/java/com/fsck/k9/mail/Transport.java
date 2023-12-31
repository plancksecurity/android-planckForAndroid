
package com.fsck.k9.mail;

import com.fsck.k9.mail.ServerSettings.Type;
import com.fsck.k9.mail.transport.smtp.SmtpTransport;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

public abstract class Transport {

    protected static final int SOCKET_CONNECT_TIMEOUT = 10000;

    // RFC 1047
    protected static final int SOCKET_READ_TIMEOUT = 300000;

    /**
     * Decodes the contents of transport-specific URIs and puts them into a {@link ServerSettings}
     * object.
     *
     * @param uri
     *         the transport-specific URI to decode
     *
     * @return A {@link ServerSettings} object holding the settings contained in the URI.
     *
     * @see SmtpTransport#decodeUri(String)
     */
    public static ServerSettings decodeTransportUri(String uri) {
        if (uri.startsWith("smtp")) {
            return SmtpTransport.decodeUri(uri);
        } else {
            throw new IllegalArgumentException("Not a valid transport URI");
        }
    }

    /**
     * Creates a transport URI from the information supplied in the {@link ServerSettings} object.
     *
     * @param server
     *         The {@link ServerSettings} object that holds the server settings.
     *
     * @return A transport URI that holds the same information as the {@code server} parameter.
     *
     * @see SmtpTransport#createUri(ServerSettings)
     */
    public static String createTransportUri(ServerSettings server) {
        if (Type.SMTP == server.type) {
            return SmtpTransport.createUri(server);
        } else {
            throw new IllegalArgumentException("Not a valid transport URI");
        }
    }


    public abstract void open() throws MessagingException;

    public abstract void sendMessage(Message message) throws MessagingException;

    public abstract void close();

    protected static String encodeUtf8(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 not found");
        }
    }
    protected static String decodeUtf8(String s) {
        try {
            return URLDecoder.decode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 not found");
        }
    }
}
