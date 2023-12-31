
package com.fsck.k9.mail.transport.smtp;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.net.TrafficStats;
import androidx.annotation.VisibleForTesting;
import android.text.TextUtils;

import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.Authentication;
import com.fsck.k9.mail.AuthenticationFailedException;
import com.fsck.k9.mail.CertificateValidationException;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.K9MailLib;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mail.Transport;
import com.fsck.k9.mail.filter.Base64;
import com.fsck.k9.mail.filter.EOLConvertingOutputStream;
import com.fsck.k9.mail.filter.LineWrapOutputStream;
import com.fsck.k9.mail.filter.PeekableInputStream;
import com.fsck.k9.mail.filter.SmtpDataStuffing;
import com.fsck.k9.mail.internet.CharsetSupport;
import com.fsck.k9.mail.oauth.OAuth2TokenProvider;
import com.fsck.k9.mail.oauth.XOAuth2ChallengeParser;
import com.fsck.k9.mail.ssl.TrustedSocketFactory;
import com.fsck.k9.mail.store.StoreConfig;
import com.fsck.k9.mail.store.imap.sasl.OAuthBearer;

import javax.net.ssl.SSLException;
import org.apache.commons.io.IOUtils;
import timber.log.Timber;

import static com.fsck.k9.mail.CertificateValidationException.Reason.MissingCapability;
import static com.fsck.k9.mail.K9MailLib.DEBUG_PROTOCOL_SMTP;

public class SmtpTransport extends Transport {
    public static final int SMTP_CONTINUE_REQUEST = 334;
    public static final int SMTP_AUTHENTICATION_FAILURE_ERROR_CODE = 535;
    private static final int SMTP_SOCKET_TAG = 443;

    private TrustedSocketFactory mTrustedSocketFactory;
    private OAuth2TokenProvider oauthTokenProvider;

    /**
     * Decodes a SmtpTransport URI.
     *
     * NOTE: In contrast to ImapStore and Pop3Store, the authType is appended at the end!
     *
     * <p>Possible forms:</p>
     * <pre>
     * smtp://user:password:auth@server:port ConnectionSecurity.NONE
     * smtp+tls+://user:password:auth@server:port ConnectionSecurity.STARTTLS_REQUIRED
     * smtp+ssl+://user:password:auth@server:port ConnectionSecurity.SSL_TLS_REQUIRED
     * </pre>
     */
    public static ServerSettings decodeUri(String uri) {
        String host;
        int port;
        ConnectionSecurity connectionSecurity;
        AuthType authType = null;
        String username = null;
        String password = null;
        String clientCertificateAlias = null;

        URI smtpUri;
        try {
            smtpUri = new URI(uri);
        } catch (URISyntaxException use) {
            throw new IllegalArgumentException("Invalid SmtpTransport URI", use);
        }

        String scheme = smtpUri.getScheme();
        /*
         * Currently available schemes are:
         * smtp
         * smtp+tls+
         * smtp+ssl+
         *
         * The following are obsolete schemes that may be found in pre-existing
         * settings from earlier versions or that may be found when imported. We
         * continue to recognize them and re-map them appropriately:
         * smtp+tls
         * smtp+ssl
         */
        if (scheme.equals("smtp")) {
            connectionSecurity = ConnectionSecurity.NONE;
            port = ServerSettings.Type.SMTP.defaultPort;
        } else if (scheme.startsWith("smtp+tls")) {
            connectionSecurity = ConnectionSecurity.STARTTLS_REQUIRED;
            port = ServerSettings.Type.SMTP.defaultPort;
        } else if (scheme.startsWith("smtp+ssl")) {
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED;
            port = ServerSettings.Type.SMTP.defaultTlsPort;
        } else {
            throw new IllegalArgumentException("Unsupported protocol (" + scheme + ")");
        }

        host = smtpUri.getHost();

        if (smtpUri.getPort() != -1) {
            port = smtpUri.getPort();
        }

        if (smtpUri.getUserInfo() != null) {
            String[] userInfoParts = smtpUri.getUserInfo().split(":");
            if (userInfoParts.length == 1) {
                authType = AuthType.PLAIN;
                username = decodeUtf8(userInfoParts[0]);
            } else if (userInfoParts.length == 2) {
                authType = AuthType.PLAIN;
                username = decodeUtf8(userInfoParts[0]);
                password = decodeUtf8(userInfoParts[1]);
            } else if (userInfoParts.length == 3) {
                // NOTE: In SmtpTransport URIs, the authType comes last!
                authType = AuthType.valueOf(userInfoParts[2]);
                username = decodeUtf8(userInfoParts[0]);
                if (authType == AuthType.EXTERNAL) {
                    clientCertificateAlias = decodeUtf8(userInfoParts[1]);
                } else {
                    password = decodeUtf8(userInfoParts[1]);
                }
            } else if (userInfoParts.length == 4) {
                authType = AuthType.valueOf(userInfoParts[3]);
                username = decodeUtf8(userInfoParts[0]);
                password = decodeUtf8(userInfoParts[1]);
                clientCertificateAlias = decodeUtf8(userInfoParts[2]);
            }
        }

        return new ServerSettings(ServerSettings.Type.SMTP, host, port, connectionSecurity,
                authType, username, password, clientCertificateAlias);
    }

