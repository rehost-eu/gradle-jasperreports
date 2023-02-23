# Gradle JasperReports Plugin
## Info
Forked from https://github.com/gmazelier/gradle-jasperreports as it seems to be abandoned.
Rewritten in Kotlin as of 02.2023
## Description

Provides the capability to compile JasperReports design files.

## Usage

Just apply the plugin.
It automatically adds a compile task that compiles .jrxml inside "src/main/jasper" to "build/reports"

You can define a custom task with custom paths.

### Installation

Using the pluging DSL...

    plugins {
      id "eu.rehost.jasperreports" version "0.14"
    }

### Configuration

config documentation currently WIP

## Getting Help

To ask questions or report bugs, please use the [GitLab project](https://git.rehost.eu/rehost/gradle-jasperreports/issues).


## Change Log
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
This plugin is licensed under [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)
without warranties or conditions of any kind, either express or implied.
