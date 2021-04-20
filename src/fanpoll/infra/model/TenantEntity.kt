/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.model

interface TenantEntity<ID : Any> : Entity<ID> {

    val tenantId: TenantId
}