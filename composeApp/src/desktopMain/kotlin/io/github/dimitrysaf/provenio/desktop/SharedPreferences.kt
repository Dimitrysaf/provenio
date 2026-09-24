package io.github.dimitrysaf.provenio.desktop

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Android's SharedPreferences API over one JSON file per preference set, so the Android storage
 * code runs on desktop unchanged. Values keep their type, as Android's do.
 */
class SharedPreferences private constructor(private val file: File) {
    private val values = ConcurrentHashMap<String, Any>()

    init {
        load()
    }

    val all: Map<String, *> get() = HashMap(values)

    fun contains(key: String): Boolean = values.containsKey(key)

    fun getString(key: String, defaultValue: String?): String? = values[key] as? String ?: defaultValue

    fun getBoolean(key: String, defaultValue: Boolean): Boolean = values[key] as? Boolean ?: defaultValue

    fun getInt(key: String, defaultValue: Int): Int = (values[key] as? Number)?.toInt() ?: defaultValue

    fun getLong(key: String, defaultValue: Long): Long = (values[key] as? Number)?.toLong() ?: defaultValue

    fun getFloat(key: String, defaultValue: Float): Float = (values[key] as? Number)?.toFloat() ?: defaultValue

    @Suppress("UNCHECKED_CAST")
    fun getStringSet(key: String, defaultValues: Set<String>?): Set<String>? =
        (values[key] as? Set<String>)?.toSet() ?: defaultValues

    fun edit(): Editor = Editor()

    inner class Editor internal constructor() {
        private val changes = LinkedHashMap<String, Any?>()
        private var clearFirst = false

        fun putString(key: String, value: String?): Editor {
            changes[key] = value
            return this
        }
        fun putBoolean(key: String, value: Boolean): Editor {
            changes[key] = value
            return this
        }
        fun putInt(key: String, value: Int): Editor {
            changes[key] = value
            return this
        }
        fun putLong(key: String, value: Long): Editor {
            changes[key] = value
            return this
        }
        fun putFloat(key: String, value: Float): Editor {
            changes[key] = value
            return this
        }
        fun putStringSet(key: String, value: Set<String>?): Editor {
            changes[key] = value?.toSet()
            return this
        }
        fun remove(key: String): Editor {
            changes[key] = null
            return this
        }
        fun clear(): Editor {
            clearFirst = true
            return this
        }

        /** Updates memory now and writes the file in the background, as Android's apply does. */
        fun apply() {
            merge()
            writer.execute { write() }
        }

        fun commit(): Boolean {
            merge()
            return write()
        }

        private fun merge() {
            synchronized(this@SharedPreferences) {
                if (clearFirst) values.clear()
                for ((key, value) in changes) {
                    if (value == null) values.remove(key) else values[key] = value
                }
            }
        }
    }

    private fun load() {
        if (!file.isFile) return
        val root = runCatching { Json.parseToJsonElement(file.readText()).jsonObject }.getOrNull() ?: return
        for ((key, entry) in root) {
            val typed = entry as? JsonObject ?: continue
            val value = typed["v"] ?: continue
            val decoded: Any? = when (typed["t"]?.jsonPrimitive?.contentOrNull) {
                "s" -> value.jsonPrimitive.contentOrNull
                "b" -> value.jsonPrimitive.booleanOrNull
                "i" -> value.jsonPrimitive.intOrNull
                "l" -> value.jsonPrimitive.longOrNull
                "f" -> value.jsonPrimitive.floatOrNull
                "set" -> value.jsonArray.mapNotNull { it.jsonPrimitive.contentOrNull }.toSet()
                else -> null
            }
            if (decoded != null) values[key] = decoded
        }
    }

    @Synchronized
    private fun write(): Boolean = runCatching {
        val snapshot = buildJsonObject {
            for ((key, value) in values.entries.sortedBy { it.key }) {
                val (type, encoded) = when (value) {
                    is String -> "s" to JsonPrimitive(value)
                    is Boolean -> "b" to JsonPrimitive(value)
                    is Int -> "i" to JsonPrimitive(value)
                    is Long -> "l" to JsonPrimitive(value)
                    is Float -> "f" to JsonPrimitive(value)
                    is Set<*> -> "set" to JsonArray(value.map { JsonPrimitive(it.toString()) })
                    else -> continue
                }
                put(key, JsonObject(mapOf("t" to JsonPrimitive(type), "v" to encoded)))
            }
        }
        file.parentFile?.mkdirs()
        val temporary = File(file.parentFile, ".${file.name}.tmp")
        temporary.writeText(snapshot.toString())
        Files.move(
            temporary.toPath(),
            file.toPath(),
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE,
        )
        true
    }.getOrDefault(false)

    companion object {
        private val opened = ConcurrentHashMap<String, SharedPreferences>()
        private val writer = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "preferences-writer").apply { isDaemon = true }
        }

        internal fun open(directory: File, name: String): SharedPreferences {
            val safeName = name.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val file = File(directory, "$safeName.json")
            return opened.getOrPut(file.absolutePath) { SharedPreferences(file) }
        }

        /** Every preference set, for wiping local account data. */
        internal fun deleteAll(directory: File) {
            opened.values.forEach { preferences ->
                synchronized(preferences) { preferences.values.clear() }
                preferences.write()
            }
            directory.listFiles()?.forEach { it.delete() }
        }
    }
}
