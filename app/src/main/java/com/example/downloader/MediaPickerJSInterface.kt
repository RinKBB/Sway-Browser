package com.example.downloader

import android.webkit.JavascriptInterface
import android.util.Log

class MediaPickerJSInterface(
    private val onDiscovered: (jsonArrayStr: String) -> Unit,
    private val onError: (message: String) -> Unit,
    private val onSpaUrlChanged: ((url: String) -> Unit)? = null
) {
    @JavascriptInterface
    fun onMediaDiscovered(jsonStr: String) {
        Log.d("MediaPickerJSInterface", "Discovered media results: ${jsonStr.take(500)}...")
        onDiscovered(jsonStr)
    }

    @JavascriptInterface
    fun logError(message: String) {
        Log.e("MediaPickerJSInterface", "JS Context Error: $message")
        onError(message)
    }

    @JavascriptInterface
    fun onSpaUrlChanged(url: String) {
        Log.d("MediaPickerJSInterface", "SPA URL Changed: $url")
        onSpaUrlChanged?.invoke(url)
    }
}
