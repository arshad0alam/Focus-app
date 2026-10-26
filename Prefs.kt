package com.stayfocused.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Keyword(val word: String, val minutes: Int)

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("sf", Context.MODE_PRIVATE)

    fun limits(c: Context): Map<String, Int> {
        val o = JSONObject(sp(c).getString("limits", "{}")!!)
        return o.keys().asSequence().associateWith { o.getInt(it) }
    }

    fun setLimit(c: Context, pkg: String, minutes: Int?) {
        val o = JSONObject(sp(c).getString("limits", "{}")!!)
        if (minutes == null) o.remove(pkg) else o.put(pkg, minutes)
        sp(c).edit().putString("limits", o.toString()).apply()
    }

    fun keywords(c: Context): List<Keyword> {
        val a = JSONArray(sp(c).getString("keywords", "[]")!!)
        return (0 until a.length()).map {
            val o = a.getJSONObject(it)
            Keyword(o.getString("w"), o.getInt("m"))
        }
    }

    private fun saveKeywords(c: Context, list: List<Keyword>) {
        val a = JSONArray()
        list.forEach { a.put(JSONObject().put("w", it.word).put("m", it.minutes)) }
        sp(c).edit().putString("keywords", a.toString()).apply()
    }

    fun addKeyword(c: Context, k: Keyword) =
        saveKeywords(c, keywords(c).filter { it.word != k.word } + k)

    fun removeKeyword(c: Context, word: String) =
        saveKeywords(c, keywords(c).filter { it.word != word })

    fun focusUntil(c: Context): Long = sp(c).getLong("focus_until", 0L)

    fun isFocus(c: Context) = focusUntil(c) > System.currentTimeMillis()

    fun startFocus(c: Context, minutes: Int) {
        val end = System.currentTimeMillis() + minutes * 60_000L
        sp(c).edit().putLong("focus_until", maxOf(end, focusUntil(c))).apply()
    }
}