    /**
     * Creates a SmtpTransport URI with the supplied settings.
     *
     * @param server
     *         The {@link ServerSettings} object that holds the server settings.
     *
     * @return A SmtpTransport URI that holds the same information as the {@code server} parameter.
     *
     * @see com.fsck.k9.mail.store.StoreConfig#getTransportUri()
     * @see SmtpTransport#decodeUri(String)
     */
    public static String createUri(ServerSettings server) {
        String userEnc = (server.username != null) ?
                encodeUtf8(server.username) : "";
        String passwordEnc = (server.password != null) ?
                encodeUtf8(server.password) : "";
        String clientCertificateAliasEnc = (server.clientCertificateAlias != null) ?
                encodeUtf8(server.clientCertificateAlias) : "";

        String scheme;
        switch (server.connectionSecurity) {
            case SSL_TLS_REQUIRED:
                scheme = "smtp+ssl+";
                break;
            case STARTTLS_REQUIRED:
                scheme = "smtp+tls+";
                break;
            default:
            case NONE:
                scheme = "smtp";
                break;
        }

        String userInfo;
        AuthType authType = server.authenticationType;
        // NOTE: authType is append at last item, in contrast to ImapStore and Pop3Store!
        if (authType != null) {
            if (AuthType.EXTERNAL == authType) {
                userInfo = userEnc + ":" + clientCertificateAliasEnc + ":" + authType.name();
            } else if (AuthType.EXTERNAL_PLAIN == authType) {
                userInfo = userEnc + ":" + passwordEnc + ":" + clientCertificateAliasEnc +
                        ":" + authType.name();
            } else {
                userInfo = userEnc + ":" + passwordEnc + ":" + authType.name();
            }
        } else {
            userInfo = userEnc + ":" + passwordEnc;
        }
        try {
            return new URI(scheme, userInfo, server.host, server.port, null, null,
                    null).toString();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Can't create SmtpTransport URI", e);
        }
    }


    private String mHost;
    private int mPort;
    private String mUsername;
    private String mPassword;
    private String mClientCertificateAlias;
    private AuthType mAuthType;
    private ConnectionSecurity mConnectionSecurity;
    private Socket mSocket;
    private PeekableInputStream mIn;
    private OutputStream mOut;
    private boolean m8bitEncodingAllowed;
    private boolean mEnhancedStatusCodesProvided;
    private int mLargestAcceptableMessage;
    private boolean retryOAuthWithNewToken;

