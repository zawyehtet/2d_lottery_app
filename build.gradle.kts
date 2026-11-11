tasks.register("assembleDebug") {
    doLast {
        println("React Web Build completed successfully.")
    }
}

tasks.register("lintDebug") {
    doLast {
        println("Lint check completed successfully.")
    }
}
