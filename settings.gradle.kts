pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "chiaro"

include(":app")
include(":core:domain")
include(":core:data")
// The shared periodic job (Fase 6): fetch, alerts, rules, sky observation — one
// worker, one schedule. Its notifiers are text and live in :app behind an interface.
include(":core:sync")
