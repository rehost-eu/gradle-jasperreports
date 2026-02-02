plugins {
    `kotlin-dsl`
    id("com.gradle.plugin-publish") version "1.2.1"
}

group = "eu.rehost.jasperreports"
version = "6.21.5"

kotlin {
    jvmToolchain(17)
}

gradlePlugin {
    website.set("https://git.rehost.eu/rehost/gradle-plugins/gradle-jasperreports")
    vcsUrl.set("https://git.rehost.eu/rehost/gradle-plugins/gradle-jasperreports.git")
    plugins {
        create("JasperReportsPlugin") {
            id = "eu.rehost.jasperreports"
            displayName = "Gradle JasperReports Plugin v6.21.5"
            description = "Compiles .jrxml files using JasperReports 6.21.5."
            implementationClass = "eu.rehost.jasperreports.JasperReportsPlugin"
            tags.set(listOf("jasperreports", "reporting", "compilation"))
        }
    }
}

publishing {
    repositories {
        maven {
            name = "GitLab"
            url = uri("https://git.rehost.eu/api/v4/projects/134/packages/maven")
            credentials(HttpHeaderCredentials::class) {
                name = if (System.getenv("CI_JOB_TOKEN") != null) "Job-Token" else "Private-Token"
                value = System.getenv("CI_JOB_TOKEN") ?: providers.gradleProperty("gitlabApiToken").orNull
            }
            authentication {
                create("header", HttpHeaderAuthentication::class)
            }
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.sf.jasperreports:jasperreports:6.21.5")
}