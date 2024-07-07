/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.base.entity

import fanpoll.infra.base.form.Form

abstract class EntityForm<Self : EntityForm<Self, E, EID>, E : Entity<EID>, EID : Comparable<EID>> : Form<Self>() {

    abstract fun getEntityId(): EID?

    abstract fun toEntity(): Entity<EID>
}