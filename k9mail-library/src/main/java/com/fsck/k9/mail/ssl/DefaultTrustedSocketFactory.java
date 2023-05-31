package com.fsck.k9.mail.ssl;


import android.content.Context;
import android.net.SSLCertificateSocketFactory;
import android.text.TextUtils;

import com.fsck.k9.mail.MessagingException;

import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import timber.log.Timber;


/**
 * Prior to API 21 (and notably from API 10 - 2.3.4) Android weakened it's cipher list
 * by ordering them badly such that RC4-MD5 was preferred. To work around this we 
 * remove the insecure ciphers and reorder them so the latest more secure ciphers are at the top.
 *
 * On more modern versions of Android we keep the system configuration.
 */
public class DefaultTrustedSocketFactory implements TrustedSocketFactory {
    protected static final String[] ENABLED_CIPHERS;
    public static final String TLS_PROTOCOL = "TLSv1.3";


    protected static final String[] BLACKLISTED_CIPHERS = {
            "SSL_RSA_WITH_DES_CBC_SHA",
            "SSL_DHE_RSA_WITH_DES_CBC_SHA",
            "SSL_DHE_DSS_WITH_DES_CBC_SHA",
            "SSL_RSA_EXPORT_WITH_RC4_40_MD5",
            "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
            "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA",
            "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA",
            "SSL_RSA_WITH_3DES_EDE_CBC_SHA",
            "SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA",
            "SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA",
            "TLS_ECDHE_RSA_WITH_RC4_128_SHA",
            "TLS_ECDHE_ECDSA_WITH_RC4_128_SHA",
            "TLS_ECDH_RSA_WITH_RC4_128_SHA",
            "TLS_ECDH_ECDSA_WITH_RC4_128_SHA",
            "SSL_RSA_WITH_RC4_128_SHA",
            "SSL_RSA_WITH_RC4_128_MD5",
            "TLS_ECDH_RSA_WITH_NULL_SHA",
            "TLS_ECDH_anon_WITH_3DES_EDE_CBC_SHA",
            "TLS_ECDH_anon_WITH_NULL_SHA",
            "TLS_ECDH_anon_WITH_RC4_128_SHA",
            "TLS_RSA_WITH_NULL_SHA256"
    };

    static {
        String[] enabledCiphers = null;

        try {
            SSLContext sslContext = SSLContext.getInstance(TLS_PROTOCOL);
            sslContext.init(null, null, null);
            SSLSocketFactory sf = sslContext.getSocketFactory();
            SSLSocket sock = (SSLSocket) sf.createSocket();
            enabledCiphers = sock.getEnabledCipherSuites();
        } catch (Exception e) {
            Timber.e(e, "Error getting information about available SSL/TLS ciphers and protocols");
        }

        ENABLED_CIPHERS = (enabledCiphers == null) ? null :
                remove(enabledCiphers, BLACKLISTED_CIPHERS);

    }

    public DefaultTrustedSocketFactory(Context context) {
        this.context = context;
    }

    protected static String[] reorder(String[] enabled, String[] known, String[] blacklisted) {
        List<String> unknown = new ArrayList<String>();
        Collections.addAll(unknown, enabled);

        // Remove blacklisted items
        if (blacklisted != null) {
            for (String item : blacklisted) {
                unknown.remove(item);
            }
        }

        // Order known items
        List<String> result = new ArrayList<String>();
        for (String item : known) {
            if (unknown.remove(item)) {
                result.add(item);
            }
        }

        // Add unknown items at the end. This way security won't get worse when unknown ciphers
        // start showing up in the future.
        result.addAll(unknown);

        return result.toArray(new String[result.size()]);
    }

    protected static String[] remove(String[] enabled, String[] blacklisted) {
        List<String> items = new ArrayList<String>();
        Collections.addAll(items, enabled);

        // Remove blacklisted items
        if (blacklisted != null) {
            for (String item : blacklisted) {
                items.remove(item);
            }
        }

        return items.toArray(new String[items.size()]);
    }

    private Context context;

    public Socket createSocket(Socket socket, String host, int port, String clientCertificateAlias)
            throws NoSuchAlgorithmException, KeyManagementException, MessagingException, IOException {

        TrustManager[] trustManagers = new TrustManager[] { TrustManagerFactory.get(host, port) };
        KeyManager[] keyManagers = null;
        if (!TextUtils.isEmpty(clientCertificateAlias)) {
            keyManagers = new KeyManager[] { new KeyChainKeyManager(context, clientCertificateAlias) };
        }

        SSLContext sslContext = SSLContext.getInstance(TLS_PROTOCOL);
        sslContext.init(keyManagers, trustManagers, null);
        SSLSocketFactory socketFactory = sslContext.getSocketFactory();
        Socket trustedSocket;
        if (socket == null) {
            trustedSocket = socketFactory.createSocket();
        } else {
            trustedSocket = socketFactory.createSocket(socket, host, port, true);
        }

        SSLSocket sslSocket = (SSLSocket) trustedSocket;

        hardenSocket(sslSocket);

        setSniHost(socketFactory, sslSocket, host);

        return trustedSocket;
    }

    private static void hardenSocket(SSLSocket sock) {
        if (ENABLED_CIPHERS != null) {
            sock.setEnabledCipherSuites(ENABLED_CIPHERS);
        }
    }

    public static void setSniHost(SSLSocketFactory factory, SSLSocket socket, String hostname) {
        if (factory instanceof android.net.SSLCertificateSocketFactory) {
            SSLCertificateSocketFactory sslCertificateSocketFactory = (SSLCertificateSocketFactory) factory;
            sslCertificateSocketFactory.setHostname(socket, hostname);
        } else {
            setHostnameViaReflection(socket, hostname);
        }
    }

    private static void setHostnameViaReflection(SSLSocket socket, String hostname) {
        try {
            socket.getClass().getMethod("setHostname", String.class).invoke(socket, hostname);
        } catch (Throwable e) {
            Timber.e(e, "Could not call SSLSocket#setHostname(String) method ");
        }
    }
}
