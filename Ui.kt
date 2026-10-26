package com.stayfocused.app

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

// ---------- Theme ----------

@Composable
fun SFTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF8AB4FF),
            onPrimary = Color(0xFF0B1220),
            secondary = Color(0xFF7EE0C3),
            background = Color(0xFF0D1117),
            surface = Color(0xFF161B22),
            surfaceVariant = Color(0xFF222A35),
            onBackground = Color(0xFFE6EDF3),
            onSurface = Color(0xFFE6EDF3),
            onSurfaceVariant = Color(0xFF9DA7B3),
            error = Color(0xFFFF7B72)
        ),
        content = content
    )
}

// ---------- Helpers ----------

fun fmtMin(m: Int): String =
    if (m >= 60) (if (m % 60 == 0) "${m / 60}h" else "${m / 60}h ${m % 60}m") else "${m}m"

fun fmtMs(ms: Long): String = if (ms < 60_000) "<1m" else fmtMin((ms / 60_000).toInt())

fun fmtClock(ms: Long): String {
    val s = maxOf(ms, 0L) / 1000
    val h = s / 3600
    return if (h > 0) "%d:%02d:%02d".format(h, (s % 3600) / 60, s % 60)
    else "%02d:%02d".format(s / 60, s % 60)
}

@Composable
fun SFCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) { Column(Modifier.padding(16.dp), content = content) }
}

@Composable
fun Header(title: String, sub: String) {
    Column(Modifier.padding(top = 12.dp, bottom = 4.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(sub, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun AppIcon(pkg: String, size: Dp = 40.dp) {
    val ctx = LocalContext.current
    val bmp = remember(pkg) { Sys.icon(ctx, pkg) }
    val shape = RoundedCornerShape(10.dp)
    if (bmp != null) {
        Image(bitmap = bmp, contentDescription = null, modifier = Modifier.size(size).clip(shape))
    } else {
        Box(Modifier.size(size).clip(shape).background(MaterialTheme.colorScheme.surfaceVariant))
    }
}

// ---------- App shell ----------

@Composable
fun App() {
    var tab by remember { mutableIntStateOf(0) }
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(5000); tick++ } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                listOf(
                    "Home" to Icons.Filled.Home,
                    "Limits" to Icons.Filled.Lock,
                    "Keywords" to Icons.Filled.Search
                ).forEachIndexed { i, (name, icon) ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(icon, name) },
                        label = { Text(name) }
                    )
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> HomeScreen(tick)
                1 -> LimitsScreen()
                else -> KeywordsScreen()
            }
        }
    }
}

// ---------- Home ----------

@Composable
fun PermCard(title: String, text: String, action: String, onClick: () -> Unit) {
    SFCard {
        Text(title, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onClick) { Text(action) }
    }
}

