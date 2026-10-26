package com.stayfocused.app

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.InputMethodManager

class FocusService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var lastScan = 0L
    private var lastBlock = 0L

    private val browsers = setOf(
        "com.android.chrome", "com.chrome.beta", "org.mozilla.firefox", "com.brave.browser",
        "com.microsoft.emmx", "com.opera.browser", "com.opera.mini.native",
        "com.sec.android.app.sbrowser", "com.duckduckgo.mobile.android",
        "com.vivaldi.browser", "com.kiwibrowser.browser", "com.google.android.googlequicksearchbox"
    )

    private val ignored: Set<String> by lazy {
        val s = mutableSetOf(packageName, "com.android.systemui", "android")
        Sys.homePkg(this)?.let { s.add(it) }
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
            .enabledInputMethodList.forEach { s.add(it.packageName) }
        s
    }

    private val dialer: String by lazy {
        packageManager.resolveActivity(Intent(Intent.ACTION_DIAL), 0)?.activityInfo?.packageName
            ?: "com.android.dialer"
    }

    private val tick = object : Runnable {
        override fun run() {
            enforce()
            handler.postDelayed(this, 10_000)
        }
    }

    override fun onServiceConnected() {
        handler.post(tick)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        handler.removeCallbacks(tick)
        return super.onUnbind(intent)
    }

    override fun onInterrupt() {}

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        e ?: return
        val pkg = e.packageName?.toString() ?: return
        when (e.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> enforce(pkg)
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                val now = SystemClock.elapsedRealtime()
                if (pkg in browsers && now - lastScan > 400) {
                    lastScan = now
                    scanBrowser()
                }
            }
        }
    }

    private fun isAllowedInFocus(pkg: String) =
        pkg == dialer || pkg.contains("incallui") || pkg.contains("telecom")

    private fun enforce(p: String? = null) {
        val pkg = p ?: rootInActiveWindow?.packageName?.toString() ?: return
        if (pkg in ignored) return

        if (Prefs.isFocus(this)) {
            if (!isAllowedInFocus(pkg)) {
                show("Focus mode is on", "Only phone calls are allowed right now.")
            }
            return
        }

        val limit = Prefs.limits(this)[pkg] ?: return
        val used = Sys.todayUsage(this)[pkg] ?: 0L
        if (used >= limit * 60_000L) {
            show("Daily limit reached", "You've used your ${limit} min for ${Sys.label(this, pkg)} today.")
        }
    }

    private fun scanBrowser() {
        if (Prefs.isFocus(this)) return
        val kws = Prefs.keywords(this)
        if (kws.isEmpty()) return
        val root = rootInActiveWindow ?: return
        val texts = ArrayList<String>()
        collect(root, texts, 0)
        for (t in texts) {
            val low = t.lowercase().replace("+", " ").replace("%20", " ")
            val hit = kws.firstOrNull { low.contains(it.word) } ?: continue
            Prefs.startFocus(this, hit.minutes)
            show(
                "Focus mode started",
                "You searched “${hit.word}”. Phone is locked for ${hit.minutes} min."
            )
            return
        }
    }

    private fun collect(n: AccessibilityNodeInfo?, out: MutableList<String>, depth: Int) {
        if (n == null || depth > 30 || out.size > 40) return
        val id = n.viewIdResourceName?.lowercase() ?: ""
        if (n.isEditable || id.contains("url") || id.contains("search") || id.contains("omnibox")) {
            n.text?.toString()?.takeIf { it.isNotBlank() }?.let { out.add(it) }
        }
        for (i in 0 until n.childCount) collect(n.getChild(i), out, depth + 1)
    }

    private fun show(title: String, msg: String) {
        val t = SystemClock.elapsedRealtime()
        if (t - lastBlock < 1500) return
        lastBlock = t
        startActivity(
            Intent(this, BlockActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra("title", title)
                .putExtra("msg", msg)
        )
    }
}
