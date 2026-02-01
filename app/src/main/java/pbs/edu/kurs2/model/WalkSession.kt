package pbs.edu.kurs2.model

data class WalkSession(
    val id: Int,
    val title: String,
    val startedAtMillis: Long,
    val endedAtMillis: Long,
    val durationSec: Int,
    val distanceMeters: Double,
    val steps: Int,
    val avgSpeedMps: Double,
    val photoUris: List<String>
)
