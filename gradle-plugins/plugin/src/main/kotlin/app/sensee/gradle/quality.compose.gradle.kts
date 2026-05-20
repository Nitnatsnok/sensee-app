package app.sensee.gradle

import app.sensee.gradle.dsl.requiredLibrary

plugins {
    id("app.sensee.gradle.quality")
}

dependencies {
    detektPlugins(requiredLibrary("detekt-compose"))
}
