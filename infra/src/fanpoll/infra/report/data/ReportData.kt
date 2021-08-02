/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.report.data

import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.extension.myEquals
import fanpoll.infra.base.extension.myHashCode
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.base.util.DateTimeUtils
import fanpoll.infra.base.util.IdentifiableObject
import kotlinx.serialization.json.JsonElement
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*
import java.util.stream.Collectors

class ReportData(
    override val id: String,
    var name: String,
    val tables: MutableList<Table> = mutableListOf()
) : IdentifiableObject<String>() {

    fun toExcel(): ByteArray {

        fun createWorkbook(): XSSFWorkbook {
            val workbook = XSSFWorkbook()
            val xmlProps = workbook.properties
            val coreProps = xmlProps.coreProperties
            //coreProps.creator = "xxx"
            return workbook
        }

        fun createDescriptionStyle(workbook: Workbook): CellStyle {
            val style = workbook.createCellStyle()
            style.wrapText = false
            return style
        }

        fun createColumnNameStyle(workbook: Workbook): CellStyle {
            val style = workbook.createCellStyle()
            style.wrapText = false
            style.alignment = HorizontalAlignment.CENTER
            val columnNameFont = workbook.createFont()
            columnNameFont.bold = true
            style.setFont(columnNameFont)
            return style
        }

        fun createCellStyle(workbook: Workbook): CellStyle {
            val style = workbook.createCellStyle()
            style.wrapText = true
            return style
        }

        fun setCellValue(cell: org.apache.poi.ss.usermodel.Cell, value: Any?) {
            when (value) {
                null -> cell.setBlank()
                is String -> cell.setCellValue(value)
                is Boolean -> cell.setCellValue((value))
                is Number -> cell.setCellValue((value.toDouble()))
                is BigDecimal -> cell.setCellValue(value.setScale(0, RoundingMode.UP).toDouble())
                is Date -> cell.setCellValue(DateTimeUtils.TAIWAN_DATE_TIME_FORMATTER.format(value.toInstant()))
                is Instant -> cell.setCellValue(DateTimeUtils.TAIWAN_DATE_TIME_FORMATTER.format(value))
                is ZonedDateTime -> cell.setCellValue(DateTimeUtils.TAIWAN_DATE_TIME_FORMATTER.format(value))
                is LocalDate -> cell.setCellValue(value)
                is LocalDateTime -> cell.setCellValue(value)
                else -> cell.setCellValue(value.toString())
            }
        }

        fun toWorkbook(): Workbook {
            val workbook = createWorkbook()
            val descriptionStyle = createDescriptionStyle(workbook)
            val columnNameStyle = createColumnNameStyle(workbook)
            val cellStyle = createCellStyle(workbook)
            for (table in tables) {
                val sheet: Sheet = workbook.createSheet(table.name)
                var currentRowNum = 0

                if (table.description != null) {
                    val descriptionRow = sheet.createRow(currentRowNum)
                    val cell = descriptionRow.createCell(0)
                    cell.cellStyle = descriptionStyle
                    setCellValue(cell, table.description)
                    currentRowNum++
                }

                val columnNameRow = sheet.createRow(currentRowNum)
                table.columnNames.forEachIndexed { index, name ->
                    val cell = columnNameRow.createCell(index)
                    cell.cellStyle = columnNameStyle
                    setCellValue(cell, name)
                }
                currentRowNum++

                val tableRows = table.getRows()
                for (i in tableRows.indices) {
                    val row = sheet.createRow(currentRowNum + i)
                    for (j in tableRows[i].cells.indices) {
                        val cell = row.createCell(j)
                        cell.cellStyle = cellStyle
                        setCellValue(cell, tableRows[i].cells[j].value)
                    }
                }

                for (i in 0 until sheet.getRow(0).lastCellNum) {
                    sheet.autoSizeColumn(i)
                }
            }
            return workbook
        }

        fun workbookToByteArray(workbook: Workbook): ByteArray {
            return try {
                val outputStream = ByteArrayOutputStream()
                workbook.write(outputStream)
                outputStream.flush()
                val bytes = outputStream.toByteArray()
                outputStream.close()
                bytes
            } catch (e: IOException) {
                throw InternalServerException(InfraResponseCode.IO_ERROR, "fail to write excel report $id", e)
            }
        }

        val workbook = toWorkbook()
        return workbookToByteArray(workbook)
    }

    fun toCSV(): ByteArray {
        return try {
            val printer = CSVPrinter(StringBuilder(), CSVFormat.DEFAULT)
            for (table in tables) {
                printer.printComment("========== " + table.name + " ==========")
                for (row in table.getRows()) {
                    printer.printRecord(row.cells.stream().map { cell -> cell ?: "" }.collect(Collectors.toList()))
                }
                printer.println()
                printer.println()
            }
            printer.out.toString().toByteArray(StandardCharsets.UTF_8)
        } catch (e: IOException) {
            throw InternalServerException(InfraResponseCode.IO_ERROR, "fail to write csv report $id", e)
        }
    }

    fun toJson(): JsonElement = TODO()

    override fun toString(): String = "$name($id)"
}

class Table(val name: String, val columnIds: List<String>, val columnNames: List<String> = columnIds, val description: String? = null) {

    private val rows: MutableList<Row> = mutableListOf()

    fun getRows(): List<Row> = rows.toList()

    fun addRow(cellValues: List<Any?>) {
        val rowIndex = rows.size
        val cells = cellValues.mapIndexed { columnIndex, value -> Cell(rowIndex, columnIndex, value) }
        rows.add(Row(rowIndex, cells))
    }

    fun addRow(objPropValue: Map<String, Any?>) {
        val rowIndex = rows.size
        val cells = objPropValue.map { Cell(rowIndex, columnIds.indexOf(it.key), it.value) }
        rows.add(Row(rowIndex, cells))
    }

    fun rowCount(): Int = rows.size

    fun columnCount(): Int = columnIds.size

    override fun equals(other: Any?): Boolean = myEquals(other, { name })

    override fun hashCode(): Int = myHashCode({ name })

    override fun toString(): String = name
}

class Row(val index: Int, val cells: List<Cell>) {

    override fun equals(other: Any?): Boolean = myEquals(other, { index })

    override fun hashCode(): Int = myHashCode({ index })

    override fun toString(): String = index.toString()
}

class Cell(val rowIndex: Int, val columnIndex: Int, val value: Any?) {

    override fun equals(other: Any?): Boolean = myEquals(other, { rowIndex }, { columnIndex })

    override fun hashCode(): Int = myHashCode({ rowIndex }, { columnIndex })

    override fun toString(): String = "($rowIndex,$columnIndex) => $value"
}