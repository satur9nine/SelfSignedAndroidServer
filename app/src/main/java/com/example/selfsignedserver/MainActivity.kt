package com.example.selfsignedserver

import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main)

        WebView.setWebContentsDebuggingEnabled(true)

        val webview = findViewById<WebView>(R.id.webview);
        webview.settings.javaScriptEnabled = true
        //webview.webViewClient = CheckServerTrustedWebViewClient()

        webview.loadDataWithBaseURL("https://localhost:4433/", readRawTextFile(resources, R.raw.demo),
            "text/html", "UTF-8", null);

        startService(Intent(this, HttpServer::class.java))
    }

    companion object {
        @JvmStatic
        fun readRawTextFile(resources: Resources, id: Int): String {
            return resources.openRawResource(id).bufferedReader().use { it.readText() }
        }
    }

}
