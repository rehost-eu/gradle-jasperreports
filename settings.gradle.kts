plugins {
    id("org.ajoberstar.reckon.settings") version "1.0.1"
}

extensions.configure<org.ajoberstar.reckon.gradle.ReckonExtension> {
    setDefaultInferredScope("patch")
    snapshots()
    setScopeCalc(calcScopeFromProp().or(calcScopeFromCommitMessages()))
    setStageCalc(calcStageFromProp())
    setTagWriter { version -> "v$version" }
}


rootProject.name = "jasperreports-plugin"
