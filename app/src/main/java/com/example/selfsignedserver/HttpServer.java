package com.example.selfsignedserver;

import static fi.iki.elonen.NanoHTTPD.newFixedLengthResponse;

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
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import fi.iki.elonen.NanoHTTPD;

public class HttpServer extends Service {

    public static final String TAG = HttpServer.class.getSimpleName();

    public static final String SERVER_CERT_PEM = "-----BEGIN CERTIFICATE-----\n" +
            "MIIEHTCCAoUCFBOy3WJdWO+ZdLgB8xREVaLGeMgjMA0GCSqGSIb3DQEBCwUAMIGJ\n" +
            "MQswCQYDVQQGEwJVUzELMAkGA1UECAwCQ0ExEjAQBgNVBAcMCVNhbiBNYXRlbzEO\n" +
            "MAwGA1UECgwFSmFjb2IxFDASBgNVBAsMC1RMUyBUZXN0IENBMRIwEAYDVQQDDAls\n" +
            "b2NhbGhvc3QxHzAdBgkqhkiG9w0BCQEWEGphY29iQGNsb3Zlci5jb20wHhcNMjQw\n" +
            "MjEwMDQzMzI5WhcNNDQwMjA1MDQzMzI5WjCBizELMAkGA1UEBhMCVVMxCzAJBgNV\n" +
            "BAgMAkNBMRIwEAYDVQQHDAlTYW4gTWF0ZW8xDjAMBgNVBAoMBUphY29iMRYwFAYD\n" +
            "VQQLDA1UTFMgVGVzdCAyMDQ4MRIwEAYDVQQDDAlsb2NhbGhvc3QxHzAdBgkqhkiG\n" +
            "9w0BCQEWEGphY29iQGNsb3Zlci5jb20wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAw\n" +
            "ggEKAoIBAQCfDdZCvQW3VGnkBGevWAps/gOza64T7Qb4wItit7+4YezYMyxE+1Tt\n" +
            "JGflT6QRzuiFURrF3MIQsr8017TezCm5+eRht612PQduOih289YApcXucBEDJlk4\n" +
            "cLfex/uND7ewTfV120DSCvBLaE1+VISLHve8Mw1RbgYpNC6bOy67uPRaDWjl8E5O\n" +
            "fksQ/Y63SukS+e2MNdnsmI91b/GxIerfEq03kdo/kjPVaZqoJxbjFUBdCUUAZsdA\n" +
            "FTQBRY1pUmSsxNz+ZuxpZAb5dbXpRt8qvSjBQCtP7YZhnexAse/GMnRUI9mQDZRN\n" +
            "mNKCwwneoj9rVuESyaIxCyPntsZxRPrLAgMBAAEwDQYJKoZIhvcNAQELBQADggGB\n" +
            "AJQRBCy620I+/1tztYzqSdgY5RgYaBkt8h9Kl06Xbb+Yr1/aPXHQG741jYGFuIQE\n" +
            "mACnMvhHzCwOoZUotG2FYEiKmi5KeufSDrOL1Pj6E3EtIX+UBGbeg/YpPy54iesQ\n" +
            "7+LW93OAkloWEfboSmHOJeKH2QaFTOY7mIvrLFiyh5I+2SkwMVSZ1syp1Dksy+1v\n" +
            "H5TJKCnOHQL/xlOMZy+2F5+lIAKNvuDqBOao60JkxDnYDl5ywsBBK/vYLJUeVg8C\n" +
            "nkQg9Vb0ibJq8lFrhdrTf05TIHH7jazZa9x+KBZZq/oLiR9bVIFznHtaulbHCT3r\n" +
            "XMN4WA7qzOric99RwfxuRyccyK4/u/u3EfKJt5dBbx5NhTZ9MOyR9DY59NFeFzyh\n" +
            "kG6ty6GXVu2/+lh4QrkCshl29UISQrj5oGuHg7QH1cX31u69zxtN2MPlXNDeZiOz\n" +
            "BgTsXSPl6lkZQA39gvTz3moyLMgrbWpk6JGca1PocwHuIXInmD1devdhuNcdyUyM\n" +
            "1g==\n" +
            "-----END CERTIFICATE-----";

