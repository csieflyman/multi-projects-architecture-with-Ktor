/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.base.tenant

import fanpoll.infra.base.exception.RequestException
import fanpoll.infra.base.form.Form

abstract class TenantForm<Self> : Form<Self>() {

    abstract val tenantId: TenantId

    override fun validate() {
        try {
            super.validate()
        } catch (e: RequestException) {
            e.tenantId = tenantId
            throw e
        }
    }
}