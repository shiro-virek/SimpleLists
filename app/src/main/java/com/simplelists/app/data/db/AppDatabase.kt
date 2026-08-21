package com.simplelists.app.data.db

import android.content.Context
import android.net.Uri
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Database(
    entities = [TabEntity::class, ItemEntity::class, TagEntity::class, ItemTagCrossRef::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tabDao(): TabDao
    abstract fun itemDao(): ItemDao
    abstract fun tagDao(): TagDao

    companion object {
        const val DB_NAME = "simplelists.db"
    }
}

object DbProvider {

    @Volatile
    private var instance: AppDatabase? = null

    fun get(context: Context): AppDatabase =
        instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                AppDatabase.DB_NAME
            ).build().also { instance = it }
        }

    fun close() {
        synchronized(this) {
            instance?.close()
            instance = null
        }
    }

    suspend fun export(context: Context, target: Uri): Boolean = withContext(Dispatchers.IO) {
        val db = get(context)
        // Volcar el WAL al archivo principal para que la copia esté completa
        db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }

        val dbFile = context.getDatabasePath(AppDatabase.DB_NAME)
        try {
            context.contentResolver.openOutputStream(target)?.use { out ->
                dbFile.inputStream().use { it.copyTo(out) }
            } ?: return@withContext false
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun import(context: Context, source: Uri): Boolean = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val dbFile = appContext.getDatabasePath(AppDatabase.DB_NAME)
        val walFile = java.io.File(dbFile.parentFile, AppDatabase.DB_NAME + "-wal")
        val shmFile = java.io.File(dbFile.parentFile, AppDatabase.DB_NAME + "-shm")

        // Validar que sea una base SQLite antes de pisar la actual
        val header = CharArray(16)
        try {
            appContext.contentResolver.openInputStream(source)?.use { input ->
                val bytes = ByteArray(16)
                if (input.read(bytes) != 16) return@withContext false
                for (i in 0 until 16) header[i] = bytes[i].toInt().toChar()
            } ?: return@withContext false
        } catch (e: Exception) {
            return@withContext false
        }
        if (!String(header).contentEquals("SQLite format 3\u0000")) return@withContext false

        close()
        try {
            walFile.delete()
            shmFile.delete()
            appContext.contentResolver.openInputStream(source)?.use { input ->
                dbFile.outputStream().use { output -> input.copyTo(output) }
            } ?: return@withContext false
            get(appContext)
            true
        } catch (e: Exception) {
            false
        }
    }
}