    public SmtpTransport(StoreConfig storeConfig, TrustedSocketFactory trustedSocketFactory,
            OAuth2TokenProvider oauth2TokenProvider) throws MessagingException {
        ServerSettings settings;
        try {
            settings = decodeUri(storeConfig.getTransportUri());
        } catch (IllegalArgumentException e) {
            throw new MessagingException("Error while decoding transport URI", e);
        }

        mHost = settings.host;
        mPort = settings.port;

        mConnectionSecurity = settings.connectionSecurity;

        mAuthType = settings.authenticationType;
        mUsername = settings.username;
        mPassword = settings.password;
        mClientCertificateAlias = settings.clientCertificateAlias;
        mTrustedSocketFactory = trustedSocketFactory;
        oauthTokenProvider = oauth2TokenProvider;
        TrafficStats.setThreadStatsTag(SMTP_SOCKET_TAG);
    }

    @Override
    public void open() throws MessagingException {
        try {
            Timber.e("Open");
            InetAddress[] addresses = InetAddress.getAllByName(mHost);
            for (int i = 0; i < addresses.length; i++) {
                Timber.e("Open: " + addresses[i] + " " + addresses.length);

                try {
                    SocketAddress socketAddress = new InetSocketAddress(addresses[i], mPort);
                    if (mConnectionSecurity == ConnectionSecurity.SSL_TLS_REQUIRED) {
                        mSocket = mTrustedSocketFactory.createSocket(null, mHost, mPort, mClientCertificateAlias);
                        mSocket.connect(socketAddress, SOCKET_CONNECT_TIMEOUT);
                    } else {
                        mSocket = new Socket();
                        TrafficStats.setThreadStatsTag(((int) Thread.currentThread().getId()));
                        mSocket.connect(socketAddress, SOCKET_CONNECT_TIMEOUT);
                    }
                } catch (SocketException e) {
                    if (i < (addresses.length - 1)) {
                        // there are still other addresses for that host to try
                        continue;
                    }
                    throw new MessagingException("Cannot connect to host", e);
                }
                break; // connection success
            }

            // RFC 1047
            mSocket.setSoTimeout(SOCKET_READ_TIMEOUT);

            mIn = new PeekableInputStream(new BufferedInputStream(mSocket.getInputStream(), 1024));
            mOut = new BufferedOutputStream(mSocket.getOutputStream(), 1024);

            // Eat the banner
            executeCommand(null);

            InetAddress localAddress = mSocket.getLocalAddress();
            String localHost = getCanonicalHostName(localAddress);
            String ipAddr = localAddress.getHostAddress();

            if (localHost.equals("") || localHost.equals(ipAddr) || localHost.contains("_")) {
                // We don't have a FQDN or the hostname contains invalid
                // characters (see issue 2143), so use IP address.
                if (!ipAddr.equals("")) {
                    if (localAddress instanceof Inet6Address) {
                        localHost = "[IPv6:" + ipAddr + "]";
                    } else {
                        localHost = "[" + ipAddr + "]";
                    }
                } else {
                    // If the IP address is no good, set a sane default (see issue 2750).
                    localHost = "android";
                }
            }

            Map<String, String> extensions = sendHello(localHost);

            m8bitEncodingAllowed = extensions.containsKey("8BITMIME");
            mEnhancedStatusCodesProvided = extensions.containsKey("ENHANCEDSTATUSCODES");

            if (mConnectionSecurity == ConnectionSecurity.STARTTLS_REQUIRED) {
                if (extensions.containsKey("STARTTLS")) {
                    executeCommand("STARTTLS");

                    mSocket = mTrustedSocketFactory.createSocket(
                            mSocket,
                            mHost,
                            mPort,
                            mClientCertificateAlias);

                    mIn = new PeekableInputStream(new BufferedInputStream(mSocket.getInputStream(),
                            1024));
                    mOut = new BufferedOutputStream(mSocket.getOutputStream(), 1024);
                    /*
                     * Now resend the EHLO. Required by RFC2487 Sec. 5.2, and more specifically,
                     * Exim.
                     */
                    extensions = sendHello(localHost);
                } else {
                    /*
                     * This exception triggers a "Certificate error"
                     * notification that takes the user to the incoming
                     * server settings for review. This might be needed if
                     * the account was configured with an obsolete
                     * "STARTTLS (if available)" setting.
                     */
                    throw new CertificateValidationException(
                            "STARTTLS connection security not available");
                }
            }

            boolean authLoginSupported = false;
            boolean authPlainSupported = false;
            boolean authCramMD5Supported = false;
            boolean authExternalSupported = false;
            boolean authXoauth2Supported = false;
            boolean authOAuthBearerSupported = false;
            if (extensions.containsKey("AUTH")) {
                List<String> saslMech = Arrays.asList(extensions.get("AUTH").split(" "));
                authLoginSupported = saslMech.contains("LOGIN");
                authPlainSupported = saslMech.contains("PLAIN");
                authCramMD5Supported = saslMech.contains("CRAM-MD5");
                authExternalSupported = saslMech.contains("EXTERNAL");
                authXoauth2Supported = saslMech.contains("XOAUTH2");
                authOAuthBearerSupported = saslMech.contains("OAUTHBEARER");
            }
            parseOptionalSizeValue(extensions);

            if (!TextUtils.isEmpty(mUsername)
                    && (!TextUtils.isEmpty(mPassword) ||
                    AuthType.EXTERNAL == mAuthType ||
                    AuthType.XOAUTH2 == mAuthType)) {

                switch (mAuthType) {
                    case EXTERNAL_PLAIN:
                    case PLAIN:
                        // try saslAuthPlain first, because it supports UTF-8 explicitly
                        if (authPlainSupported) {
                            saslAuthPlain(mUsername, mPassword);
                        } else if (authLoginSupported) {
                            saslAuthLogin(mUsername, mPassword);
                        } else {
                            throw new MessagingException(
                                    "Authentication methods SASL PLAIN and LOGIN are unavailable.");
                        }
                        break;

                    case CRAM_MD5:
                        if (authCramMD5Supported) {
                            saslAuthCramMD5();
                        } else {
                            throw new MessagingException("Authentication method CRAM-MD5 is unavailable.");
                        }
                        break;
                    case XOAUTH2:
                        if (oauthTokenProvider == null) {
                            throw new MessagingException("No OAuth2TokenProvider available.");
                        } else if (authOAuthBearerSupported) {
                            saslOAuth(OAuthMethod.OAUTHBEARER);
                        } else if (authXoauth2Supported) {
                            saslOAuth(OAuthMethod.XOAUTH2);
                        } else {
                            throw new MessagingException("Server doesn't support SASL OAUTHBEARER or XOAUTH2.");
                        }
                        break;
                    case EXTERNAL:
                        if (authExternalSupported) {
                            saslAuthExternal(mUsername);
                        } else {
                        /*
                         * Some SMTP servers are known to provide no error
                         * indication when a client certificate fails to
                         * validate, other than to not offer the AUTH EXTERNAL
                         * capability.
                         *
                         * So, we treat it is an error to not offer AUTH
                         * EXTERNAL when using client certificates. That way, the
                         * user can be notified of a problem during account setup.
                         */
                            throw new CertificateValidationException(MissingCapability);
                        }
                        break;

                    default:
                        throw new MessagingException(
                                "Unhandled authentication method found in the server settings (bug).");
                }
            }
        } catch (MessagingException e) {
            close();
            throw e;
        } catch (SSLException e) {
            close();
            throw new CertificateValidationException(e.getMessage(), e);
        } catch (GeneralSecurityException gse) {
            close();
            throw new MessagingException(
                "Unable to open connection to SMTP server due to security error.", gse);
        } catch (IOException ioe) {
            close();
            throw new MessagingException("Unable to open connection to SMTP server.", ioe);
        }
    }

