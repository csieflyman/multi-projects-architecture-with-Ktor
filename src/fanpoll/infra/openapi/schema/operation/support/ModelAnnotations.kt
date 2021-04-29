/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.openapi.schema.operation.support

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class OpenApiModel(val propertyNameOrder: Array<String> = [])

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class OpenApiIgnore