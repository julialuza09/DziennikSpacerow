package pbs.edu.kurs2.walk

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pbs.edu.kurs2.model.WalkSession
import pbs.edu.kurs2.storage.WalkStorage
import kotlin.math.roundToInt

class WalkRecorderViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx = app.applicationContext

    private val fused = LocationServices.getFusedLocationProviderClient(ctx)

    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        2000L
    ).setMinUpdateDistanceMeters(2f).build()

    private var lastLocation: Location? = null
    private var startedAt: Long? = null
    private var elapsedJob: Job? = null

    // kroki
    private val sensorManager = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private var stepsAtStart: Float? = null
    private var stepListener: SensorEventListener? = null

    private val _ui = MutableStateFlow(
        RecorderUiState(stepSensorAvailable = stepCounter != null)
    )
    val ui: StateFlow<RecorderUiState> = _ui

    private val _sessions = MutableStateFlow(WalkStorage.load(ctx).toList())
    val sessions: StateFlow<List<WalkSession>> = _sessions

    private fun nextId(): Int = (_sessions.value.maxOfOrNull { it.id } ?: 0) + 1

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: return
            val prev = lastLocation
            if (prev != null) {
                val d = prev.distanceTo(loc).toDouble()
                _ui.value = _ui.value.copy(distanceMeters = _ui.value.distanceMeters + d)
                updateDerived()
            }
            lastLocation = loc
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    private fun updateDerived() {
        val start = startedAt ?: return
        val sec = ((System.currentTimeMillis() - start) / 1000L).toInt().coerceAtLeast(1)
        val avg = _ui.value.distanceMeters / sec.toDouble()
        _ui.value = _ui.value.copy(elapsedSec = sec, avgSpeedMps = avg)
    }

    fun start() {
        if (_ui.value.isRecording) return

        startedAt = System.currentTimeMillis()
        lastLocation = null
        stepsAtStart = null

        _ui.value = RecorderUiState(
            isRecording = true,
            status = "Nagrywanie...",
            stepSensorAvailable = stepCounter != null
        )

        // lokalizacja
        if (hasLocationPermission()) {
            try {
                fused.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            } catch (_: SecurityException) {
                _ui.value = _ui.value.copy(status = "Brak dostępu do GPS")
            }
        } else {
            _ui.value = _ui.value.copy(status = "Brak uprawnień GPS")
        }

        // kroki
        if (stepCounter != null) {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val v = event.values.firstOrNull() ?: return
                    if (stepsAtStart == null) stepsAtStart = v
                    val diff = (v - (stepsAtStart ?: v)).roundToInt().coerceAtLeast(0)
                    _ui.value = _ui.value.copy(steps = diff)
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            stepListener = listener
            sensorManager.registerListener(listener, stepCounter, SensorManager.SENSOR_DELAY_NORMAL)
        }

        // timer
        elapsedJob?.cancel()
        elapsedJob = viewModelScope.launch {
            while (_ui.value.isRecording) {
                updateDerived()
                delay(1000)
            }
        }
    }

    fun addPhoto(uri: String) {
        if (!_ui.value.isRecording) return
        _ui.value = _ui.value.copy(photoUris = _ui.value.photoUris + uri)
    }

    fun stopAndSave() {
        if (!_ui.value.isRecording) return
        val start = startedAt ?: return
        val end = System.currentTimeMillis()

        try {
            fused.removeLocationUpdates(locationCallback)
        } catch (_: SecurityException) {
        }
        stepListener?.let { sensorManager.unregisterListener(it) }
        stepListener = null
        elapsedJob?.cancel()
        elapsedJob = null

        val durationSec = ((end - start) / 1000L).toInt().coerceAtLeast(1)
        val dist = _ui.value.distanceMeters
        val avgSpeed = dist / durationSec.toDouble()

        val id = nextId()
        val session = WalkSession(
            id = id,
            title = "Spacer #$id",
            startedAtMillis = start,
            endedAtMillis = end,
            durationSec = durationSec,
            distanceMeters = dist,
            steps = _ui.value.steps,
            avgSpeedMps = avgSpeed,
            photoUris = _ui.value.photoUris
        )

        val newList = _sessions.value.toMutableList().apply { add(0, session) }
        _sessions.value = newList
        WalkStorage.save(ctx, newList)

        // reset
        startedAt = null
        lastLocation = null
        stepsAtStart = null
        _ui.value = RecorderUiState(
            status = "Zapisano spacer",
            stepSensorAvailable = stepCounter != null
        )
    }

    fun deleteSession(id: Int) {
        val newList = _sessions.value.filterNot { it.id == id }
        _sessions.value = newList
        WalkStorage.save(ctx, newList)
    }

    fun resetAll() {
        _sessions.value = emptyList()
        WalkStorage.reset(ctx)
    }

    fun getSession(id: Int?): WalkSession? = _sessions.value.find { it.id == id }
}
