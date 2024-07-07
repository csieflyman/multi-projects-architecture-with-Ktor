/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.report.data.loaders

import fanpoll.infra.report.Report

interface ReportDataLoader {
    suspend fun load(report: Report, queryParams: Map<String, String>? = null)
}