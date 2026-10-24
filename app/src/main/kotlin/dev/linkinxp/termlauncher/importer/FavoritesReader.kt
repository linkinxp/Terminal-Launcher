package dev.linkinxp.termlauncher.importer

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipInputStream

private const val NO_LAYOUT = "import: no launcher layout found in this file"
private const val TOO_LARGE = "import: file is too large (64 MB max)"
private const val UNREADABLE = "import: could not read that file"
private const val DEFAULT_MAX = 64L * 1024 * 1024
private val SQLITE_MAGIC = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
private val ZIP_MAGIC = byteArrayOf('P'.code.toByte(), 'K'.code.toByte())

private class TooLarge : Exception()

/**
 * Reads the `favorites` table of a Nova/Lawnchair (Launcher3-family) backup: either a ZIP containing a file named
 * `launcher.db` (any folder, any case) or a raw SQLite file. The caller owns [input]. Everything is copied to
 * temporary files in [cacheDir] (never more than [maxBytes] each, so a ZIP bomb is refused) and deleted again on
 * every path. Failures carry the message to show.
 */
object FavoritesReader {
    fun read(input: InputStream, cacheDir: File, maxBytes: Long = DEFAULT_MAX): Result<List<FavRow>> {
        val temp = mutableListOf<File>()
        return try {
            val raw = File.createTempFile("import-", ".tmp", cacheDir).also { temp += it }
            copyLimited(input, raw, maxBytes)
            when {
                startsWith(raw, ZIP_MAGIC) -> {
                    val db = extractLauncherDb(raw, cacheDir, maxBytes, temp)
                    if (db == null) fail(NO_LAYOUT) else query(db)
                }
                startsWith(raw, SQLITE_MAGIC) -> query(raw)
                else -> fail(NO_LAYOUT)
            }
        } catch (_: TooLarge) {
            fail(TOO_LARGE)
        } catch (_: Exception) {
            fail(UNREADABLE)
        } finally {
            temp.forEach { it.delete() }
        }
    }

    private fun fail(message: String) = Result.failure<List<FavRow>>(IOException(message))

    private fun startsWith(file: File, magic: ByteArray): Boolean = file.inputStream().use { s ->
        val head = ByteArray(magic.size)
        s.readNBytes(head, 0, head.size) == magic.size && head.contentEquals(magic)
    }

    private fun copyLimited(input: InputStream, dest: File, maxBytes: Long) {
        FileOutputStream(dest).use { out ->
            val buffer = ByteArray(8 * 1024)
            var total = 0L
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                total += n
                if (total > maxBytes) throw TooLarge()
                out.write(buffer, 0, n)
            }
        }
    }

    private fun extractLauncherDb(zip: File, cacheDir: File, maxBytes: Long, temp: MutableList<File>): File? {
        ZipInputStream(zip.inputStream()).use { z ->
            while (true) {
                val entry = z.nextEntry ?: return null
                if (!entry.isDirectory && entry.name.substringAfterLast('/').equals("launcher.db", ignoreCase = true)) {
                    val out = File.createTempFile("import-db-", ".db", cacheDir).also { temp += it }
                    copyLimited(z, out, maxBytes)
                    return out
                }
            }
        }
    }

    private fun query(file: File): Result<List<FavRow>> {
        var db: SQLiteDatabase? = null
        return try {
            db = SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)
            val rows = ArrayList<FavRow>()
            db.rawQuery("SELECT _id, itemType, container, screen, cellX, cellY, title, intent FROM favorites", null).use { c ->
                while (c.moveToNext()) {
                    rows += FavRow(
                        id = c.getInt(0), itemType = c.getInt(1), container = c.getInt(2), screen = c.getInt(3),
                        cellX = c.getInt(4), cellY = c.getInt(5),
                        title = if (c.isNull(6)) null else c.getString(6),
                        intent = if (c.isNull(7)) null else c.getString(7),
                    )
                }
            }
            Result.success(rows)
        } catch (_: SQLiteException) {
            fail(NO_LAYOUT) // not a database, no `favorites` table, or a column is missing
        } finally {
            db?.close()
        }
    }
}
