package de.mm20.launcher2.activity

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Color
import android.view.Gravity
import android.view.inputmethod.EditorInfo

/** Lightweight desktop-site browser for the launcher desktop workspace. */
class DesktopBrowserActivity : Activity() {
    private lateinit var web: WebView
    private lateinit var address: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Desktop Browser"
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(24, 27, 33))
        }
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(10, 8, 10, 8)
        }
        val back = TextView(this).apply { text = "‹"; textSize = 28f; setTextColor(Color.WHITE); setPadding(8, 0, 14, 0); setOnClickListener { if (this@DesktopBrowserActivity::web.isInitialized && web.canGoBack()) web.goBack() } }
        address = EditText(this).apply {
            singleLine = true
            textSize = 14f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            hint = "Search or enter website"
            imeOptions = EditorInfo.IME_ACTION_GO
            setPadding(12, 4, 12, 4)
            setOnEditorActionListener { _, action, _ ->
                if (action == EditorInfo.IME_ACTION_GO) { loadAddress(text.toString()); true } else false
            }
        }
        bar.addView(back)
        bar.addView(address, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(bar)
        web = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.userAgentString = settings.userAgentString.replace("Mobile", "X11; Linux x86_64").replace("Android", "X11; Linux x86_64")
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) { address.setText(url ?: "") }
            }
            webChromeClient = WebChromeClient()
            setBackgroundColor(Color.WHITE)
        }
        root.addView(web, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
        DesktopRefreshRateController.requestMaximumForWindow(this)
        loadAddress(intent.getStringExtra(EXTRA_URL) ?: "https://www.youtube.com")
    }

    override fun onResume() {
        super.onResume()
        DesktopRefreshRateController.reapply(this)
    }

    private fun loadAddress(raw: String) {
        val value = raw.trim()
        if (value.isEmpty()) return
        val url = if (value.contains('.') && !value.contains(' ')) {
            if (value.startsWith("http://") || value.startsWith("https://")) value else "https://$value"
        } else "https://www.google.com/search?q=" + android.net.Uri.encode(value)
        web.loadUrl(url)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && this::web.isInitialized && web.canGoBack()) { web.goBack(); return true }
        if (event?.isCtrlPressed == true) {
            when (keyCode) {
                KeyEvent.KEYCODE_L -> { address.requestFocus(); return true }
                KeyEvent.KEYCODE_R -> { web.reload(); return true }
                KeyEvent.KEYCODE_W -> { finish(); return true }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() { if (this::web.isInitialized) web.destroy(); super.onDestroy() }

    companion object { const val EXTRA_URL = "desktop_browser_url" }
}
