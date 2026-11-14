package com.lloyd.attendance.feature.receipts

import android.content.Context
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.google.gson.JsonParser
import com.lloyd.attendance.api.ErpApiClient
import java.io.FileOutputStream

object ReceiptPrintAdapter {

    // Retain strong reference to prevent WebView garbage collection during background print spooling
    private var activeWebView: WebView? = null

    /**
     * Prints the official receipt from the ERP website.
     * If the server returned an exact binary PDF, it sends raw PDF bytes directly to the
     * PrintManager with zero conversion or corruption.
     * If the server returned HTML, it renders and prints via a properly-retained WebView.
     * ZERO developer branding is injected.
     */
    fun printReceipt(
        context: Context,
        receiptData: ErpApiClient.ReceiptData,
        jobName: String = "Lloyd_Fee_Receipt"
    ) {
        if (receiptData.isPdf && receiptData.pdfBytes != null && receiptData.pdfBytes.isNotEmpty()) {
            printDirectPdf(context, receiptData.pdfBytes, jobName)
        } else if (!receiptData.htmlContent.isNullOrBlank()) {
            printHtml(context, receiptData.htmlContent, jobName)
        } else {
            throw IllegalArgumentException("No receipt content available to print")
        }
    }

    /**
     * Directly streams exact binary PDF bytes from the ERP server into Android's print spooler.
     * Guarantees 100% byte-for-byte fidelity with the website ERP receipt.
     */
    fun printDirectPdf(
        context: Context,
        pdfBytes: ByteArray,
        jobName: String = "Lloyd_Fee_Receipt"
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            ?: throw IllegalStateException("Print service is not available on this device")

        val printAttributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setResolution(PrintAttributes.Resolution("res_id", "Default", 300, 300))
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()

        printManager.print(
            jobName,
            object : PrintDocumentAdapter() {
                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes?,
                    cancellationSignal: CancellationSignal?,
                    callback: LayoutResultCallback?,
                    extras: Bundle?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onLayoutCancelled()
                        return
                    }
                    val info = PrintDocumentInfo.Builder(jobName)
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                        .build()
                    callback?.onLayoutFinished(info, true)
                }

                override fun onWrite(
                    pages: Array<out PageRange>?,
                    destination: ParcelFileDescriptor?,
                    cancellationSignal: CancellationSignal?,
                    callback: WriteResultCallback?
                ) {
                    if (destination == null) {
                        callback?.onWriteFailed("Print destination descriptor is null")
                        return
                    }
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onWriteCancelled()
                        return
                    }
                    try {
                        FileOutputStream(destination.fileDescriptor).use { out ->
                            out.write(pdfBytes)
                            out.flush()
                        }
                        callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        callback?.onWriteFailed(e.message ?: "Failed to write PDF receipt")
                    }
                }
            },
            printAttributes
        )
    }

    /**
     * Prints the official receipt HTML using Android's PrintManager and an offscreen WebView.
     * Retains WebView instance across print spooling to prevent empty/corrupted PDFs.
     */
    fun printHtml(
        context: Context,
        htmlContent: String,
        jobName: String = "Lloyd_Fee_Receipt"
    ) {
        val sanitizedHtml = preparePrintableHtml(htmlContent, jobName)

        val webView = WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadsImagesAutomatically = true
                useWideViewPort = true
                loadWithOverviewMode = true
            }
        }
        activeWebView = webView

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false

            override fun onPageFinished(view: WebView, url: String) {
                // Post with 500ms delay to let fonts, CSS and layout settle
                Handler(Looper.getMainLooper()).postDelayed({
                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                    if (printManager == null) {
                        activeWebView = null
                        return@postDelayed
                    }

                    val adapter = view.createPrintDocumentAdapter(jobName)
                    val printAttributes = PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .setResolution(PrintAttributes.Resolution("res_id", "Default", 300, 300))
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                        .build()

                    printManager.print(
                        jobName,
                        object : PrintDocumentAdapter() {
                            override fun onLayout(
                                oldAttributes: PrintAttributes?,
                                newAttributes: PrintAttributes?,
                                cancellationSignal: CancellationSignal?,
                                callback: LayoutResultCallback?,
                                extras: Bundle?
                            ) {
                                adapter.onLayout(oldAttributes, newAttributes, cancellationSignal, callback, extras)
                            }

                            override fun onWrite(
                                pages: Array<out PageRange>?,
                                destination: ParcelFileDescriptor?,
                                cancellationSignal: CancellationSignal?,
                                callback: WriteResultCallback?
                            ) {
                                adapter.onWrite(pages, destination, cancellationSignal, callback)
                            }

                            override fun onFinish() {
                                super.onFinish()
                                adapter.onFinish()
                                activeWebView = null
                            }
                        },
                        printAttributes
                    )
                }, 500L)
            }
        }

        webView.loadDataWithBaseURL("https://erp.lloydcollege.in", sanitizedHtml, "text/html", "UTF-8", null)
    }

    /**
     * Unwraps JSON envelopes if the server returns HTML inside a JSON payload,
     * and injects clean A4 print styling.
     */
    private fun preparePrintableHtml(rawContent: String, title: String): String {
        val trimmed = rawContent.trim()
        var html = trimmed

        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            try {
                val element = JsonParser.parseString(trimmed)
                if (element.isJsonObject) {
                    val obj = element.asJsonObject
                    when {
                        // ERP returns: {"status":true,"data":{"log_id":...,"html":"..."}}
                        obj.has("data") && obj.get("data").isJsonObject -> {
                            val dataObj = obj.getAsJsonObject("data")
                            html = when {
                                dataObj.has("html") && dataObj.get("html").isJsonPrimitive
                                    -> dataObj.get("html").asString
                                dataObj.has("content") && dataObj.get("content").isJsonPrimitive
                                    -> dataObj.get("content").asString
                                else -> html
                            }
                        }
                        obj.has("data") && obj.get("data").isJsonPrimitive
                            && obj.get("data").asJsonPrimitive.isString ->
                            html = obj.get("data").asString
                        obj.has("html") && obj.get("html").isJsonPrimitive
                            && obj.get("html").asJsonPrimitive.isString ->
                            html = obj.get("html").asString
                        obj.has("content") && obj.get("content").isJsonPrimitive
                            && obj.get("content").asJsonPrimitive.isString ->
                            html = obj.get("content").asString
                    }
                }
            } catch (_: Exception) {}
        }

        val printCss = """
            <style>
                @page {
                    size: A4 portrait;
                    margin: 10mm 12mm;
                }
                html, body {
                    width: 100% !important;
                    height: auto !important;
                    background: #FFFFFF !important;
                    color: #1a1a1a !important;
                    font-family: "Segoe UI", Roboto, Helvetica, Arial, sans-serif !important;
                    font-size: 11pt !important;
                    margin: 0 !important;
                    padding: 0 !important;
                    box-sizing: border-box !important;
                    line-height: 1.5 !important;
                }
                table {
                    width: 100% !important;
                    border-collapse: collapse !important;
                    margin-bottom: 8px !important;
                }
                th, td {
                    padding: 6px 10px !important;
                    border: 1px solid #cccccc !important;
                    text-align: left !important;
                    font-size: 10.5pt !important;
                    vertical-align: top !important;
                    word-break: break-word !important;
                }
                th {
                    background-color: #f0f0f0 !important;
                    font-weight: 600 !important;
                }
                tr:nth-child(even) td {
                    background-color: #fafafa !important;
                }
                h1, h2, h3, h4 {
                    margin: 8px 0 4px 0 !important;
                    font-weight: 700 !important;
                }
                img {
                    max-width: 100% !important;
                    height: auto !important;
                }
                @media print {
                    body {
                        -webkit-print-color-adjust: exact !important;
                        print-color-adjust: exact !important;
                    }
                    .no-print, button, a[href^="javascript"],
                    input[type="button"], input[type="submit"] {
                        display: none !important;
                    }
                    table { page-break-inside: avoid !important; }
                }
            </style>
        """.trimIndent()

        return if (html.contains("<head>", ignoreCase = true)) {
            html.replaceFirst("(?i)<head>".toRegex(), "<head>$printCss")
        } else if (html.contains("<html>", ignoreCase = true)) {
            html.replaceFirst("(?i)<html>".toRegex(), "<html><head>$printCss</head>")
        } else {
            """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>$title</title>
                $printCss
            </head>
            <body>
                $html
            </body>
            </html>
            """.trimIndent()
        }
    }
}
