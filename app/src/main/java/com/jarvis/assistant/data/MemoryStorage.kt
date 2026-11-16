package com.jarvis.assistant.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class MemoryRecord(
    val id: Long = System.currentTimeMillis(),
    val fact: String,
    val timestamp: Long = System.currentTimeMillis()
)

class MemoryStorage(context: Context) {

    private val prefs = context.getSharedPreferences("jarvis_memory", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val type = object : TypeToken<MutableList<MemoryRecord>>() {}.type

    @Synchronized
    fun getAllMemories(): List<MemoryRecord> {
        val json = prefs.getString("memories", null) ?: return emptyList()
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun addMemory(fact: String) {
        val list = getAllMemories().toMutableList()
        list.add(0, MemoryRecord(fact = fact.trim()))
        prefs.edit().putString("memories", gson.toJson(list)).apply()
    }

    @Synchronized
    fun removeMemory(id: Long) {
        val list = getAllMemories().filterNot { it.id == id }
        prefs.edit().putString("memories", gson.toJson(list)).apply()
    }

    @Synchronized
    fun clearAll() {
        prefs.edit().remove("memories").apply()
    }
}
