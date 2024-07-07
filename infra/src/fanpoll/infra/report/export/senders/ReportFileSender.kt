/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.report.export.senders

import fanpoll.infra.report.export.ReportFile

interface ReportFileSender {
    suspend fun send(reportFile: ReportFile)
}

