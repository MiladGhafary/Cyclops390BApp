package ir.cyclops.app

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class Tube(
    val id: String,
    val temperature: Double? = null,
    val timestamp: Long = 0L,
    val confirmed: Boolean = false
)

object TemperatureParser {
    // JCCP+897.0 / JHCP+923.0 / JCCP+_ _ _ را نادیده می‌گیرد
    private val regex = Regex("""J[CH]CP([+-][\d.]+)""")
    fun parse(line: String): Double? =
        regex.find(line)?.groupValues?.get(1)?.toDoubleOrNull()
}

object AppState {
    val defaultTubeIds = listOf(
        "SEBS", "MEBS", "NEBS",
        "SETS", "SETN", "METS", "METN", "NETS", "NETN",
        "NWTN", "NRTN", "NWTS", "NRTS",
        "MWTN", "MRTN", "MWTS", "MRTS",
        "SWTN", "SRTN", "SWTS", "SRTS"
    )

    val tubes = MutableStateFlow(defaultTubeIds.map { Tube(it) })
    val currentIndex = MutableStateFlow(0)
    val lastTemperature = MutableStateFlow<Double?>(null)
    val status = MutableStateFlow("قطع")
    val isConnected = MutableStateFlow(false)
    val deviceName = MutableStateFlow("Cyclops186034")

    private var dataFile: File? = null

    fun init(context: Context) {
        dataFile = File(context.filesDir, "measurements.json")
        load()
    }

    fun save() {
        val f = dataFile ?: return
        val arr = JSONArray()
        tubes.value.forEach { t ->
            arr.put(JSONObject().apply {
                put("id", t.id)
                t.temperature?.let { put("temp", it) }
                put("ts", t.timestamp)
                put("ok", t.confirmed)
            })
        }
        val root = JSONObject().apply {
            put("tubes", arr)
            put("currentIndex", currentIndex.value)
        }
        f.writeText(root.toString())
    }

    fun load() {
        val f = dataFile ?: return
        if (!f.exists()) return
        try {
            val root = JSONObject(f.readText())
            val arr = root.getJSONArray("tubes")
            val list = mutableListOf<Tube>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    Tube(
                        id = o.getString("id"),
                        temperature = if (o.has("temp")) o.getDouble("temp") else null,
                        timestamp = o.optLong("ts", 0L),
                        confirmed = o.optBoolean("ok", false)
                    )
                )
            }
            tubes.value = list
            currentIndex.value = root.optInt("currentIndex", 0)
        } catch (_: Exception) {}
    }

    fun confirmCurrent(temp: Double): Boolean {
        val idx = currentIndex.value
        val list = tubes.value
        if (idx >= list.size) return false
        val updated = list.toMutableList()
        updated[idx] = updated[idx].copy(
            temperature = temp,
            timestamp = System.currentTimeMillis(),
            confirmed = true
        )
        tubes.value = updated
        currentIndex.value = idx + 1
        save()
        return true
    }

    fun undo() {
        if (currentIndex.value == 0) return
        val idx = currentIndex.value - 1
        val list = tubes.value.toMutableList()
        list[idx] = list[idx].copy(temperature = null, timestamp = 0L, confirmed = false)
        tubes.value = list
        currentIndex.value = idx
        save()
    }

    fun reset() {
        tubes.value = defaultTubeIds.map { Tube(it) }
        currentIndex.value = 0
        lastTemperature.value = null
        save()
    }

    fun toCsv(): String {
        val sb = StringBuilder("tube,temperature_c,timestamp\n")
        tubes.value.forEach { t ->
            sb.append(t.id).append(",")
            sb.append(t.temperature?.toString() ?: "").append(",")
            sb.append(if (t.timestamp > 0) t.timestamp else "").append("\n")
        }
        return sb.toString()
    }
}