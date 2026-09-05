package com.example.arena_set_sharer.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

@Component
class CoverService(
    @Value("\${app.covers-dir}") coversDir: String
) {
    private val root: Path = Path.of(coversDir).toAbsolutePath().normalize()

    fun fileFor(setCode: String): Path {
        val code = normalize(setCode)
        val file = root.resolve("$code.jpg").normalize()
        require(file.startsWith(root)) { "Invalid set code" }
        return file
    }

    fun put(setCode: String, bytes: ByteArray) {
        require(bytes.size <= MAX_BYTES) { "Cover exceeds $MAX_BYTES bytes" }
        require(isJpeg(bytes)) { "Cover must be a JPEG" }
        Files.createDirectories(root)
        Files.write(fileFor(setCode), bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
    }

    private fun normalize(raw: String): String {
        val code = raw.trim().lowercase()
        require(code.matches(SET_CODE)) { "Invalid set code" }
        return code
    }

    private fun isJpeg(bytes: ByteArray): Boolean =
        bytes.size >= 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()

    companion object {
        private val SET_CODE = Regex("^[a-z0-9]{2,16}$")
        private const val MAX_BYTES = 8 * 1024 * 1024
    }
}
