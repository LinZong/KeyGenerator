package moe.nemesiss.keygenerator.service

import java.io.File

object Configuration {
    var KeyPath: File = File(".")


    fun ensureKeyPathCreated() {
        KeyPath.mkdirs()
    }
}