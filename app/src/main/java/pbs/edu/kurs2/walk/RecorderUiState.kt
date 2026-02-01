package pbs.edu.kurs2.walk

data class RecorderUiState(
    val isRecording: Boolean = false,
    val elapsedSec: Int = 0,
    val distanceMeters: Double = 0.0,
    val steps: Int = 0,
    val avgSpeedMps: Double = 0.0,
    val photoUris: List<String> = emptyList(),
    val status: String = "Gotowe",
    val stepSensorAvailable: Boolean = true
)
