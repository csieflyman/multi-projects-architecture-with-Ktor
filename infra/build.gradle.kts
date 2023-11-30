plugins {
    id("app.java-conventions")
    id("app.test-conventions")
    `java-library`
    id("org.jetbrains.kotlinx.kover")
}

dependencies {
    val kotlinVersion = "1.9.21"
    val ktorVersion = "2.3.6"
    val koinVersion = "3.5.0"
    val exposedVersion = "0.45.0"
    val jacksonVersion = "2.16.0"
    val opentelemetryVersion = "1.32.0"

    // =============== kotlin, kotlinx ===============
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:$kotlinVersion"))
    api("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.1")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // =============== ktor ===============
    api("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")

    api("io.ktor:ktor-server-locations-jvm:$ktorVersion")
    api("io.ktor:ktor-server-auth-jvm:$ktorVersion")
    api("io.ktor:ktor-server-auth-jwt-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-sessions:$ktorVersion")
    implementation("io.ktor:ktor-server-data-conversion:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-forwarded-header:$ktorVersion")
    implementation("io.ktor:ktor-server-compression:$ktorVersion")
    implementation("io.ktor:ktor-server-double-receive:$ktorVersion")
    implementation("io.ktor:ktor-server-call-id:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")

    api("io.ktor:ktor-client-cio-jvm:$ktorVersion")
    api("io.ktor:ktor-client-logging-jvm:$ktorVersion")
    api("io.ktor:ktor-client-serialization-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")

    api("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")

    // ===============  koin (DI) ===============
    api("io.insert-koin:koin-ktor:$koinVersion")
    //implementation("io.insert-koin:koin-logger-slf4j:$koinVersion") // use https://github.com/oshai/kotlin-logging

    // =============== logging ===============
    api("io.github.oshai:kotlin-logging-jvm:5.1.1")
    runtimeOnly("ch.qos.logback:logback-classic:1.4.13")

    // =============== database ===============
    api("org.jetbrains.exposed:exposed-core:$exposedVersion")
    api("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    api("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    api("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    api("com.github.jasync-sql:jasync-postgresql:2.2.4")

    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.flywaydb:flyway-database-postgresql:10.1.0")
    runtimeOnly("org.postgresql:postgresql:42.7.0")

    // =============== jackson ===============
    implementation("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-annotations:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-blackbird:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

    // =============== opentelemetry ===============
    api("io.opentelemetry:opentelemetry-api:$opentelemetryVersion")
    api("io.opentelemetry:opentelemetry-extension-kotlin:$opentelemetryVersion")
    api("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:$opentelemetryVersion")
    api("io.opentelemetry.semconv:opentelemetry-semconv:1.23.1-alpha")

    // =============== utils ===============
    api("io.konform:konform-jvm:0.4.0")
    implementation("com.github.rocketraman:kpropmap:0.0.2")
    implementation("com.ufoscout.properlty:properlty-kotlin:1.9.0")
    implementation("io.github.config4k:config4k:0.6.0")
    implementation("com.vdurmont:semver4j:3.1.0")
    implementation("com.github.kittinunf.result:result:5.5.0")
    implementation("com.github.kittinunf.result:result-coroutines:4.0.0")
    implementation("at.favre.lib:bcrypt:0.10.2")

    implementation("org.apache.poi:poi:5.2.5")
    implementation("org.apache.poi:poi-ooxml:5.2.5")
    implementation("org.apache.commons:commons-csv:1.10.0")
    implementation("org.apache.commons:commons-text:1.11.0")

    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.9.1")
    implementation("org.freemarker:freemarker:2.3.32")

    // ===============  third-party service ===============
    implementation("com.google.firebase:firebase-admin:9.2.0")
    implementation("com.google.guava:guava:32.1.3-jre") // [workaround] missing dependency on maven repository required by firebase-admin
    implementation("com.sendgrid:sendgrid-java:4.10.1")
    implementation("io.sentry:sentry:6.34.0")
}

koverReport {
    filters {
        excludes {
            classes("fanpoll.infra.redis.ktorio.*")
        }
    }
}