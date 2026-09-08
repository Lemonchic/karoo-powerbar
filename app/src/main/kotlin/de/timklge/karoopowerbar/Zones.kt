package de.timklge.karoopowerbar

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import io.hammerhead.karooext.models.UserProfile

enum class Zone(val colorResource: Int){
    Zone0(R.color.zone0),
    Zone1(R.color.zone1),
    Zone2(R.color.zone2),
    Zone3(R.color.zone3),
    Zone4(R.color.zone4),
    Zone5(R.color.zone5),
    Zone6(R.color.zone6),
    Zone7(R.color.zone7),
    Zone8(R.color.zone8),
}

val zones = mapOf(
    1 to listOf(Zone.Zone7),
    2 to listOf(Zone.Zone1, Zone.Zone7),
    3 to listOf(Zone.Zone1, Zone.Zone3, Zone.Zone7),
    4 to listOf(Zone.Zone1, Zone.Zone3, Zone.Zone5, Zone.Zone7),
    5 to listOf(Zone.Zone1, Zone.Zone2, Zone.Zone3, Zone.Zone5, Zone.Zone7),
    6 to listOf(Zone.Zone1, Zone.Zone2, Zone.Zone3, Zone.Zone5, Zone.Zone7, Zone.Zone8),
    7 to listOf(Zone.Zone1, Zone.Zone2, Zone.Zone3, Zone.Zone5, Zone.Zone6, Zone.Zone7, Zone.Zone8),
    8 to listOf(Zone.Zone0, Zone.Zone1, Zone.Zone2, Zone.Zone3, Zone.Zone5, Zone.Zone6, Zone.Zone7, Zone.Zone8),
    9 to listOf(Zone.Zone0, Zone.Zone1, Zone.Zone2, Zone.Zone3, Zone.Zone4, Zone.Zone5, Zone.Zone6, Zone.Zone7, Zone.Zone8)
)

fun getZone(userZones: List<UserProfile.Zone>, value: Int): Zone? {
    val zoneList = zones[userZones.size] ?: return null

    userZones.forEachIndexed { index, zone ->
        if (value in zone.min..zone.max) {
            return zoneList.getOrNull(index) ?: Zone.Zone7
        }
    }

    return null
}

fun getShadedArrowColor(context: Context, userZones: List<UserProfile.Zone>, watts: Int): Int {
    if (userZones.isEmpty()) return ContextCompat.getColor(context, R.color.zone1)

    val zoneList = zones[userZones.size] ?: zones[7] ?: return ContextCompat.getColor(context, R.color.zone1)
    val colors = zoneList.map { ContextCompat.getColor(context, it.colorResource) }

    // Calculate center power for each zone:
    val midpoints = userZones.map { zone ->
        val effectiveMax = if (zone.max > 1500) (zone.min * 1.3).toInt() else zone.max
        (zone.min + effectiveMax) / 2.0
    }

    if (watts <= midpoints.first()) return colors.first()
    if (watts >= midpoints.last()) return colors.last()

    for (i in 0 until midpoints.size - 1) {
        val m1 = midpoints[i]
        val m2 = midpoints[i + 1]
        if (watts.toDouble() in m1..m2) {
            val fraction = if (m2 > m1) ((watts - m1) / (m2 - m1)).coerceIn(0.0, 1.0).toFloat() else 0f
            return ColorUtils.blendARGB(colors[i], colors[i + 1], fraction)
        }
    }

    return colors.last()
}

val zoneList = listOf(Zone.Zone0, Zone.Zone1, Zone.Zone2, Zone.Zone3, Zone.Zone4, Zone.Zone5, Zone.Zone6, Zone.Zone7, Zone.Zone8)

fun getZone(progress: Double): Zone {
    val index = (progress * zoneList.size).toInt().coerceIn(0, zoneList.size - 1)
    return zoneList[index]
}