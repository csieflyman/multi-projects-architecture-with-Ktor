/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.report.export.writers

import fanpoll.infra.report.Report

interface ReportFileWriter {
    suspend fun write(report: Report)
}