    public static final String SERVER_PRIVATE_KEY_PEM = "-----BEGIN PRIVATE KEY-----\n" +
            "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCfDdZCvQW3VGnk\n" +
            "BGevWAps/gOza64T7Qb4wItit7+4YezYMyxE+1TtJGflT6QRzuiFURrF3MIQsr80\n" +
            "17TezCm5+eRht612PQduOih289YApcXucBEDJlk4cLfex/uND7ewTfV120DSCvBL\n" +
            "aE1+VISLHve8Mw1RbgYpNC6bOy67uPRaDWjl8E5OfksQ/Y63SukS+e2MNdnsmI91\n" +
            "b/GxIerfEq03kdo/kjPVaZqoJxbjFUBdCUUAZsdAFTQBRY1pUmSsxNz+ZuxpZAb5\n" +
            "dbXpRt8qvSjBQCtP7YZhnexAse/GMnRUI9mQDZRNmNKCwwneoj9rVuESyaIxCyPn\n" +
            "tsZxRPrLAgMBAAECggEAQ943IVYfxMj/wByDE6ZZGIXRFXOqnXUEBwu9zjtC0w3x\n" +
            "7pTzXGH0LJZewNHZXAHyZMSlTG1034QuI4Gxe2oZmfI/2dRy72N+MrIdRxinpbIW\n" +
            "wbXPBI2N/4/VXXnu5BOjWVcCmK4cPCQPwdV5T6EHx4dIxWTxV2A/v8A07Ob1e4L1\n" +
            "ggEGJZaVa9b5EpTfBzVBpAknv/dq9ydSRMKmQXHiC1JLNpMMIJOCYsyRQPqmyWBC\n" +
            "Dlee2Z2csXX53VrJOQpuUP6afmvlQFie0C6tW6EJAGErA77CEPURZR2or+3hWBd2\n" +
            "DQ/ybZsJngJgeoOP94JGv1+Zp9zuNijpB5Mzb8ztkQKBgQDNN/VX1e+MpZ4eOVQh\n" +
            "faOsLJnyWiY4YJhgGf+pJbj4/DKUaE/ihWsEhz3UZy/TKEznu1EqfyHjmIYhivVm\n" +
            "hScGpQ+6RflgZG4nyGW/rl3jv2QAMt65aZzVR76vDfXl73gDbXNMCvqOPwlfYiNo\n" +
            "NADTMXE2LR3qsI2HNmQNyLlYUQKBgQDGaXrwJFxBWMnwlSoQbiWd3H6a2SkYRT0Z\n" +
            "eQIeUG0soOjoY2W8pc+DSRvnmTYQ+0oD5tMX/WnCuH46wKQxmoU0uKVRGxmua21y\n" +
            "LEZPv7JR0ed4WOcASUYS8A3PQuxvgi3v35Jz+UuM6S2bXxfe+StHCOdiPjryZXYE\n" +
            "R7cSi8m2WwKBgQCBL8Da0mpCd9/Z1u0HSDhIoqsmzWlSBDzoAnXW3VBcLScKFcB0\n" +
            "MNj1uM1LcMnCe5QuLNUjk3SZ+eI2K2vgZeHzZOVJtdMOwyo1EQo8aF/ihxFErsFW\n" +
            "pw17lfaL7JXncaBzR+tU63RfJ3+W9AimCFacnHtQR4aVvx7ZB3xk0P8cgQKBgC6m\n" +
            "hFpMIkjXCHwBhmdgjoWkXzwnTQtA1FHV6tKX1GOG3dwt6rDFR5o1qVL16glqHAf4\n" +
            "0K82TRSUblGAE9r52tH/jBcayRoCdjQ/BrUffFzSfpsERCQEFm7DdGvD73V0ZTqe\n" +
            "FYAhIEtcU/XREPrDGRLVnBm0SgXOJ0ZwWjIJHf87AoGAcPufvOtwuG+NQ5DVBqu/\n" +
            "GcSfFQWEcsytLMrs/mvW/IPks0u7HXIEU2XYFssYlWBwiK5DzAuEGZCDm8O8Wv7n\n" +
            "VNQLfBP1SwXV6Mv7TsrHFrwh0pFqiT+BJFOYMyC2qY8rtXg9l0yBISJihwxFavZS\n" +
            "w+5P+yGzJjud2fHayfqoKHo=\n" +
            "-----END PRIVATE KEY-----";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    interface HttpServerFinisher {
        void stop();
    }

    private static final String KEY_PASSWORD = "changeit";

    private KeyStore getKeyStore() {
        try {
            Certificate clientCertificate = parseCert(SERVER_CERT_PEM);
            PrivateKey privateKey = loadPrivateKey(SERVER_PRIVATE_KEY_PEM);

            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(null, null);
            keyStore.setCertificateEntry("cert", clientCertificate);
            keyStore.setKeyEntry("key", privateKey, KEY_PASSWORD.toCharArray(), new Certificate[]{clientCertificate});
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

            byte[] pkcs8EncodedKey = Base64.getDecoder().decode(privateKeyPem);

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
                return newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, "Howdy");
            }
        };

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("X509");
        keyManagerFactory.init(getKeyStore(), KEY_PASSWORD.toCharArray());

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), null, new SecureRandom());

        // Setting sslProtocols param to null tells NanoHTTPD to allow all protocols supported by the ServerSocketFactory
        nano.makeSecure(sslContext.getServerSocketFactory(), null);

        nano.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);

        Log.i(TAG, "Server started");

        return nano::stop;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            testTlsPskNanoHttpdWithBcFactory();
        } catch (Exception e) {
            Log.w(TAG, e);
        }
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
