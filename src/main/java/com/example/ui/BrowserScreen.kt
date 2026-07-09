package com.example.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tab
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.draw.scale
import kotlin.math.roundToInt
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.BackgroundPlayWebView
import com.example.BackgroundAudioService
import com.example.downloader.MediaPickerJSInterface
import com.example.model.MediaItem
import com.example.model.SearchImage
import com.example.viewmodel.BrowserViewModel
import com.example.viewmodel.DownloadProgressState
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

private const val JS_CRAWLER_CODE = """
(function() {
    try {
        window.urlCheck = window.urlCheck || {};
        
        function getBestFromSrcset(srcset) {
            if (!srcset) return null;
            try {
                var candidates = srcset.split(',');
                var bestUrl = null;
                var maxDim = -1;
                for (var c = 0; c < candidates.length; c++) {
                    var part = candidates[c].trim();
                    var parts = part.split(/\s+/);
                    if (parts.length > 0) {
                        var url = parts[0];
                        var dim = 0;
                        if (parts.length > 1) {
                            var dimStr = parts[1].toLowerCase();
                            if (dimStr.endsWith('w')) {
                                dim = parseInt(dimStr.substring(0, dimStr.length - 1)) || 0;
                            } else if (dimStr.endsWith('x')) {
                                dim = (parseFloat(dimStr.substring(0, dimStr.length - 1)) || 1) * 300;
                            }
                        }
                        if (dim > maxDim) {
                            maxDim = dim;
                            bestUrl = url;
                        }
                    }
                }
                return bestUrl;
            } catch(e){}
            return null;
        }

        function extract() {
            var items = [];
            
            function addMedia(url, type, ext, tag, extra) {
                if (!url) return;
                try {
                    var absUrl = new URL(url, window.location.href).href;
                    if (window.urlCheck[absUrl]) return;
                    
                    var urlLower = absUrl.toLowerCase();
                    // Extensive blacklist for non-meaningful layout elements, trackers, icons, and UI assets
                    var blacklist = [
                        'google-analytics', 'googletagmanager', 'facebook.com/tr', 'doubleclick', 'adsystem',
                        'tracking', '/pixel', 'spacer.gif', 'favicon', 'wp-content/themes', 'wp-content/plugins',
                        '/assets/images/icons/', '/spinner', 'star-rating', 'arrow', 'captcha', 'badge',
                        'loader', 'loading', 'ajax', 'metric', 'telemetry', 'analytics', 'advertisement',
                        'banner', 'widget', 'button', 'share-button', 'social', 'sprite', 'decor',
                        'background-pattern', 'placeholder', 'blank', 'transparent', 'pixel.gif',
                        'dot.gif', 'cleardot', 'shim', 'load.gif', 'indicator', 'close_icon', 'menu_icon',
                        'search_icon', 'phone_icon', 'mail_icon', 'logo_mini', 'nav_', 'sidebar', 'ad_unit',
                        'adsbygoogle', 'yandex_ads', 'header-bg', 'footer-bg', 'icon-', '-icon', 'btn-', '-btn'
                    ];
                    
                    for (var b = 0; b < blacklist.length; b++) {
                        if (urlLower.indexOf(blacklist[b]) !== -1) {
                            return;
                        }
                    }

                    // Reject vector SVGs and inline base64 indicators which are layout code rather than real media
                    if (ext === 'svg' || absUrl.startsWith('data:image/svg+xml')) {
                        return;
                    }
                    if (absUrl.startsWith('data:') && absUrl.length < 5000) {
                        return;
                    }

                    // Ignore tiny icons, badges, UI arrows, and small thumbnails under 120dp
                    if (type === 'image') {
                        var width = extra && extra.width ? extra.width : -1;
                        var height = extra && extra.height ? extra.height : -1;
                        if ((width > 0 && width < 120) || (height > 0 && height < 120)) {
                            return;
                        }
                    }

                    window.urlCheck[absUrl] = true;
                    
                    var filename = "download";
                    try {
                        var parsedUrl = new URL(absUrl);
                        var pathname = parsedUrl.pathname;
                        var lastSegment = pathname.substring(pathname.lastIndexOf('/') + 1);
                        if (lastSegment && lastSegment.indexOf('.') !== -1) {
                            filename = lastSegment;
                        } else {
                            filename = type + "_" + Math.random().toString(36).substr(2, 5) + (ext ? "." + ext : "");
                        }
                    } catch(e){}
                    
                    try { filename = decodeURIComponent(filename); } catch(e){}
                    filename = filename.replace(/[\\\/:*?"<>|]/g, "_");
                    
                    var width = extra && extra.width ? extra.width : -1;
                    var height = extra && extra.height ? extra.height : -1;
                    
                    items.push({
                        url: absUrl,
                        type: type,
                        ext: ext || "dat",
                        filename: filename,
                        tagName: tag,
                        width: parseInt(width),
                        height: parseInt(height)
                    });
                } catch(e) {}
            }
            
            // 1. Images with high resolution srcset candidate selection
            var imgs = document.getElementsByTagName('img');
            for (var i = 0; i < imgs.length; i++) {
                var img = imgs[i];
                var src = null;
                
                // Check srcset first for high-res source files
                var srcset = img.getAttribute('srcset') || img.getAttribute('data-srcset');
                if (srcset) {
                    src = getBestFromSrcset(srcset);
                }
                
                if (!src) {
                    var keys = [
                        'data-src', 'data-original', 'data-lazy-src', 'lazy-src', 'data-zoom', 
                        'src-webp', 'data-src-webp', 'data-defer', 'data-src-original',
                        'data-media', 'data-image', 'data-url', 'data-full', 'data-hi-res', 'data-highres'
                    ];
                    for (var k = 0; k < keys.length; k++) {
                        var attr = img.getAttribute(keys[k]);
                        if (attr && attr.trim().length > 0) {
                            src = attr.trim();
                            break;
                        }
                    }
                }
                if (!src) {
                    for (var attrIdx = 0; attrIdx < img.attributes.length; attrIdx++) {
                        var attribute = img.attributes[attrIdx];
                        var name = attribute.name.toLowerCase();
                        var val = attribute.value;
                        if ((name.indexOf('src') !== -1 || name.indexOf('original') !== -1 || name.indexOf('url') !== -1 || name.indexOf('image') !== -1) && val) {
                            if (val.substring(0, 4) === 'http' || val.substring(0, 2) === '//' || val.indexOf('/') === 0) {
                                src = val.trim();
                                break;
                            }
                        }
                    }
                }
                if (!src) {
                    src = img.src;
                }
                
                // Read geometry details, falling back on attributes if naturalWidth is zero or not yet loaded
                var wAttr = parseInt(img.getAttribute('width')) || -1;
                var hAttr = parseInt(img.getAttribute('height')) || -1;
                var extra = { 
                    width: img.naturalWidth || img.width || wAttr, 
                    height: img.naturalHeight || img.height || hAttr 
                };
                
                // Fast filter elements that have small HTML inline styles/attributes
                if ((extra.width > 0 && extra.width < 120) || (extra.height > 0 && extra.height < 120)) {
                    continue;
                }
                
                var ext = "jpg";
                if (src) {
                    var extMatch = src.toLowerCase().match(/\.(png|jpg|jpeg|gif|webp|svg|bmp)/);
                    if (extMatch) ext = extMatch[1];
                }
                
                addMedia(src, "image", ext, "IMG", extra);
            }
            
            // 2. CSS backgrounds
            var allElems = document.querySelectorAll('*');
            for (var i = 0; i < Math.min(allElems.length, 600); i++) {
                var el = allElems[i];
                try {
                    var bg = window.getComputedStyle(el).backgroundImage;
                    if (bg && bg !== 'none') {
                        var match = bg.match(/url\(['"]?([^'"]+)['"]?\)/);
                        if (match && match[1]) {
                            var bgUrl = match[1];
                            var wVal = el.offsetWidth || -1;
                            var hVal = el.offsetHeight || -1;
                            if (wVal > 0 && wVal < 120) continue;
                            if (hVal > 0 && hVal < 120) continue;
                            
                            var ext = "jpg";
                            var extMatch = bgUrl.toLowerCase().match(/\.(png|jpg|jpeg|gif|webp|svg|bmp)/);
                            if (extMatch) ext = extMatch[1];
                            addMedia(bgUrl, "image", ext, "CSS_BG", { width: wVal, height: hVal });
                        }
                    }
                } catch(e){}
            }
            
            // 3. Videos
            var videos = document.getElementsByTagName('video');
            for (var i = 0; i < videos.length; i++) {
                var v = videos[i];
                var src = v.src || v.getAttribute('src');
                var ext = "mp4";
                if (src) {
                    var extMatch = src.toLowerCase().match(/\.(mp4|webm|ogg|mov|avi|mkv)/);
                    if (extMatch) ext = extMatch[1];
                    addMedia(src, "video", ext, "VIDEO", null);
                }
                
                var sources = v.getElementsByTagName('source');
                for (var j = 0; j < sources.length; j++) {
                    var s = sources[j];
                    var sUrl = s.src || s.getAttribute('src');
                    if (sUrl) {
                        var sExt = "mp4";
                        var extMatch = sUrl.toLowerCase().match(/\.(mp4|webm|ogg|mov|avi|mkv)/);
                        if (extMatch) sExt = extMatch[1];
                        addMedia(sUrl, "video", sExt, "VIDEO_SOURCE", null);
                    }
                }
            }
            
            // 4. Special search engine extraction
            // A. Google Images high resolution deep links
            var googleImgres = document.querySelectorAll('a[href*="/imgres"]');
            for (var i = 0; i < googleImgres.length; i++) {
                var a = googleImgres[i];
                try {
                    var m = a.href.match(/[?&]imgurl=([^&]+)/);
                    if (m && m[1]) {
                        var realUrl = decodeURIComponent(m[1]);
                        var ext = "jpg";
                        var extMatch = realUrl.toLowerCase().match(/\.(png|jpg|jpeg|gif|webp|bmp)/);
                        if (extMatch) ext = extMatch[1];
                        addMedia(realUrl, "image", ext, "GOOGLE_HIGHRES", { width: 1200, height: 1200 });
                    }
                } catch(e){}
            }

            // B. Bing Images iusc components
            var bingIusc = document.querySelectorAll('a.iusc');
            for (var i = 0; i < bingIusc.length; i++) {
                var a = bingIusc[i];
                try {
                    var mAttr = a.getAttribute('m');
                    if (mAttr) {
                        var parsedRecord = JSON.parse(mAttr);
                        if (parsedRecord && parsedRecord.murl) {
                            var realUrl = parsedRecord.murl;
                            var ext = "jpg";
                            var extMatch = realUrl.toLowerCase().match(/\.(png|jpg|jpeg|gif|webp|bmp)/);
                            if (extMatch) ext = extMatch[1];
                            addMedia(realUrl, "image", ext, "BING_HIGHRES", { width: 1200, height: 1200 });
                        }
                    }
                } catch(e){}
            }

            // C. Yandex Images serp-item configurations
            var yandexItems = document.querySelectorAll('.serp-item, a[href*="img_url="]');
            for (var i = 0; i < yandexItems.length; i++) {
                var item = yandexItems[i];
                try {
                    var href = item.href || item.getAttribute('href') || '';
                    var m = href.match(/[?&]img_url=([^&]+)/);
                    if (m && m[1]) {
                        var realUrl = decodeURIComponent(m[1]);
                        var ext = "jpg";
                        var extMatch = realUrl.toLowerCase().match(/\.(png|jpg|jpeg|gif|webp|bmp)/);
                        if (extMatch) ext = extMatch[1];
                        addMedia(realUrl, "image", ext, "YANDEX_HIGHRES", { width: 1200, height: 1200 });
                    }
                    
                    var bem = item.getAttribute('data-bem');
                    if (bem) {
                        var parsedBem = JSON.parse(bem);
                        var serpItem = parsedBem['serp-item'];
                        if (serpItem && serpItem.img_href) {
                            var realUrl = serpItem.img_href;
                            var ext = "jpg";
                            var extMatch = realUrl.toLowerCase().match(/\.(png|jpg|jpeg|gif|webp|bmp)/);
                            if (extMatch) ext = extMatch[1];
                            addMedia(realUrl, "image", ext, "YANDEX_HIGHRES_BEM", { width: 1200, height: 1200 });
                        }
                    }
                } catch(e){}
            }

            // 5. General Anchors
            var anchors = document.getElementsByTagName('a');
            for (var i = 0; i < anchors.length; i++) {
                var a = anchors[i];
                var href = a.href || a.getAttribute('href');
                if (href) {
                    var matchImg = href.toLowerCase().match(/\.(png|jpg|jpeg|gif|webp|svg|bmp)(\?|$)/);
                    if (matchImg) addMedia(href, "image", matchImg[1], "A_IMG", null);
                    var matchVid = href.toLowerCase().match(/\.(mp4|webm|ogg|mov|avi|mkv)(\?|$)/);
                    if (matchVid) addMedia(href, "video", matchVid[1], "A_VID", null);
                }
            }

            // 6. JSON-LD scripts (Pinterest, Instagram, and generic media schemas)
            var scripts = document.querySelectorAll('script[type="application/ld+json"]');
            for (var i = 0; i < scripts.length; i++) {
                try {
                    var json = JSON.parse(scripts[i].innerText);
                    function parseJsonLD(obj) {
                        if (!obj) return;
                        if (typeof obj === 'object') {
                            if (obj.contentUrl && typeof obj.contentUrl === 'string') {
                                var isVid = obj['@type'] === 'VideoObject' || obj.contentUrl.match(/\.(mp4|m3u8|webm)/i);
                                var ext = isVid ? "mp4" : "jpg";
                                addMedia(obj.contentUrl, isVid ? "video" : "image", ext, "JSON_LD_CONTENT", null);
                            }
                            if (obj.embedUrl && typeof obj.embedUrl === 'string') {
                                addMedia(obj.embedUrl, "video", "mp4", "JSON_LD_EMBED", null);
                            }
                            if (obj.video && typeof obj.video === 'object') {
                                parseJsonLD(obj.video);
                            }
                            if (obj.image && typeof obj.image === 'object') {
                                parseJsonLD(obj.image);
                            }
                            if (obj.thumbnailUrl && typeof obj.thumbnailUrl === 'string') {
                                addMedia(obj.thumbnailUrl, "image", "jpg", "JSON_LD_THUMB", null);
                            }
                            for (var key in obj) {
                                if (obj.hasOwnProperty(key)) {
                                    parseJsonLD(obj[key]);
                                }
                            }
                        } else if (Array.isArray(obj)) {
                            for (var k = 0; k < obj.length; k++) {
                                parseJsonLD(obj[k]);
                            }
                        }
                    }
                    parseJsonLD(json);
                } catch(e){}
            }
            
            if (items.length > 0 && window.AndroidMediaDownloader) {
                window.AndroidMediaDownloader.onMediaDiscovered(JSON.stringify(items));
            }
        }
        
        // Initial run
        extract();
        
        // Setup MutationObserver & listener once globally
        if (!window.hasMediaCrawlerRegistered) {
            window.hasMediaCrawlerRegistered = true;
            
            try {
                var originalPushState = window.history.pushState;
                window.history.pushState = function() {
                    var ret = originalPushState.apply(this, arguments);
                    window.urlCheck = {};
                    if (window.AndroidMediaDownloader && window.AndroidMediaDownloader.onSpaUrlChanged) {
                        try { window.AndroidMediaDownloader.onSpaUrlChanged(window.location.href); } catch(e){}
                    }
                    return ret;
                };
                
                var originalReplaceState = window.history.replaceState;
                window.history.replaceState = function() {
                    var ret = originalReplaceState.apply(this, arguments);
                    window.urlCheck = {};
                    if (window.AndroidMediaDownloader && window.AndroidMediaDownloader.onSpaUrlChanged) {
                        try { window.AndroidMediaDownloader.onSpaUrlChanged(window.location.href); } catch(e){}
                    }
                    return ret;
                };
                
                window.addEventListener('popstate', function() {
                    window.urlCheck = {};
                    if (window.AndroidMediaDownloader && window.AndroidMediaDownloader.onSpaUrlChanged) {
                        try { window.AndroidMediaDownloader.onSpaUrlChanged(window.location.href); } catch(e){}
                    }
                });
                
                window.addEventListener('hashchange', function() {
                    window.urlCheck = {};
                    if (window.AndroidMediaDownloader && window.AndroidMediaDownloader.onSpaUrlChanged) {
                        try { window.AndroidMediaDownloader.onSpaUrlChanged(window.location.href); } catch(e){}
                    }
                });
            } catch(e){}

            var scrollTimeout;
            window.addEventListener('scroll', function() {
                clearTimeout(scrollTimeout);
                scrollTimeout = setTimeout(extract, 1500);
            }, { passive: true });
            
            var observer = new MutationObserver(function(mutations) {
                var shouldExtract = false;
                for (var i = 0; i < mutations.length; i++) {
                    var m = mutations[i];
                    if (m.addedNodes.length > 0) {
                        shouldExtract = true;
                        break;
                    }
                }
                if (shouldExtract) {
                    extract();
                }
            });
            observer.observe(document.body, { childList: true, subtree: true });
            
            setInterval(extract, 3000);
        }
    } catch(err) {
        if (window.AndroidMediaDownloader) {
            window.AndroidMediaDownloader.logError(err.message);
        }
    }
})();
"""

