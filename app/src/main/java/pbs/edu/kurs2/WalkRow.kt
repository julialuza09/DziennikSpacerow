package pbs.edu.kurs2

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import pbs.edu.kurs2.model.WalkSession
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WalkRow(
    session: WalkSession,
    onClick: (Int) -> Unit,
    onDelete: (Int) -> Unit
) {
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val thumb = session.photoUris.firstOrNull()

    Card(
        modifier = Modifier
            .padding(vertical = 6.dp)
            .fillMaxWidth()
            .clickable { onClick(session.id) }
    ) {
        Row(Modifier.padding(12.dp)) {

            if (thumb != null) {
                Card(
                    modifier = Modifier.size(64.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(thumb),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(Modifier.width(12.dp))
            }

            Column(Modifier.weight(1f)) {
                Text(
                    session.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text("Start: ${sdf.format(Date(session.startedAtMillis))}", style = MaterialTheme.typography.bodySmall)
                Text("Dystans: ${"%.2f".format(session.distanceMeters / 1000.0)} km", style = MaterialTheme.typography.bodyMedium)
                Text("Czas: ${formatDuration(session.durationSec)} • Kroki: ${session.steps}", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = { onDelete(session.id) }) { Text("Usuń") }
        }
    }
}

private fun formatDuration(sec: Int): String {
    val m = sec / 60
    val s = sec % 60
    return "${m}m ${s}s"
}
