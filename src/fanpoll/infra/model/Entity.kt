/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.model

import fanpoll.infra.utils.Identifiable

interface Entity<ID : Any> : Identifiable<ID>