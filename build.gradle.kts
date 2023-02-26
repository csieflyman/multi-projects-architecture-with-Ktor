plugins {
    id("org.jetbrains.kotlinx.kover")
}

repositories {
    mavenCentral()
}

koverMerged {
    enable()

    filters {
        classes {
            includes.add("fanpoll.*")
            excludes.addAll(
                listOf(
                    "fanpoll.infra.redis.ktorio.*"
                )
            )
        }
        projects {
            excludes += listOf("app", "postman")
        }
    }

    xmlReport {
        onCheck.set(false)
    }
    htmlReport {
        onCheck.set(false)
    }
}