private fun getYtEnhancerJs(isBackground: Boolean, isSponsorBlock: Boolean, isAutoQuality: Boolean): String {
    return """
(function() {
    const isBackgroundPlayEnabled = $isBackground;
    const isSponsorBlockEnabled = $isSponsorBlock;
    const isAutoMaxQualityEnabled = $isAutoQuality;

    // Helper to send message back to Android
    function reportAdBlocked() {
        try {
            if (window.AndroidMediaDownloader && typeof window.AndroidMediaDownloader.reportAdBlocked === 'function') {
                window.AndroidMediaDownloader.reportAdBlocked();
            }
        } catch(e) {}
    }

    // Advanced API-level AdBlock for YouTube (Intercepting player/next response JSON to delete ads entirely)
    try {
        const originFetch = window.fetch;
        window.fetch = async function(...args) {
            const url = args[0];
            if (typeof url === 'string' && (url.includes('/youtubei/v1/player') || url.includes('/youtubei/v1/next'))) {
                try {
                    const response = await originFetch.apply(this, args);
                    const rawText = await response.text();
                    let json = JSON.parse(rawText);
                    let changed = false;
                    
                    if (json.adPlacements) {
                        delete json.adPlacements;
                        changed = true;
                    }
                    if (json.playerAds) {
                        delete json.playerAds;
                        changed = true;
                    }
                    if (json.adSlots) {
                        delete json.adSlots;
                        changed = true;
                    }
                    if (json.adSafetyReason) {
                        delete json.adSafetyReason;
                        changed = true;
                    }
                    
                    if (changed) {
                        reportAdBlocked();
                    }
                    
                    return new Response(JSON.stringify(json), {
                        status: response.status,
                        statusText: response.statusText,
                        headers: response.headers
                    });
                } catch (e) {}
            }
            return originFetch.apply(this, args);
        };

        const originOpen = XMLHttpRequest.prototype.open;
        XMLHttpRequest.prototype.open = function(method, url) {
            this._url = url;
            return originOpen.apply(this, arguments);
        };

        const originSend = XMLHttpRequest.prototype.send;
        XMLHttpRequest.prototype.send = function() {
            const xhr = this;
            if (xhr._url && (xhr._url.includes('/youtubei/v1/player') || xhr._url.includes('/youtubei/v1/next'))) {
                const originOnReadyStateChange = xhr.onreadystatechange;
                xhr.onreadystatechange = function() {
                    if (xhr.readyState === 4) {
                        try {
                            let responseData = xhr.responseText;
                            let json = JSON.parse(responseData);
                            let changed = false;
                            if (json.adPlacements) { delete json.adPlacements; changed = true; }
                            if (json.playerAds) { delete json.playerAds; changed = true; }
                            if (json.adSlots) { delete json.adSlots; changed = true; }
                            if (json.adSafetyReason) { delete json.adSafetyReason; changed = true; }
                            if (changed) {
                                Object.defineProperty(xhr, 'responseText', { value: JSON.stringify(json), configurable: true });
                                Object.defineProperty(xhr, 'response', { value: JSON.stringify(json), configurable: true });
                                reportAdBlocked();
                            }
                        } catch(e) {}
                    }
                    if (originOnReadyStateChange) {
                        originOnReadyStateChange.apply(this, arguments);
                    }
                };
            }
            return originSend.apply(this, arguments);
        };

        if (window.ytInitialPlayerResponse) {
            if (window.ytInitialPlayerResponse.adPlacements) { delete window.ytInitialPlayerResponse.adPlacements; reportAdBlocked(); }
            if (window.ytInitialPlayerResponse.playerAds) { delete window.ytInitialPlayerResponse.playerAds; reportAdBlocked(); }
        }
        let initialPlayerResponse = window.ytInitialPlayerResponse;
        Object.defineProperty(window, 'ytInitialPlayerResponse', {
            get: function() { return initialPlayerResponse; },
            set: function(val) {
                if (val) {
                    if (val.adPlacements) { delete val.adPlacements; reportAdBlocked(); }
                    if (val.playerAds) { delete val.playerAds; reportAdBlocked(); }
                    if (val.adSlots) { delete val.adSlots; reportAdBlocked(); }
                }
                initialPlayerResponse = val;
            },
            configurable: true
        });
    } catch(e) {}

    try {
        if (isBackgroundPlayEnabled) {
            Object.defineProperty(document, 'hidden', { get: function() { return false; }, configurable: true });
            Object.defineProperty(document, 'visibilityState', { get: function() { return 'visible'; }, configurable: true });
            Object.defineProperty(document, 'webkitHidden', { get: function() { return false; }, configurable: true });
            Object.defineProperty(document, 'webkitVisibilityState', { get: function() { return 'visible'; }, configurable: true });
            Object.defineProperty(document, 'hasFocus', { value: function() { return true; }, writable: true, configurable: true });
        }
    } catch(e) {}

    const originalAddEventListener = EventTarget.prototype.addEventListener;
    EventTarget.prototype.addEventListener = function(type, listener, options) {
        if (isBackgroundPlayEnabled && (type === 'visibilitychange' || type === 'webkitvisibilitychange' || type === 'blur' || type === 'pagehide')) {
            return;
        }
        return originalAddEventListener.call(this, type, listener, options);
    };

    let lastInteraction = 0;
    const regInteraction = () => { lastInteraction = Date.now(); };
    document.addEventListener('touchstart', regInteraction, true);
    document.addEventListener('click', regInteraction, true);
    document.addEventListener('mousedown', regInteraction, true);
    document.addEventListener('keydown', regInteraction, true);

    let isUserPaused = false;
    try {
        const originalPlay = HTMLVideoElement.prototype.play;
        const originalPause = HTMLVideoElement.prototype.pause;

        HTMLVideoElement.prototype.pause = function() {
            if (isBackgroundPlayEnabled) {
                const timeDiff = Date.now() - lastInteraction;
                if (timeDiff > 1200 && (document.hidden || !document.hasFocus())) {
                    return Promise.resolve();
                }
            }
            isUserPaused = true;
            window.swayUserPaused = true;
            return originalPause.apply(this, arguments);
        };

        HTMLVideoElement.prototype.play = function() {
            isUserPaused = false;
            window.swayUserPaused = false;
            return originalPlay.apply(this, arguments);
        };
    } catch(e) {}

    setInterval(() => {
        if (isBackgroundPlayEnabled && !isUserPaused && (document.hidden || !document.hasFocus())) {
            const videos = document.querySelectorAll('video');
            videos.forEach(v => {
                if (v.paused && !v.ended) {
                    v.play().catch(() => {});
                }
            });
        }
    }, 1000);

    if (window.location.hostname.includes('youtube.com') || window.location.hostname.includes('youtu.be')) {
        const yStyle = document.createElement('style');
        yStyle.id = 'sway-yt-ad-blocker-style';
        yStyle.innerHTML = ' .ytp-ad-overlay-container, .ytp-ad-message-container, .ytp-ad-action-button, .ytp-ad-image-overlay, ytd-companion-ad-renderer, ytd-display-ad-renderer, ytd-compact-promoted-video-renderer, ytm-promoted-sparkles-web-renderer, ytd-ad-slot-renderer, .ytd-ad-slot-renderer, .ytp-ad-overlay-slot, ytm-companion-ad-renderer, .yt-ad-tile-renderer, ytm-ad-landing-page-spec, .ad-container, .ad-image, .ad-showing, .ad-interrupting, yt-mealbar-promo-renderer, #masthead-ad, .video-ads, .ytp-ad-player-overlay, .ytp-ad-player-overlay-layout-entry, ytd-promoted-video-renderer, ytd-banner-promo-renderer, ytd-rich-grid-ad-slot, ytm-promoted-video-renderer, .ytm-ad-survey-renderer, .ytp-ad-player-overlay-flyout-wrapper, .ytp-ad-simple-ad-header, .ytp-ad-overlay-image, #player-ads { display: none !important; visibility: hidden !important; height: 0 !important; min-height: 0 !important; width: 0 !important; opacity: 0 !important; pointer-events: none !important; } ';
        if (!document.getElementById('sway-yt-ad-blocker-style')) {
            document.head.appendChild(yStyle);
        }

        function showSwayMsg(text, duration = 3000) {
            let container = document.getElementById('sway-toast-container');
            if (!container) {
                container = document.createElement('div');
                container.id = 'sway-toast-container';
                container.style.cssText = 'position:fixed; bottom: 85px; left: 50%; transform: translateX(-50%); z-index: 100000; display: flex; flex-direction: column; gap: 8px; font-family: Roboto, sans-serif; pointer-events: none;';
                document.body.appendChild(container);
            }
            const toast = document.createElement('div');
            toast.style.cssText = 'background: rgba(26, 26, 26, 0.9); color: #fff; padding: 10px 16px; border-radius: 20px; font-size: 13px; font-weight: 500; border: 1px solid rgba(255,255,255,0.1); box-shadow: 0 4px 12px rgba(0,0,0,0.3); animation: fadein 0.3s, fadeout 0.3s ' + (duration - 300) + 'ms forwards; white-space: nowrap; transition: all 0.3s;';
            toast.innerText = text;
            
            if (!document.getElementById('sway-toast-keyframes')) {
                const kf = document.createElement('style');
                kf.id = 'sway-toast-keyframes';
                kf.innerHTML = '@keyframes fadein { from { bottom: 0; opacity: 0; } to { bottom: 30px; opacity: 1; } } @keyframes fadeout { from { opacity: 1; } to { opacity: 0; } }';
                document.head.appendChild(kf);
            }
            
            container.appendChild(toast);
            setTimeout(() => { toast.remove(); }, duration);
        }

        let lastVideoId = '';
        let sponsorSegments = [];

        function getVideoId() {
            const urlParams = new URLSearchParams(window.location.search);
            if (urlParams.has('v')) return urlParams.get('v');
            const pathParts = window.location.pathname.split('/');
            for (let i = 0; i < pathParts.length; i++) {
                if (pathParts[i] === 'v' || pathParts[i] === 'embed' || pathParts[i] === 'shorts') {
                    if (pathParts[i+1]) return pathParts[i+1];
                }
            }
            const player = document.getElementById('movie_player') || document.querySelector('.html5-video-player');
            if (player && typeof player.getVideoData === 'function') {
                const data = player.getVideoData();
                if (data && data.video_id) return data.video_id;
            }
            return null;
        }

        function loadSponsorSegments(vid) {
            if (!isSponsorBlockEnabled) return;
            if (!vid) return;
            sponsorSegments = [];
            const categories = '["sponsor","selfpromo","interaction","intro","outro","preview"]';
            const sUrl = "https://sponsor.ajay.app/api/skipSegments?videoID=" + vid + "&categories=" + encodeURIComponent(categories);
            
            fetch(sUrl)
                .then(res => {
                    if (!res.ok) throw new Error();
                    return res.json();
                })
                .then(data => {
                    if (Array.isArray(data)) {
                        sponsorSegments = data.map(item => item.segment);
                        if (sponsorSegments.length > 0) {
                            showSwayMsg("SponsorBlock: найден сегмент для пропуска 🛠️");
                        }
                    }
                })
                .catch(() => {});
        }

        function runAdSkipper() {
            const video = document.querySelector('video');
            if (!video) return;

            // Highly optimized guard: only scan for skip buttons if an ad is actively showing!
            const isAdShowing = document.querySelector('.ad-showing, .ad-interrupting, .video-ads, .ytp-ad-player-overlay') !== null;
            if (!isAdShowing) {
                if (video.playbackRate > 8.0) {
                    video.playbackRate = 1.0;
                    video.muted = false;
                }
                return;
            }

            video.muted = true;
            video.playbackRate = 16.0;
            if (video.duration && isFinite(video.duration) && video.currentTime < video.duration - 0.1) {
                video.currentTime = video.duration - 0.05;
            }

            const skipBtn = document.querySelector(
                '.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-ad-skip-button-text, ' +
                '.ytp-ad-skip-button-container, ytm-ad-skip-button, button.ytp-ad-skip-button'
            );

            if (skipBtn) {
                try { 
                    skipBtn.click(); 
                    showSwayMsg("Реклама пропущена ✨");
                } catch(e) {}
            }
            reportAdBlocked();

            const closeBtn = document.querySelector('.ytp-ad-overlay-close-container button, .ytp-ad-image-overlay .ytp-ad-overlay-close-button');
            if (closeBtn) {
                try { 
                    closeBtn.click(); 
                    reportAdBlocked();
                } catch(e) {}
            }
        }

        // We run a simple, lightweight interval check instead of a heavy DOM MutationObserver subtree
        setInterval(() => {
            const currentVid = getVideoId();
            if (currentVid && currentVid !== lastVideoId) {
                lastVideoId = currentVid;
                loadSponsorSegments(currentVid);
                window.swayForcedQuality = false;
            }

            const video = document.querySelector('video');
            if (!video) return;

            runAdSkipper();

            // Handle SponsorBlock segments
            if (isSponsorBlockEnabled && sponsorSegments.length > 0) {
                const currentT = video.currentTime;
                for (let i = 0; i < sponsorSegments.length; i++) {
                    const start = sponsorSegments[i][0];
                    const end = sponsorSegments[i][1];
                    if (currentT >= start && currentT < end - 0.2) {
                        video.currentTime = end;
                        showSwayMsg("Пропуск спонсорской вставки (SponsorBlock) ⏭️");
                        reportAdBlocked();
                        break;
                    }
                }
            }

            // Handle forced high quality settings
            const player = document.getElementById('movie_player') || document.querySelector('.html5-video-player');
            if (isAutoMaxQualityEnabled && player) {
                if (typeof player.getAvailableQualityLevels === 'function' && typeof player.setPlaybackQualityRange === 'function') {
                    if (!window.swayForcedQuality || window.location.href !== window.swayLastQualityUrl) {
                        const levels = player.getAvailableQualityLevels();
                        if (levels && levels.length > 0) {
                            const highest = levels[0] || 'highres';
                            player.setPlaybackQualityRange(highest);
                            player.setPlaybackQuality(highest);
                            window.swayForcedQuality = true;
                            window.swayLastQualityUrl = window.location.href;
                            showSwayMsg("Качество: Максимальное (" + highest + ") 🎬");
                        }
                    }
                }
            }
        }, 600);
    }
})();
""";
}

