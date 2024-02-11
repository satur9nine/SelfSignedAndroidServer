package com.example.selfsignedserver;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLServerSocketFactory;

import fi.iki.elonen.NanoHTTPD;

public class HttpServer extends Service {

    private HttpServerFinisher finisher;

    public static final String TAG = HttpServer.class.getSimpleName();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    interface HttpServerFinisher {
        void stop();
    }

    private static final String KEY_PASSWORD = "changeit";

    private KeyStore createKeyStore() {
        try {
            Certificate serverCertificate = parseCert(MainActivity.readRawTextFile(getResources(), R.raw.server2048_crt));
            PrivateKey privateKey = loadPrivateKey(MainActivity.readRawTextFile(getResources(), R.raw.server2048_key));

            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(null, null);
            keyStore.setCertificateEntry("cert", serverCertificate);
            keyStore.setKeyEntry("key", privateKey, KEY_PASSWORD.toCharArray(),
                    new Certificate[]{serverCertificate});
            return keyStore;
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Cannot build keystore", e);
        }
    }

    private PrivateKey loadPrivateKey(String privateKeyPem) throws IOException, GeneralSecurityException {
        return pemLoadPrivateKeyPkcs1OrPkcs8Encoded(privateKeyPem);
    }

    public static X509Certificate parseCert(String pem) {
        return parseCert(pem.getBytes());
    }

    public static X509Certificate parseCert(byte[] der) {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(der));
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static PrivateKey pemLoadPrivateKeyPkcs1OrPkcs8Encoded(String privateKeyPem) throws GeneralSecurityException, IOException {
        // PKCS#8 format
        final String PEM_PRIVATE_START = "-----BEGIN PRIVATE KEY-----";
        final String PEM_PRIVATE_END = "-----END PRIVATE KEY-----";


        if (privateKeyPem.contains(PEM_PRIVATE_START)) { // PKCS#8 format
            privateKeyPem = privateKeyPem.replace(PEM_PRIVATE_START, "").replace(PEM_PRIVATE_END, "");
            privateKeyPem = privateKeyPem.replaceAll("\\s", "");

            byte[] pkcs8EncodedKey = Base64.decode(privateKeyPem, 0);

            KeyFactory factory = KeyFactory.getInstance("RSA");
            return factory.generatePrivate(new PKCS8EncodedKeySpec(pkcs8EncodedKey));
        }
        throw new GeneralSecurityException("Not supported format of a private key");
    }

    private HttpServerFinisher testTlsPskNanoHttpdWithBcFactory() throws Exception {
        Log.i(TAG, "Starting server");

        NanoHTTPD nano = new NanoHTTPD(4433) {
            @Override
            public Response serve(IHTTPSession session) {
                Log.i(TAG, "Got hit");

                Map<String, String> parameters = new HashMap<>();
                try {
                    // Must consume the body
                    session.parseBody(parameters);
                } catch (IOException e) {
                    return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "SERVER INTERNAL ERROR: IOException: " + e.getMessage());
                } catch (ResponseException e) {
                    return newFixedLengthResponse(e.getStatus(), NanoHTTPD.MIME_PLAINTEXT, e.getMessage());
                }
                Response resp = newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, "Howdy");
                resp.addHeader("Access-Control-Allow-Origin", "*");
                return resp;
            }
        };

        KeyStore ks = createKeyStore();
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("X509");
        keyManagerFactory.init(ks, KEY_PASSWORD.toCharArray());

        SSLServerSocketFactory factory = NanoHTTPD.makeSSLSocketFactory(ks, keyManagerFactory.getKeyManagers());

        // Setting sslProtocols param to null tells NanoHTTPD to allow all protocols supported by the ServerSocketFactory
        nano.makeSecure(factory, null);

        nano.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);

        Log.i(TAG, "Server started");

        return nano::stop;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (finisher == null) {
            try {
                finisher = testTlsPskNanoHttpdWithBcFactory();
            } catch (Exception e) {
                Log.w(TAG, e);
            }
        }
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (finisher != null) {
            finisher.stop();
            finisher = null;
        }
        super.onDestroy();
    }
}
