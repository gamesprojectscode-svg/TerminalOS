package com.terminalos.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import org.json.JSONObject
import java.net.Inet4Address
import java.net.NetworkInterface

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
            try { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) startActivity(Intent("android.settings.panel.action.WIFI")) else startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) }
            catch (_: Exception) { safeSettings(Settings.ACTION_WIFI_SETTINGS) }
        } }
        @JavascriptInterface fun openBluetooth() { runOnUiThread {
            try { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) startActivity(Intent("android.settings.panel.action.BLUETOOTH")) else startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
            catch (_: Exception) { safeSettings(Settings.ACTION_BLUETOOTH_SETTINGS) }
        } }
        @JavascriptInterface fun openSettings() { runOnUiThread { safeSettings(Settings.ACTION_SETTINGS) } }
        @JavascriptInterface fun deviceInfo(): String = "Android ${Build.VERSION.RELEASE} • ${Build.MANUFACTURER} ${Build.MODEL} • SDK ${Build.VERSION.SDK_INT}"
        @JavascriptInterface fun networkInfo(): String {
            val result = JSONObject()
            try {
                val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                @Suppress("DEPRECATION") val dhcp = wm.dhcpInfo
                @Suppress("DEPRECATION") val info = wm.connectionInfo
                fun ip(n: Int): String = if (n == 0) "" else "${n and 255}.${n shr 8 and 255}.${n shr 16 and 255}.${n shr 24 and 255}"
                result.put("ip", ip(dhcp.ipAddress).ifBlank { currentIpv4() })
                result.put("gateway", ip(dhcp.gateway))
                result.put("dns1", ip(dhcp.dns1))
                result.put("dns2", ip(dhcp.dns2))
                var ssid = "Indisponível (restrição do Android)"
                try { if (info != null && info.ssid != null && info.ssid != "<unknown ssid>") ssid = info.ssid.trim('"') } catch (_: Exception) {}
                result.put("ssid", ssid)
                result.put("password", "Não disponibilizada pelo Android")
            } catch (_: Exception) {
                result.put("ip", currentIpv4()); result.put("gateway", ""); result.put("dns1", ""); result.put("dns2", ""); result.put("ssid", "Indisponível")
            }
            return result.toString()
        }
        @JavascriptInterface fun toast(message: String) { runOnUiThread { Toast.makeText(this@MainActivity, message.take(120), Toast.LENGTH_SHORT).show() } }
    }
    private fun currentIpv4(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val ni = interfaces.nextElement()
                val addresses = ni.inetAddresses
                while (addresses.hasMoreElements()) {
                    val a = addresses.nextElement()
                    if (!a.isLoopbackAddress && a is Inet4Address) return a.hostAddress ?: ""
                }
            }
        } catch (_: Exception) {}
        return "Indisponível"
    }
    private fun safeSettings(action: String) {
        try { startActivity(Intent(action)) }
        catch (_: Exception) { Toast.makeText(this, "Não foi possível abrir essa tela neste aparelho.", Toast.LENGTH_LONG).show() }
    }
    @Deprecated("Deprecated in Android API")
    override fun onBackPressed() { if (::web.isInitialized && web.canGoBack()) web.goBack() else super.onBackPressed() }
}
