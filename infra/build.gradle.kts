plugins {
    id("app.java-conventions")
    id("app.test-conventions")
    `java-library`
    id("org.jetbrains.kotlinx.kover")
}

dependencies {
    // =============== kotlin ===============
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:2.0.0"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    api("org.jetbrains.kotlin:kotlin-reflect")

    // =============== kotlinx ===============
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0-RC")

    // =============== ktor server ===============
    api(platform("io.ktor:ktor-bom:2.3.12"))
    api("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-netty")

    api("io.ktor:ktor-server-locations")
    api("io.ktor:ktor-server-auth")
    api("io.ktor:ktor-server-auth-jwt")
    implementation("io.ktor:ktor-server-sessions")
    implementation("io.ktor:ktor-server-data-conversion")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-server-forwarded-header")
    implementation("io.ktor:ktor-server-compression")
    implementation("io.ktor:ktor-server-double-receive")
    implementation("io.ktor:ktor-server-call-id")
    implementation("io.ktor:ktor-server-status-pages")

    // =============== ktor client ===============
    api("io.ktor:ktor-client-cio")
    api("io.ktor:ktor-client-logging")
    api("io.ktor:ktor-client-serialization")
    implementation("io.ktor:ktor-client-content-negotiation")

    // =============== ktor share ===============
    api("io.ktor:ktor-serialization-kotlinx-json")

    // ===============  koin ===============
    api("io.insert-koin:koin-ktor:3.5.6")
    //implementation("io.insert-koin:koin-logger-slf4j:$koinVersion") // use https://github.com/oshai/kotlin-logging

    // =============== logging ===============
    api("io.github.oshai:kotlin-logging:7.0.0")
    runtimeOnly("ch.qos.logback:logback-classic:1.5.6")

    // =============== database ===============
    api(platform("org.jetbrains.exposed:exposed-bom:0.52.0"))
    api("org.jetbrains.exposed:exposed-core")
    api("org.jetbrains.exposed:exposed-dao")
    api("org.jetbrains.exposed:exposed-jdbc")
    api("org.jetbrains.exposed:exposed-java-time")
    api("org.jetbrains.exposed:exposed-json")

    api("com.github.jasync-sql:jasync-postgresql:2.2.4")

    implementation("com.zaxxer:HikariCP:5.1.0")

    implementation("org.flywaydb:flyway-database-postgresql:10.16.0")

    runtimeOnly("org.postgresql:postgresql:42.7.3")

    // =============== jackson ===============
    implementation(platform("com.fasterxml.jackson:jackson-bom:2.17.2"))
    implementation("com.fasterxml.jackson.core:jackson-core")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.core:jackson-annotations")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.fasterxml.jackson.module:jackson-module-blackbird")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // =============== opentelemetry ===============
    // download javaagent.jar and put it into "app/dist/lib" folder
    // javaagent configuration file => "app/dist/config/common/otel-javaagent.properties"
    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar
    api(platform("io.opentelemetry:opentelemetry-bom:1.40.0"))
    api("io.opentelemetry:opentelemetry-api")
    api("io.opentelemetry:opentelemetry-extension-kotlin")
    api("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:2.6.0")
    api("io.opentelemetry.semconv:opentelemetry-semconv:1.26.0-alpha")

    // don't use opentelemetry-ktor-2.0 now due to blocking issue https://stackoverflow.com/questions/77499671
    //implementation("io.opentelemetry.instrumentation:opentelemetry-ktor-2.0:$2.1.0-alpha")

    // =============== utils ===============
    api("io.konform:konform:0.6.1")
    implementation("com.github.rocketraman:kpropmap:0.0.2")
    implementation("com.ufoscout.properlty:properlty-kotlin:1.9.0")
    api("io.github.config4k:config4k:0.7.0")
    implementation("org.semver4j:semver4j:5.3.0")
    implementation("com.github.kittinunf.result:result:5.6.0")
    implementation("com.github.kittinunf.result:result-coroutines:4.0.0")
    implementation("at.favre.lib:bcrypt:0.10.2")

    implementation("org.apache.poi:poi:5.3.0")
    implementation("org.apache.poi:poi-ooxml:5.3.0")
    implementation("org.apache.commons:commons-csv:1.11.0")
    implementation("org.apache.commons:commons-text:1.12.0")
    implementation("org.apache.commons:commons-compress:1.26.2")

    implementation("org.jetbrains.kotlinx:kotlinx-html:0.11.0")
    implementation("org.freemarker:freemarker:2.3.33")

    // ===============  third-party service ===============
    implementation("com.google.firebase:firebase-admin:9.3.0")
    implementation("com.sendgrid:sendgrid-java:4.10.2")
    implementation("com.twilio.sdk:twilio:10.4.1")
    implementation("io.sentry:sentry:7.11.0")
}

kover {
    reports {
        filters {
            excludes {
                classes("fanpoll.infra.redis.ktorio.*")
            }
        }
    }
}