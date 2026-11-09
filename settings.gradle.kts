plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "film-app-backend"

include(":app", ":api", ":domain", ":data")