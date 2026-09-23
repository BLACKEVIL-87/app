package com.example.gate

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader

class MainActivity : Activity() {
    private lateinit var web: WebView
    private val base = "https://appassets.androidplatform.net/assets/coach.html"

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        web = WebView(this)
        setContentView(web)
        val loader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this)).build()
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.mediaPlaybackRequiresUserGesture = false
        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(v: WebView, r: WebResourceRequest): WebResourceResponse? =
                loader.shouldInterceptRequest(r.url)
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(r: PermissionRequest) {
                runOnUiThread { r.grant(r.resources) }
            }
        }
        web.addJavascriptInterface(Bridge(), "Android")
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 1)
        open(intent)
    }

    override fun onNewIntent(i: Intent) {
        super.onNewIntent(i)
        setIntent(i)
        open(i)
    }

    private fun open(i: Intent) {
        val g = i.getStringExtra("gate")
        web.loadUrl(base + "?t=" + System.currentTimeMillis() + if (g != null) "#gate=$g" else "")
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (intent.hasExtra("gate"))
            startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        else super.onBackPressed()
    }

    inner class Bridge {
        private val p get() = getSharedPreferences("gate", MODE_PRIVATE)

        @JavascriptInterface fun setGated(csv: String) {
            p.edit().putStringSet("gated", csv.split(",").filter { it.isNotBlank() }.toSet()).apply()
        }
        @JavascriptInterface fun grant(pkg: String, min: Int) {
            p.edit().putLong("pass_$pkg", System.currentTimeMillis() + min * 60_000L).apply()
        }
        @JavascriptInterface fun openApp(pkg: String) {
            packageManager.getLaunchIntentForPackage(pkg)?.let { startActivity(it) }
            runOnUiThread { finish() }
        }
        @JavascriptInterface fun a11yOn(): Boolean =
            (Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: "").contains(packageName)
        @JavascriptInterface fun openA11y() {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
