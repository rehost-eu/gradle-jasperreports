# Gradle JasperReports Plugin

## Info
Forked from [https://github.com/gmazelier/gradle-jasperreports](https://github.com/gmazelier/gradle-jasperreports) and completely rewritten in Kotlin.

**Current Status:** This plugin now follows a **Strict Versioning** policy.
The plugin version matches exactly the JasperReports library version it uses.

## Description

This plugin provides the capability to compile JasperReports design files (`.jrxml`) into `.jasper` files using a **pinned, specific version** of the JasperReports library.

It is a lightweight, direct wrapper around a specific JasperReports engine.

**Current Version:** `6.21.5`
* Uses JasperReports `6.21.5`
* Requires Java 17+

## Usage

### 1. Apply the Plugin

Using the plugins DSL:

```kotlin
    plugins {
        id("eu.rehost.jasperreports") version "6.21.5"
    }
```

### 2. Default Behavior
By default, the plugin creates a task named `compileJasper` which:
* Scans for `.jrxml` files in `src/main/jasper`
* Compiles them to `build/reports`

### 3. Configuration
You can customize the source and destination directories in your `build.gradle.kts` using the `jasper` extension block:

```kotlin
    jasper {
        // Change source directory (default: src/main/jasper)
        source.set(layout.projectDirectory.dir("src/main/resources/reports"))
        
        // Change output directory (default: build/reports)
        output.set(layout.buildDirectory.dir("generated/jasper"))
    }
```

## Getting Help
To ask questions or report bugs, please use the [GitLab project](https://git.rehost.eu/rehost/gradle-jasperreports/issues).

## Change Log

### 6.21.5 (2025-02-02)
* **Strict Versioning:** Plugin version now matches the JasperReports library version.
* **Pinned Dependency:** Hardcoded dependency on JasperReports 6.21.5.
* **Performance:** Removed worker isolation in favor of direct compilation for simplicity and speed.
* **Requirement:** Bumped to Java 17.

### 0.14 (2023-02-23)
* Kotlin rewrite
* Ability to change the used JasperReports Version

### 0.10 (2023-02-03)
* Forked from gmazelier
* Dependencies upgrade (gradle, maven, jasper)

### 0.4 (2019-10-20)
* Dependencies upgrade (Gradle and Jasper).
* Move to Gradle publishing plugin.

### 0.3.2 (2015-12-07)
* Adds Microsoft OS support.

### 0.3.1 (2015-11-24)
* Fix an issue if there are multiple files in subdirectories when using `useRelativeOutDir`.

### 0.3.0 (2015-11-17)
* Adds Java 8 support.
* Configures Travis CI.
* Improves tests.

### 0.2.1 (2015-04-03)
* Adds `useRelativeOutDir` option.
* Enable Gradle wrapper for developers.

### 0.2.0 (2015-02-26)
* Adds `classpath` option.

### 0.1.0 (2014-08-24)
* Initial release.

## License
This plugin is licensed under [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).