/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.report.export.writers

import fanpoll.infra.base.datetime.DateTimeUtils
import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.report.Report
import fanpoll.infra.report.data.table.DataTable
import fanpoll.infra.report.export.ReportFile
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*

class ExcelReportFileWriter : ReportFileWriter {
    override suspend fun write(report: Report) {
        report.file = ReportFile(report.title, ReportFile.MimeType.EXCEL)
        report.file.content = workbookToByteArray(toWorkbook(report))
    }

    private fun createWorkbook(): XSSFWorkbook {
        val workbook = XSSFWorkbook()
        val xmlProps = workbook.properties
        val coreProps = xmlProps.coreProperties
        //coreProps.creator = "xxx"
        return workbook
    }

    private fun createColumnNameStyle(workbook: Workbook): CellStyle {
        val style = workbook.createCellStyle()
        style.wrapText = false
        style.alignment = HorizontalAlignment.CENTER
        val columnNameFont = workbook.createFont()
        columnNameFont.bold = true
        style.setFont(columnNameFont)
        return style
    }

    private fun createCellStyle(workbook: Workbook): CellStyle {
        val style = workbook.createCellStyle()
        style.wrapText = true
        return style
    }

    private fun setCellValue(cell: org.apache.poi.ss.usermodel.Cell, value: Any?) {
        when (value) {
            null -> cell.setBlank()
            is String -> cell.setCellValue(value)
            is Boolean -> cell.setCellValue((value))
            is BigDecimal -> cell.setCellValue(value.setScale(0, RoundingMode.UP).toDouble())
            is Number -> cell.setCellValue((value.toDouble()))
            is Date -> cell.setCellValue(DateTimeUtils.TAIWAN_DATE_TIME_FORMATTER.format(value.toInstant()))
            is Instant -> cell.setCellValue(DateTimeUtils.TAIWAN_DATE_TIME_FORMATTER.format(value))
            is ZonedDateTime -> cell.setCellValue(DateTimeUtils.TAIWAN_DATE_TIME_FORMATTER.format(value))
            is LocalDate -> cell.setCellValue(value)
            is LocalDateTime -> cell.setCellValue(value)
            else -> cell.setCellValue(value.toString())
        }
    }

    private fun toWorkbook(report: Report): Workbook {
        val workbook = createWorkbook()
        val columnNameStyle = createColumnNameStyle(workbook)
        val cellStyle = createCellStyle(workbook)
        for (datasetItem in report.dataset) {
            val table = datasetItem as DataTable
            val sheet: Sheet = workbook.createSheet(table.name)
            var currentRowNum = 0

            val columnNameRow = sheet.createRow(currentRowNum)
            table.columns.forEachIndexed { index, dataColumn ->
                val cell = columnNameRow.createCell(index)
                cell.cellStyle = columnNameStyle
                setCellValue(cell, dataColumn.name)
            }
            currentRowNum++

            val dataRows = table.data
            for (i in dataRows.indices) {
                val row = sheet.createRow(currentRowNum + i)
                for (j in dataRows[i].indices) {
                    val cell = row.createCell(j)
                    cell.cellStyle = cellStyle
                    val dataCell = dataRows[i][j]
                    setCellValue(cell, dataCell.value)
                }
            }

            for (i in 0 until sheet.getRow(0).lastCellNum) {
                sheet.autoSizeColumn(i)
            }
        }
        return workbook
    }

    private fun workbookToByteArray(workbook: Workbook): ByteArray {
        return try {
            val outputStream = ByteArrayOutputStream()
            workbook.write(outputStream)
            outputStream.flush()
            val bytes = outputStream.toByteArray()
            outputStream.close()
            bytes
        } catch (e: IOException) {
            throw InternalServerException(InfraResponseCode.IO_ERROR, "fail to write excel report", e)
        }
    }
}