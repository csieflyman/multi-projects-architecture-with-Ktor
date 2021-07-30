plugins {
    id("fanpoll.java-conventions")
    id("com.github.johnrengelman.shadow") version "7.0.0"
    kotlin("plugin.serialization") version "1.5.21"
    `java-library`
}

dependencies {
    val kotlinVersion = "1.5.21"
    val ktorVersion = "1.6.1"
    val koinVersion = "3.1.2"
    val exposedVersion = "0.32.1"
    val jacksonVersion = "2.12.4"

    // =============== kotlin, kotlinx ===============
    implementation(kotlin("stdlib", kotlinVersion))
    api("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.2")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.1")

    // ===============  ktor ===============
    api("io.ktor:ktor-server-netty:$ktorVersion")
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

    // =============== utils - feature ===============
    api("org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.3")
    api("io.konform:konform-jvm:0.3.0")
    api("at.favre.lib:bcrypt:0.9.0")

    // =============== utils - config file parser ===============
    api("com.ufoscout.properlty:properlty-kotlin:1.9.0")
    api("io.github.config4k:config4k:0.4.2")
    //api("com.charleskorn.kaml:kaml:0.19.0")

    // =============== utils - bean ===============
    api("com.github.rocketraman:kpropmap:0.0.2")

    //api("org.mapstruct:mapstruct:1.4.2.Final")
    //kapt("org.mapstruct:mapstruct-processor:1.4.2.Final")

    // Kotlin DataClass DeepCopy
    //api("com.bennyhuo.kotlin:deepcopy-reflect:1.1.0")
    //kapt ("com.bennyhuo.kotlin:deepcopy-compiler:1.3.72")
    //api("com.bennyhuo.kotlin:deepcopy-runtime:1.3.72")

    // =============== utils - logging ===============
    api("io.github.microutils:kotlin-logging-jvm:2.0.10")
    runtimeOnly("ch.qos.logback:logback-classic:1.2.3")

    // =============== utils - general ===============
    api("com.github.kittinunf.result:result:5.1.0")
    api("com.github.kittinunf.result:result-coroutines:4.0.0")
    api("org.apache.commons:commons-text:1.9")
    api("com.google.guava:guava:30.1.1-jre")

    // =============== test ===============
    //testImplementation("io.ktor:ktor-server-tests:$ktor_version")

    implementation("com.zaxxer:HikariCP:4.0.3")
    implementation("org.flywaydb:flyway-core:7.11.2")

    runtimeOnly("org.postgresql:postgresql:42.2.23")

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
    implementation("org.apache.commons:commons-csv:1.8")

    // jackson only used for openapi
    // maybe need in the future https://github.com/FasterXML/jackson-modules-java8
    implementation("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-annotations:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-blackbird:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
}