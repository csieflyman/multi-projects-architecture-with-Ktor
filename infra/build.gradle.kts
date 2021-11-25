plugins {
    id("app.java-conventions")
    `java-library`
}

dependencies {
    val kotlinVersion = "1.5.31"
    val ktorVersion = "1.6.4"
    val koinVersion = "3.1.2"
    val exposedVersion = "0.35.2"
    val jacksonVersion = "2.13.0"

    // =============== kotlin, kotlinx ===============
    implementation(kotlin("stdlib", kotlinVersion))
    api("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.0")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")

    // ===============  ktor ===============
    api("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    api("io.ktor:ktor-locations:$ktorVersion")
    api("io.ktor:ktor-serialization:$ktorVersion")
    api("io.ktor:ktor-client-cio:$ktorVersion")
    api("io.ktor:ktor-client-logging-jvm:$ktorVersion")
    api("io.ktor:ktor-client-serialization-jvm:$ktorVersion")
    api("io.ktor:ktor-auth:$ktorVersion")
    api("io.ktor:ktor-auth-jwt:$ktorVersion")

    // ===============  koin (DI) ===============
    api("io.insert-koin:koin-core:$koinVersion")
    api("io.insert-koin:koin-core-ext:3.0.2")
    api("io.insert-koin:koin-ktor:$koinVersion")
    //api("io.insert-koin:koin-test-junit5:$koinVersion")

    // =============== database ===============
    api("org.jetbrains.exposed:exposed-core:$exposedVersion")
    api("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    api("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    api("org.jetbrains.exposed:exposed-java-time:$exposedVersion")

    api("com.github.jasync-sql:jasync-postgresql:2.0.2")

    // =============== utils - feature ===============
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.3")
    api("io.konform:konform-jvm:0.3.0")
    implementation("at.favre.lib:bcrypt:0.9.0")

    // =============== utils - config file parser ===============
    implementation("com.ufoscout.properlty:properlty-kotlin:1.9.0")
    implementation("io.github.config4k:config4k:0.4.2")
    //api("com.charleskorn.kaml:kaml:0.19.0")

    // =============== utils - bean ===============
    implementation("com.github.rocketraman:kpropmap:0.0.2")

    //api("org.mapstruct:mapstruct:1.4.2.Final")
    //kapt("org.mapstruct:mapstruct-processor:1.4.2.Final")

    // Kotlin DataClass DeepCopy
    //api("com.bennyhuo.kotlin:deepcopy-reflect:1.1.0")
    //kapt ("com.bennyhuo.kotlin:deepcopy-compiler:1.3.72")
    //api("com.bennyhuo.kotlin:deepcopy-runtime:1.3.72")

    // =============== utils - logging ===============
    api("io.github.microutils:kotlin-logging-jvm:2.0.11")
    runtimeOnly("ch.qos.logback:logback-classic:1.2.3")

    // =============== utils - general ===============
    implementation("com.github.kittinunf.result:result:5.2.0")
    implementation("com.github.kittinunf.result:result-coroutines:4.0.0")
    implementation("org.apache.commons:commons-text:1.9")

    // =============== test ===============
    //testImplementation("io.ktor:ktor-server-tests:$ktor_version")

    implementation("com.zaxxer:HikariCP:5.0.0")
    implementation("org.flywaydb:flyway-core:8.0.1")

    runtimeOnly("org.postgresql:postgresql:42.2.24")

    // ===============  AWS Java SDK Version 2 ===============
    implementation(platform("software.amazon.awssdk:bom:2.16.34"))

    implementation("software.amazon.awssdk:netty-nio-client")

    implementation("software.amazon.awssdk:firehose")

    implementation("software.amazon.awssdk:ses")
    implementation("javax.mail:javax.mail-api:1.6.2")
    implementation("com.sun.mail:javax.mail:1.6.2")

    // ===============  third-party service ===============
    implementation("com.google.firebase:firebase-admin:7.1.1")

    // =============== utils - feature ===============
    implementation("org.freemarker:freemarker:2.3.31")

    implementation("org.apache.poi:poi:5.0.0")
    implementation("org.apache.poi:poi-ooxml:5.0.0")
    implementation("org.apache.commons:commons-csv:1.9.0")

    // jackson only used for openapi
    // maybe need in the future https://github.com/FasterXML/jackson-modules-java8
    implementation("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-annotations:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-blackbird:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
}