package com.lloyd.attendance.campus.auth

import android.content.Context
import android.net.Network
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.lloyd.attendance.campus.diagnostics.CampusConnectionEvent
import com.lloyd.attendance.campus.diagnostics.CampusDiagnosticsStore
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/**
 * Secondary fallback authenticator using Android WebView.
 * Invoked only when DirectHttp indicates [PortalAuthResult.RequiresWebView].
 */
class WebViewPortalAuthenticator(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Attempts interactive/semi-automated authentication within an isolated WebView instance.
     */
    suspend fun authenticateWithWebView(
        network: Network,
        portalUrl: String,
        username: String,
        password: String
    ): PortalAuthResult = suspendCancellableCoroutine { continuation ->

        mainHandler.post {
            val isFinished = AtomicBoolean(false)
            val webView = WebView(context)

            // Bind WebView process to the Wi-Fi network if supported
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
                    connectivityManager?.bindProcessToNetwork(network)
                } catch (ignored: Exception) {
                }
            }

            webView.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadsImagesAutomatically = false // Faster rendering
            }

            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)

                    // Inject credential filling script with safely quoted values
                    val quotedUser = org.json.JSONObject.quote(username)
                    val quotedPass = org.json.JSONObject.quote(password)
                    val js = """
                        (function() {
                            try {
                                var userInputs = document.querySelectorAll('input[type="text"], input[type="email"], input:not([type])');
                                var passInputs = document.querySelectorAll('input[type="password"]');
                                if (userInputs.length > 0 && passInputs.length > 0) {
                                    userInputs[0].value = $quotedUser;
                                    passInputs[0].value = $quotedPass;
                                    var submitBtn = document.querySelector('button[type="submit"], input[type="submit"], button');
                                    if (submitBtn) {
                                        submitBtn.click();
                                        return "SUBMITTED";
                                    }
                                }
                                return "NO_FORM";
                            } catch (e) {
                                return "ERROR: " + e;
                            }
                        })();
                    """.trimIndent()

                    view?.evaluateJavascript(js) { result ->
                        CampusDiagnosticsStore.instance.record(
                            CampusConnectionEvent.Type.DIRECT_HTTP_ATTEMPT,
                            "WebView JS Form Fill",
                            "Result: $result on URL: $url"
                        )
                    }
                }

                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val currentUrl = request?.url?.toString().orEmpty()
                    if (currentUrl.contains("google.com") || currentUrl.contains("generate_204")) {
                        if (isFinished.compareAndSet(false, true)) {
                            cleanupWebView(webView)
                            continuation.resume(PortalAuthResult.Success("WebView navigated to open internet"))
                            return true
                        }
                    }
                    return false
                }
            }

            // Set safety timeout of 20 seconds
            mainHandler.postDelayed({
                if (isFinished.compareAndSet(false, true)) {
                    cleanupWebView(webView)
                    continuation.resume(
                        PortalAuthResult.TransientFailure("WebView authentication timed out", retryAfterSeconds = 10)
                    )
                }
            }, 20_000L)

            continuation.invokeOnCancellation {
                mainHandler.post {
                    cleanupWebView(webView)
                }
            }

            webView.loadUrl(portalUrl)
        }
    }

    private fun cleanupWebView(webView: WebView) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
                connectivityManager?.bindProcessToNetwork(null)
            } catch (ignored: Exception) {
            }
        }
        try {
            webView.destroy()
        } catch (ignored: Exception) {
        }
    }
}
