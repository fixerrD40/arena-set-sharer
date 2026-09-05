package com.example.arena_set_sharer.api

import com.example.arena_set_sharer.service.CoverService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files

@RestController
@Tag(name = "Cover Rest Controller", description = "Set key-art covers.")
@RequestMapping("/api/assets/covers")
class CoverRestController(
    private val covers: CoverService
) {

    @Operation(summary = "Download a set cover JPEG. Public so clients can fetch without a Bearer.")
    @GetMapping("/{setCode}.jpg")
    fun getCover(@PathVariable setCode: String): ResponseEntity<Resource> {
        val file = try {
            covers.fileFor(setCode)
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.badRequest().build()
        }
        if (!Files.isRegularFile(file)) {
            return ResponseEntity.notFound().build()
        }
        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_JPEG)
            .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
            .body(FileSystemResource(file))
    }

    @Operation(summary = "Replace a set cover. Admin only.")
    @PutMapping("/{setCode}.jpg", consumes = [MediaType.IMAGE_JPEG_VALUE, MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun putCover(@PathVariable setCode: String, @RequestBody bytes: ByteArray): ResponseEntity<Void> =
        writeCover(setCode, bytes)

    @Operation(summary = "Replace a set cover from multipart. Admin only.")
    @PutMapping("/{setCode}.jpg", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun putCoverMultipart(
        @PathVariable setCode: String,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<Void> = writeCover(setCode, file.bytes)

    private fun writeCover(setCode: String, bytes: ByteArray): ResponseEntity<Void> =
        try {
            covers.put(setCode, bytes)
            ResponseEntity.noContent().build()
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
}
