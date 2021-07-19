/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.base.tenant

import fanpoll.infra.base.entity.Entity

interface TenantEntity<ID : Any> : Entity<ID> {

    val tenantId: TenantId
}