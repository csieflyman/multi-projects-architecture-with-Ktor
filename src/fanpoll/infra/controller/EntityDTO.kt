/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.controller

import fanpoll.infra.utils.Identifiable

interface EntityDTO<ID : Any> : Identifiable<ID>