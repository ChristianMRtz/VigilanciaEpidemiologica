package com.chrismr.vigilancia.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.chrismr.vigilancia.domain.enums.MonitoringStatus
import com.chrismr.vigilancia.domain.model.DailyMonitoringModel
import com.chrismr.vigilancia.domain.model.PatientModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ExcelExporter {

    // Estilos (cellXfs index):
    //  0 = normal
    //  1 = título: negrita grande, centrado
    //  2 = encabezado: negrita blanca, fondo azul, centrado
    private val STYLES_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="3">
    <font><sz val="11"/><name val="Calibri"/></font>
    <font><b/><sz val="16"/><name val="Calibri"/></font>
    <font><b/><color rgb="FFFFFFFF"/><sz val="11"/><name val="Calibri"/></font>
  </fonts>
  <fills count="3">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FF2E75B6"/><bgColor indexed="64"/></patternFill></fill>
  </fills>
  <borders count="1">
    <border><left/><right/><top/><bottom/><diagonal/></border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="3">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1" applyAlignment="1">
      <alignment horizontal="center"/>
    </xf>
    <xf numFmtId="0" fontId="2" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1" applyAlignment="1">
      <alignment horizontal="center"/>
    </xf>
  </cellXfs>
</styleSheet>"""

    private fun statusSymbol(status: MonitoringStatus): String = when (status) {
        MonitoringStatus.INICIO          -> "1"
        MonitoringStatus.CONTINUA        -> "✔"
        MonitoringStatus.RETIRO          -> "2"
        MonitoringStatus.SIN_DISPOSITIVO -> "-"
        MonitoringStatus.EGRESO          -> "X"
        MonitoringStatus.VACIO           -> ""
    }

    /** Índice 0-base → nombre de columna Excel (0→A, 1→B, 26→AA …) */
    private fun colName(index: Int): String {
        var n = index + 1
        val sb = StringBuilder()
        while (n > 0) {
            val rem = (n - 1) % 26
            sb.insert(0, 'A' + rem)
            n = (n - 1) / 26
        }
        return sb.toString()
    }

    private fun esc(s: String) = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    private fun buildXlsx(
        patients: List<PatientModel>,
        monitorings: List<DailyMonitoringModel>,
        year: Int,
        month: Int
    ): ByteArray {
        val days      = DateUtils.getDaysInMonth(year, month)
        val lastCol   = colName(days)          // colName(days): día 1→B, día 31→AF
        val lookup    = monitorings
            .groupBy { it.patientId }
            .mapValues { (_, list) -> list.associate { it.day to it.status } }
        val withData  = patients.filter { it.id in lookup.keys }
        val title     = "Vigilancia Epidemiológica — ${DateUtils.getMonthName(month)} $year"

        // ── xl/worksheets/sheet1.xml ─────────────────────────────────────
        val sheetXml = buildString {
            append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")

            // Anchos de columna: A más ancho, resto compacto
            append("""<cols>""")
            append("""<col min="1" max="1" width="14" customWidth="1"/>""")
            append("""<col min="2" max="33" width="4" customWidth="1"/>""")
            append("""</cols>""")

            // Panel congelado: filas 1-2 (título + cabecera) y columna A
            append("""<sheetViews><sheetView workbookViewId="0">""")
            append("""<pane xSplit="1" ySplit="2" topLeftCell="B3" activePane="bottomRight" state="frozen"/>""")
            append("""</sheetView></sheetViews>""")

            append("""<sheetData>""")

            // Fila 1 — Título (solo celda A1, se mergeará luego)
            append("""<row r="1"><c r="A1" t="inlineStr" s="1"><is><t>${esc(title)}</t></is></c></row>""")

            // Fila 2 — Cabecera (estilo azul)
            append("""<row r="2">""")
            append("""<c r="A2" t="inlineStr" s="2"><is><t>DNI</t></is></c>""")
            for (d in 1..days) append("""<c r="${colName(d)}2" s="2"><v>$d</v></c>""")
            append("""</row>""")

            // Filas de datos desde fila 3
            withData.forEachIndexed { i, patient ->
                val r = i + 3
                append("""<row r="$r">""")
                append("""<c r="A$r" t="inlineStr"><is><t>${esc(patient.dni)}</t></is></c>""")
                val dayMap = lookup[patient.id] ?: emptyMap()
                for (d in 1..days) {
                    val sym = dayMap[d]?.let { statusSymbol(it) } ?: ""
                    if (sym.isNotEmpty())
                        append("""<c r="${colName(d)}$r" t="inlineStr"><is><t>$sym</t></is></c>""")
                }
                append("""</row>""")
            }

            append("""</sheetData>""")

            // Merge de título: A1 → {lastCol}1
            append("""<mergeCells count="1"><mergeCell ref="A1:${lastCol}1"/></mergeCells>""")

            append("""</worksheet>""")
        }

        // ── Construir ZIP ────────────────────────────────────────────────
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { z ->
            fun add(name: String, xml: String) {
                z.putNextEntry(ZipEntry(name))
                z.write(xml.toByteArray(Charsets.UTF_8))
                z.closeEntry()
            }

            add("[Content_Types].xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>""")

            add("_rels/.rels", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>""")

            add("xl/workbook.xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets><sheet name="Seg ${DateUtils.formatMonthYear(month, year)}" sheetId="1" r:id="rId1"/></sheets>
</workbook>""")

            add("xl/_rels/workbook.xml.rels", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>""")

            add("xl/styles.xml", STYLES_XML)
            add("xl/worksheets/sheet1.xml", sheetXml)
        }
        return bos.toByteArray()
    }

    suspend fun export(
        context: Context,
        patients: List<PatientModel>,
        monitorings: List<DailyMonitoringModel>,
        year: Int,
        month: Int
    ) {
        val appContext = context.applicationContext

        val result = withContext(Dispatchers.IO) {
            runCatching {
                val fileName = "vigilancia_${year}_${month.toString().padStart(2, '0')}.xlsx"
                val bytes = buildXlsx(patients, monitorings, year, month)

                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = appContext.contentResolver
                    val col = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    resolver.delete(col, "${MediaStore.Downloads.DISPLAY_NAME} = ?", arrayOf(fileName))
                    val cv = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                        put(MediaStore.Downloads.MIME_TYPE,
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }
                    val itemUri = resolver.insert(col, cv)!!
                    resolver.openOutputStream(itemUri)!!.use { it.write(bytes) }
                    cv.clear(); cv.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(itemUri, cv, null, null)
                    itemUri
                } else {
                    val dir = appContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                        ?: appContext.filesDir
                    dir.mkdirs()
                    val file = File(dir, fileName)
                    FileOutputStream(file).use { it.write(bytes) }
                    FileProvider.getUriForFile(appContext, "${appContext.packageName}.provider", file)
                }

                uri to fileName
            }
        }

        withContext(Dispatchers.Main) {
            result.fold(
                onSuccess = { (uri, fileName) ->
                    Toast.makeText(appContext, "✓ Guardado en Descargas: $fileName", Toast.LENGTH_LONG).show()
                    val openIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri,
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try { appContext.startActivity(openIntent) } catch (_: Exception) { }
                },
                onFailure = { e ->
                    Toast.makeText(appContext, "Error al exportar: ${e.message}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }
}
