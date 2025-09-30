plugins {
    id("com.gradle.plugin-publish") version "2.0.0"
    kotlin("jvm") version "2.2.20"
}

group = "eu.rehost.jasperreports"

repositories {
    mavenCentral()
}
gradlePlugin {
    website.set("https://git.rehost.eu/rehost/gradle-plugins/gradle-jasperreports")
    vcsUrl.set("https://git.rehost.eu/rehost/gradle-plugins/gradle-jasperreports.git")
    plugins {
        create("JasperReportsPlugin") {
            id = "eu.rehost.jasperreports"
            description = "Provides the capability to compile JasperReports design files."
            implementationClass = "eu.rehost.jasperreports.JasperReportsPlugin"
            displayName = "Gradle JasperReports Plugin"
            tags.set(listOf("jasper", "jasperreports", "reporting"))
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