@Composable
fun FocusCard() {
    val ctx = LocalContext.current
    var left by remember { mutableLongStateOf(Prefs.focusUntil(ctx) - System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            left = Prefs.focusUntil(ctx) - System.currentTimeMillis()
            delay(1000)
        }
    }
    SFCard {
        if (left > 0) {
            Text("Focus mode is ON", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(fmtClock(left), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Text("Only calls are allowed until the timer ends.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text("Start focus mode", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text("Locks every app except calls.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15, 30, 60, 120).forEach { m ->
                    FilledTonalButton(
                        onClick = { Prefs.startFocus(ctx, m); left = m * 60_000L },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) { Text(fmtMin(m)) }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(tick: Int) {
    val ctx = LocalContext.current
    val hasUsage = remember(tick) { Sys.hasUsageAccess(ctx) }
    val hasA11y = remember(tick) { Sys.hasAccessibility(ctx) }
    val home = remember { Sys.homePkg(ctx) }
    val usage = remember(tick, hasUsage) {
        if (hasUsage) Sys.todayUsage(ctx).filterKeys {
            it != ctx.packageName && it != home && it != "com.android.systemui"
        } else emptyMap()
    }
    val limits = remember(tick) { Prefs.limits(ctx) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Header("StayFocused", "Take your phone back.") }
        if (!hasUsage) item {
            PermCard("Usage access needed", "Required to measure how long you use each app.", "Grant") {
                ctx.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
        if (!hasA11y) item {
            PermCard(
                "Accessibility service needed",
                "Required to block apps and detect keyword searches. On Android 13+, if the switch is greyed out: App info → ⋮ → Allow restricted settings.",
                "Open settings"
            ) {
                ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
        item { FocusCard() }
        item {
            SFCard {
                Text("Screen time today", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    fmtMs(usage.values.sum()),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        val top = usage.entries.sortedByDescending { it.value }.take(10)
        items(top, key = { it.key }) { (pkg, ms) ->
            val limit = limits[pkg]
            SFCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(pkg)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(Sys.label(ctx, pkg), fontWeight = FontWeight.Medium, maxLines = 1)
                        Text(
                            if (limit != null) "${fmtMs(ms)} / ${fmtMin(limit)}" else fmtMs(ms),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (limit != null) {
                    val p = (ms / (limit * 60_000f)).coerceIn(0f, 1f)
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { p },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = if (p >= 1f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

// ---------- App limits ----------

@Composable
fun LimitsScreen() {
    val ctx = LocalContext.current
    val apps = remember { Sys.launchableApps(ctx) }
    var limits by remember { mutableStateOf(Prefs.limits(ctx)) }
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<AppItem?>(null) }
    val shown = remember(query, apps, limits) {
        apps.filter { it.label.contains(query, true) }.sortedByDescending { limits.containsKey(it.pkg) }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Header("App limits", "Set a daily limit per app. When time is up, the app is blocked until tomorrow.")
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search apps") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(shown, key = { it.pkg }) { app ->
                val lim = limits[app.pkg]
                SFCard(Modifier.clickable { editing = app }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppIcon(app.pkg)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(app.label, fontWeight = FontWeight.Medium, maxLines = 1)
                            Text(
                                if (lim != null) "Limit: ${fmtMin(lim)} / day" else "No limit",
                                color = if (lim != null) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (lim != null) Icon(Icons.Filled.Lock, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }

    editing?.let { app ->
        var mins by remember(app) { mutableFloatStateOf((limits[app.pkg] ?: 30).toFloat()) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(app.label) },
            text = {
                Column {
                    Text(
                        fmtMin(mins.toInt()) + " per day",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Slider(value = mins, onValueChange = { mins = it }, valueRange = 5f..240f, steps = 46)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    Prefs.setLimit(ctx, app.pkg, mins.toInt())
                    limits = Prefs.limits(ctx)
                    editing = null
                }) { Text("Save") }
            },
            dismissButton = {
                Row {
                    if (limits.containsKey(app.pkg)) {
                        TextButton(onClick = {
                            Prefs.setLimit(ctx, app.pkg, null)
                            limits = Prefs.limits(ctx)
                            editing = null
                        }) { Text("Remove") }
                    }
                    TextButton(onClick = { editing = null }) { Text("Cancel") }
                }
            }
        )
    }
}

// ---------- Keywords ----------

@Composable
fun KeywordsScreen() {
    val ctx = LocalContext.current
    var list by remember { mutableStateOf(Prefs.keywords(ctx)) }
    var word by remember { mutableStateOf("") }
    var mins by remember { mutableFloatStateOf(30f) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Header(
                "Keyword triggers",
                "If you search any of these words in a browser, your phone goes into focus mode for the time you set."
            )
        }
        item {
            SFCard {
                OutlinedTextField(
                    value = word,
                    onValueChange = { word = it },
                    label = { Text("Word or phrase") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Text("Focus time: ${fmtMin(mins.toInt())}", fontWeight = FontWeight.Medium)
                Slider(value = mins, onValueChange = { mins = it }, valueRange = 5f..180f, steps = 34)
                Button(
                    onClick = {
                        val w = word.trim().lowercase()
                        if (w.isNotEmpty()) {
                            Prefs.addKeyword(ctx, Keyword(w, mins.toInt()))
                            list = Prefs.keywords(ctx)
                            word = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add keyword")
                }
            }
        }
        items(list, key = { it.word }) { k ->
            SFCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("“${k.word}”", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Search → ${fmtMin(k.minutes)} focus mode",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = {
                        Prefs.removeKeyword(ctx, k.word)
                        list = Prefs.keywords(ctx)
                    }) { Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}

// ---------- Block screen ----------

@Composable
fun BlockScreen(title: String, msg: String, onHome: () -> Unit) {
    val ctx = LocalContext.current
    var left by remember { mutableLongStateOf(Prefs.focusUntil(ctx) - System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            left = Prefs.focusUntil(ctx) - System.currentTimeMillis()
            delay(1000)
        }
    }
    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0D1117), Color(0xFF1B2540))))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Lock, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(24.dp))
            Text(
                title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(msg, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            if (left > 0) {
                Spacer(Modifier.height(24.dp))
                Text(
                    fmtClock(left), style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary
                )
                Text("focus time left", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(32.dp))
            Button(onClick = onHome) { Text("Go to home screen") }
        }
    }
}
