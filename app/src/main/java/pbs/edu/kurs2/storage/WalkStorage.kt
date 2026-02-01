package pbs.edu.kurs2.storage

import android.content.Context
import pbs.edu.kurs2.model.WalkSession
import java.io.File

object WalkStorage {
    private const val FILE_NAME = "walk_sessions.csv"
    private fun file(ctx: Context) = File(ctx.filesDir, FILE_NAME)

    fun load(ctx: Context): MutableList<WalkSession> {
        val f = file(ctx)
        if (!f.exists()) return mutableListOf()

        return f.readLines().mapNotNull { line ->
            val p = line.split(";")
            if (p.size < 9) return@mapNotNull null
            try {
                WalkSession(
                    id = p[0].toInt(),
                    title = p[1],
                    startedAtMillis = p[2].toLong(),
                    endedAtMillis = p[3].toLong(),
                    durationSec = p[4].toInt(),
                    distanceMeters = p[5].toDouble(),
                    steps = p[6].toInt(),
                    avgSpeedMps = p[7].toDouble(),
                    photoUris = if (p[8].isBlank()) emptyList() else p[8].split("|")
                )
            } catch (_: Exception) { null }
        }.toMutableList()
    }

    fun save(ctx: Context, sessions: List<WalkSession>) {
        val f = file(ctx)
        val content = sessions.joinToString("\n") { s ->
            val photos = s.photoUris.joinToString("|") { it.replace(";", "") }
            listOf(
                s.id,
                s.title.replace(";", ","),
                s.startedAtMillis,
                s.endedAtMillis,
                s.durationSec,
                s.distanceMeters,
                s.steps,
                s.avgSpeedMps,
                photos
            ).joinToString(";")
        }
        f.writeText(content)
    }

    fun reset(ctx: Context) {
        val f = file(ctx)
        if (f.exists()) f.delete()
    }
}