    private void parseOptionalSizeValue(Map<String, String> extensions) {
        if (extensions.containsKey("SIZE")) {
            String optionalsizeValue = extensions.get("SIZE");
            if (optionalsizeValue != null && optionalsizeValue != "") {
                try {
                    mLargestAcceptableMessage = Integer.parseInt(optionalsizeValue);
                } catch (NumberFormatException e) {
                    if (K9MailLib.isDebug() && DEBUG_PROTOCOL_SMTP) {
                        Timber.d(e, "Tried to parse %s and get an int", optionalsizeValue);
                    }
                }
            }
        }
    }

    /**
     * Send the client "identity" using the EHLO or HELO command.
     *
     * <p>
     * We first try the EHLO command. If the server sends a negative response, it probably doesn't
     * support the EHLO command. So we try the older HELO command that all servers need to support.
     * And if that fails, too, we pretend everything is fine and continue unimpressed.
     * </p>
     *
     * @param host
     *         The EHLO/HELO parameter as defined by the RFC.
     *
     * @return A (possibly empty) {@code Map<String,String>} of extensions (upper case) and
     * their parameters (possibly 0 length) as returned by the EHLO command
     *
     * @throws IOException
     *          In case of a network error.
     * @throws MessagingException
     *          In case of a malformed response.
     */
    private Map<String, String> sendHello(String host) throws IOException, MessagingException {
        Map<String, String> extensions = new HashMap<String, String>();
        try {
            List<String> results = executeCommand("EHLO %s", host).results;
            // Remove the EHLO greeting response
            results.remove(0);
            for (String result : results) {
                String[] pair = result.split(" ", 2);
                extensions.put(pair[0].toUpperCase(Locale.US), pair.length == 1 ? "" : pair[1]);
            }
        } catch (NegativeSmtpReplyException e) {
            if (K9MailLib.isDebug()) {
                Timber.v("Server doesn't support the EHLO command. Trying HELO...");
            }

            try {
                executeCommand("HELO %s", host);
            } catch (NegativeSmtpReplyException e2) {
                Timber.w("Server doesn't support the HELO command. Continuing anyway.");
            }
        }
        return extensions;
    }

