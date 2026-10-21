package com.lloyd.attendance.feature.receipts

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ReceiptPrintAdapter {

    /**
     * Prints the official receipt HTML using Android's PrintManager and an offscreen WebView.
     * ZERO developer branding is injected.
     */
    fun printHtml(
        context: Context,
        htmlContent: String,
        jobName: String = "Lloyd_Fee_Receipt"
    ) {
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false
            }

            override fun onPageFinished(view: WebView, url: String) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                    ?: return

                val printAdapter = webView.createPrintDocumentAdapter(jobName)
                val printAttributes = PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setResolution(PrintAttributes.Resolution("id", "res", 300, 300))
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build()

                printManager.print(jobName, printAdapter, printAttributes)
            }
        }

        // Load HTML with base URL
        webView.loadDataWithBaseURL("https://erp.lloydcollege.in", htmlContent, "text/html", "UTF-8", null)
    }
}
