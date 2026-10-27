package com.terminalos.app

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var web: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.allowFileAccess = true
        web.webViewClient = WebViewClient()
        web.webChromeClient = WebChromeClient()
        web.addJavascriptInterface(AndroidBridge(), "TerminalOS")
        setContentView(web)
        web.loadUrl("file:///android_asset/index.html")
    }

    inner class AndroidBridge {
        @JavascriptInterface fun openWifi() { runOnUiThread {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startActivity(Intent("android.settings.panel.action.WIFI"))
                } else startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            } catch (_: Exception) { safeSettings(Settings.ACTION_WIFI_SETTINGS) }
        } }

        @JavascriptInterface fun openBluetooth() { runOnUiThread {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startActivity(Intent("android.settings.panel.action.BLUETOOTH"))
                } else startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            } catch (_: Exception) { safeSettings(Settings.ACTION_BLUETOOTH_SETTINGS) }
        } }

        @JavascriptInterface fun openSettings() { runOnUiThread { safeSettings(Settings.ACTION_SETTINGS) } }
        @JavascriptInterface fun deviceInfo(): String = "Android ${Build.VERSION.RELEASE} • ${Build.MANUFACTURER} ${Build.MODEL}"
        @JavascriptInterface fun toast(message: String) { runOnUiThread { Toast.makeText(this@MainActivity, message.take(120), Toast.LENGTH_SHORT).show() } }
    }

    private fun safeSettings(action: String) {
        try { startActivity(Intent(action)) }
        catch (_: Exception) { Toast.makeText(this, "Não foi possível abrir essa tela neste aparelho.", Toast.LENGTH_LONG).show() }
    }

    @Deprecated("Deprecated in Android API")
    override fun onBackPressed() {
        if (::web.isInitialized && web.canGoBack()) web.goBack() else super.onBackPressed()
    }
}
