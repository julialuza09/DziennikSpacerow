package pbs.edu.kurs2.screens.details

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import pbs.edu.kurs2.walk.WalkRecorderViewModel

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun DetailsScreen(navController: NavController, id: Int?) {
    val vm: WalkRecorderViewModel = viewModel()
    val session = vm.getSession(id)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(session?.title ?: "Szczegóły spaceru") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Text("←")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (session == null) {
                Text("Nie znaleziono spaceru.", color = MaterialTheme.colorScheme.error)
                return@Column
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Dystans: ${"%.2f".format(session.distanceMeters / 1000.0)} km")
                    Text("Czas: ${formatDuration(session.durationSec)}")
                    Text("Kroki: ${session.steps}")
                    Text("Śr. prędkość: ${"%.2f".format(session.avgSpeedMps)} m/s")
                    Text("Zdjęć: ${session.photoUris.size}")
                }
            }

            if (session.photoUris.isNotEmpty()) {
                Text("Zdjęcia", style = MaterialTheme.typography.titleMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(session.photoUris) { uri ->
                        Card(Modifier.size(180.dp)) {
                            Image(
                                painter = rememberAsyncImagePainter(uri),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            } else {
                Text("Brak zdjęć w tym spacerze.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun formatDuration(sec: Int): String {
    val m = sec / 60
    val s = sec % 60
    return "${m}m ${s}s"
}
