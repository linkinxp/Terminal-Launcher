package dev.linkinxp.termlauncher.importer

import android.database.sqlite.SQLiteDatabase
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FavoritesReaderTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var cache: File

    @Before fun setUp() { cache = tmp.newFolder("cache") }

    private val noLayout = "import: no launcher layout found in this file"

    private fun dbBytes(
        create: String = "CREATE TABLE favorites (_id INTEGER, itemType INTEGER, container INTEGER, screen INTEGER, cellX INTEGER, cellY INTEGER, title TEXT, intent TEXT, extra TEXT)",
        rows: List<List<Any?>> = listOf(
            listOf(1, 0, -100, 0, 1, 2, "Chrome", "#Intent;component=com.android.chrome/x.Main;end", "z"),
            listOf(2, 2, -101, 0, 0, 0, null, null, null),
        ),
    ): ByteArray {
        val f = tmp.newFile()
        f.delete()
        val db = SQLiteDatabase.openOrCreateDatabase(f, null)
        db.execSQL(create)
        for (r in rows) db.execSQL("INSERT INTO favorites VALUES (${r.joinToString { if (it == null) "NULL" else if (it is String) "'$it'" else "$it" }})")
        db.close()
        return f.readBytes()
    }

    private fun zip(name: String, content: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { z -> z.putNextEntry(ZipEntry(name)); z.write(content); z.closeEntry() }
        return out.toByteArray()
    }

    private fun read(bytes: ByteArray, max: Long = 64L * 1024 * 1024) =
        FavoritesReader.read(ByteArrayInputStream(bytes), cache, max)

    private fun assertCacheEmpty() = assertEquals(emptyList<String>(), cache.list()?.toList())

    @Test fun aRawDatabaseReturnsItsRows() {
        val rows = read(dbBytes()).getOrThrow()
        assertEquals(2, rows.size)
        assertEquals(FavRow(1, 0, -100, 0, 1, 2, "Chrome", "#Intent;component=com.android.chrome/x.Main;end"), rows[0])
        assertEquals(FavRow(2, 2, -101, 0, 0, 0, null, null), rows[1])
        assertCacheEmpty()
    }

    @Test fun aZipWithLauncherDbAtAnyDepthAndCaseIsRead() {
        assertEquals(2, read(zip("backup/launcher.db", dbBytes())).getOrThrow().size)
        assertEquals(2, read(zip("LAUNCHER.DB", dbBytes())).getOrThrow().size)
        assertCacheEmpty()
    }

    @Test fun aZipWithoutLauncherDbHasNoLayout() {
        assertEquals(noLayout, read(zip("settings.xml", "<x/>".toByteArray())).exceptionOrNull()?.message)
        assertCacheEmpty()
    }

    @Test fun textAndEmptyFilesHaveNoLayout() {
        assertEquals(noLayout, read("just some text".toByteArray()).exceptionOrNull()?.message)
        assertEquals(noLayout, read(ByteArray(0)).exceptionOrNull()?.message)
        assertCacheEmpty()
    }

    @Test fun aDatabaseWithoutFavoritesHasNoLayout() {
        val bytes = dbBytes(create = "CREATE TABLE other (_id INTEGER, itemType INTEGER, container INTEGER, screen INTEGER, cellX INTEGER, cellY INTEGER, title TEXT, intent TEXT)", rows = emptyList())
        // The helper inserts into `favorites`, so build this one without rows and the wrong table name.
        assertEquals(noLayout, read(bytes).exceptionOrNull()?.message)
        assertCacheEmpty()
    }

    @Test fun aFavoritesTableMissingAColumnHasNoLayout() {
        val bytes = dbBytes(create = "CREATE TABLE favorites (_id INTEGER, itemType INTEGER, container INTEGER, screen INTEGER, cellX INTEGER, cellY INTEGER, title TEXT)", rows = emptyList())
        assertEquals(noLayout, read(bytes).exceptionOrNull()?.message)
        assertCacheEmpty()
    }

    @Test fun anEmptyFavoritesTableIsAValidEmptyLayout() {
        assertEquals(emptyList<FavRow>(), read(dbBytes(rows = emptyList())).getOrThrow())
    }

    @Test fun filesOverTheLimitAreRefused() {
        val msg = read(ByteArray(2_000), max = 1_000).exceptionOrNull()?.message
        assertEquals("import: file is too large (64 MB max)", msg)
        assertCacheEmpty()
    }

    @Test fun aZipThatInflatesPastTheLimitIsRefused() {
        val bomb = zip("launcher.db", ByteArray(50_000)) // compresses to a few hundred bytes
        assertTrue(bomb.size < 2_000)
        val msg = read(bomb, max = 10_000).exceptionOrNull()?.message
        assertEquals("import: file is too large (64 MB max)", msg)
        assertCacheEmpty()
    }
}
