package com.example.arena_set_sharer.api

import com.example.arena_set_sharer.security.JwtRequestFilter
import com.example.arena_set_sharer.service.SetCommunityService
import com.example.arena_set_sharer.service.community.CommunityDocument
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Tag(name = "Community Rest Controller", description = "Per-set pairing and copy histogram.")
@RequestMapping("/api/sets")
class CommunityRestController(
    private val community: SetCommunityService
) {

    @Operation(summary = "Community rollup for one set.")
    @GetMapping("/{setId}/community")
    fun read(@PathVariable setId: String): ResponseEntity<CommunityDocument> {
        val user = JwtRequestFilter.authenticatedUser()
        val userId = user.id ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        if (!user.emailVerified) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        return ResponseEntity.ok(community.read(setId))
    }
}
