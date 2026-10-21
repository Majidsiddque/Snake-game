// Root build.gradle.kts
// Keep it minimal — no Android plugin here!

plugins {
    // no android plugin here
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
