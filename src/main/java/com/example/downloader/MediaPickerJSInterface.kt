package com.example.downloader

import android.webkit.JavascriptInterface
import android.util.Log

class MediaPickerJSInterface(
    private val onDiscovered: (jsonArrayStr: String) -> Unit,
    private val onError: (message: String) -> Unit,
    private val onSpaUrlChanged: ((url: String) -> Unit)? = null,
    private val onAdBlocked: (() -> Unit)? = null
) {
    @JavascriptInterface
    fun reportAdBlocked() {
        Log.d("MediaPickerJSInterface", "Ad blocked or skipped reported from JS")
        onAdBlocked?.invoke()
    }

    @JavascriptInterface
    fun onMediaDiscovered(jsonStr: String?) {
        if (jsonStr == null) return
        
        // Ограничение размера входного JSON для предотвращения исчерпания памяти (DoS)
        if (jsonStr.length > 5 * 1024 * 1024) { // Максимум 5 MB
            Log.e("MediaPickerJSInterface", "Security Violation: JSON payload size exceeds limit!")
            return
        }
        
        Log.d("MediaPickerJSInterface", "Discovered media results: ${jsonStr.take(500)}...")
        onDiscovered(jsonStr)
    }

    @JavascriptInterface
    fun logError(message: String?) {
        val safeMessage = message ?: "Unknown JS error"
        Log.e("MediaPickerJSInterface", "JS Context Error: $safeMessage")
        onError(safeMessage)
    }

    @JavascriptInterface
    fun onSpaUrlChanged(url: String?) {
        if (url == null) return
        if (url.length > 2048) { // Стандартный предел URL
            Log.e("MediaPickerJSInterface", "Security Warning: SPA URL is too long")
            return
        }
        Log.d("MediaPickerJSInterface", "SPA URL Changed: $url")
        onSpaUrlChanged?.invoke(url)
    }
}