    @Override
    public void sendMessage(Message message) throws MessagingException {
        List<Address> addresses = new ArrayList<Address>();
        {
            addresses.addAll(Arrays.asList(message.getRecipients(RecipientType.TO)));
            addresses.addAll(Arrays.asList(message.getRecipients(RecipientType.CC)));
            addresses.addAll(Arrays.asList(message.getRecipients(RecipientType.BCC)));
        }
        message.setRecipients(RecipientType.BCC, null);

        Map<String, List<String>> charsetAddressesMap =
            new HashMap<String, List<String>>();
        for (Address address : addresses) {
            String addressString = address.getAddress();
            String charset = CharsetSupport.getCharsetFromAddress(addressString);
            List<String> addressesOfCharset = charsetAddressesMap.get(charset);
            if (addressesOfCharset == null) {
                addressesOfCharset = new ArrayList<String>();
                charsetAddressesMap.put(charset, addressesOfCharset);
            }
            addressesOfCharset.add(addressString);
        }

        for (Map.Entry<String, List<String>> charsetAddressesMapEntry :
                charsetAddressesMap.entrySet()) {
            String charset = charsetAddressesMapEntry.getKey();
            List<String> addressesOfCharset = charsetAddressesMapEntry.getValue();
            message.setCharset(charset);
            sendMessageTo(addressesOfCharset, message);
        }
    }

