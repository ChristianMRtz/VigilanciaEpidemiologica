package com.chrismr.vigilancia.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object BackupManager {

    private const val DB_NAME = "vigilancia_epidemiologica.db"

    /**
     * Crea un ZIP con los archivos de la BD (incluye .db, -shm y -wal si existen)
     * y lo guarda en la carpeta Descargas del dispositivo.
     * Devuelve el Uri y nombre del archivo generado.
     */
    suspend fun backupToDevice(context: Context): Result<Pair<Uri, String>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val appContext = context.applicationContext
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "vigilancia_backup_$timestamp.zip"
                val bytes = createBackupZip(appContext)

                val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android 10+ → MediaStore (carpeta Descargas pública)
                    val resolver = appContext.contentResolver
                    val col = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    val cv = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                        put(MediaStore.Downloads.MIME_TYPE, "application/zip")
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }
                    val itemUri = resolver.insert(col, cv)!!
                    resolver.openOutputStream(itemUri)!!.use { it.write(bytes) }
                    cv.clear()
                    cv.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(itemUri, cv, null, null)
                    itemUri
                } else {
                    // Android < 10 → carpeta propia en almacenamiento externo
                    val dir = appContext.getExternalFilesDir("Backups") ?: appContext.filesDir
                    dir.mkdirs()
                    val file = File(dir, fileName)
                    file.writeBytes(bytes)
                    FileProvider.getUriForFile(appContext, "${appContext.packageName}.provider", file)
                }

                uri to fileName
            }
        }

    /**
     * Hace backup y abre el selector nativo de Android para compartir
     * (correo, WhatsApp, Google Drive, etc.).
     */
    suspend fun backupAndShare(context: Context) {
        val appContext = context.applicationContext
        val result = backupToDevice(appContext)

        withContext(Dispatchers.Main) {
            result.fold(
                onSuccess = { (uri, fileName) ->
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/zip"
                        putExtra(Intent.EXTRA_SUBJECT, "Backup – Vigilancia Epidemiológica")
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "Backup de la base de datos de Vigilancia Epidemiológica\nArchivo: $fileName"
                        )
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    appContext.startActivity(
                        Intent.createChooser(shareIntent, "Enviar backup por…")
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                },
                onFailure = { e ->
                    Toast.makeText(
                        appContext,
                        "Error al crear backup: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Comprime los archivos de la BD en un ZIP en memoria.
     * Room usa WAL por defecto, por eso se incluyen también los archivos -shm y -wal.
     */
    private fun createBackupZip(context: Context): ByteArray {
        val dbFile  = context.getDatabasePath(DB_NAME)
        val shmFile = File("${dbFile.path}-shm")
        val walFile = File("${dbFile.path}-wal")

        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { zip ->
            listOf(dbFile, shmFile, walFile)
                .filter { it.exists() }
                .forEach { file ->
                    zip.putNextEntry(ZipEntry(file.name))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
        }
        return bos.toByteArray()
    }
}

