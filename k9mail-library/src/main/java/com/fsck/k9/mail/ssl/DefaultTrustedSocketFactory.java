package com.fsck.k9.mail.ssl;


import android.content.Context;
import android.net.SSLCertificateSocketFactory;
import android.text.TextUtils;

import com.fsck.k9.mail.MessagingException;

import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;


/**
 * Prior to API 21 (and notably from API 10 - 2.3.4) Android weakened it's cipher list
 * by ordering them badly such that RC4-MD5 was preferred. To work around this we 
 * remove the insecure ciphers and reorder them so the latest more secure ciphers are at the top.
 *
 * On more modern versions of Android we keep the system configuration.
 */
public class DefaultTrustedSocketFactory implements TrustedSocketFactory {
    public static final String TLS_PROTOCOL = "TLSv1.3";

    public DefaultTrustedSocketFactory(Context context) {
        this.context = context;
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

        setSniHost(socketFactory, sslSocket, host);

        return trustedSocket;
    }

    public static void setSniHost(SSLSocketFactory factory, SSLSocket socket, String hostname) {
        if (factory instanceof android.net.SSLCertificateSocketFactory) {
            SSLCertificateSocketFactory sslCertificateSocketFactory = (SSLCertificateSocketFactory) factory;
            sslCertificateSocketFactory.setHostname(socket, hostname);
        } else {
            SSLParameters sslParameters = socket.getSSLParameters();
            List<SNIServerName> sniServerNames = Collections.singletonList(new SNIHostName(hostname));
            sslParameters.setServerNames(sniServerNames);
            socket.setSSLParameters(sslParameters);
        }
    }
}