    private void sendMessageTo(List<String> addresses, Message message)
    throws MessagingException {
        close();
        open();

        if (!m8bitEncodingAllowed) {
            Timber.d("Server does not support 8bit transfer encoding");
        }
        // If the message has attachments and our server has told us about a limit on
        // the size of messages, count the message's size before sending it
        if (mLargestAcceptableMessage > 0 && message.hasAttachments()) {
            if (message.calculateSize() > mLargestAcceptableMessage) {
                throw new MessagingException("Message too large for server", true);
            }
        }

        boolean entireMessageSent = false;
        Address[] from = message.getFrom();
        try {
            String fromAddress = from[0].getAddress();
            if (m8bitEncodingAllowed) {
                executeCommand("MAIL FROM:<%s> BODY=8BITMIME", fromAddress);
            } else {
                executeCommand("MAIL FROM:<%s>", fromAddress);
            }

            for (String address : addresses) {
                executeCommand("RCPT TO:<%s>", address);
            }

            executeCommand("DATA");

            EOLConvertingOutputStream msgOut = new EOLConvertingOutputStream(
                    new LineWrapOutputStream(new SmtpDataStuffing(mOut), 1000));

            message.writeTo(msgOut);
            msgOut.endWithCrLfAndFlush();

            entireMessageSent = true; // After the "\r\n." is attempted, we may have sent the message
            executeCommand(".");
        } catch (NegativeSmtpReplyException e) {
            throw e;
        } catch (Exception e) {
            MessagingException me = new MessagingException("Unable to send message", e);
            me.setPermanentFailure(entireMessageSent);

            throw me;
        } finally {
            close();
        }

    }

    @Override
    public void close() {
        try {
            executeCommand("QUIT");
        } catch (Exception e) {

        }
        IOUtils.closeQuietly(mIn);
        IOUtils.closeQuietly(mOut);
        IOUtils.closeQuietly(mSocket);
        mIn = null;
        mOut = null;
        mSocket = null;
    }

    private String readLine() throws IOException {
        StringBuilder sb = new StringBuilder();
        int d;
        while ((d = mIn.read()) != -1) {
            if (((char)d) == '\r') {
                continue;
            } else if (((char)d) == '\n') {
                break;
            } else {
                sb.append((char)d);
            }
        }
        String ret = sb.toString();
        if (K9MailLib.isDebug() && DEBUG_PROTOCOL_SMTP)
            Timber.d("SMTP <<< %s", ret);

        return ret;
    }

    private void writeLine(String s, boolean sensitive) throws IOException {
        if (K9MailLib.isDebug() && DEBUG_PROTOCOL_SMTP) {
            final String commandToLog;
            if (sensitive && !K9MailLib.isDebugSensitive()) {
                commandToLog = "SMTP >>> *sensitive*";
            } else {
                commandToLog = "SMTP >>> " + s;
            }
            Timber.d(commandToLog);
        }

        byte[] data = s.concat("\r\n").getBytes();

        /*
         * Important: Send command + CRLF using just one write() call. Using
         * multiple calls will likely result in multiple TCP packets and some
         * SMTP servers misbehave if CR and LF arrive in separate pakets.
         * See issue 799.
         */
        mOut.write(data);
        mOut.flush();
    }

    private static class CommandResponse {

        private final int replyCode;
        private final List<String> results;

        public CommandResponse(int replyCode, List<String> results) {
            this.replyCode = replyCode;
            this.results = results;
        }
    }

    private CommandResponse executeSensitiveCommand(String format, Object... args)
            throws IOException, MessagingException {
        return executeCommand(true, format, args);
    }

    private CommandResponse executeCommand(String format, Object... args) throws IOException, MessagingException {
        return executeCommand(false, format, args);
    }

    private CommandResponse executeCommand(boolean sensitive, String format, Object... args)
            throws IOException, MessagingException {
        List<String> results = new ArrayList<>();
        if (format != null) {
            String command = String.format(Locale.ROOT, format, args);
            writeLine(command, sensitive);
        }

        String line = readCommandResponseLine(results);

        int length = line.length();
        if (length < 1) {
            throw new MessagingException("SMTP response is 0 length");
        }

        int replyCode = -1;
        if (length >= 3) {
            try {
                replyCode = Integer.parseInt(line.substring(0, 3));
            } catch (NumberFormatException e) { /* ignore */ }
        }

        char replyCodeCategory = line.charAt(0);
        boolean isReplyCodeErrorCategory = (replyCodeCategory == '4') || (replyCodeCategory == '5');
        if (isReplyCodeErrorCategory) {
            if (mEnhancedStatusCodesProvided) {
                throw buildEnhancedNegativeSmtpReplyException(replyCode, results);
            } else {
                String replyText = TextUtils.join(" ", results);
                throw new NegativeSmtpReplyException(replyCode, replyText);
            }
        }

        return new CommandResponse(replyCode, results);
    }

