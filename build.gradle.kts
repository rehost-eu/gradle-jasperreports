plugins {
    id("com.gradle.plugin-publish") version "2.0.0"
    id("org.ajoberstar.reckon") version "1.0.1"
    kotlin("jvm") version "2.2.20"
}

group = "eu.rehost.jasperreports"

repositories {
    mavenCentral()
}

gradlePlugin {
    website = "https://git.rehost.eu/rehost/gradle-plugins/gradle-jasperreports"
    vcsUrl = "https://git.rehost.eu/rehost/gradle-plugins/gradle-jasperreports.git"
    plugins {
        create("JasperReportsPlugin") {
            id = "eu.rehost.jasperreports"
            description = "Provides the capability to compile JasperReports design files."
            implementationClass = "eu.rehost.jasperreports.JasperReportsPlugin"
            displayName = "Gradle JasperReports Plugin"
            tags.set(listOf("jasperreports"))
        }
    }
}

publishing {
    repositories {
        maven {
            url = uri("https://git.rehost.eu/api/v4/projects/134/packages/maven")
            credentials {
                username = "maven"
                password = ""
            }
            authentication {
                create("basic", BasicAuthentication::class)
            }
        }
    }
}