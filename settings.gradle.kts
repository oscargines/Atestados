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
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") } // Para la dependencia de JitPack
        maven { url = uri("https://repo.osgeo.org/repository/release/") } // Para versiones estables
        maven { url = uri("https://repo.osgeo.org/repository/snapshot/") } // para versiones SNAPSHOT
        flatDir {
            dirs("libs") // Añadido para buscar .aar y .jar en la carpeta libs
        }
    }
}

rootProject.name = "Atestados"
include(":app")
 