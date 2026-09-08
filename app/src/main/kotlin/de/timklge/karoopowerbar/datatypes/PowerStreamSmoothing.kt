package de.timklge.karoopowerbar.datatypes

import io.hammerhead.karooext.models.DataType

enum class PowerStreamSmoothing(val dataTypeId: String){
    RAW(DataType.Type.POWER),
    SMOOTHED_3S(DataType.Type.SMOOTHED_3S_AVERAGE_POWER),
    SMOOTHED_5S(DataType.Type.SMOOTHED_5S_AVERAGE_POWER),
    SMOOTHED_10S(DataType.Type.SMOOTHED_10S_AVERAGE_POWER),
    SMOOTHED_30S(DataType.Type.SMOOTHED_30S_AVERAGE_POWER),
}