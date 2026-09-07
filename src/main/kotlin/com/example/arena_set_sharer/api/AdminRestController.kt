package com.example.arena_set_sharer.api

import com.example.arena_set_sharer.api.model.AdminGrant
import com.example.arena_set_sharer.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Tag(name = "Admin Rest Controller", description = "Grant ROLE_ADMIN via mail-password handshake.")
@RequestMapping("/api/admins")
class AdminRestController(
    private val users: UserService
) {

    @Operation(summary = "Grant or revoke users.admin. Body must include APP_MAIL_PASSWORD as rootPassword.")
    @PutMapping
    fun setAdmin(@RequestBody grant: AdminGrant): ResponseEntity<Void> {
        if (!users.matchesMailRootPassword(grant.rootPassword)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
        return try {
            users.setAdmin(grant.email, grant.admin)
                ?: return ResponseEntity.notFound().build()
            ResponseEntity.noContent().build()
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }
}
