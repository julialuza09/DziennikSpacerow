package pbs.edu.kurs2.screens.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import pbs.edu.kurs2.WalkRow
import pbs.edu.kurs2.navigation.WalkScreens
import pbs.edu.kurs2.walk.WalkRecorderViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val vm: WalkRecorderViewModel = viewModel()
    val ui by vm.ui.collectAsState()
    val sessions by vm.sessions.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current

    val requiredPerms = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.CAMERA,
        Manifest.permission.ACTIVITY_RECOGNITION
    )

    fun allGranted(): Boolean = requiredPerms.all { context.hasPermission(it) }

    var permissionsOk by remember { mutableStateOf(allGranted()) }
    var status by remember { mutableStateOf("Gotowe") }

    // najpierw zapytanie o uprawnienia
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        permissionsOk = result.values.all { it }
        status = if (permissionsOk) "Uprawnienia OK" else "Brak uprawnień – nadaj je, aby używać GPS/kamery"
    }

    LaunchedEffect(Unit) {
        if (!allGranted()) {
            status = "Proszę o uprawnienia..."
            permLauncher.launch(requiredPerms)
        } else {
            status = "Uprawnienia OK"
        }
    }

    // aparat
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok ->
        if (ok) {
            pendingUri?.toString()?.let { vm.addPhoto(it) }
        }
        pendingUri = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Dziennik spacerów")
                        Text(
                            text = status,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(ui.status, color = MaterialTheme.colorScheme.secondary)
                    Text("Czas: ${formatDuration(ui.elapsedSec)}")
                    Text("Dystans: ${"%.2f".format(ui.distanceMeters / 1000.0)} km")
                    Text("Kroki: ${ui.steps}")
                    if (!ui.stepSensorAvailable) {
                        Text(
                            "Uwaga: brak sensora kroków na tym urządzeniu (kroki mogą być 0).",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text("Śr. prędkość: ${"%.2f".format(ui.avgSpeedMps)} m/s")
                    Text("Zdjęcia: ${ui.photoUris.size}")
                }
            }

            if (!permissionsOk) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { permLauncher.launch(requiredPerms) }
                ) { Text("Nadaj uprawnienia") }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = permissionsOk && !ui.isRecording,
                    onClick = { vm.start() }
                ) { Text("START") }

                Button(
                    modifier = Modifier.weight(1f),
                    enabled = ui.isRecording,
                    onClick = { vm.stopAndSave() }
                ) { Text("STOP + Zapisz") }
            }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = permissionsOk && ui.isRecording,
                onClick = {
                    val uri = createPhotoUri(context)
                    pendingUri = uri
                    takePictureLauncher.launch(uri)
                }
            ) { Text("Dodaj zdjęcie podczas spaceru") }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { vm.resetAll() }
            ) { Text("Reset danych") }

            Divider()

            Text("Historia", style = MaterialTheme.typography.titleLarge)

            if (sessions.isEmpty()) {
                Text(
                    "Brak zapisanych spacerów. Kliknij START, dodaj zdjęcia i STOP + Zapisz.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn {
                    items(sessions) { s ->
                        WalkRow(
                            session = s,
                            onClick = { navController.navigate("${WalkScreens.DetailsScreen.name}/$it") },
                            onDelete = { vm.deleteSession(it) }
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(sec: Int): String {
    val m = sec / 60
    val s = sec % 60
    return "${m}m ${s}s"
}

private fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

private fun createPhotoUri(context: Context): Uri {
    val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        ?: File(context.filesDir, "pictures")

    val dir = File(baseDir, "walk_photos").apply { mkdirs() }
    val file = File(dir, "walk_${System.currentTimeMillis()}.jpg")

    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
}