    private MessagingException buildEnhancedNegativeSmtpReplyException(int replyCode, List<String> results) {
        StatusCodeClass statusCodeClass = null;
        StatusCodeSubject statusCodeSubject = null;
        StatusCodeDetail statusCodeDetail = null;

        String message = "";
        for (String resultLine : results) {
            message += resultLine.split(" ", 2)[1] + " ";
        }
        if (results.size() > 0) {
            String[] statusCodeParts = results.get(0).split(" ", 2)[0].split("\\.");

            statusCodeClass = StatusCodeClass.parse(statusCodeParts[0]);
            statusCodeSubject = StatusCodeSubject.parse(statusCodeParts[1]);
            statusCodeDetail = StatusCodeDetail.parse(statusCodeSubject, statusCodeParts[2]);
        }

        return new EnhancedNegativeSmtpReplyException(replyCode, statusCodeClass, statusCodeSubject, statusCodeDetail,
                message.trim());
    }


    /*
     * Read lines as long as the length is 4 or larger, e.g. "220-banner text here".
     * Shorter lines are either errors of contain only a reply code.
     */
    private String readCommandResponseLine(List<String> results) throws IOException {
        String line = readLine();
        while (line.length() >= 4) {
            if (line.length() > 4) {
                // Everything after the first four characters goes into the results array.
                results.add(line.substring(4));
            }

            if (line.charAt(3) != '-') {
                // If the fourth character isn't "-" this is the last line of the response.
                break;
            }
            line = readLine();
        }
        return line;
    }


//    C: AUTH LOGIN
//    S: 334 VXNlcm5hbWU6
//    C: d2VsZG9u
//    S: 334 UGFzc3dvcmQ6
//    C: dzNsZDBu
//    S: 235 2.0.0 OK Authenticated
//
//    Lines 2-5 of the conversation contain base64-encoded information. The same conversation, with base64 strings decoded, reads:
//
//
//    C: AUTH LOGIN
//    S: 334 Username:
//    C: weldon
//    S: 334 Password:
//    C: w3ld0n
//    S: 235 2.0.0 OK Authenticated

    private void saslAuthLogin(String username, String password) throws MessagingException,
        AuthenticationFailedException, IOException {
        try {
            executeCommand("AUTH LOGIN");
            executeSensitiveCommand(Base64.encode(username));
            executeSensitiveCommand(Base64.encode(password));
        } catch (NegativeSmtpReplyException exception) {
            if (exception.getReplyCode() == SMTP_AUTHENTICATION_FAILURE_ERROR_CODE) {
                // Authentication credentials invalid
                throw new AuthenticationFailedException("AUTH LOGIN failed ("
                        + exception.getMessage() + ")");
            } else {
                throw exception;
            }
        }
    }

    private void saslAuthPlain(String username, String password) throws MessagingException,
        AuthenticationFailedException, IOException {
        String data = Base64.encode("\000" + username + "\000" + password);
        try {
            executeSensitiveCommand("AUTH PLAIN %s", data);
        } catch (NegativeSmtpReplyException exception) {
            if (exception.getReplyCode() == SMTP_AUTHENTICATION_FAILURE_ERROR_CODE) {
                // Authentication credentials invalid
                throw new AuthenticationFailedException("AUTH PLAIN failed ("
                        + exception.getMessage() + ")");
            } else {
                throw exception;
            }
        }
    }

    private void saslAuthCramMD5() throws MessagingException,
        AuthenticationFailedException, IOException {

        List<String> respList = executeCommand("AUTH CRAM-MD5").results;
        if (respList.size() != 1) {
            throw new MessagingException("Unable to negotiate CRAM-MD5");
        }

        String b64Nonce = respList.get(0);
        String b64CRAMString = Authentication.computeCramMd5(mUsername, mPassword, b64Nonce);

        try {
            executeSensitiveCommand(b64CRAMString);
        } catch (NegativeSmtpReplyException exception) {
            if (exception.getReplyCode() == SMTP_AUTHENTICATION_FAILURE_ERROR_CODE) {
                // Authentication credentials invalid
                throw new AuthenticationFailedException(exception.getMessage(), exception);
            } else {
                throw exception;
            }
        }
    }

