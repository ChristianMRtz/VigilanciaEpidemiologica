package com.chrismr.vigilancia.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
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

    // Índices cellXfs:
    //  0  = default
    //  1  = título (bold 16, centrado)
    //  2  = cabecera (bold blanco, fondo azul, centrado, borde)
    //  3  = DNI fila normal (izquierda, borde)
    //  4  = día vacío fila normal (centrado, borde)
    //  5  = INICIO  – fondo verde claro, bold, centrado, borde
    //  6  = CONTINUA – fondo azul claro, bold, centrado, borde
    //  7  = RETIRO  – fondo naranja claro, bold, centrado, borde
    //  8  = SIN_DISP – fondo gris claro, centrado, borde
    //  9  = EGRESO  – fondo rojo claro, bold, centrado, borde
    // 10  = DNI fila alterna (fondo azul pálido, izquierda, borde)
    // 11  = día vacío fila alterna (fondo azul pálido, centrado, borde)
    private val STYLES_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="4">
    <font><sz val="11"/><name val="Calibri"/></font>
    <font><b/><sz val="16"/><name val="Calibri"/></font>
    <font><b/><color rgb="FFFFFFFF"/><sz val="11"/><name val="Calibri"/></font>
    <font><b/><sz val="11"/><name val="Calibri"/></font>
  </fonts>
  <fills count="10">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FF1565C0"/><bgColor indexed="64"/></patternFill></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFC8E6C9"/><bgColor indexed="64"/></patternFill></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFBBDEFB"/><bgColor indexed="64"/></patternFill></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFFFE0B2"/><bgColor indexed="64"/></patternFill></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFECEFF1"/><bgColor indexed="64"/></patternFill></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFFFCDD2"/><bgColor indexed="64"/></patternFill></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFE8EDF5"/><bgColor indexed="64"/></patternFill></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFFFFFFF"/><bgColor indexed="64"/></patternFill></fill>
  </fills>
  <borders count="2">
    <border><left/><right/><top/><bottom/><diagonal/></border>
    <border>
      <left style="thin"><color rgb="FFBDBDBD"/></left>
      <right style="thin"><color rgb="FFBDBDBD"/></right>
      <top style="thin"><color rgb="FFBDBDBD"/></top>
      <bottom style="thin"><color rgb="FFBDBDBD"/></bottom>
      <diagonal/>
    </border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="12">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center"/>
    </xf>
    <xf numFmtId="0" fontId="2" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center"/>
    </xf>
    <xf numFmtId="0" fontId="0" fillId="9" borderId="1" xfId="0" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment vertical="center"/>
    </xf>
    <xf numFmtId="0" fontId="0" fillId="9" borderId="1" xfId="0" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center"/>
    </xf>
    <xf numFmtId="0" fontId="3" fillId="3" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center"/>
    </xf>
    <xf numFmtId="0" fontId="3" fillId="4" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center"/>
    </xf>
    <xf numFmtId="0" fontId="3" fillId="5" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center"/>
    </xf>
    <xf numFmtId="0" fontId="0" fillId="6" borderId="1" xfId="0" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center"/>
    </xf>
    <xf numFmtId="0" fontId="3" fillId="7" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center"/>
    </xf>
    <xf numFmtId="0" fontId="0" fillId="8" borderId="1" xfId="0" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment vertical="center"/>
    </xf>
    <xf numFmtId="0" fontId="0" fillId="8" borderId="1" xfId="0" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center"/>
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

    /** Devuelve el índice de estilo cellXfs según estado y si la fila es alterna */
    private fun dayCellStyle(status: MonitoringStatus?, isAltRow: Boolean): Int = when (status) {
        MonitoringStatus.INICIO          -> 5
        MonitoringStatus.CONTINUA        -> 6
        MonitoringStatus.RETIRO          -> 7
        MonitoringStatus.SIN_DISPOSITIVO -> 8
        MonitoringStatus.EGRESO          -> 9
        else                             -> if (isAltRow) 11 else 4
    }

    private fun buildSheetXml(
        patients: List<PatientModel>,
        monitorings: List<DailyMonitoringModel>,
        year: Int,
        month: Int
    ): String {
        val days     = DateUtils.getDaysInMonth(year, month)
        val lastCol  = colName(days)
        val lookup   = monitorings
            .groupBy { it.patientId }
            .mapValues { (_, list) -> list.associate { it.day to it.status } }
        val withData = patients.filter { it.id in lookup.keys }
        val title    = "Vigilancia Epidemiológica — ${DateUtils.getMonthName(month)} $year"

        return buildString {
            append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
            append("""<cols>""")
            append("""<col min="1" max="1" width="14" customWidth="1"/>""")
            append("""<col min="2" max="33" width="4.5" customWidth="1"/>""")
            append("""</cols>""")
            append("""<sheetViews><sheetView workbookViewId="0">""")
            append("""<pane xSplit="1" ySplit="2" topLeftCell="B3" activePane="bottomRight" state="frozen"/>""")
            append("""</sheetView></sheetViews>""")
            append("""<sheetData>""")
            // Fila 1 — Título
            append("""<row r="1" ht="28" customHeight="1">""")
            append("""<c r="A1" t="inlineStr" s="1"><is><t>${esc(title)}</t></is></c>""")
            append("""</row>""")
            // Fila 2 — Cabecera
            append("""<row r="2" ht="22" customHeight="1">""")
            append("""<c r="A2" t="inlineStr" s="2"><is><t>DNI</t></is></c>""")
            for (d in 1..days) append("""<c r="${colName(d)}2" s="2"><v>$d</v></c>""")
            append("""</row>""")
            // Filas de datos
            withData.forEachIndexed { i, patient ->
                val r        = i + 3
                val isAltRow = i % 2 == 1
                val dniStyle = if (isAltRow) 10 else 3
                append("""<row r="$r" ht="20" customHeight="1">""")
                append("""<c r="A$r" t="inlineStr" s="$dniStyle"><is><t>${esc(patient.dni)}</t></is></c>""")
                val dayMap = lookup[patient.id] ?: emptyMap()
                for (d in 1..days) {
                    val status = dayMap[d]
                    val sym    = status?.let { statusSymbol(it) } ?: ""
                    val sIdx   = dayCellStyle(status, isAltRow)
                    if (sym.isNotEmpty()) {
                        append("""<c r="${colName(d)}$r" t="inlineStr" s="$sIdx"><is><t>$sym</t></is></c>""")
                    } else {
                        append("""<c r="${colName(d)}$r" s="$sIdx"/>""")
                    }
                }
                append("""</row>""")
            }
            append("""</sheetData>""")
            append("""<mergeCells count="1"><mergeCell ref="A1:${lastCol}1"/></mergeCells>""")
            append("""</worksheet>""")
        }
    }

    private fun buildXlsx(
        patients: List<PatientModel>,
        monitorings: List<DailyMonitoringModel>,
        year: Int,
        month: Int
    ): ByteArray {
        val sheetXml = buildSheetXml(patients, monitorings, year, month)

        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { z ->
            fun add(name: String, xml: String) {
                z.putNextEntry(ZipEntry(name)); z.write(xml.toByteArray(Charsets.UTF_8)); z.closeEntry()
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

    /** Construye un XLSX con una hoja por cada mes en [months]. */
    private fun buildYearlyXlsx(
        patients: List<PatientModel>,
        monitoringsByMonth: Map<Int, List<DailyMonitoringModel>>,
        year: Int,
        months: List<Int>
    ): ByteArray {
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { z ->
            fun add(name: String, xml: String) {
                z.putNextEntry(ZipEntry(name)); z.write(xml.toByteArray(Charsets.UTF_8)); z.closeEntry()
            }

            // Content Types — una entrada por hoja
            val contentTypes = buildString {
                append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
                append("""<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""")
                append("""<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""")
                append("""<Default Extension="xml" ContentType="application/xml"/>""")
                append("""<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""")
                append("""<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>""")
                months.forEachIndexed { i, _ ->
                    append("""<Override PartName="/xl/worksheets/sheet${i + 1}.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""")
                }
                append("""</Types>""")
            }
            add("[Content_Types].xml", contentTypes)

            add("_rels/.rels", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>""")

            // Workbook — una <sheet> por mes
            val workbook = buildString {
                append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
                append("""<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">""")
                append("""<sheets>""")
                months.forEachIndexed { i, month ->
                    append("""<sheet name="${DateUtils.getMonthName(month)}" sheetId="${i + 1}" r:id="rId${i + 1}"/>""")
                }
                append("""</sheets></workbook>""")
            }
            add("xl/workbook.xml", workbook)

            // Workbook rels — una relación por hoja + estilos al final
            val workbookRels = buildString {
                append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
                append("""<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
                months.forEachIndexed { i, _ ->
                    append("""<Relationship Id="rId${i + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet${i + 1}.xml"/>""")
                }
                append("""<Relationship Id="rId${months.size + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>""")
                append("""</Relationships>""")
            }
            add("xl/_rels/workbook.xml.rels", workbookRels)

            add("xl/styles.xml", STYLES_XML)

            // Una hoja por mes
            months.forEachIndexed { i, month ->
                val monitorings = monitoringsByMonth[month] ?: emptyList()
                add("xl/worksheets/sheet${i + 1}.xml", buildSheetXml(patients, monitorings, year, month))
            }
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
                saveXlsx(appContext, fileName, bytes) to fileName
            }
        }

        withContext(Dispatchers.Main) {
            result.fold(
                onSuccess = { (uri, fileName) ->
                    Toast.makeText(appContext, "✓ Guardado en Descargas: $fileName", Toast.LENGTH_LONG).show()
                    openXlsx(appContext, uri)
                },
                onFailure = { e ->
                    Toast.makeText(appContext, "Error al exportar: ${e.message}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    /**
     * Exporta un año completo con una hoja por cada mes.
     * Si es el año actual, solo incluye hasta el mes actual.
     */
    suspend fun exportYear(
        context: Context,
        patients: List<PatientModel>,
        monitoringsByMonth: Map<Int, List<DailyMonitoringModel>>,
        year: Int,
        months: List<Int>
    ) {
        val appContext = context.applicationContext

        val result = withContext(Dispatchers.IO) {
            runCatching {
                val fileName = "vigilancia_anual_$year.xlsx"
                val bytes = buildYearlyXlsx(patients, monitoringsByMonth, year, months)
                saveXlsx(appContext, fileName, bytes) to fileName
            }
        }

        withContext(Dispatchers.Main) {
            result.fold(
                onSuccess = { (uri, fileName) ->
                    Toast.makeText(appContext, "✓ Guardado en Descargas: $fileName", Toast.LENGTH_LONG).show()
                    openXlsx(appContext, uri)
                },
                onFailure = { e ->
                    Toast.makeText(appContext, "Error al exportar: ${e.message}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    // ── Helpers de I/O ───────────────────────────────────────────────────

    private fun saveXlsx(context: Context, fileName: String, bytes: ByteArray): android.net.Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
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
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            dir.mkdirs()
            val file = File(dir, fileName)
            FileOutputStream(file).use { it.write(bytes) }
            androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        }
    }

    private fun openXlsx(context: Context, uri: android.net.Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try { context.startActivity(intent) } catch (_: Exception) { }
    }
}