enum class FabTrajectoryMode {
    SNAP_EDGE,
    FREE_FLOAT,
    ORBIT_PATH
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(viewModel: BrowserViewModel) {
    var customView by remember { mutableStateOf<android.view.View?>(null) }
    var customViewCallback by remember { mutableStateOf<android.webkit.WebChromeClient.CustomViewCallback?>(null) }

    val l10n = LocalAppStrings.current
    val localContext = LocalContext.current
    val currentUrl by viewModel.currentUrl.collectAsState()
    val webTitle by viewModel.webTitle.collectAsState()
    val webLoadProgress by viewModel.webLoadProgress.collectAsState()
    val rawMediaItems by viewModel.rawMediaItems.collectAsState()
    val canGoBack by viewModel.canGoBack.collectAsState()
    val canGoForward by viewModel.canGoForward.collectAsState()
    val canRefresh by viewModel.canRefresh.collectAsState()

    // Draggable FAB with Trajectory configurations & animations
    var fabTrajectoryMode by remember { mutableStateOf(FabTrajectoryMode.SNAP_EDGE) }
    var isTrajectoryMenuExpanded by remember { mutableStateOf(false) }
    val dragOffsetX = remember { Animatable(0f) }
    val dragOffsetY = remember { Animatable(0f) }

    val selectedIds by viewModel.selectedIds.collectAsState()
    val filteredMedia by viewModel.filteredAndSortedMedia.collectAsState()
    val filterType by viewModel.filterType.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val searchQuery by viewModel.mediaSearchQuery.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()

    val isTrackerBlockingEnabled by viewModel.isTrackerBlockingEnabled.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val isHttpsEverywhereEnabled by viewModel.isHttpsEverywhereEnabled.collectAsState()
    val isScriptBlockingEnabled by viewModel.isScriptBlockingEnabled.collectAsState()
    val isBlockThirdPartyCookiesEnabled by viewModel.isBlockThirdPartyCookiesEnabled.collectAsState()
    val isClearHistoryOnExitEnabled by viewModel.isClearHistoryOnExitEnabled.collectAsState()
    val searchEngine by viewModel.searchEngine.collectAsState()
    val userAgentMode by viewModel.userAgentMode.collectAsState()
    val homePageUrl by viewModel.homePageUrl.collectAsState()
    val adBlockMode by viewModel.adBlockMode.collectAsState()
    val blockedAdsCount by viewModel.blockedAdsCount.collectAsState()
    val isFastRenderingEnabled by viewModel.isFastRenderingEnabled.collectAsState()
    val isAutoCookieEnabled by viewModel.isAutoCookieEnabled.collectAsState()
    var showSettingsPage by remember { mutableStateOf(false) }

    var textInputUrl by remember { mutableStateOf(currentUrl) }
    var urlTextFieldValue by remember {
        mutableStateOf(TextFieldValue(text = textInputUrl, selection = TextRange.Zero))
    }
    var isBarVisible by remember { mutableStateOf(true) }

    LaunchedEffect(textInputUrl) {
        if (urlTextFieldValue.text != textInputUrl) {
            urlTextFieldValue = TextFieldValue(text = textInputUrl, selection = TextRange.Zero)
        }
    }
    
    // Retrieve cached persistent WebView to enable continuous audio play and avoid state reloads
    val persistentWebView = viewModel.getOrCreateWebView(localContext)
    var webViewRef by remember { mutableStateOf<WebView?>(persistentWebView) }
    
    val isYtBackgroundEnabled by viewModel.isYtBackgroundEnabled.collectAsState()
    val isSponsorBlockEnabled by viewModel.isSponsorBlockEnabled.collectAsState()
    val isYtAutoMaxQualityEnabled by viewModel.isYtAutoMaxQualityEnabled.collectAsState()

    // Pass the setting state to BackgroundPlayWebView
    LaunchedEffect(isYtBackgroundEnabled) {
        persistentWebView.setKeepPlayingInBackground(isYtBackgroundEnabled)
    }

    // Manage background audio service when app changes focus/stops
    DisposableEffect(key1 = localContext, key2 = isYtBackgroundEnabled) {
        val activity = localContext as? androidx.activity.ComponentActivity
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (isYtBackgroundEnabled) {
                if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                    // App went to background / screen lock. Start Foreground Service to hold CPU/thread wake lock
                    try {
                        val serviceIntent = android.content.Intent(localContext, BackgroundAudioService::class.java)
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            localContext.startForegroundService(serviceIntent)
                        } else {
                            localContext.startService(serviceIntent)
                        }
                    } catch (e: Exception) {}
                } else if (event == androidx.lifecycle.Lifecycle.Event.ON_START) {
                    // App came back to foreground. Stop the service.
                    try {
                        val serviceIntent = android.content.Intent(localContext, BackgroundAudioService::class.java)
                        localContext.stopService(serviceIntent)
                    } catch (e: Exception) {}
                }
            }
        }

        activity?.lifecycle?.addObserver(observer)

        onDispose {
            activity?.lifecycle?.removeObserver(observer)
            // Stop service when composable is destroyed
            try {
                val serviceIntent = android.content.Intent(localContext, BackgroundAudioService::class.java)
                localContext.stopService(serviceIntent)
            } catch (e: Exception) {}
        }
    }

    // Handle pause signal from Background Service notification button click
    DisposableEffect(key1 = localContext) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                if (intent?.action == BackgroundAudioService.ACTION_SERVICE_STOPPED) {
                    // User explicitly pressed "Stop" on background playback notification
                    webViewRef?.evaluateJavascript("""
                        (function() {
                            const video = document.querySelector('video');
                            if (video) {
                                video.pause();
                            }
                        })();
                    """.trimIndent(), null)
                }
            }
        }
        val filter = android.content.IntentFilter(BackgroundAudioService.ACTION_SERVICE_STOPPED)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            localContext.registerReceiver(receiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
        } else {
            localContext.registerReceiver(receiver, filter)
        }

        onDispose {
            try {
                localContext.unregisterReceiver(receiver)
            } catch (e: Exception) {}
        }
    }

    // Фоновый режим для YouTube / Внешний триггер активности WebView
    LaunchedEffect(webViewRef, isYtBackgroundEnabled) {
        if (!isYtBackgroundEnabled) return@LaunchedEffect
        while (true) {
            delay(1500)
            webViewRef?.let { wv ->
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    try {
                        val currentUrlStr = wv.url ?: ""
                        if (currentUrlStr.contains("youtube.com") || currentUrlStr.contains("youtu.be")) {
                            wv.evaluateJavascript("""
                                (function() {
                                    const video = document.querySelector('video');
                                    if (video && video.paused && !video.ended && !window.swayUserPaused) {
                                        video.play().catch(function(){});
                                    }
                                })();
                            """.trimIndent(), null)
                        }
                    } catch (e: Exception) {}
                }
            }
        }
    }

    // Лаунчер для запроса прав на запись на устройствах с Android 9 и ниже (SDK <= 28)
    val writePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            android.widget.Toast.makeText(localContext, "Разрешение получено. Повторите загрузку.", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            android.widget.Toast.makeText(localContext, "Ошибка: требуется разрешение на запись файлов", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    val safelyTriggerDownload = { onGranted: () -> Unit ->
        if (android.os.Build.VERSION.SDK_INT <= 28) {
            val permissionState = androidx.core.content.ContextCompat.checkSelfPermission(
                localContext,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            if (permissionState == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                onGranted()
            } else {
                writePermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        } else {
            onGranted()
        }
    }

    var isMediaPanelVisible by remember { mutableStateOf(false) }
    var previewMediaItem by remember { mutableStateOf<MediaItem?>(null) }
    var showCustomZipNameDialog by remember { mutableStateOf(false) }
    var customZipName by remember { mutableStateOf("") }

    // Bottom Navigation States
    var currentTabItem by remember { mutableStateOf(0) } // 0: Главная, 1: Поиск, 2: Вкладки, 3: Сохранено, 4: Меню
    val isBrowsingActive by viewModel.isBrowsingActive.collectAsState()
    var isBrowsing by remember(isBrowsingActive) { mutableStateOf(isBrowsingActive) }
    LaunchedEffect(isBrowsingActive) {
        if (isBrowsing != isBrowsingActive) {
            isBrowsing = isBrowsingActive
        }
    }
    LaunchedEffect(isBrowsing) {
        if (isBrowsing != isBrowsingActive) {
            viewModel.setBrowsing(isBrowsing)
        }
    }
    var isPrivateMode by remember { mutableStateOf(false) }

    LaunchedEffect(currentUrl, isBrowsing) {
        isBarVisible = true
    }
    
    val tabs by viewModel.tabs.collectAsState()
    val activeTabId by viewModel.activeTabId.collectAsState()
    val bookmarksList by viewModel.bookmarks.collectAsState()
    val historyList by viewModel.history.collectAsState()
    val downloadsList by viewModel.downloads.collectAsState()

    var selectedSavedTab by remember { mutableStateOf(0) } // 0: Закладки, 1: История, 2: Загрузки
    var searchQueryText by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    var isImageSearchTabSelected by remember { mutableStateOf(true) }
    val searchScreenFocusRequester = remember { FocusRequester() }
    val imageSearchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(currentTabItem, isImageSearchTabSelected) {
        if (currentTabItem == 1) {
            delay(300)
            try {
                if (isImageSearchTabSelected) {
                    imageSearchFocusRequester.requestFocus()
                } else {
                    searchScreenFocusRequester.requestFocus()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    var imageSearchQuery by remember(l10n.keywordCosmos) { mutableStateOf(l10n.keywordCosmos) }
    var isImageSearchLoading by remember { mutableStateOf(false) }

    val initialMockList = listOf(
        SearchImage(
            imageUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?q=80&w=600",
            title = "Галактика и звёздное скопление во Вселенной",
            siteUrl = "nasa.gov",
            siteName = "NASA Science"
        ),
        SearchImage(
            imageUrl = "https://images.unsplash.com/photo-1506318137071-a8e063b4bec0?q=80&w=600",
            title = "Космическая пыль и далёкие созвездия",
            siteUrl = "esa.int",
            siteName = "ESA Portal"
        ),
        SearchImage(
            imageUrl = "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?q=80&w=600",
            title = "Вид на планету Земля с орбиты МКС",
            siteUrl = "space.com",
            siteName = "SpaceNews"
        ),
        SearchImage(
            imageUrl = "https://images.unsplash.com/photo-1419242902214-272b3f66ee7a?q=80&w=600",
            title = "Млечный Путь над ночной пустыней Мохаве",
            siteUrl = "nationalgeographic.com",
            siteName = "National Geographic"
        )
    )

    var imageSearchResults by remember { mutableStateOf<List<SearchImage>>(initialMockList) }

    val focusManager = LocalFocusManager.current

    // Sync input box when viewModel URL changes
    LaunchedEffect(currentUrl) {
        textInputUrl = currentUrl
    }

    // Explicitly handle manual changes to the User-Agent mode inside settings
    LaunchedEffect(userAgentMode) {
        webViewRef?.let { webView ->
            val activeUA = viewModel.getUserAgent()
            if (webView.settings.userAgentString != activeUA) {
                webView.settings.userAgentString = activeUA
                webView.reload()
            }
        }
    }

    // Auto crawling script helper
    val runCrawlerInWebView = {
        webViewRef?.let { wv ->
            // Save user agent so we can perform HEAD checks with correct header signatures
            viewModel.browserUserAgent = wv.settings.userAgentString
            wv.evaluateJavascript(JS_CRAWLER_CODE, null)
        }
    }

    val MediaPanelContent = @Composable {
        Column(modifier = Modifier.fillMaxSize()) {
            // Media Drawer Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Analytics,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Найдено медиа-файлов: ${rawMediaItems.size}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Выбрано: ${selectedIds.size}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TextButton(
                    onClick = {
                        viewModel.clearDiscoveredMedia()
                        runCrawlerInWebView()
                    }
                ) {
                    Icon(Icons.Default.ChangeCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Обновить")
                }

                IconButton(onClick = { isMediaPanelVisible = false }) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть")
                }
            }

            // Search and Filter Controls Layout
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearch(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    placeholder = { Text("Фильтр по имени файла...", fontSize = 13.sp) },
                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                    leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearch("") }) {
                                Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Chip groups for filters
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val chips = listOf("All" to "Все", "image" to "Фото", "video" to "Видео")
                    chips.forEach { (filterVal, label) ->
                        FilterChip(
                            selected = filterType == filterVal,
                            onClick = { viewModel.updateFilter(filterVal) },
                            label = { Text(label, fontSize = 11.sp) },
                            shape = RoundedCornerShape(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Sorting select button toggles
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            val nextSort = when (sortOption) {
                                "None" -> "Name"
                                "Name" -> "Type"
                                "Type" -> "Size"
                                else -> "None"
                            }
                            viewModel.updateSort(nextSort)
                        }
                    ) {
                        Icon(
                            Icons.Default.SwapVert,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = when(sortOption) {
                                "Name" -> "Имя"
                                "Type" -> "Тип"
                                "Size" -> "Размер"
                                else -> "Без сорт."
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 2.dp, end = 4.dp)
                        )
                    }
                }
            }

            // Grid containing media cards
            if (filteredMedia.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CloudQueue,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Медиа не найдено на этой странице.",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            "Попробуйте перейти на другой сайт или обновить страницу.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredMedia, key = { it.id }) { item ->
                        MediaGridCard(
                            item = item,
                            isSelected = selectedIds.contains(item.id),
                            userAgent = viewModel.browserUserAgent,
                            referer = currentUrl,
                            onToggleSelect = { viewModel.toggleSelection(item.id) },
                            onTapPreview = { previewMediaItem = item }
                        )
                    }
                }
            }

            // Bottom Actions Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(12.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            if (selectedIds.size == filteredMedia.size && filteredMedia.isNotEmpty()) {
                                viewModel.deselectAll()
                            } else {
                                viewModel.selectAll()
                            }
                        }
                    ) {
                        val actText = if (selectedIds.size == filteredMedia.size && filteredMedia.isNotEmpty()) "Снять всё" else "Выбрать всё"
                        Icon(Icons.Default.SelectAll, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(actText, fontSize = 12.sp)
                    }

                    TextButton(
                        onClick = { viewModel.deselectAll() },
                        enabled = selectedIds.isNotEmpty()
                    ) {
                        Text("Очистить выбор (${selectedIds.size})", fontSize = 12.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            customZipName = "media_pack_${System.currentTimeMillis()}"
                            showCustomZipNameDialog = true
                        },
                        enabled = selectedIds.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("download_zip_button"),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.FolderZip, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Скачать ZIP (${selectedIds.size})", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            safelyTriggerDownload {
                                viewModel.downloadSelectedFilesIndividually()
                            }
                        },
                        enabled = selectedIds.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("download_individually_button"),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("По одному", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        safelyTriggerDownload {
                            viewModel.downloadAllAsZip()
                        }
                    },
                    enabled = filteredMedia.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Скачать всё в ZIP (${filteredMedia.size})", fontSize = 12.sp)
                }
            }
        }
    }

    // Back nav management
    BackHandler(enabled = customView != null || isMediaPanelVisible || currentTabItem != 0 || isBrowsing) {
        if (customView != null) {
            customViewCallback?.onCustomViewHidden()
            customView = null
            customViewCallback = null
        } else if (isMediaPanelVisible) {
            isMediaPanelVisible = false
        } else if (currentTabItem != 0) {
            currentTabItem = 0
        } else if (isBrowsing) {
            webViewRef?.let {
                if (it.canGoBack()) {
                    it.goBack()
                } else {
                    isBrowsing = false
                }
            } ?: run {
                isBrowsing = false
            }
        }
    }

    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = !isBrowsing || currentTabItem != 0 || isBarVisible,
                enter = slideInVertically { -it } + fadeIn(animationSpec = tween(200)),
                exit = slideOutVertically { -it } + fadeOut(animationSpec = tween(200))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                // In-App Update Banner
                val updateInfoState by viewModel.updateInfo.collectAsState()
                val updateProgressState by viewModel.updateProgress.collectAsState()
                val bannerDismissed by viewModel.updateBannerDismissed.collectAsState()

                if (updateInfoState?.hasUpdate == true && !bannerDismissed) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .shadow(4.dp, RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFF2424)) // Crimson background for update attention
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = { viewModel.checkForUpdates(isManualCheck = true, forceSimulate = false) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Проверить заново",
                                    tint = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = updateInfoState?.latestVersionName ?: "",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = l10n.newUpdateAvailable,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }

                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                when (val progress = updateProgressState) {
                                    is com.example.viewmodel.UpdateDownloadProgress.Idle -> {
                                        Button(
                                            onClick = { viewModel.downloadAndInstallUpdate(localContext) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF1E1E1E), // Dark matte element
                                                contentColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            modifier = Modifier.height(34.dp).testTag("update_install_button")
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CloudDownload,
                                                    contentDescription = "Скачать",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(l10n.updateButtonLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    is com.example.viewmodel.UpdateDownloadProgress.Downloading -> {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = if (progress.progress >= 0) {
                                                    "Скачивание: ${(progress.progress * 100).roundToInt()}%"
                                                } else {
                                                    "Скачивание..."
                                                },
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (progress.progress >= 0) {
                                                LinearProgressIndicator(
                                                    progress = { progress.progress },
                                                    modifier = Modifier.width(90.dp).height(4.dp).clip(RoundedCornerShape(2.dp)),
                                                    color = Color.White,
                                                    trackColor = Color.White.copy(alpha = 0.3f)
                                                )
                                            } else {
                                                LinearProgressIndicator(
                                                    modifier = Modifier.width(90.dp).height(4.dp).clip(RoundedCornerShape(2.dp)),
                                                    color = Color.White,
                                                    trackColor = Color.White.copy(alpha = 0.3f)
                                                )
                                            }
                                        }
                                    }
                                    is com.example.viewmodel.UpdateDownloadProgress.Completed -> {
                                        Button(
                                            onClick = { viewModel.downloadAndInstallUpdate(localContext) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF4CD964), // iOS Green
                                                contentColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Text(l10n.installButtonLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    is com.example.viewmodel.UpdateDownloadProgress.Error -> {
                                        Button(
                                            onClick = { viewModel.downloadAndInstallUpdate(localContext) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF333333),
                                                contentColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Text("Повторить", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            IconButton(
                                onClick = { viewModel.dismissUpdateBanner() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Закрыть",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                if (currentTabItem == 0 && isBrowsing) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp))
                    ) {
                        // Main URL Bar Header: Clean row system with Left, Center, and Right blocks properly aligned
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Left block: navigation arrows grouped with correct spacing
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    webViewRef?.let {
                                        if (it.canGoBack()) {
                                            it.goBack()
                                        }
                                    }
                                },
                                enabled = canGoBack,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                            }

                            IconButton(
                                onClick = {
                                    webViewRef?.let {
                                        if (it.canGoForward()) {
                                            it.goForward()
                                        }
                                    }
                                },
                                enabled = canGoForward,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Вперед")
                            }

                            IconButton(
                                onClick = {
                                    isBrowsing = false
                                    viewModel.setBrowsing(false)
                                },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = "На главную",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Center block: URL input field (expanding dynamically)
                        var isUrlBarFocused by remember { mutableStateOf(false) }
                        TextField(
                            value = urlTextFieldValue,
                            onValueChange = {
                                urlTextFieldValue = it
                                textInputUrl = it.text
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("url_input_bar")
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) {
                                        if (!isUrlBarFocused) {
                                            urlTextFieldValue = urlTextFieldValue.copy(
                                                selection = TextRange(0, urlTextFieldValue.text.length)
                                            )
                                            isUrlBarFocused = true
                                        }
                                        isBarVisible = true
                                    } else {
                                        isUrlBarFocused = false
                                    }
                                },
                            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                            placeholder = { Text("Поиск картинок и видео...", fontSize = 13.sp) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent
                             ),
                            shape = RoundedCornerShape(24.dp),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Search,
                                keyboardType = KeyboardType.Uri
                            ),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    focusManager.clearFocus()
                                    val parsedUrl = smartUrlParse(textInputUrl, isHttpsEverywhereEnabled, viewModel::getSearchUrl)
                                    if (parsedUrl.isNotEmpty()) {
                                        viewModel.updateUrl(parsedUrl)
                                        webViewRef?.loadUrl(parsedUrl)
                                    }
                                }
                            ),
                            leadingIcon = {
                                Box {
                                    var showShieldDialog by remember { mutableStateOf(false) }
                                    IconButton(
                                        onClick = { showShieldDialog = true },
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Security,
                                                contentDescription = "Sway Protection Shield",
                                                tint = if (adBlockMode != "off") MaterialTheme.colorScheme.primary else Color.Gray,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            if (adBlockMode != "off" && blockedAdsCount > 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .offset(x = 1.dp, y = (-1).dp)
                                                        .background(MaterialTheme.colorScheme.error, shape = CircleShape)
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = if (blockedAdsCount > 99) "99+" else blockedAdsCount.toString(),
                                                        color = Color.White,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (showShieldDialog) {
                                        AlertDialog(
                                            onDismissRequest = { showShieldDialog = false },
                                            title = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Security,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text("Sway Shield", fontWeight = FontWeight.Bold)
                                                }
                                            },
                                            text = {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .verticalScroll(androidx.compose.foundation.rememberScrollState())
                                                ) {
                                                    Text(
                                                        text = l10n.adBlockStatistics + ":",
                                                        fontSize = 14.sp
                                                    )
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(vertical = 8.dp)
                                                    ) {
                                                        Text(
                                                            text = "$blockedAdsCount",
                                                            fontSize = 32.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (blockedAdsCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = l10n.adBlockCounterLabel,
                                                            fontSize = 14.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    
                                                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                                                    
                                                    Text(
                                                        text = l10n.adBlockHeader,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = l10n.adBlockDesc,
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Spacer(modifier = Modifier.height(12.dp))
                                                    
                                                    listOf(
                                                        "off" to l10n.adBlockOff,
                                                        "standard" to l10n.adBlockStandard,
                                                        "aggressive" to l10n.adBlockAggressive
                                                    ).forEach { (mode, name) ->
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clickable { viewModel.selectAdBlockMode(mode) }
                                                                .padding(vertical = 6.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            RadioButton(
                                                                selected = (adBlockMode == mode),
                                                                onClick = { viewModel.selectAdBlockMode(mode) }
                                                            )
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text(text = name, fontSize = 14.sp)
                                                         }
                                                     }

                                                     val isRu = appLanguage == "ru"
                                                     val isKk = appLanguage == "kk"

                                                     val httpsTitle = if (isRu) "HTTPS Everywhere" else if (isKk) "Шифрленген HTTPS" else "HTTPS Everywhere"
                                                     val httpsDesc = if (isRu) "Принудительный перенос соединений с HTTP на safe HTTPS" else if (isKk) "HTTP байланысын қауіпсіз HTTPS-ке мәжбүрлі түрде бағыттау" else "Redirect HTTP connections to secure HTTPS"

                                                     val scriptTitle = if (isRu) "Блокировка сценариев (JS)" else if (isKk) "JS сценарийлерін блоктау" else "Block scripts (JavaScript)"
                                                     val scriptDesc = if (isRu) "Отключать выполнение JavaScript кода на страницах" else if (isKk) "Парақтағы JavaScript кодын орындауды өшіру" else "Disable execution of JavaScript code on pages"

                                                     val cookieTitle = if (isRu) "Блокировать сторонние куки" else if (isKk) "Бөгде cookie файлдарын блоктау" else "Block third-party cookies"
                                                     val cookieDesc = if (isRu) "Запрет сторонним рекламодателям сохранять куки" else if (isKk) "Үшінші тарап жарнама берушілерінің куки сақтауына тыйым салу" else "Prevent third-party advertisers from setting cookies"

                                                     HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                                     // HTTPS Everywhere switch
                                                     Row(
                                                         modifier = Modifier
                                                             .fillMaxWidth()
                                                             .padding(vertical = 4.dp),
                                                         horizontalArrangement = Arrangement.SpaceBetween,
                                                         verticalAlignment = Alignment.CenterVertically
                                                     ) {
                                                         Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                             Text(httpsTitle, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                             Text(httpsDesc, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                         }
                                                         Switch(
                                                             checked = isHttpsEverywhereEnabled,
                                                             onCheckedChange = { viewModel.toggleHttpsEverywhere(it) }
                                                         )
                                                     }

                                                     HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                                     // Script Blocking switch
                                                     Row(
                                                         modifier = Modifier
                                                             .fillMaxWidth()
                                                             .padding(vertical = 4.dp),
                                                         horizontalArrangement = Arrangement.SpaceBetween,
                                                         verticalAlignment = Alignment.CenterVertically
                                                     ) {
                                                         Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                             Text(scriptTitle, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                             Text(scriptDesc, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                         }
                                                         Switch(
                                                             checked = isScriptBlockingEnabled,
                                                             onCheckedChange = { viewModel.toggleScriptBlocking(it) }
                                                         )
                                                     }

                                                     HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                                     // Cookie blocker switch
                                                     Row(
                                                         modifier = Modifier
                                                             .fillMaxWidth()
                                                             .padding(vertical = 4.dp),
                                                         horizontalArrangement = Arrangement.SpaceBetween,
                                                         verticalAlignment = Alignment.CenterVertically
                                                     ) {
                                                         Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                             Text(cookieTitle, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                             Text(cookieDesc, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                         }
                                                         Switch(
                                                             checked = isBlockThirdPartyCookiesEnabled,
                                                             onCheckedChange = { viewModel.toggleBlockThirdPartyCookies(it) }
                                                         )
                                                     }

                                                     listOf("dummy" to "").forEach { (_, dummyName) ->
                                                         Row {
                                                             Spacer(modifier = Modifier.size(0.dp))
                                                        }
                                                    }
                                                }
                                            },
                                            confirmButton = {
                                                TextButton(onClick = { showShieldDialog = false }) {
                                                    Text("OK")
                                                }
                                            }
                                        )
                                    }
                                }
                            },
                            trailingIcon = {
                                if (urlTextFieldValue.text.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            urlTextFieldValue = TextFieldValue("", selection = TextRange.Zero)
                                            textInputUrl = ""
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Clear, contentDescription = "Очистить URL")
                                    }
                                }
                            }
                        )

                        // Right block: actions (Search glass & Refresh) grouped with correct spacing
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val isBookmarked by viewModel.isBookmarkedFlow(currentUrl).collectAsState(initial = false)
                            IconButton(
                                onClick = {
                                    viewModel.toggleBookmark(webTitle, currentUrl)
                                },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.Bookmark,
                                    contentDescription = "В закладки",
                                    tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = {
                                    focusManager.clearFocus()
                                    val parsedUrl = smartUrlParse(textInputUrl, isHttpsEverywhereEnabled, viewModel::getSearchUrl)
                                    if (parsedUrl.isNotEmpty()) {
                                        viewModel.updateUrl(parsedUrl)
                                        webViewRef?.loadUrl(parsedUrl)
                                    }
                                },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "Перейти")
                            }

                            IconButton(
                                onClick = {
                                    webViewRef?.reload()
                                },
                                enabled = canRefresh,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Обновить",
                                    tint = if (canRefresh) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                            }
                        }
                    }

                    // Web loading progress indicator
                    if (webLoadProgress > 0 && webLoadProgress < 100) {
                        LinearProgressIndicator(
                            progress = { webLoadProgress / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    } else {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
            }
        }
    },
        floatingActionButton = {},
        bottomBar = {
            AnimatedVisibility(
                visible = !isBrowsing || currentTabItem != 0 || isBarVisible,
                enter = slideInVertically { it } + fadeIn(animationSpec = tween(200)),
                exit = slideOutVertically { it } + fadeOut(animationSpec = tween(200))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFEEF1F6)) // Мягкий светло-серый фон
                        .border(width = 0.5.dp, color = Color(0xFFE2E8F0))
                        .navigationBarsPadding()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Кнопка Назад
                    IconButton(
                        onClick = {
                            webViewRef?.let {
                                if (it.canGoBack()) {
                                    it.goBack()
                                }
                            }
                        },
                        enabled = canGoBack
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (canGoBack) Color(0xFF0F172A) else Color(0xFF94A3B8),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Кнопка Вперед
                    IconButton(
                        onClick = {
                            webViewRef?.let {
                                if (it.canGoForward()) {
                                    it.goForward()
                                }
                            }
                        },
                        enabled = canGoForward
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Forward",
                            tint = if (canGoForward) Color(0xFF0F172A) else Color(0xFF94A3B8),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Кнопка Домой
                    val isHomeSelected = currentTabItem == 0 && !isBrowsing
                    IconButton(
                        onClick = {
                            isBrowsing = false
                            viewModel.setBrowsing(false)
                            currentTabItem = 0
                        }
                    ) {
                        if (isHomeSelected) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color(0xFF4F46E5), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Home,
                                    contentDescription = "Home",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Home,
                                contentDescription = "Home",
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Кнопка Вкладок (Менеджер)
                    val isTabsSelected = currentTabItem == 2 || currentTabItem == 3
                    IconButton(
                        onClick = {
                            currentTabItem = 2
                        }
                    ) {
                        if (isTabsSelected) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color(0xFF4F46E5), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Tab,
                                    contentDescription = "Tabs",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        } else {
                            val currentTabsCount = tabs.filter { it.isIncognito == isPrivateMode }.size
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .border(1.5.dp, Color(0xFF475569), RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = currentTabsCount.toString(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF475569)
                                )
                            }
                        }
                    }

                    // Кнопка Меню
                    val isMenuSelected = currentTabItem == 4
                    IconButton(
                        onClick = {
                            currentTabItem = 4
                        }
                    ) {
                        if (isMenuSelected) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color(0xFF4F46E5), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Menu,
                                    contentDescription = "Menu",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Menu,
                                contentDescription = "Menu",
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isWideScreen = maxWidth >= 800.dp
            val browserScreenWidth = maxWidth
            val browserScreenHeight = maxHeight

            val showWebView = currentTabItem == 0 && isBrowsing
            AnimatedVisibility(
                visible = showWebView,
                enter = fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.95f, animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.82f, animationSpec = tween(300)),
                modifier = Modifier.fillMaxSize()
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left side: Nest WebView. It always remains here, ensuring no recreation/reloading!
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f)
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                (persistentWebView.parent as? android.view.ViewGroup)?.removeView(persistentWebView)
                                persistentWebView.apply {
                                    isFocusable = true
                                    isFocusableInTouchMode = true
                                    // Enable safe standard cookies and session management
                                    try {
                                        CookieManager.getInstance().setAcceptCookie(true)
                                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, !isBlockThirdPartyCookiesEnabled)
                                    } catch (e: Exception) {}

                                    @Suppress("DEPRECATION")
                                    settings.apply {
                                        javaScriptEnabled = !isScriptBlockingEnabled
                                        domStorageEnabled = true
                                        databaseEnabled = true
                                        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                                        useWideViewPort = true
                                        loadWithOverviewMode = true
                                        builtInZoomControls = true
                                        displayZoomControls = false
                                        cacheMode = WebSettings.LOAD_DEFAULT
                                        
                                        // Essential settings for modern auth/login multi-window support (e.g. Google/Facebook accounts Popup redirect flow)
                                        setSupportMultipleWindows(false)
                                        setJavaScriptCanOpenWindowsAutomatically(true)
                                        
                                        userAgentString = viewModel.getUserAgent()
                                        if (isFastRenderingEnabled) {
                                            loadsImagesAutomatically = true
                                        }
                                    }
                                    
                                    if (android.os.Build.VERSION.SDK_INT >= 23) {
                                        setOnScrollChangeListener { _, _, _, _, oldScrollY ->
                                            val currentY = scrollY
                                            val delta = currentY - oldScrollY
                                            if (delta > 10 && currentY > 80) {
                                                if (isBarVisible) {
                                                    isBarVisible = false
                                                }
                                            } else if (delta < -2) {
                                                if (!isBarVisible) {
                                                    isBarVisible = true
                                                }
                                            }
                                        }
                                    }
                                    
                                    setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

                                    webViewClient = object : WebViewClient() {
                                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                            super.onPageStarted(view, url, favicon)
                                            
                                            // Handle Google Sign-in User Agent Bypassing ("disallowed_useragent")
                                            val currentUrlStr = url ?: ""
                                            val isGoogleAuth = false
                                            if (isGoogleAuth) {
                                                // High compatibility mobile Safari user agent completely acceptable to Google Authentication
                                                view?.settings?.userAgentString = viewModel.getUserAgent()
                                            } else {
                                                view?.settings?.userAgentString = viewModel.getUserAgent()
                                            }

                                            viewModel.resetBlockedAdsCount()
                                            url?.let {
                                                viewModel.updateUrl(it)
                                            }
                                            viewModel.clearDiscoveredMedia()
                                            try {
                                                val js = getYtEnhancerJs(isYtBackgroundEnabled, isSponsorBlockEnabled, isYtAutoMaxQualityEnabled)
                                                view?.evaluateJavascript(js, null)
                                            } catch (e: Exception) {}
                                        }

                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            super.onPageFinished(view, url)
                                            if (isFastRenderingEnabled) {
                                                try {
                                                    val fastCss = "body { text-rendering: optimizeSpeed; -webkit-font-smoothing: antialiased; }"
                                                    val speedStyleStr = """
                                                        (function() {
                                                            const sStyle = document.createElement('style');
                                                            sStyle.innerHTML = '$fastCss';
                                                            document.head.appendChild(sStyle);
                                                            // Force lazy load for off-screen images & frames to accelerate visual layout render
                                                            document.querySelectorAll('img:not([loading])').forEach(el => el.setAttribute('loading', 'lazy'));
                                                            document.querySelectorAll('iframe:not([loading])').forEach(el => el.setAttribute('loading', 'lazy'));
                                                        })();
                                                    """.trimIndent()
                                                    view?.evaluateJavascript(speedStyleStr, null)
                                                } catch (e: Exception) {}
                                            }
                                            if (isAutoCookieEnabled) {
                                                try {
                                                    val cookieJs = """
                                                        (function() {
                                                            const rejectSelectors = [
                                                                "button[id*='cookie-reject']", "button[class*='cookie-reject']",
                                                                "button[id*='reject-cookie']", "button[class*='reject-cookie']",
                                                                "[id*='cookie-accept-necessary']", "[class*='cookie-accept-necessary']",
                                                                "button[id*='decline']", "button[class*='decline']",
                                                                ".cookie-consent-reject", "#cookie-reject", ".cookie-decline", "#cookie-decline",
                                                                "button[aria-label*='Decline']", "button[aria-label*='Reject']",
                                                                "button[aria-label*='cookie-decline']", "button[aria-label*='cookie-reject']",
                                                                "button[class*='reject']", "button[id*='reject']",
                                                                "#cn-refuse-cookie", ".cn-refuse-cookie"
                                                            ];
                                                            const rejectWords = [
                                                                "принять только необходимые", "отклонить все", "не согласен", "отклонить", "только обязательные", "только необходимые", "отказаться", "без согласия",
                                                                "reject all", "decline all", "decline", "reject", "only necessary", "necessary only", "essential only", 
                                                                "use necessary", "refuse", "do not accept", "block cookies", "deny", "disagree", "reject cookies", "no, thanks", "no thanks",
                                                                "бұғаттау", "қабылдамау"
                                                            ];
                                                            function findButtonByText(textList) {
                                                                const buttons = document.querySelectorAll("button, a, div[role='button'], span");
                                                                for (let btn of buttons) {
                                                                    const btnText = (btn.textContent || btn.innerText || "").trim();
                                                                    if (!btnText) continue;
                                                                    for (let word of textList) {
                                                                        if (btnText.toLowerCase() === word.toLowerCase() || 
                                                                            (btnText.length < 50 && btnText.toLowerCase().includes(word.toLowerCase()))) {
                                                                            return btn;
                                                                        }
                                                                    }
                                                                }
                                                                return null;
                                                            }
                                                            function restoreScrolling() {
                                                                const resetStyles = (el) => {
                                                                    if (!el) return;
                                                                    const style = window.getComputedStyle(el);
                                                                    if (style.overflow === 'hidden' || style.overflowY === 'hidden') {
                                                                        el.style.setProperty('overflow', 'auto', 'important');
                                                                        el.style.setProperty('overflow-y', 'auto', 'important');
                                                                    }
                                                                    if (style.position === 'fixed') {
                                                                        el.style.setProperty('position', 'static', 'important');
                                                                    }
                                                                };
                                                                resetStyles(document.body);
                                                                resetStyles(document.documentElement);
                                                            }
                                                            function handleCookies() {
                                                                for (let selector of rejectSelectors) {
                                                                    try {
                                                                        const btn = document.querySelector(selector);
                                                                        if (btn && btn.offsetHeight > 0) {
                                                                            btn.click();
                                                                            setTimeout(restoreScrolling, 200);
                                                                            return true;
                                                                        }
                                                                    } catch(e){}
                                                                }
                                                                const rejectBtn = findButtonByText(rejectWords);
                                                                if (rejectBtn && rejectBtn.offsetHeight > 0) {
                                                                    rejectBtn.click();
                                                                    setTimeout(restoreScrolling, 200);
                                                                    return true;
                                                                }
                                                                const bannerSelectors = [
                                                                    "[class*='cookie-consent']", "[id*='cookie-consent']",
                                                                    "[class*='cookie-banner']", "[id*='cookie-banner']",
                                                                    "[class*='cookiebar']", "[id*='cookiebar']",
                                                                    "[class*='cookie-notice']", "[id*='cookie-notice']",
                                                                    "[class*='cookie_consent']", "[id*='cookie_consent']",
                                                                    "[class*='qc-cmp2-container']", "#onetrust-consent-sdk",
                                                                    ".sail-cookie-consent", "#cookieyes-banner", ".cookieyes-banner",
                                                                    ".terms-feed-cookie-consent-banner", "[class*='consent-banner']"
                                                                ];
                                                                let foundAndHidden = false;
                                                                bannerSelectors.forEach(sel => {
                                                                    document.querySelectorAll(sel).forEach(el => {
                                                                        el.style.setProperty('display', 'none', 'important');
                                                                        el.style.setProperty('visibility', 'hidden', 'important');
                                                                        el.style.setProperty('opacity', '0', 'important');
                                                                        el.style.setProperty('pointer-events', 'none', 'important');
                                                                        foundAndHidden = true;
                                                                    });
                                                                });
                                                                const backdropSelectors = [
                                                                    "[class*='cookie-backdrop']", "[id*='cookie-backdrop']",
                                                                    "[class*='cookie-overlay']", "[id*='cookie-overlay']",
                                                                    ".cc-overlay", ".cc-backdrop", ".fancybox-overlay"
                                                                ];
                                                                backdropSelectors.forEach(sel => {
                                                                    document.querySelectorAll(sel).forEach(el => {
                                                                        el.style.setProperty('display', 'none', 'important');
                                                                        el.style.setProperty('visibility', 'hidden', 'important');
                                                                    });
                                                                });
                                                                if (foundAndHidden) {
                                                                    restoreScrolling();
                                                                }
                                                            }
                                                            handleCookies();
                                                            setTimeout(handleCookies, 500);
                                                            setTimeout(handleCookies, 1500);
                                                            setTimeout(handleCookies, 3000);
                                                            if (window.cookieObserver) { window.cookieObserver.disconnect(); }
                                                            window.cookieObserver = new MutationObserver(() => { handleCookies(); });
                                                            window.cookieObserver.observe(document.body || document.documentElement, { childList: true, subtree: true });
                                                        })();
                                                    """.trimIndent()
                                                    view?.evaluateJavascript(cookieJs, null)
                                                } catch (e: Exception) {}
                                            }
                                             if (adBlockMode != "off") {
                                                 try {
                                                     val hideSelectors = " .ad, .ads, .adsbox, .ad-banner, .ad-container, .ad-image, .advertisement, .adv, .popup-ad, [id*='google_ads'], [class*='google_ads'], [id*='yandex_rtb'], [class*='yandex_rtb'], iframe[src*='doubleclick'], iframe[src*='googleads'] "
                                                     val styleStr = "const style = document.createElement('style'); style.innerHTML = '$hideSelectors { display: none !important; visibility: hidden !important; height: 0 !important; min-height: 0 !important; width: 0 !important; opacity: 0 !important; pointer-events: none !important; }'; document.head.appendChild(style);"
                                                     view?.evaluateJavascript(styleStr, null)
                                                 } catch (e: Exception) {}
                                             }
                                            url?.let {
                                                viewModel.updateUrl(it)
                                                viewModel.updateWebNavigation(
                                                    title = view?.title ?: it,
                                                    canGoBack = view?.canGoBack() ?: false,
                                                    canGoForward = view?.canGoForward() ?: false,
                                                     backForwardList = view?.copyBackForwardList()
                                                )
                                            }
                                            // Auto inject crawler on finish
                                            runCrawlerInWebView()
                                            try {
                                                val js = getYtEnhancerJs(isYtBackgroundEnabled, isSponsorBlockEnabled, isYtAutoMaxQualityEnabled)
                                                view?.evaluateJavascript(js, null)
                                            } catch (e: Exception) {}
                                        }

                                        override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                                            val url = request?.url?.toString() ?: return false
                                            if (url.startsWith("http://") || url.startsWith("https://")) {
                                                // Let WebView handle standard HTTP/HTTPS URLs natively to prevent session/redirect disruption and white screens
                                                return false
                                            }
                                            // Safely handle custom deep-link intents (tel:, whatsapp:, telegram:, etc) without crashing or opening blank screen
                                            try {
                                                val intent = android.content.Intent.parseUri(url, android.content.Intent.URI_INTENT_SCHEME)
                                                view?.context?.startActivity(intent)
                                            } catch (e: Exception) {
                                                // ignore unhandled intents
                                            }
                                            return true
                                        }

                                        override fun shouldInterceptRequest(
                                            view: WebView?,
                                            request: android.webkit.WebResourceRequest?
                                        ): android.webkit.WebResourceResponse? {
                                            val urlStr = request?.url?.toString() ?: ""
                                            val urlLower = urlStr.lowercase()
                                            
                                            val isTracker = urlLower.contains("analytics") || 
                                                    urlLower.contains("telemetry") || 
                                                    urlLower.contains("google-analytics") || 
                                                    urlLower.contains("googletagmanager") || 
                                                    urlLower.contains("facebook.com/tr") ||
                                                    urlLower.contains("amplitude") ||
                                                    urlLower.contains("mixpanel") ||
                                                    urlLower.contains("mc.yandex") ||
                                                    urlLower.contains("clarity.ms") ||
                                                    urlLower.contains("scorecardresearch") ||
                                                    urlLower.contains("sentry.io") ||
                                                    urlLower.contains("bugsnag") ||
                                                    urlLower.contains("crashlytics") ||
                                                    urlLower.contains("segment.io") ||
                                                    urlLower.contains("statcounter") ||
                                                    urlLower.contains("hotjar") ||
                                                    urlLower.contains("ads-twitter") ||
                                                    urlLower.contains("pixel.facebook")
                                            
                                            // Block trackers if tracker blocking switch is on, or adblock is not off
                                            val blockTracker = isTracker && (isTrackerBlockingEnabled || adBlockMode != "off")
                                            
                                            var blockAd = false
                                            val mode = adBlockMode
                                            if (mode != "off") {
                                                val isPromo = urlLower.contains("doubleclick") ||
                                                        urlLower.contains("googleads") ||
                                                        urlLower.contains("googlesyndication") ||
                                                        urlLower.contains("pagead2") ||
                                                        urlLower.contains("adservice") ||
                                                        urlLower.contains("adserver") ||
                                                        urlLower.contains("adsystem") ||
                                                        urlLower.contains("amazon-adsystem") ||
                                                        urlLower.contains("adnxs") ||
                                                        urlLower.contains("criteo") ||
                                                        urlLower.contains("taboola") ||
                                                        urlLower.contains("outbrain") ||
                                                        urlLower.contains("popads") ||
                                                        urlLower.contains("adcash") ||
                                                        urlLower.contains("propellerads") ||
                                                        urlLower.contains("mgid") ||
                                                        urlLower.contains("an.yandex") ||
                                                        urlLower.contains("rtb.yandex") ||
                                                        urlLower.contains("direct.yandex") ||
                                                        urlLower.contains("/ads/") || 
                                                        urlLower.contains("_ads_") || 
                                                        urlLower.contains("-ads-") || 
                                                        urlLower.contains(".ads.") ||
                                                        urlLower.contains("/banners/") ||
                                                        urlLower.contains("popunder") ||
                                                        urlLower.contains("popupads") ||
                                                        urlLower.contains("youtube.com/api/stats/ads") ||
                                                        urlLower.contains("youtube.com/pagead") ||
                                                        urlLower.contains("video-stats.l.google.com") ||
                                                        urlLower.contains("pubads.g.doubleclick.net") ||
                                                        urlLower.contains("youtube.com/ptracking") ||
                                                        urlLower.contains("youtube.com/ad_companion") ||
                                                        urlLower.contains("youtube.com/get_midroll_info") ||
                                                        urlLower.contains("youtube.com/api/stats/atr") ||
                                                        urlLower.contains("youtube.com/api/stats/delayplay")
                                                
                                                if (isPromo) {
                                                    blockAd = true
                                                } else if (mode == "aggressive") {
                                                    blockAd = urlLower.contains("banner") || 
                                                            urlLower.contains("advert") || 
                                                            urlLower.contains("sponsor") ||
                                                            urlLower.contains("pop-up") ||
                                                            urlLower.contains("popup") ||
                                                            urlLower.contains("clickunder") ||
                                                            urlLower.contains("external-tracking")
                                                }
                                            }

                                            // Under Fast Rendering, also block non-essential slow social frame/script loading
                                            var blockFastRenderingSubresource = false
                                            if (isFastRenderingEnabled) {
                                                blockFastRenderingSubresource = urlLower.contains("platform.twitter.com") ||
                                                        urlLower.contains("connect.facebook.net") ||
                                                        urlLower.contains("platform.instagram.com") ||
                                                        urlLower.contains("/show_ads.js") ||
                                                        urlLower.contains("coingate.com/merchant") ||
                                                        urlLower.contains("disqus.com/embed")
                                            }

                                            if (blockTracker || blockAd || blockFastRenderingSubresource) {
                                                viewModel.incrementBlockedAdsCount()
                                                return android.webkit.WebResourceResponse(
                                                    "text/plain", "UTF-8", 
                                                    java.io.ByteArrayInputStream("".toByteArray())
                                                )
                                            }
                                            return super.shouldInterceptRequest(view, request)
                                        }
                                    }

                                    webChromeClient = object : WebChromeClient() {
                                         override fun onShowCustomView(v: android.view.View?, c: CustomViewCallback?) {
                                             super.onShowCustomView(v, c)
                                             customView = v
                                             customViewCallback = c
                                         }

                                         override fun onHideCustomView() {
                                             super.onHideCustomView()
                                             customView = null
                                             customViewCallback = null
                                         }
                                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                            super.onProgressChanged(view, newProgress)
                                            viewModel.updateWebProgress(newProgress)
                                        }

                                        // Support nested popup windows for logging in via Google/OAuth accounts seamlessly inside the same WebView
                                        override fun onCreateWindow(
                                            view: WebView?,
                                            isDialog: Boolean,
                                            isUserGesture: Boolean,
                                            resultMsg: android.os.Message?
                                        ): Boolean {
                                            val transport = resultMsg?.obj as? WebView.WebViewTransport
                                            if (transport != null) {
                                                val tempWebView = WebView(view?.context ?: return false).apply {
                                                    webViewClient = object : WebViewClient() {
                                                        override fun shouldOverrideUrlLoading(
                                                            v: WebView?,
                                                            request: android.webkit.WebResourceRequest?
                                                        ): Boolean {
                                                            val targetUrl = request?.url?.toString() ?: ""
                                                            if (targetUrl.isNotEmpty()) {
                                                                view?.loadUrl(targetUrl)
                                                            }
                                                            return true
                                                        }
                                                        
                                                        @Deprecated("Deprecated in Java")
                                                        override fun shouldOverrideUrlLoading(v: WebView?, targetUrl: String?): Boolean {
                                                            if (targetUrl != null && targetUrl.isNotEmpty()) {
                                                                view?.loadUrl(targetUrl)
                                                            }
                                                            return true
                                                        }

                                                        override fun onPageStarted(v: WebView?, targetUrl: String?, favicon: Bitmap?) {
                                                            super.onPageStarted(v, targetUrl, favicon)
                                                            if (targetUrl != null && targetUrl.isNotEmpty()) {
                                                                view?.loadUrl(targetUrl)
                                                            }
                                                        }
                                                    }
                                                }
                                                transport.webView = tempWebView
                                                resultMsg.sendToTarget()
                                                return true
                                            }
                                            return false
                                        }
                                    }

                                    // Handle native clicks on files and download links automatically inside WebView
                                    setDownloadListener { url, userAgentLocal, contentDisposition, mimetype, contentLength ->
                                        val guessName = android.webkit.URLUtil.guessFileName(url, contentDisposition, mimetype) ?: "file_${url.hashCode()}"
                                        val ext = guessName.substringAfterLast('.', "bin")
                                        val isImage = mimetype.startsWith("image/")
                                        val isVideo = mimetype.startsWith("video/")
                                        val item = MediaItem(
                                            id = url.hashCode().toString(),
                                            url = url,
                                            type = if (isImage) "image" else if (isVideo) "video" else "other",
                                            ext = ext,
                                            filename = guessName,
                                            tagName = "DIRECT_WEB_DOWNLOAD",
                                            sizeBytes = if (contentLength > 0) contentLength else -1L
                                        )
                                        safelyTriggerDownload {
                                            viewModel.downloadSingleMediaFile(item)
                                        }
                                    }

                                    // Connect Javascript Bridge
                                    addJavascriptInterface(
                                        MediaPickerJSInterface(
                                            onDiscovered = { json ->
                                                viewModel.parseAndAddDiscoveredMedia(json)
                                            },
                                            onError = { _ -> },
                                            onAdBlocked = {
                                                viewModel.incrementBlockedAdsCount()
                                            },
                                            onSpaUrlChanged = { newUrl ->
                                                coroutineScope.launch {
                                                    viewModel.updateUrl(newUrl)
                                                    viewModel.clearDiscoveredMedia()
                                                    runCrawlerInWebView()
                                                }
                                            }
                                        ),
                                        "AndroidMediaDownloader"
                                    )

                                    webViewRef = this
                                    loadUrl(currentUrl)
                                }
                            },
                            update = { webView ->
                                webView.settings.apply {
                                    val shouldEnableJs = !isScriptBlockingEnabled
                                    if (javaScriptEnabled != shouldEnableJs) {
                                        javaScriptEnabled = shouldEnableJs
                                    }
                                    if (isFastRenderingEnabled) {
                                        loadsImagesAutomatically = true
                                        if (android.os.Build.VERSION.SDK_INT >= 23) {
                                            offscreenPreRaster = true
                                        }
                                    }
                                }
                                if (isFastRenderingEnabled) {
                                    webView.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                                } else {
                                    webView.setLayerType(android.view.View.LAYER_TYPE_NONE, null)
                                }
                                try {
                                    CookieManager.getInstance().setAcceptThirdPartyCookies(webView, !isBlockThirdPartyCookiesEnabled)
                                } catch (e: Exception) {}
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Custom Draggable & Snapping Floating Action Button with Animated Trajectory Settings!
                        if (!isMediaPanelVisible) {
                            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                val density = LocalDensity.current
                                val fabSizePx = with(density) { 56.dp.toPx() }
                                
                                // Get the Box constraints measurements in pixels safely
                                val screenWidthPx = with(density) { maxWidth.toPx() }
                                val screenHeightPx = with(density) { maxHeight.toPx() }

                                Box(modifier = Modifier.fillMaxSize()) {
                                    // Pulsing scale animation when rawMediaItems updates
                                    val count = rawMediaItems.size
                                    var lastCount by remember { mutableStateOf(0) }
                                    val scaleAnim = remember { Animatable(1f) }
                                    
                                    LaunchedEffect(count) {
                                        if (count > lastCount) {
                                            scaleAnim.animateTo(1.25f, animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium))
                                            scaleAnim.animateTo(1.0f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
                                        }
                                        lastCount = count
                                    }
                                    
                                    // Orbital loop trajectory animation when ORBIT_PATH mode is active
                                    val infiniteTransition = rememberInfiniteTransition(label = "orbital_movement")
                                    val orbitOffsetX by infiniteTransition.animateFloat(
                                        initialValue = -12f,
                                        targetValue = 12f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(1500, easing = FastOutSlowInEasing),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "orbitX"
                                    )
                                    val orbitOffsetY by infiniteTransition.animateFloat(
                                        initialValue = -12f,
                                        targetValue = 12f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(1900, easing = FastOutSlowInEasing),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "orbitY"
                                    )
                                    
                                    val orbitX = if (fabTrajectoryMode == FabTrajectoryMode.ORBIT_PATH) orbitOffsetX else 0f
                                    val orbitY = if (fabTrajectoryMode == FabTrajectoryMode.ORBIT_PATH) orbitOffsetY else 0f
                                    
                                    val finalOffsetX = dragOffsetX.value + orbitX
                                    val finalOffsetY = dragOffsetY.value + orbitY
                                    
                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(bottom = 24.dp, end = 24.dp, start = 24.dp, top = 24.dp) // Generous margin for padding bounds and safety
                                            .offset {
                                                IntOffset(
                                                    finalOffsetX.roundToInt(),
                                                    finalOffsetY.roundToInt()
                                                )
                                            }
                                    ) {
                                        // Floating configuration bar for selecting the trajectory mode
                                        AnimatedVisibility(
                                            visible = isTrajectoryMenuExpanded,
                                            enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                                            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
                                        ) {
                                            Card(
                                                shape = RoundedCornerShape(20.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(12.dp)
                                                ),
                                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                                modifier = Modifier.padding(bottom = 12.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // Mode: Magnet (Snap to Edges)
                                                    TextButton(
                                                        onClick = {
                                                            fabTrajectoryMode = FabTrajectoryMode.SNAP_EDGE
                                                            isTrajectoryMenuExpanded = false
                                                            // Trigger instant snap calculation
                                                            val halfScreen = -(screenWidthPx - fabSizePx) / 2f
                                                            val targetX = if (dragOffsetX.value < halfScreen) {
                                                                -(screenWidthPx - fabSizePx - with(density) { 48.dp.toPx() })
                                                            } else {
                                                                0f
                                                            }
                                                            coroutineScope.launch {
                                                                dragOffsetX.animateTo(targetX, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
                                                            }
                                                            android.widget.Toast.makeText(localContext, "Выбрано: Магнитная траектория", android.widget.Toast.LENGTH_SHORT).show()
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                                        colors = ButtonDefaults.textButtonColors(
                                                            contentColor = if (fabTrajectoryMode == FabTrajectoryMode.SNAP_EDGE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                            Icon(Icons.Default.KeyboardDoubleArrowLeft, contentDescription = null, modifier = Modifier.size(16.dp))
                                                            Text("Магнит", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                    
                                                    // Mode: Free Float
                                                    TextButton(
                                                        onClick = {
                                                            fabTrajectoryMode = FabTrajectoryMode.FREE_FLOAT
                                                            isTrajectoryMenuExpanded = false
                                                            android.widget.Toast.makeText(localContext, "Выбрано: Свободное парение", android.widget.Toast.LENGTH_SHORT).show()
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                                        colors = ButtonDefaults.textButtonColors(
                                                            contentColor = if (fabTrajectoryMode == FabTrajectoryMode.FREE_FLOAT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                            Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(16.dp))
                                                            Text("Оставить тут", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                    
                                                    // Mode: Orbit Path (Interactive Floating Cosmos Mode)
                                                    TextButton(
                                                        onClick = {
                                                            fabTrajectoryMode = FabTrajectoryMode.ORBIT_PATH
                                                            isTrajectoryMenuExpanded = false
                                                            android.widget.Toast.makeText(localContext, "Выбрано: Космическая орбита 🪐", android.widget.Toast.LENGTH_SHORT).show()
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                                        colors = ButtonDefaults.textButtonColors(
                                                            contentColor = if (fabTrajectoryMode == FabTrajectoryMode.ORBIT_PATH) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                                            Text("Парение", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        
                                        // Main button cluster including Badge and settings trigger
                                        Box(
                                            contentAlignment = Alignment.TopStart,
                                            modifier = Modifier.size(72.dp) // Size that accommodates the FAB and its tune gear badge nicely
                                        ) {
                                            BadgedBox(
                                                badge = {
                                                    if (rawMediaItems.isNotEmpty()) {
                                                        Badge(
                                                            modifier = Modifier.offset(x = (-4).dp, y = 4.dp),
                                                            containerColor = MaterialTheme.colorScheme.error,
                                                            contentColor = Color.White
                                                        ) {
                                                            Text(
                                                                text = rawMediaItems.size.toString(),
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 11.sp
                                                            )
                                                        }
                                                    }
                                                },
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .scale(scaleAnim.value)
                                                    .pointerInput(fabTrajectoryMode) {
                                                        detectDragGestures(
                                                            onDragStart = { _ ->
                                                                coroutineScope.launch {
                                                                    dragOffsetX.stop()
                                                                    dragOffsetY.stop()
                                                                }
                                                            },
                                                            onDrag = { change, dragAmount ->
                                                                change.consume()
                                                                // Safety margins: exactly 48dp so it fits beautifully, leaving 16dp pad
                                                                val limitX = -(screenWidthPx - fabSizePx - with(density) { 48.dp.toPx() })
                                                                val limitY = -(screenHeightPx - fabSizePx - with(density) { 48.dp.toPx() })
                                                                
                                                                val newX = (dragOffsetX.value + dragAmount.x).coerceIn(limitX, 0f)
                                                                val newY = (dragOffsetY.value + dragAmount.y).coerceIn(limitY, 0f)
                                                                
                                                                coroutineScope.launch {
                                                                    dragOffsetX.snapTo(newX)
                                                                    dragOffsetY.snapTo(newY)
                                                                }
                                                            },
                                                            onDragEnd = {
                                                                // Handle snapping behaviour if mode requires it
                                                                if (fabTrajectoryMode == FabTrajectoryMode.SNAP_EDGE) {
                                                                    val limitX = -(screenWidthPx - fabSizePx - with(density) { 48.dp.toPx() })
                                                                    val halfScreen = limitX / 2f
                                                                    val targetX = if (dragOffsetX.value < halfScreen) limitX else 0f
                                                                    coroutineScope.launch {
                                                                        dragOffsetX.animateTo(
                                                                            targetX,
                                                                            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        )
                                                    }
                                            ) {
                                                FloatingActionButton(
                                                    onClick = {
                                                        // Fetch again to ensure newly lazy loaded images are crawled
                                                        runCrawlerInWebView()
                                                        isMediaPanelVisible = true
                                                    },
                                                    containerColor = MaterialTheme.colorScheme.primary,
                                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.testTag("floating_crawler_toggle")
                                                ) {
                                                    Icon(
                                                        Icons.Default.Download, 
                                                        contentDescription = "Скачать медиа", 
                                                        modifier = Modifier.size(28.dp)
                                                    )
                                                }
                                            }
                                            
                                            // Tiny settings gear badge overlapping the FAB to quickly trigger movement trajectory customization
                                            IconButton(
                                                onClick = { 
                                                    isTrajectoryMenuExpanded = !isTrajectoryMenuExpanded 
                                                    if (isTrajectoryMenuExpanded) {
                                                        android.widget.Toast.makeText(localContext, "Вы можете перетаскивать эту кнопку в любое место экрана!", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                modifier = Modifier
                                                    .size(26.dp)
                                                    .background(MaterialTheme.colorScheme.tertiaryContainer, shape = CircleShape)
                                                    .align(Alignment.TopStart)
                                                    .offset(x = 4.dp, y = 4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Settings,
                                                    contentDescription = "Траектория",
                                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // If wide screen and media panel is visible, show indeed as a side-by-side split screen supporting panel!
                    if (isWideScreen && isMediaPanelVisible) {
                        VerticalDivider(
                            modifier = Modifier.fillMaxHeight(),
                            color = MaterialTheme.colorScheme.outlineVariant,
                            thickness = 1.dp
                        )
                        Surface(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(380.dp)
                                .background(MaterialTheme.colorScheme.background),
                            color = MaterialTheme.colorScheme.background,
                            tonalElevation = 8.dp
                        ) {
                            MediaPanelContent()
                        }
                    }
                }

                // If compact screen (or portrait mode), show the media panel as a beautiful sliding bottom overlay!
                if (!isWideScreen) {
                    AnimatedVisibility(
                        visible = isMediaPanelVisible,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it })
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.45f))
                                .clickable { isMediaPanelVisible = false }, // Clicking backdrop closes overlay
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .widthIn(max = 680.dp)
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background)
                                    .clickable(enabled = false) {}, // Consume clicks over content to prevent closing
                                color = MaterialTheme.colorScheme.background,
                                tonalElevation = 8.dp
                            ) {
                                MediaPanelContent()
                            }
                        }
                    }
                }
            }
            if (currentTabItem == 0 && !isBrowsing) {
                // MULTI-THEME HOME / INCOGNITO SCREEN (Screen 01 & Screen 10)
                var isSearchFocused by remember { mutableStateOf(false) }
                val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                
                // Select localized greeting based on time of day
                val timeGreeting = when {
                    currentHour in 5..11 -> "Доброе утро"
                    currentHour in 12..16 -> "Добрый день"
                    currentHour in 17..22 -> "Добрый вечер"
                    else -> "Доброй ночи"
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (isPrivateMode) Color(0xFF0F172A) else MaterialTheme.colorScheme.background)
                        .statusBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    if (isPrivateMode) {
                        // --- SCREEN 10: INCOGNITO MODE ACTIVE ---
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        val incognitoGradient = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF818CF8),
                                Color(0xFFC084FC)
                            )
                        )

                        // Glass-morphic glowing shield container
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .size(96.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Neon glow halo
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .graphicsLayer { alpha = 0.15f }
                                    .background(Color(0xFF818CF8), CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(Color(0xFF1E1B4B), CircleShape)
                                    .border(2.dp, Color(0xFF818CF8), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Private Mode Active",
                                    tint = Color(0xFFC084FC),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Text(
                            text = l10n.privateModeActive,
                            style = androidx.compose.ui.text.TextStyle(
                                brush = incognitoGradient,
                                fontWeight = FontWeight.Black,
                                fontSize = 24.sp,
                                letterSpacing = (-0.5).sp
                            ),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Text(
                            text = "Вы вошли в приватный режим сессии Sway. Данные поиска, файлы cookies и история посещений будут стерты с этого устройства сразу при закрытии вкладок.",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8),
                            lineHeight = 18.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )

                        Spacer(modifier = Modifier.height(26.dp))

                        // Custom voice-search search box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp)
                                .background(Color(0xFF0F172A), RoundedCornerShape(29.dp))
                                .border(1.5.dp, if (isSearchFocused) Color(0xFF818CF8) else Color(0xFF1E293B), RoundedCornerShape(29.dp))
                        ) {
                            TextField(
                                value = textInputUrl,
                                onValueChange = { textInputUrl = it },
                                placeholder = { 
                                    Text(
                                        text = "Поиск Sway инкогнито...", 
                                        fontSize = 14.sp,
                                        color = Color(0xFF475569)
                                    ) 
                                },
                                leadingIcon = { 
                                    Icon(
                                        imageVector = Icons.Default.Search, 
                                        contentDescription = null, 
                                        tint = if (isSearchFocused) Color(0xFF818CF8) else Color(0xFF475569)
                                    ) 
                                },
                                trailingIcon = {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 4.dp)) {
                                        if (textInputUrl.isNotEmpty()) {
                                            IconButton(onClick = { textInputUrl = "" }) {
                                                Icon(Icons.Default.Clear, contentDescription = "Очистить", tint = Color(0xFF94A3B8))
                                            }
                                        }
                                        IconButton(onClick = {
                                            android.widget.Toast.makeText(localContext, "Голосовой поиск активирован", android.widget.Toast.LENGTH_SHORT).show()
                                        }) {
                                            Icon(Icons.Default.Mic, contentDescription = "Голосовой ввод", tint = Color(0xFF818CF8))
                                        }
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .onFocusChanged { isSearchFocused = it.isFocused },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(29.dp),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        val query = textInputUrl.trim()
                                        if (query.isNotEmpty()) {
                                            val parsedUrl = smartUrlParse(query, isHttpsEverywhereEnabled, viewModel::getSearchUrl)
                                            viewModel.updateUrl(parsedUrl)
                                            webViewRef?.loadUrl(parsedUrl)
                                            isBrowsing = true
                                            focusManager.clearFocus()
                                        }
                                    }
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val query = textInputUrl.trim()
                                if (query.isNotEmpty()) {
                                    val parsedUrl = smartUrlParse(query, isHttpsEverywhereEnabled, viewModel::getSearchUrl)
                                    viewModel.updateUrl(parsedUrl)
                                    webViewRef?.loadUrl(parsedUrl)
                                    isBrowsing = true
                                    focusManager.clearFocus()
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.End)
                                .height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6366F1),
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 4.dp)
                        ) {
                            Text("Поиск инкогнито", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Features outline in Private mode (Replacing bullet points with high-end verify chips)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color(0xFF1E293B))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = "БЕЗОПАСНОСТЬ И КОНФИДЕНЦИАЛЬНОСТЬ",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF818CF8),
                                    letterSpacing = 1.2.sp
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                
                                val incognitoRules = listOf(
                                    "История просмотра надёжно скрыта на устройстве",
                                    "Автоматическая блокировка рекламных трекеров",
                                    "Очистка cookies и сессий при выходе из вкладок",
                                    "Скачивание файлов и картинок происходит скрытно",
                                    "Защита от случайного захвата экрана"
                                )
                                incognitoRules.forEach { rule ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF34D399), // Emerald checkmark
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = rule,
                                            fontSize = 13.sp,
                                            color = Color(0xFFCBD5E1)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        
                        OutlinedButton(
                            onClick = { isPrivateMode = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(25.dp),
                            border = BorderStroke(1.5.dp, Color(0xFFE2E8F0).copy(alpha = 0.2f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFA5B4FC))
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Выйти из режима инкогнито", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    } else {
                        // --- SCREEN 01: BEAUTIFUL NORMAL HOME SCREEN ---
                        // Top App Bar Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = "Sway",
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier.size(26.dp)
                            )
                            Text(
                                text = "Sway",
                                color = Color(0xFF4F46E5),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black
                            )
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More",
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Gradient squircle logo
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .size(80.dp)
                                .shadow(8.dp, RoundedCornerShape(22.dp))
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(Color(0xFF8B5CF6), Color(0xFF4F46E5))
                                    ),
                                    shape = RoundedCornerShape(22.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "S",
                                color = Color.White,
                                fontSize = 44.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Sway Browser",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Custom Search Field Capsule
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .background(Color(0xFFEEF1F6), RoundedCornerShape(28.dp))
                                .clickable {
                                    currentTabItem = 1 // Switch to Search tab
                                }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Search or enter URL",
                                    color = Color(0xFF475569).copy(alpha = 0.6f),
                                    fontSize = 15.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice Search",
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // БЫСТРЫЙ ДОСТУП Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "БЫСТРЫЙ ДОСТУП",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.2.sp,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = "Edit",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4F46E5),
                                modifier = Modifier.clickable {
                                    android.widget.Toast.makeText(localContext, "Редактирование ссылок", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Static 2x4 Quick Links Grid using Row and Column
                        @Composable
                        fun QuickLinkItemLocal(name: String, domain: String, url: String) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        viewModel.updateUrl(url)
                                        webViewRef?.loadUrl(url)
                                        isBrowsing = true
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(Color.White, RoundedCornerShape(16.dp))
                                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                                        .shadow(1.dp, RoundedCornerShape(16.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = "https://www.google.com/s2/favicons?sz=128&domain=$domain",
                                        contentDescription = name,
                                        modifier = Modifier.size(32.dp),
                                        error = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Language)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF475569),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        val linksRow1 = listOf(
                            Triple("Google", "google.com", "https://google.com"),
                            Triple("YouTube", "youtube.com", "https://youtube.com"),
                            Triple("Yandex", "yandex.ru", "https://yandex.ru"),
                            Triple("Twitter", "twitter.com", "https://twitter.com")
                        )

                        val linksRow2 = listOf(
                            Triple("Reddit", "reddit.com", "https://reddit.com"),
                            Triple("GitHub", "github.com", "https://github.com"),
                            Triple("Wikipedia", "wikipedia.org", "https://wikipedia.org")
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                linksRow1.forEach { (name, domain, url) ->
                                    QuickLinkItemLocal(name, domain, url)
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                linksRow2.forEach { (name, domain, url) ->
                                    QuickLinkItemLocal(name, domain, url)
                                }
                                // Add button (+)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            android.widget.Toast.makeText(localContext, "Добавление ярлыка быстрого доступа", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .background(Color(0xFFEEF1F6), RoundedCornerShape(16.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add",
                                            tint = Color(0xFF475569),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Add",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF475569)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Discover Header
                        Text(
                            text = "DISCOVER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp,
                            color = Color(0xFF94A3B8)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Premium Article Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val url = "https://techreflect.io/fluid-interfaces-2024"
                                    viewModel.updateUrl(url)
                                    webViewRef?.loadUrl(url)
                                    isBrowsing = true
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column {
                                AsyncImage(
                                    model = com.example.R.drawable.img_home_premium_preview_1780644228802,
                                    contentDescription = "Discover Preview",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp),
                                    contentScale = ContentScale.Crop
                                )
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "TECH TRENDS",
                                        color = Color(0xFF4F46E5),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "The Future of Fluid Interfaces in 2024",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A),
                                        lineHeight = 22.sp
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (currentTabItem == 1) {
                // --- SCREEN 02 & SCREEN 03: INTERACTIVE SEARCH & SUGGESTIONS ---
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (isPrivateMode) Color(0xFF0F172A) else MaterialTheme.colorScheme.background)
                        .statusBarsPadding()
                        .padding(20.dp)
                ) {
                    // Modern Segmented Control with smooth backgrounds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .background(
                                if (isPrivateMode) Color(0xFF1E293B) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                RoundedCornerShape(24.dp)
                            )
                            .padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isImageSearchTabSelected) {
                                        if (isPrivateMode) Color(0xFF6366F1) else MaterialTheme.colorScheme.primary
                                    } else Color.Transparent
                                )
                                .clickable { isImageSearchTabSelected = true }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    tint = if (isImageSearchTabSelected) {
                                        if (isPrivateMode) Color.White else MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        if (isPrivateMode) Color(0xFF94A3B8) else MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Изображения",
                                    color = if (isImageSearchTabSelected) {
                                        if (isPrivateMode) Color.White else MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        if (isPrivateMode) Color(0xFF94A3B8) else MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (!isImageSearchTabSelected) {
                                        if (isPrivateMode) Color(0xFF6366F1) else MaterialTheme.colorScheme.primary
                                    } else Color.Transparent
                                )
                                .clickable { isImageSearchTabSelected = false }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = if (!isImageSearchTabSelected) {
                                        if (isPrivateMode) Color.White else MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        if (isPrivateMode) Color(0xFF94A3B8) else MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Веб-поиск",
                                    color = if (!isImageSearchTabSelected) {
                                        if (isPrivateMode) Color.White else MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        if (isPrivateMode) Color(0xFF94A3B8) else MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    if (isImageSearchTabSelected) {
                        // --- SCREEN 03: IMAGE SEARCH VIEW MODULE ---
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextField(
                                value = imageSearchQuery,
                                onValueChange = { imageSearchQuery = it },
                                placeholder = { 
                                    Text(
                                        l10n.searchPlaceholderImage, 
                                        color = if (isPrivateMode) Color(0xFF64748B) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    ) 
                                },
                                leadingIcon = { 
                                    Icon(
                                        Icons.Default.Search, 
                                        contentDescription = null,
                                        tint = if (isPrivateMode) Color(0xFF818CF8) else MaterialTheme.colorScheme.primary
                                    ) 
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(imageSearchFocusRequester),
                                shape = RoundedCornerShape(24.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = if (isPrivateMode) Color(0xFF1E293B) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    unfocusedContainerColor = if (isPrivateMode) Color(0xFF1E293B) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = if (isPrivateMode) Color.White else MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = if (isPrivateMode) Color.White else MaterialTheme.colorScheme.onSurface
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        coroutineScope.launch {
                                            isImageSearchLoading = true
                                            kotlinx.coroutines.delay(500)
                                            imageSearchResults = com.example.downloader.ImageSearchService.searchImages(imageSearchQuery)
                                            isImageSearchLoading = false
                                        }
                                    }
                                )
                            )
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isImageSearchLoading = true
                                        kotlinx.coroutines.delay(500)
                                        imageSearchResults = com.example.downloader.ImageSearchService.searchImages(imageSearchQuery)
                                        isImageSearchLoading = false
                                    }
                                },
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isPrivateMode) Color(0xFF6366F1) else MaterialTheme.colorScheme.primary,
                                    contentColor = if (isPrivateMode) Color.White else MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text("Искать", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(14.dp))

                        // Category Filtering Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Космос", "Котики", "Природа", "Закат").forEach { category ->
                                val isSelected = imageSearchQuery == category
                                val chipBg = if (isSelected) {
                                    if (isPrivateMode) Color(0xFF6366F1) else MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    if (isPrivateMode) Color(0xFF1E293B) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                }
                                val chipText = if (isSelected) {
                                    if (isPrivateMode) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    if (isPrivateMode) Color(0xFF94A3B8) else MaterialTheme.colorScheme.onSurfaceVariant
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(chipBg)
                                        .clickable {
                                            imageSearchQuery = category
                                            coroutineScope.launch {
                                                isImageSearchLoading = true
                                                kotlinx.coroutines.delay(500)
                                                imageSearchResults = com.example.downloader.ImageSearchService.searchImages(category)
                                                isImageSearchLoading = false
                                            }
                                        }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(category, color = chipText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(14.dp))

                        // Grid results containing the Search Results preview
                        Box(modifier = Modifier.weight(1f)) {
                            if (isImageSearchLoading) {
                                ImageGridShimmer()
                            } else if (imageSearchResults.isEmpty()) {
                                EmptyState()
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    contentPadding = PaddingValues(0.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(imageSearchResults) { image ->
                                        ImageSearchCard(
                                            searchImage = image,
                                            onClick = {
                                                val url = image.imageUrl
                                                viewModel.updateUrl(url)
                                                webViewRef?.loadUrl(url)
                                                isBrowsing = true
                                                currentTabItem = 0
                                            },
                                            onDownloadClick = { url ->
                                                val cleanFilename = if (image.title.isNotBlank()) {
                                                    image.title.take(15).trim().replace(Regex("[^a-zA-Z0-9._-]"), "_") + ".jpg"
                                                } else {
                                                    "image_${url.hashCode()}.jpg"
                                                }
                                                val testMedia = MediaItem(
                                                    id = url.hashCode().toString(),
                                                    url = url,
                                                    type = "image",
                                                    ext = "jpg",
                                                    filename = cleanFilename,
                                                    tagName = "img"
                                                )
                                                safelyTriggerDownload {
                                                    viewModel.downloadSingleMediaFile(testMedia)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // --- SCREEN 02: WEB / TEXT SEARCH VIEW ---
                        TextField(
                            value = searchQueryText,
                            onValueChange = { searchQueryText = it },
                            placeholder = { 
                                Text(
                                    "Введите название, адрес или запрос...", 
                                    color = if (isPrivateMode) Color(0xFF64748B) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                ) 
                            },
                            leadingIcon = { 
                                Icon(
                                    Icons.Default.Search, 
                                    contentDescription = null,
                                    tint = if (isPrivateMode) Color(0xFF818CF8) else MaterialTheme.colorScheme.primary
                                ) 
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .focusRequester(searchScreenFocusRequester),
                            shape = RoundedCornerShape(28.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = if (isPrivateMode) Color(0xFF1E293B) else MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = if (isPrivateMode) Color(0xFF1E293B) else MaterialTheme.colorScheme.surface,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = if (isPrivateMode) Color.White else MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = if (isPrivateMode) Color.White else MaterialTheme.colorScheme.onSurface
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    if (searchQueryText.isNotBlank()) {
                                        val query = searchQueryText.trim()
                                        val parsedUrl = smartUrlParse(query, isHttpsEverywhereEnabled, viewModel::getSearchUrl)
                                        viewModel.updateUrl(parsedUrl)
                                        webViewRef?.loadUrl(parsedUrl)
                                        isBrowsing = true
                                        currentTabItem = 0
                                        focusManager.clearFocus()
                                    }
                                }
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Подсказки поиска", 
                            fontWeight = FontWeight.ExtraBold, 
                            fontSize = 13.sp, 
                            color = if (isPrivateMode) Color(0xFF818CF8) else MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val suggestions = listOf(
                            "material design 3 components",
                            "material design 3 color system",
                            "material 3 expressive figma",
                            "material.io",
                            "m3.material.io/components",
                            "material you android 14"
                        ).filter { it.contains(searchQueryText, ignoreCase = true) }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (searchQueryText.isNotBlank()) {
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isPrivateMode) Color(0xFF1E293B).copy(alpha = 0.6f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    val query = searchQueryText.trim()
                                                    val parsedUrl = smartUrlParse(query, isHttpsEverywhereEnabled, viewModel::getSearchUrl)
                                                    viewModel.updateUrl(parsedUrl)
                                                    webViewRef?.loadUrl(parsedUrl)
                                                    isBrowsing = true
                                                    currentTabItem = 0
                                                    focusManager.clearFocus()
                                                }
                                                .padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Search, 
                                                contentDescription = null, 
                                                tint = if (isPrivateMode) Color(0xFFA5B4FC) else MaterialTheme.colorScheme.primary, 
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(14.dp))
                                            Text(
                                                text = "Искать в сети: $searchQueryText", 
                                                color = if (isPrivateMode) Color(0xFFA5B4FC) else MaterialTheme.colorScheme.primary, 
                                                fontSize = 14.sp, 
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                            items(suggestions) { label ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isPrivateMode) Color(0xFF1E293B).copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                val url = viewModel.getSearchUrl(label)
                                                viewModel.updateUrl(url)
                                                webViewRef?.loadUrl(url)
                                                searchQueryText = label
                                                isBrowsing = true
                                                currentTabItem = 0
                                            }
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.TrendingUp, 
                                            contentDescription = null, 
                                            tint = if (isPrivateMode) Color(0xFF64748B) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), 
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Text(
                                            text = label, 
                                            fontSize = 14.sp, 
                                            fontWeight = FontWeight.Medium,
                                            color = if (isPrivateMode) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (currentTabItem == 2) {
                // TAB SWITCHER VIEW (Tab Manager)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (isPrivateMode) Color(0xFF0F1115) else MaterialTheme.colorScheme.background)
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    val filteredTabs = tabs.filter { it.isIncognito == isPrivateMode }
                    
                    // Top Bar / Header of Tab Manager
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = "Grid",
                            tint = if (isPrivateMode) Color.White else Color(0xFF4F46E5),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Sway",
                            color = Color(0xFF4F46E5),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (filteredTabs.isNotEmpty()) {
                                Text(
                                    text = "Close All",
                                    color = Color(0xFF4F46E5),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable {
                                            filteredTabs.forEach { viewModel.closeTab(it.id, persistentWebView) }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More",
                                tint = if (isPrivateMode) Color.White else Color(0xFF475569),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    
                    // Switcher pill between Regular and Incognito
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .background(
                                if (isPrivateMode) Color(0xFF211F26) else MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(24.dp)
                            )
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { isPrivateMode = false }
                                .background(
                                    if (!isPrivateMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tab,
                                contentDescription = null,
                                tint = if (!isPrivateMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Обычные",
                                color = if (!isPrivateMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { isPrivateMode = true }
                                .background(
                                    if (isPrivateMode) Color(0xFFD0BCFF) else Color.Transparent,
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (isPrivateMode) Color(0xFF381E72) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Инкогнито",
                                color = if (isPrivateMode) Color(0xFF381E72) else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Tabs Grid List
                    if (filteredTabs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = if (isPrivateMode) Icons.Default.Lock else Icons.Default.Tab,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = if (isPrivateMode) Color(0xFF49454F) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (isPrivateMode) "Нет открытых приватных вкладок" else "Все вкладки закрыты",
                                    color = if (isPrivateMode) Color(0xFF938F99) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(filteredTabs, key = { it.id }) { tab ->
                                val isTabActive = tab.id == activeTabId
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clickable {
                                            viewModel.selectTab(tab.id, persistentWebView)
                                            isBrowsing = true
                                            currentTabItem = 0
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isPrivateMode) Color(0xFF1E1B24) else Color.White
                                    ),
                                    border = BorderStroke(
                                        width = if (isTabActive) 2.dp else 1.dp,
                                        color = if (isTabActive) {
                                            if (isPrivateMode) Color(0xFFD0BCFF) else Color(0xFF4F46E5)
                                        } else {
                                            if (isPrivateMode) Color(0xFF332F41) else Color(0xFFE2E8F0)
                                        }
                                    )
                                ) {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        // Card Header row
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                val faviconUrl = getFaviconUrl(tab.url)
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .background(
                                                            if (isPrivateMode) Color(0xFF2D2A35) else Color(0xFFEEF1F6),
                                                            RoundedCornerShape(6.dp)
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (faviconUrl.isNotEmpty()) {
                                                        AsyncImage(
                                                            model = faviconUrl,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(16.dp),
                                                            error = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Language)
                                                        )
                                                    } else {
                                                        Icon(
                                                            imageVector = if (tab.isIncognito) Icons.Default.Lock else Icons.Default.Language,
                                                            contentDescription = null,
                                                            tint = if (isPrivateMode) Color(0xFFD0BCFF) else Color(0xFF4F46E5),
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = tab.title.ifBlank { "Новая вкладка" },
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = if (isPrivateMode) Color.White else Color(0xFF0F172A)
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    viewModel.closeTab(tab.id, persistentWebView)
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(18.dp)
                                                        .background(
                                                            if (isPrivateMode) Color(0xFF2B2835) else Color(0xFFEEF1F6),
                                                            CircleShape
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Close",
                                                        tint = if (isPrivateMode) Color(0xFFE6E1E5) else Color(0xFF475569),
                                                        modifier = Modifier.size(10.dp)
                                                    )
                                                }
                                            }
                                        }
                                        
                                        // Webpage Preview area
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f)
                                                .background(if (isPrivateMode) Color(0xFF0F1115) else Color(0xFFEEF1F6))
                                        ) {
                                            if (isPrivateMode) {
                                                Box(
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Security,
                                                        contentDescription = "Incognito",
                                                        tint = Color(0xFFD0BCFF).copy(alpha = 0.3f),
                                                        modifier = Modifier.size(36.dp)
                                                    )
                                                }
                                            } else {
                                                // Map domains to premium preview images
                                                val previewImage = when {
                                                    tab.url.contains("youtube.com") || tab.url.contains("youtu.be") -> "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?q=80&w=300"
                                                    tab.url.contains("github.com") -> "https://images.unsplash.com/photo-1607799279861-4dd421887fb3?q=80&w=300"
                                                    tab.url.contains("material.io") || tab.url.contains("google.com") -> "https://images.unsplash.com/photo-1507238691740-187a5b1d37b8?q=80&w=300"
                                                    else -> "https://images.unsplash.com/photo-1506318137071-a8e063b4bec0?q=80&w=300" // Fallback abstract
                                                }
                                                AsyncImage(
                                                    model = previewImage,
                                                    contentDescription = "Preview",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                            
                                            // Active Badge Overlay
                                            if (isTabActive) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomStart)
                                                        .padding(8.dp)
                                                        .background(Color(0xFF4F46E5), RoundedCornerShape(8.dp))
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = "ACTIVE",
                                                        color = Color.White,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Plus "New tab" button
                    Button(
                        onClick = {
                            viewModel.addTab(l10n.newTab, "https://www.google.com", isPrivateMode, persistentWebView)
                            isBrowsing = false
                            currentTabItem = 0
                            focusManager.clearFocus()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(bottom = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPrivateMode) Color(0xFFD0BCFF) else Color(0xFF4F46E5),
                            contentColor = if (isPrivateMode) Color(0xFF381E72) else Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = l10n.newTab, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            } else if (currentTabItem == 3) {
                // SAVED / BOOKMARKS / HISTORY / DOWNLOADS VIEW
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .statusBarsPadding()
                ) {
                    TabRow(selectedTabIndex = selectedSavedTab) {
                        Tab(selected = selectedSavedTab == 0, onClick = { selectedSavedTab = 0 }, text = { Text(l10n.bookmarksTitle, fontWeight = FontWeight.Bold) })
                        Tab(selected = selectedSavedTab == 1, onClick = { selectedSavedTab = 1 }, text = { Text(l10n.historyTab, fontWeight = FontWeight.Bold) })
                        Tab(selected = selectedSavedTab == 2, onClick = { selectedSavedTab = 2 }, text = { Text(l10n.downloadsTab, fontWeight = FontWeight.Bold) })
                    }

                    var selectedFolderCategory by remember { mutableStateOf("Все") }

                    when (selectedSavedTab) {
                        0 -> {
                            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                Text("Папки".uppercase(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(8.dp))

                                val folders = listOf(
                                    "Все" to "${bookmarksList.size} эл.",
                                    "Работа" to "${bookmarksList.count { it.category == "Работа" }} эл.",
                                    "Чтение" to "${bookmarksList.count { it.category == "Чтение" }} эл.",
                                    "Рецепты" to "${bookmarksList.count { it.category == "Рецепты" }} эл."
                                )
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(folders) { (name, count) ->
                                        val isFolderSelected = selectedFolderCategory == name
                                        Card(
                                            modifier = Modifier.width(115.dp).clickable {
                                                selectedFolderCategory = name
                                            },
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isFolderSelected) MaterialTheme.colorScheme.primaryContainer 
                                                                else MaterialTheme.colorScheme.surface
                                            ),
                                            border = BorderStroke(
                                                1.dp, 
                                                if (isFolderSelected) MaterialTheme.colorScheme.primary 
                                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                            )
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Icon(
                                                    Icons.Default.Folder, 
                                                    contentDescription = null, 
                                                    tint = if (isFolderSelected) MaterialTheme.colorScheme.primary 
                                                           else MaterialTheme.colorScheme.secondary
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(name, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text(count, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))
                                Text("ВСЕ ЗАКЛАДКИ (${selectedFolderCategory})".uppercase(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(8.dp))

                                val filteredBookmarks = if (selectedFolderCategory == "Все") {
                                    bookmarksList
                                } else {
                                    bookmarksList.filter { it.category == selectedFolderCategory }
                                }

                                if (filteredBookmarks.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("Нет закладок в этой папке", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                                    }
                                } else {
                                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                                        items(filteredBookmarks) { bookmark ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        viewModel.updateUrl(bookmark.url)
                                                        webViewRef?.loadUrl(bookmark.url)
                                                        isBrowsing = true
                                                        currentTabItem = 0
                                                    }
                                                    .padding(vertical = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                                Spacer(modifier = Modifier.width(16.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(bookmark.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Text(bookmark.url, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                }
                                                IconButton(
                                                    onClick = {
                                                        viewModel.removeBookmark(bookmark)
                                                    }
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Удалить закладку", tint = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        }
                                    }
                                }
                            }
                        }
                        1 -> {
                            var historySearchText by remember { mutableStateOf("") }
                            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                // Top app bar for History tab
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Public,
                                        contentDescription = "Globe",
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Text(
                                        text = "History",
                                        color = Color(0xFF4F46E5),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "Search",
                                            tint = Color(0xFF475569),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "More",
                                            tint = Color(0xFF475569),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                // Capsule Search Field
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .background(Color(0xFFEEF1F6), RoundedCornerShape(24.dp))
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "Search",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        TextField(
                                            value = historySearchText,
                                            onValueChange = { historySearchText = it },
                                            placeholder = { Text("Search history", fontSize = 13.sp, color = Color(0xFF94A3B8)) },
                                            singleLine = true,
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = Color.Transparent,
                                                unfocusedContainerColor = Color.Transparent,
                                                focusedIndicatorColor = Color.Transparent,
                                                unfocusedIndicatorColor = Color.Transparent,
                                                disabledIndicatorColor = Color.Transparent,
                                                focusedTextColor = Color(0xFF0F172A),
                                                unfocusedTextColor = Color(0xFF0F172A)
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                val filteredHistory = historyList.filter {
                                    it.title.contains(historySearchText, ignoreCase = true) || 
                                    it.url.contains(historySearchText, ignoreCase = true)
                                }

                                if (filteredHistory.isEmpty()) {
                                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        Text("История пуста", color = Color(0xFF94A3B8), fontSize = 14.sp)
                                    }
                                } else {
                                    val groupedHistory = filteredHistory.groupBy { item ->
                                        val currentTime = System.currentTimeMillis()
                                        val diff = currentTime - item.timestamp
                                        if (diff < 24 * 60 * 60 * 1000) {
                                            "TODAY"
                                        } else if (diff < 48 * 60 * 60 * 1000) {
                                            "YESTERDAY"
                                        } else {
                                            "EARLIER"
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        LazyColumn(modifier = Modifier.weight(1f)) {
                                            groupedHistory.forEach { (groupName, items) ->
                                                item {
                                                    Text(
                                                        text = groupName,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = Color(0xFF94A3B8),
                                                        letterSpacing = 1.2.sp,
                                                        modifier = Modifier.padding(vertical = 12.dp)
                                                    )
                                                }
                                                items(items) { item ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable {
                                                                viewModel.updateUrl(item.url)
                                                                webViewRef?.loadUrl(item.url)
                                                                isBrowsing = true
                                                                currentTabItem = 0
                                                            }
                                                            .padding(vertical = 8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        // Favicon container
                                                        val faviconUrl = getFaviconUrl(item.url)
                                                        Box(
                                                            modifier = Modifier
                                                                .size(40.dp)
                                                                .background(Color(0xFFEEF1F6), RoundedCornerShape(10.dp)),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            if (faviconUrl.isNotEmpty()) {
                                                                AsyncImage(
                                                                    model = faviconUrl,
                                                                    contentDescription = null,
                                                                    modifier = Modifier.size(22.dp),
                                                                    error = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Language)
                                                                )
                                                            } else {
                                                                Icon(
                                                                    imageVector = Icons.Default.Language,
                                                                    contentDescription = null,
                                                                    tint = Color(0xFF4F46E5),
                                                                    modifier = Modifier.size(18.dp)
                                                                )
                                                            }
                                                        }
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = item.title.ifBlank { "Без названия" },
                                                                fontWeight = FontWeight.SemiBold,
                                                                fontSize = 14.sp,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                                color = Color(0xFF0F172A)
                                                            )
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            Text(
                                                                text = item.url.replace("https://", "").replace("http://", "").replace("www.", ""),
                                                                fontSize = 11.sp,
                                                                color = Color(0xFF475569),
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        // Simple format hour
                                                        val sdf = remember { java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()) }
                                                        val timeStr = remember(item.timestamp) { sdf.format(java.util.Date(item.timestamp)) }
                                                        Text(
                                                            text = timeStr,
                                                            fontSize = 11.sp,
                                                            color = Color(0xFF94A3B8)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        IconButton(
                                                            onClick = { viewModel.deleteHistoryItem(item) },
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Close,
                                                                contentDescription = "Remove",
                                                                tint = Color(0xFF94A3B8),
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                        }
                                                    }
                                                    HorizontalDivider(color = Color(0xFFEEF1F6))
                                                }
                                            }
                                        }

                                        // Clear Browsing Data pill button at the bottom of the list
                                        Button(
                                            onClick = { viewModel.clearHistory() },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                                .padding(top = 8.dp),
                                            shape = RoundedCornerShape(24.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFFEEF1F6),
                                                contentColor = Color(0xFF4F46E5)
                                            ),
                                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Trash",
                                                    tint = Color(0xFF4F46E5),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Clear Browsing Data",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        2 -> {
                            val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                            if (downloadsList.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                            modifier = Modifier.size(64.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Список загрузок пуст", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                                    }
                                }
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp)) {
                                    items(downloadsList) { download ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    val isImg = download.filename.endsWith(".png", true) || 
                                                                download.filename.endsWith(".jpg", true) || 
                                                                download.filename.endsWith(".jpeg", true) || 
                                                                download.filename.endsWith(".webp", true) || 
                                                                download.filename.endsWith(".gif", true)
                                                    
                                                    Icon(
                                                        imageVector = if (isImg) Icons.Default.Image else if (download.filename.endsWith(".zip", true)) Icons.Default.FolderZip else Icons.Default.Description,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(28.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = download.filename,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text(
                                                                text = download.size,
                                                                fontSize = 10.sp,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text(
                                                                text = "•",
                                                                fontSize = 10.sp,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            val statusColor = when (download.status) {
                                                                "Готово" -> MaterialTheme.colorScheme.primary
                                                                "Скачивание" -> MaterialTheme.colorScheme.secondary
                                                                else -> MaterialTheme.colorScheme.error
                                                            }
                                                            Text(
                                                                text = download.status,
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = statusColor
                                                            )
                                                        }
                                                    }
                                                    
                                                    // Copy URL Button
                                                    IconButton(
                                                        onClick = {
                                                            val urlToCopy = download.url
                                                            if (urlToCopy.isNotBlank()) {
                                                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(urlToCopy))
                                                                android.widget.Toast.makeText(localContext, "Ссылка скопирована", android.widget.Toast.LENGTH_SHORT).show()
                                                            }
                                                        },
                                                        modifier = Modifier.size(36.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Attachment,
                                                            contentDescription = "Копировать ссылку",
                                                            modifier = Modifier.size(16.dp),
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    
                                                    // Delete Button
                                                    IconButton(
                                                        onClick = { viewModel.deleteDownload(download) },
                                                        modifier = Modifier.size(36.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Удалить из списка",
                                                            modifier = Modifier.size(16.dp),
                                                            tint = MaterialTheme.colorScheme.error
                                                        )
                                                    }
                                                }
                                                
                                                if (download.status == "Скачивание") {
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    LinearProgressIndicator(
                                                        progress = { download.progress },
                                                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
                                                    )
                                                }
                                                
                                                if (download.status == "Готово") {
                                                    Spacer(modifier = Modifier.height(10.dp))
                                                    val savedFolder = when {
                                                        download.filename.endsWith(".png", true) || 
                                                        download.filename.endsWith(".jpg", true) || 
                                                        download.filename.endsWith(".jpeg", true) || 
                                                        download.filename.endsWith(".webp", true) || 
                                                        download.filename.endsWith(".gif", true) -> "Pictures/MediaDownloader"
                                                        download.filename.endsWith(".mp4", true) || 
                                                        download.filename.endsWith(".webm", true) || 
                                                        download.filename.endsWith(".mov", true) -> "Movies/MediaDownloader"
                                                        else -> "Downloads/MediaDownloader"
                                                    }
                                                    
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Folder,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(12.dp),
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = "Папка: $savedFolder",
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        
                                                        // "Открыть" Button
                                                        TextButton(
                                                            onClick = { openDownloadedFile(localContext, download) },
                                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                            modifier = Modifier.height(28.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Visibility,
                                                                contentDescription = "Открыть",
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text("Открыть", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                        
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        
                                                        // "Поделиться" Button
                                                        TextButton(
                                                            onClick = { shareDownloadedFile(localContext, download) },
                                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                            modifier = Modifier.height(28.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Share,
                                                                contentDescription = "Поделиться",
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text("Поделиться", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (currentTabItem == 4) {
                if (showSettingsPage) {
                    // SETTINGS VIEW WITH DIALOGS AND CLEAN CATEGORIES
                    var showLanguageDialog by remember { mutableStateOf(false) }
                    var showEngineDialog by remember { mutableStateOf(false) }
                    var showUaDialog by remember { mutableStateOf(false) }
                    var showAdBlockDialog by remember { mutableStateOf(false) }
                    var showHomePageDialog by remember { mutableStateOf(false) }

                    // Dialogs implementation
                    if (showLanguageDialog) {
                        val currentLang by viewModel.appLanguage.collectAsState()
                        AlertDialog(
                            onDismissRequest = { showLanguageDialog = false },
                            title = { Text(l10n.languageTitle, fontWeight = FontWeight.Bold) },
                            text = {
                                Column {
                                    val languages = listOf("ru" to l10n.languageRu, "en" to l10n.languageEn, "kk" to l10n.languageKk)
                                    languages.forEach { (code, name) ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().clickable { viewModel.selectLanguage(code); showLanguageDialog = false }.padding(vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(selected = (currentLang == code), onClick = { viewModel.selectLanguage(code); showLanguageDialog = false })
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(name, fontSize = 15.sp)
                                        }
                                    }
                                }
                            },
                            confirmButton = { TextButton(onClick = { showLanguageDialog = false }) { Text("Закрыть") } }
                        )
                    }

                    if (showEngineDialog) {
                        AlertDialog(
                            onDismissRequest = { showEngineDialog = false },
                            title = { Text(l10n.searchEngine, fontWeight = FontWeight.Bold) },
                            text = {
                                Column {
                                    val searchEngines = listOf("Google", "Yandex", "Bing", "DuckDuckGo")
                                    searchEngines.forEach { engine ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().clickable { viewModel.selectSearchEngine(engine); showEngineDialog = false }.padding(vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(selected = (searchEngine == engine), onClick = { viewModel.selectSearchEngine(engine); showEngineDialog = false })
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(engine, fontSize = 15.sp)
                                        }
                                    }
                                }
                            },
                            confirmButton = { TextButton(onClick = { showEngineDialog = false }) { Text("Закрыть") } }
                        )
                    }

                    if (showUaDialog) {
                        AlertDialog(
                            onDismissRequest = { showUaDialog = false },
                            title = { Text(l10n.userAgent, fontWeight = FontWeight.Bold) },
                            text = {
                                Column {
                                    val uas = listOf("Mobile" to l10n.viewModeMobile, "Desktop" to l10n.viewModeDesktop)
                                    uas.forEach { (mode, title) ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().clickable { viewModel.selectUserAgentMode(mode); showUaDialog = false }.padding(vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(selected = (userAgentMode == mode), onClick = { viewModel.selectUserAgentMode(mode); showUaDialog = false })
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(title, fontSize = 15.sp)
                                        }
                                    }
                                }
                            },
                            confirmButton = { TextButton(onClick = { showUaDialog = false }) { Text("Закрыть") } }
                        )
                    }

                    if (showAdBlockDialog) {
                        AlertDialog(
                            onDismissRequest = { showAdBlockDialog = false },
                            title = { Text(l10n.adBlockHeader, fontWeight = FontWeight.Bold) },
                            text = {
                                Column {
                                    val modes = listOf("off" to l10n.adBlockOff, "standard" to l10n.adBlockStandard, "aggressive" to l10n.adBlockAggressive)
                                    modes.forEach { (mode, name) ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().clickable { viewModel.selectAdBlockMode(mode); showAdBlockDialog = false }.padding(vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(selected = (adBlockMode == mode), onClick = { viewModel.selectAdBlockMode(mode); showAdBlockDialog = false })
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(name, fontSize = 15.sp)
                                        }
                                    }
                                }
                            },
                            confirmButton = { TextButton(onClick = { showAdBlockDialog = false }) { Text("Закрыть") } }
                        )
                    }

                    if (showHomePageDialog) {
                        var tempUrl by remember { mutableStateOf(homePageUrl) }
                        AlertDialog(
                            onDismissRequest = { showHomePageDialog = false },
                            title = { Text(l10n.startPageAddress, fontWeight = FontWeight.Bold) },
                            text = {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = tempUrl,
                                        onValueChange = { tempUrl = it },
                                        label = { Text(l10n.addressPlaceholder) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                }
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        viewModel.setHomePageUrl(tempUrl)
                                        showHomePageDialog = false
                                    }
                                ) {
                                    Text(l10n.saveBtn)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showHomePageDialog = false }) {
                                    Text("Отмена")
                                }
                            }
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFF4F7FB))
                            .statusBarsPadding()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        // Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { showSettingsPage = false }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color(0xFF4F46E5)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Settings",
                                    color = Color(0xFF4F46E5),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More",
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Alexei Volkov Profile Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable {
                                    android.widget.Toast.makeText(localContext, "Профиль пользователя: Alexei Volkov", android.widget.Toast.LENGTH_SHORT).show()
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFEEF1F6))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Rounded Avatar Image
                                AsyncImage(
                                    model = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?q=80&w=200",
                                    contentDescription = "Profile Photo",
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, Color(0xFF4F46E5), CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Alexei Volkov",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 16.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        // Purple PRO Badge
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFF4F46E5), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "PRO MEMBER",
                                                color = Color.White,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "alex.volkov@sway.io",
                                        fontSize = 12.sp,
                                        color = Color(0xFF475569)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8)
                                )
                            }
                        }

                        // Local Reusable Components
                        @Composable
                        fun SettingHeader(title: String) {
                            Text(
                                text = title.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF4F46E5),
                                letterSpacing = 1.2.sp,
                                modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
                            )
                        }

                        @Composable
                        fun SettingRowLocal(
                            icon: ImageVector,
                            title: String,
                            subtitle: String,
                            onClick: () -> Unit = {},
                            trailing: @Composable (() -> Unit)? = null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onClick() }
                                    .padding(vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    if (subtitle.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = subtitle,
                                            fontSize = 11.sp,
                                            color = Color(0xFF475569)
                                        )
                                    }
                                }
                                if (trailing != null) {
                                    trailing()
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = Color(0xFFCBD5E1),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            HorizontalDivider(color = Color(0xFFEEF1F6))
                        }

                        // Category ОБЩИЕ
                        SettingHeader("ОБЩИЕ")
                        val currentLangFlow by viewModel.appLanguage.collectAsState()
                        val currentLangName = when (currentLangFlow) {
                            "ru" -> l10n.languageRu
                            "en" -> l10n.languageEn
                            "kk" -> l10n.languageKk
                            else -> currentLangFlow
                        }
                        SettingRowLocal(
                            icon = Icons.Default.Language,
                            title = l10n.languageTitle,
                            subtitle = currentLangName,
                            onClick = { showLanguageDialog = true }
                        )
                        SettingRowLocal(
                            icon = Icons.Default.Search,
                            title = l10n.searchEngine,
                            subtitle = searchEngine,
                            onClick = { showEngineDialog = true }
                        )
                        SettingRowLocal(
                            icon = Icons.Default.Home,
                            title = l10n.startPageAddress,
                            subtitle = homePageUrl,
                            onClick = { showHomePageDialog = true }
                        )

                        // Category ВНЕШНИЙ ВИД
                        SettingHeader("ВНЕШНИЙ ВИД")
                        SettingRowLocal(
                            icon = Icons.Default.Devices,
                            title = l10n.userAgent,
                            subtitle = if (userAgentMode == "Mobile") l10n.viewModeMobile else l10n.viewModeDesktop,
                            onClick = { showUaDialog = true }
                        )
                        SettingRowLocal(
                            icon = Icons.Default.FlashOn,
                            title = l10n.fastRenderingHeader,
                            subtitle = l10n.fastRenderingDesc,
                            onClick = {},
                            trailing = {
                                Switch(
                                    checked = isFastRenderingEnabled,
                                    onCheckedChange = { viewModel.toggleFastRendering(it) }
                                )
                            }
                        )

                        // Category КОНФИДЕНЦИАЛЬНОСТЬ
                        SettingHeader("КОНФИДЕНЦИАЛЬНОСТЬ")
                        val adblockName = when (adBlockMode) {
                            "off" -> l10n.adBlockOff
                            "standard" -> l10n.adBlockStandard
                            "aggressive" -> l10n.adBlockAggressive
                            else -> adBlockMode
                        }
                        SettingRowLocal(
                            icon = Icons.Default.Security,
                            title = "Sway Shield AdBlock",
                            subtitle = adblockName,
                            onClick = { showAdBlockDialog = true }
                        )
                        SettingRowLocal(
                            icon = Icons.Default.TrackChanges,
                            title = l10n.trackerBlockingHeader,
                            subtitle = l10n.trackerBlockingDesc,
                            onClick = {},
                            trailing = {
                                Switch(
                                    checked = isTrackerBlockingEnabled,
                                    onCheckedChange = { viewModel.toggleTrackerBlocking(it) }
                                )
                            }
                        )
                        SettingRowLocal(
                            icon = Icons.Default.Cookie,
                            title = l10n.autoCookieHeader,
                            subtitle = l10n.autoCookieDesc,
                            onClick = {},
                            trailing = {
                                Switch(
                                    checked = isAutoCookieEnabled,
                                    onCheckedChange = { viewModel.toggleAutoCookie(it) }
                                )
                            }
                        )
                        SettingRowLocal(
                            icon = Icons.Default.ExitToApp,
                            title = l10n.clearOnExitHeader,
                            subtitle = l10n.clearOnExitDesc,
                            onClick = {},
                            trailing = {
                                Switch(
                                    checked = isClearHistoryOnExitEnabled,
                                    onCheckedChange = { viewModel.toggleClearHistoryOnExit(it) }
                                )
                            }
                        )

                        // Category YOUTUBE & МУЛЬТИМЕДИА
                        SettingHeader("YOUTUBE & МУЛЬТИМЕДИА")
                        SettingRowLocal(
                            icon = Icons.Default.PlayCircle,
                            title = l10n.ytBackgroundHeader,
                            subtitle = l10n.ytBackgroundDesc,
                            onClick = {},
                            trailing = {
                                Switch(
                                    checked = isYtBackgroundEnabled,
                                    onCheckedChange = { viewModel.toggleYtBackground(it) }
                                )
                            }
                        )
                        SettingRowLocal(
                            icon = Icons.Default.Block,
                            title = l10n.sponsorBlockHeader,
                            subtitle = l10n.sponsorBlockDesc,
                            onClick = {},
                            trailing = {
                                Switch(
                                    checked = isSponsorBlockEnabled,
                                    onCheckedChange = { viewModel.toggleSponsorBlock(it) }
                                )
                            }
                        )
                        SettingRowLocal(
                            icon = Icons.Default.HighQuality,
                            title = l10n.ytAutoQualityHeader,
                            subtitle = l10n.ytAutoQualityDesc,
                            onClick = {},
                            trailing = {
                                Switch(
                                    checked = isYtAutoMaxQualityEnabled,
                                    onCheckedChange = { viewModel.toggleYtAutoMaxQuality(it) }
                                )
                            }
                        )

                        // Category ОЧИСТКА ДАННЫХ
                        SettingHeader("ОЧИСТКА ДАННЫХ")
                        SettingRowLocal(
                            icon = Icons.Default.DeleteForever,
                            title = l10n.clearHistoryBtn,
                            subtitle = "Очистить список посещенных страниц",
                            onClick = {
                                viewModel.clearHistory()
                                android.widget.Toast.makeText(localContext, l10n.toastHistoryCleared, android.widget.Toast.LENGTH_SHORT).show()
                            }
                        )
                        SettingRowLocal(
                            icon = Icons.Default.Bookmark,
                            title = l10n.clearBookmarksBtn,
                            subtitle = "Удалить все сохраненные закладки",
                            onClick = {
                                viewModel.clearBookmarks()
                                android.widget.Toast.makeText(localContext, l10n.toastBookmarksCleared, android.widget.Toast.LENGTH_SHORT).show()
                            }
                        )
                        SettingRowLocal(
                            icon = Icons.Default.Download,
                            title = l10n.clearDownloadsBtn,
                            subtitle = "Очистить список загрузок",
                            onClick = {
                                viewModel.clearDownloads()
                                android.widget.Toast.makeText(localContext, l10n.toastDownloadsCleared, android.widget.Toast.LENGTH_SHORT).show()
                            }
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = l10n.aboutInfo,
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    // MAIN MENU VIEW
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .statusBarsPadding()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        Text(l10n.settingsMenuHeader, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))

                        val menuActions = listOf(
                            Triple("incognito", l10n.newPrivateTabLabel, Icons.Default.Lock),
                            Triple("tabs", l10n.openTabs, Icons.Default.Tab),
                            Triple("bookmarks", l10n.bookmarksTitle, Icons.Default.Bookmark),
                            Triple("history", l10n.historyTab, Icons.Default.History),
                            Triple("downloads", l10n.downloadsTab, Icons.Default.Download),
                            Triple("settings", l10n.settingsTitle, Icons.Default.Settings),
                            Triple("update", l10n.checkUpdates, Icons.Default.Refresh)
                        )

                        menuActions.forEach { (id, label, icon) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        when (id) {
                                            "tabs" -> currentTabItem = 2
                                            "bookmarks" -> {
                                                selectedSavedTab = 0
                                                currentTabItem = 3
                                            }
                                            "history" -> {
                                                selectedSavedTab = 1
                                                currentTabItem = 3
                                            }
                                            "downloads" -> {
                                                selectedSavedTab = 2
                                                currentTabItem = 3
                                            }
                                            "incognito" -> {
                                                viewModel.addTab("Incognito", "https://images.google.com", isIncognito = true, persistentWebView)
                                                isPrivateMode = true
                                                isBrowsing = true
                                                currentTabItem = 0
                                            }
                                            "settings" -> {
                                                showSettingsPage = true
                                            }
                                            "update" -> {
                                                android.widget.Toast.makeText(localContext, l10n.checkingUpdatesToast, android.widget.Toast.LENGTH_SHORT).show()
                                                viewModel.updateBannerDismissed.value = false
                                                viewModel.checkForUpdates(isManualCheck = true, forceSimulate = false)
                                            }
                                        }
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(label, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Text(l10n.menuSecurity, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))

                        // AdBlock Controls inside vertical menu
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(l10n.adBlockHeader, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(l10n.adBlockDesc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(6.dp))
                            listOf(
                                "off" to l10n.adBlockOff,
                                "standard" to l10n.adBlockStandard,
                                "aggressive" to l10n.adBlockAggressive
                            ).forEach { (mode, name) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.selectAdBlockMode(mode) }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = (adBlockMode == mode),
                                        onClick = { viewModel.selectAdBlockMode(mode) }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(name, fontSize = 13.sp)
                                }
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(l10n.trackerBlockingHeader, fontWeight = FontWeight.Bold)
                                Text(l10n.trackerBlockingDesc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = isTrackerBlockingEnabled,
                                onCheckedChange = { viewModel.toggleTrackerBlocking(it) }
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(l10n.clearOnExitHeader, fontWeight = FontWeight.Bold)
                                Text(l10n.clearOnExitDesc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = isClearHistoryOnExitEnabled,
                                onCheckedChange = { viewModel.toggleClearHistoryOnExit(it) }
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(l10n.fastRenderingHeader, fontWeight = FontWeight.Bold)
                                Text(l10n.fastRenderingDesc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                modifier = Modifier.testTag("menu_fast_rendering_switch"),
                                checked = isFastRenderingEnabled,
                                onCheckedChange = { viewModel.toggleFastRendering(it) }
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(l10n.autoCookieHeader, fontWeight = FontWeight.Bold)
                                Text(l10n.autoCookieDesc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                modifier = Modifier.testTag("menu_auto_cookie_switch"),
                                checked = isAutoCookieEnabled,
                                onCheckedChange = { viewModel.toggleAutoCookie(it) }
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(l10n.ytBackgroundHeader, fontWeight = FontWeight.Bold)
                                Text(l10n.ytBackgroundDesc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                modifier = Modifier.testTag("menu_yt_background_switch"),
                                checked = isYtBackgroundEnabled,
                                onCheckedChange = { viewModel.toggleYtBackground(it) }
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(l10n.sponsorBlockHeader, fontWeight = FontWeight.Bold)
                                Text(l10n.sponsorBlockDesc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                modifier = Modifier.testTag("menu_sponsor_block_switch"),
                                checked = isSponsorBlockEnabled,
                                onCheckedChange = { viewModel.toggleSponsorBlock(it) }
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(l10n.ytAutoQualityHeader, fontWeight = FontWeight.Bold)
                                Text(l10n.ytAutoQualityDesc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                modifier = Modifier.testTag("menu_yt_auto_quality_switch"),
                                checked = isYtAutoMaxQualityEnabled,
                                onCheckedChange = { viewModel.toggleYtAutoMaxQuality(it) }
                            )
                        }
                    }
                }
            }
        }

}

    // Modal progress bar mapping active states during operations
    if (downloadProgress != DownloadProgressState.Idle) {
        Dialog(
            onDismissRequest = {
                if (downloadProgress is DownloadProgressState.Completed || downloadProgress is DownloadProgressState.Error) {
                    viewModel.dismissProgress()
                }
            },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (val state = downloadProgress) {
                        is DownloadProgressState.Loading -> {
                            Text(
                                "Загрузка медиа",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            CircularProgressIndicator(
                                progress = { state.progressFraction },
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = state.description,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            if (state.currentFileName.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = state.currentFileName,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { (state.itemIndex.toFloat()) / state.totalItems },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        is DownloadProgressState.Completed -> {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Загрузка завершена!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = state.message,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(onClick = { viewModel.dismissProgress() }) {
                                Text("ОК")
                            }
                        }

                        is DownloadProgressState.Error -> {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Ошибка",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = state.errorMessage,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(onClick = { viewModel.dismissProgress() }) {
                                Text("Попробовать снова")
                            }
                        }

                        else -> {}
                    }
                }
            }
        }
    }

    // Custom ZIP filename config Dialog
    if (showCustomZipNameDialog) {
        AlertDialog(
            onDismissRequest = { showCustomZipNameDialog = false },
            title = { Text("Имя архива ZIP") },
            text = {
                Column {
                    Text("Введите название для создаваемого ZIP-архива:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customZipName,
                        onValueChange = { customZipName = it },
                        singleLine = true,
                        placeholder = { Text("media_pack") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCustomZipNameDialog = false
                        safelyTriggerDownload {
                            viewModel.downloadSelectedAsZip(customZipName)
                        }
                    }
                ) {
                    Text("Скачать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomZipNameDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Media Viewer Detail Preview Dialog (TAP TO PREVIEW)
    if (previewMediaItem != null) {
        val media = previewMediaItem!!
        Dialog(
            onDismissRequest = { previewMediaItem = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                color = Color.Black
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Preview core render layer
                    if (media.isVideo) {
                        VideoPreviewPlayer(url = media.url)
                    } else {
                        ImagePreviewZoomLayout(
                            url = media.url,
                            userAgent = viewModel.browserUserAgent,
                            referer = currentUrl
                        )
                    }

                    // Floating overlays - details & info labels
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                            .padding(24.dp)
                    ) {
                        Text(
                            text = media.filename,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Mime: ${media.ext.uppercase()} | Sizing: ${media.sizeFormatted}" +
                                    if(media.width > 0) " | ${media.width}x${media.height}" else "",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Actions from inside the preview
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    previewMediaItem = null
                                    viewModel.toggleSelection(media.id)
                                    safelyTriggerDownload {
                                        viewModel.downloadSelectedFilesIndividually()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Скачать файл")
                            }

                            OutlinedButton(
                                onClick = { previewMediaItem = null },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = BorderStroke(1.dp, Color.White)
                            ) {
                                Text("Закрыть")
                            }
                        }
                    }

                    // Status Indicator Floating overlays
                    Badge(
                        containerColor = if (media.isVideo) Color(0xFFFF5722) else Color(0xFF009688),
                        contentColor = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = if (media.isVideo) "VIDEO" else "IMAGE",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }
            }
        }
    }

    if (customView != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {},
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = {
                    customView!!
                },
                modifier = Modifier.fillMaxSize()
            )

            // Floating exit button in case user is stuck in fullscreen video
            IconButton(
                onClick = {
                    customViewCallback?.onCustomViewHidden()
                    customView = null
                    customViewCallback = null
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Fullscreen",
                    tint = Color.White
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaGridCard(
    item: MediaItem,
    isSelected: Boolean,
    userAgent: String,
    referer: String,
    onToggleSelect: () -> Unit,
    onTapPreview: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .combinedClickable(
                onClick = onTapPreview,
                onLongClick = onToggleSelect
            )
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (item.isImage) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.url)
                        .apply {
                            if (userAgent.isNotEmpty()) {
                                addHeader("User-Agent", userAgent)
                            }
                            if (referer.isNotEmpty()) {
                                addHeader("Referer", referer)
                            }
                        }
                        .crossfade(true)
                        // Downsample images down to 350x350 to optimize load times, minimize memory allocation and prevent lag.
                        // Since these are small grid items, full-resolution bitmaps are highly wasteful.
                        .size(350, 350)
                        .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                        .build(),
                    contentDescription = item.filename,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.BrokenImage)
                )
            } else {
                // Video visualizer container displaying standard video symbol thumbnail
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF212121)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Видеофайл", color = Color.White, fontSize = 11.sp)
                    }
                }
            }

            // Dark semi-transparent text background footer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color(0x99000000))
                    .padding(6.dp)
            ) {
                Column {
                    Text(
                        text = item.filename,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Sizing: ${item.sizeFormatted}",
                        color = Color.LightGray,
                        fontSize = 9.sp
                    )
                }
            }

            // Checkbox Overlay mapping selection toggles
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelect() },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(2.dp),
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = Color.White
                )
            )

            // Dimensions Overlay (if available)
            if (item.width > 0 && item.height > 0) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0x7F000000),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    Text(
                        text = "${item.width}x${item.height}",
                        fontSize = 9.sp,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ImagePreviewZoomLayout(url: String, userAgent: String, referer: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(url)
                .apply {
                    if (userAgent.isNotEmpty()) {
                        addHeader("User-Agent", userAgent)
                    }
                    if (referer.isNotEmpty()) {
                        addHeader("Referer", referer)
                    }
                }
                .crossfade(true)
                .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                .build(),
            contentDescription = "Preview Image",
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.Fit,
            error = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.BrokenImage)
        )
    }
}

@Composable
fun VideoPreviewPlayer(url: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    setVideoURI(Uri.parse(url))
                    val mediaController = MediaController(ctx)
                    mediaController.setAnchorView(this)
                    setMediaController(mediaController)
                    start()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        )
    }
}

fun openDownloadedFile(context: android.content.Context, download: com.example.model.DownloadEntity) {
    val uriString = download.localUri
    if (uriString.isNullOrBlank()) {
        android.widget.Toast.makeText(context, "Локальный путь не найден", android.widget.Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val uri = android.net.Uri.parse(uriString)
        val ext = download.filename.substringAfterLast('.', "").lowercase()
        val mimeType = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: context.contentResolver.getType(uri) ?: "*/*"
        
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            val uri = android.net.Uri.parse(uriString)
            val ext = download.filename.substringAfterLast('.', "").lowercase()
            val mimeType = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: context.contentResolver.getType(uri) ?: "*/*"
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = android.content.Intent.createChooser(intent, "Открыть через")
            chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (ex: Exception) {
            android.widget.Toast.makeText(context, "Не удалось открыть файл: ${ex.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}

fun shareDownloadedFile(context: android.content.Context, download: com.example.model.DownloadEntity) {
    val uriString = download.localUri
    if (uriString.isNullOrBlank()) {
        android.widget.Toast.makeText(context, "Файл еще не скачан", android.widget.Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val uri = android.net.Uri.parse(uriString)
        val ext = download.filename.substringAfterLast('.', "").lowercase()
        val mimeType = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: context.contentResolver.getType(uri) ?: "*/*"
        
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = android.content.Intent.createChooser(intent, "Поделиться файлом")
        chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Не удалось отправить файл: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun ImageSearchCard(
    searchImage: com.example.model.SearchImage,
    onClick: () -> Unit,
    onDownloadClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(4.dp)
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("image_card_${searchImage.imageUrl}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = coil.request.ImageRequest.Builder(LocalContext.current)
                    .data(searchImage.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = searchImage.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                error = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Image)
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            ),
                            startY = 100f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Text(
                    text = searchImage.title,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val faviconUrl = "https://www.google.com/s2/favicons?sz=64&domain=${searchImage.siteUrl}"
                    AsyncImage(
                        model = coil.request.ImageRequest.Builder(LocalContext.current)
                            .data(faviconUrl)
                            .crossfade(true)
                            .build(),
                        placeholder = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Language),
                        error = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Language),
                        contentDescription = "Favicon",
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Fit
                    )
                    
                    Text(
                        text = searchImage.siteName,
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = { onDownloadClick(searchImage.imageUrl) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(48.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = CircleShape
                    )
                    .testTag("download_button_${searchImage.imageUrl}")
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Download Image",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun ImageGridShimmer() {
    val shimmerBrush = rememberShimmerBrush()
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(8.dp),
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = false
    ) {
        items(8) {
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(shimmerBrush)
            )
        }
    }
}

@Composable
fun rememberShimmerBrush(): Brush {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f),
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
    )

    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_anim"
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )
}

@Composable
fun EmptyState() {
    val l10n = LocalAppStrings.current
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Language,
            contentDescription = "No Results",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = l10n.nothingFound,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

fun getFaviconUrl(url: String): String {
    if (url.isBlank() || url == "about:blank") return ""
    return try {
        val uri = android.net.Uri.parse(url)
        val host = uri.host ?: ""
        if (host.isNotEmpty()) {
            "https://www.google.com/s2/favicons?sz=128&domain=$host"
        } else {
            ""
        }
    } catch (e: Exception) {
        ""
    }
}

fun smartUrlParse(input: String, isHttpsEverywhereEnabled: Boolean = true, searchUrlProvider: (String) -> String): String {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return ""

    // Rewrite http to https if HTTPS Everywhere is active
    if (isHttpsEverywhereEnabled && trimmed.startsWith("http://", ignoreCase = true)) {
        return "https://" + trimmed.substring(7)
    }

    // Already has protocol?
    if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
        return trimmed
    }

    // Heuristics checking if it's a URL:
    // 1. Doesn't contain spaces.
    // 2. Contains at least one dot '.' (e.g. google.com, subdomain.test.org, 192.168.1.1).
    // 3. Or it is "localhost".
    val hasSpace = trimmed.contains(" ")
    val hasDot = trimmed.contains(".")
    val isLocalhost = trimmed.equals("localhost", ignoreCase = true) || trimmed.startsWith("localhost:")

    return if (!hasSpace && (hasDot || isLocalhost)) {
        "https://$trimmed"
    } else {
        searchUrlProvider(trimmed)
    }
}