    private void saslOAuth(OAuthMethod method) throws MessagingException, IOException {
        retryOAuthWithNewToken = true;
        try {
            attemptOAuth(method, mUsername);
        } catch (NegativeSmtpReplyException negativeResponse) {
            if (negativeResponse.getReplyCode() != SMTP_AUTHENTICATION_FAILURE_ERROR_CODE) {
                throw negativeResponse;
            }

            oauthTokenProvider.invalidateToken();

            if (!retryOAuthWithNewToken) {
                handlePermanentFailure(negativeResponse);
            } else {
                handleTemporaryFailure(method, mUsername, negativeResponse);
            }
        }
    }

    private void handlePermanentFailure(NegativeSmtpReplyException negativeResponse) throws AuthenticationFailedException {
        throw new AuthenticationFailedException(negativeResponse.getMessage(), negativeResponse);
    }

    private void handleTemporaryFailure(
            OAuthMethod method,
            String username,
            NegativeSmtpReplyException negativeResponseFromOldToken
    )
        throws IOException, MessagingException {
        // Token was invalid

        //We could avoid this double check if we had a reasonable chance of knowing
        //if a token was invalid before use (e.g. due to expiry). But we don't
        //This is the intended behaviour per AccountManager

        Timber.v(negativeResponseFromOldToken, "Authentication exception, re-trying with new token");
        try {
            attemptOAuth(method, username);
        } catch (NegativeSmtpReplyException negativeResponseFromNewToken) {
            if (negativeResponseFromNewToken.getReplyCode() != SMTP_AUTHENTICATION_FAILURE_ERROR_CODE) {
                throw negativeResponseFromNewToken;
            }

            //Okay, we failed on a new token.
            //Invalidate the token anyway but assume it's permanent.
            Timber.v(negativeResponseFromNewToken, "Authentication exception for new token, permanent error assumed");

            oauthTokenProvider.invalidateToken();

            handlePermanentFailure(negativeResponseFromNewToken);
        }
    }

    private void attemptOAuth(OAuthMethod method, String username) throws MessagingException, IOException {
        String token = oauthTokenProvider.getToken((long) OAuth2TokenProvider.OAUTH2_TIMEOUT);
        String authString = method.buildInitialClientResponse(username, token);
        CommandResponse response = executeSensitiveCommand("%s %s", method.getCommand(), authString);

        if (response.replyCode == SMTP_CONTINUE_REQUEST) {
            String replyText = TextUtils.join(" ", response.results);
            retryOAuthWithNewToken = XOAuth2ChallengeParser.shouldRetry(replyText, mHost);

            //Per Google spec, respond to challenge with empty response
            executeCommand("");
        }
    }

    private void saslAuthExternal(String username) throws MessagingException, IOException {
        executeCommand("AUTH EXTERNAL %s", Base64.encode(username));
    }

    @VisibleForTesting
    protected String getCanonicalHostName(InetAddress localAddress) {
        return localAddress.getCanonicalHostName();
    }

    private enum OAuthMethod {
        XOAUTH2 {
            @Override
            String getCommand() {
                return "AUTH XOAUTH2";
            }

            @Override
            String buildInitialClientResponse(String username, String token) {
                return Authentication.computeXoauth(username, token);
            }
        },
        OAUTHBEARER {
            @Override
            String getCommand() {
                return "AUTH OAUTHBEARER";
            }

            @Override
            String buildInitialClientResponse(String username, String token) {
                return OAuthBearer.buildOAuthBearerInitialClientResponse(username, token);
            }
        };

        abstract String getCommand();
        abstract String buildInitialClientResponse(String username, String token);
    }
}
