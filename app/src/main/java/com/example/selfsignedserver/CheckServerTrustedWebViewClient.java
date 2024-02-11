package com.example.selfsignedserver;

import android.net.http.SslCertificate;
import android.net.http.SslError;
import android.util.Log;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class CheckServerTrustedWebViewClient extends WebViewClient {
    public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {
        boolean passVerify = true;

//        if (error.getPrimaryError() == SslError.SSL_UNTRUSTED) {
//            SslCertificate cert = error.getCertificate();
//            try{
//                X509Certificate[] chain = {x509};
//                for (TrustManager trustManager: tmf.getTrustManagers()) {
//                    if (trustManager instanceof X509TrustManager) {
//                        X509TrustManager x509TrustManager = (X509TrustManager)trustManager;
//                        try {
//                            x509TrustManager.checkServerTrusted(chain, "generic");
//                            passVerify = true;break;
//                        } catch (Exception e) {
//                            passVerify = false;
//                        }
//                    }
//                }
//            } catch(Exception e) {
//                throw new RuntimeException(e);
//            }
//        }
        if (passVerify == true) {
            handler.proceed();
        } else {
            handler.cancel();
        }

    }

}
