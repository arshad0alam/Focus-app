package com.stayfocused.app

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import java.util.Calendar

data class AppItem(val pkg: String, val label: String)

object Sys {
    fun hasUsageAccess(c: Context): Boolean {
        val ops = c.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        @Suppress("DEPRECATION")
        val mode = ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), c.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun hasAccessibility(c: Context): Boolean {
        val s = Settings.Secure.getString(c.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false
        return s.contains("${c.packageName}/${FocusService::class.java.name}")
    }

    fun homePkg(c: Context): String? =
        c.packageManager.resolveActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0
        )?.activityInfo?.packageName

    fun label(c: Context, pkg: String): String = try {
        c.packageManager.getApplicationLabel(c.packageManager.getApplicationInfo(pkg, 0)).toString()
    } catch (e: Exception) { pkg }

    fun icon(c: Context, pkg: String): ImageBitmap? = try {
        c.packageManager.getApplicationIcon(pkg).toBitmap(96, 96).asImageBitmap()
    } catch (e: Exception) { null }

    fun launchableApps(c: Context): List<AppItem> {
        val pm = c.packageManager
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(i, 0)
            .map { AppItem(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
            .filter { it.pkg != c.packageName }
            .distinctBy { it.pkg }
            .sortedBy { it.label.lowercase() }
    }

    /** Foreground time per package since midnight, in ms. */
    fun todayUsage(c: Context): Map<String, Long> {
        val usm = c.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val now = System.currentTimeMillis()
        val ev = usm.queryEvents(cal.timeInMillis, now)
        val e = UsageEvents.Event()
        val start = HashMap<String, Long>()
        val total = HashMap<String, Long>()
        fun close(key: String, ts: Long) {
            val s = start.remove(key) ?: return
            val pkg = key.substringBefore('/')
            total[pkg] = (total[pkg] ?: 0L) + (ts - s)
        }
        while (ev.hasNextEvent()) {
            ev.getNextEvent(e)
            val key = e.packageName + "/" + (e.className ?: "")
            when (e.eventType) {
                1 -> start[key] = e.timeStamp          // ACTIVITY_RESUMED
                2 -> close(key, e.timeStamp)           // ACTIVITY_PAUSED
                16 -> start.keys.toList().forEach { close(it, e.timeStamp) } // screen off
            }
        }
        start.keys.toList().forEach { close(it, now) }
        return total
    }
}
