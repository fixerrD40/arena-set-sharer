package com.example.arena_set_sharer.api

import com.example.arena_set_sharer.api.model.EmailVerification
import com.example.arena_set_sharer.api.model.PasswordReset
import com.example.arena_set_sharer.api.model.RegistrationPending
import com.example.arena_set_sharer.api.model.Session
import com.example.arena_set_sharer.api.model.User
import com.example.arena_set_sharer.security.JwtUtil
import com.example.arena_set_sharer.service.EmailVerificationService
import com.example.arena_set_sharer.service.PasswordResetService
import com.example.arena_set_sharer.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import io.swagger.v3.oas.annotations.parameters.RequestBody as SwaggerRequestBody

@RestController
@Tag(name = "Authentication Rest Controller", description = "Manage user authentication.")
@RequestMapping("auth")
class AuthenticationRestController(
    private val passwordResetService: PasswordResetService,
    private val emailVerificationService: EmailVerificationService,
    private val userService: UserService,
    private val jwtUtil: JwtUtil
) {

    @Operation(
        summary = "Login user",
        method = "POST",
        description = "Authenticate an existing verified user.",
        responses = [
            ApiResponse(responseCode = "200", description = "Authentication successful.",
                content = [Content(schema = Schema(implementation = Session::class))]
            ),
            ApiResponse(responseCode = "400", description = "Authentication malformed."),
            ApiResponse(responseCode = "401", description = "Invalid email or password."),
            ApiResponse(responseCode = "403", description = "Email not verified."),
            ApiResponse(responseCode = "500", description = "Unexpected server error.")
        ],
        requestBody = SwaggerRequestBody(
            required = true,
            description = "Email and password.",
            content = [
                Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = User::class, requiredProperties = ["email", "password"]),
                    examples = [ExampleObject(MY_USER)]
                )
            ]
        )
    )
    @PostMapping("login")
    fun loginUser(@RequestBody credentials: User): ResponseEntity<Session> {
        val email = credentials.email?.takeIf { it.isNotBlank() }
            ?: return ResponseEntity.badRequest().build()
        val user = userService.authenticate(email, credentials.password)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        if (!user.emailVerified) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        return ResponseEntity.ok(Session(jwtUtil.generateToken(user), user.username))
    }

    @Operation(
        summary = "Register user",
        method = "POST",
        description = "Create an unverified account and email a confirmation link. No session until verify.",
        responses = [
            ApiResponse(responseCode = "202", description = "Registration accepted; check email.",
                content = [Content(schema = Schema(implementation = RegistrationPending::class))]
            ),
            ApiResponse(responseCode = "400", description = "Registration malformed."),
            ApiResponse(responseCode = "409", description = "You already have an account."),
            ApiResponse(responseCode = "500", description = "Unexpected server error.")
        ],
        requestBody = SwaggerRequestBody(
            required = true,
            description = "Email and password. Username is optional.",
            content = [
                Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = User::class, requiredProperties = ["email", "password"]),
                    examples = [ExampleObject(MY_USER)]
                )
            ]
        )
    )
    @PostMapping("register")
    fun registerUser(@RequestBody credentials: User): ResponseEntity<RegistrationPending> {
        return try {
            val email = credentials.email?.takeIf { it.isNotBlank() }
                ?: return ResponseEntity.badRequest().build()
            val user = userService.registerUser(credentials)
            emailVerificationService.sendForNewUser(user, email)
            ResponseEntity.status(HttpStatus.ACCEPTED).body(RegistrationPending(user.username))
        } catch (e: DataIntegrityViolationException) {
            ResponseEntity.status(HttpStatus.CONFLICT).build()
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @Operation(
        summary = "Confirm email",
        method = "POST",
        description = "Mark the account verified and return a session JWT.",
        responses = [
            ApiResponse(responseCode = "200", description = "Email verified.",
                content = [Content(schema = Schema(implementation = Session::class))]
            ),
            ApiResponse(responseCode = "400", description = "Invalid or expired token."),
            ApiResponse(responseCode = "500", description = "Unexpected server error.")
        ]
    )
    @PostMapping("verify-email")
    fun verifyEmail(@RequestBody request: EmailVerification): ResponseEntity<Session> =
        try {
            val user = emailVerificationService.confirm(request.token)
            ResponseEntity.ok(Session(jwtUtil.generateToken(user), user.username))
        } catch (e: IllegalStateException) {
            ResponseEntity.badRequest().build()
        }

    @Operation(
        summary = "Resend verification email",
        method = "POST",
        description = "Resend confirm link for an unverified account. Always 202.",
        responses = [
            ApiResponse(responseCode = "202", description = "Request accepted."),
            ApiResponse(responseCode = "500", description = "Unexpected server error.")
        ],
        requestBody = SwaggerRequestBody(
            required = true,
            description = "The email used to register.",
            content = [
                Content(
                    mediaType = MediaType.TEXT_PLAIN_VALUE,
                    schema = Schema(implementation = String::class, example = MY_EMAIL)
                )
            ]
        )
    )
    @PostMapping("resend-verification")
    fun resendVerification(@RequestBody email: String): ResponseEntity<Any> {
        emailVerificationService.resend(email)
        return ResponseEntity.accepted().build()
    }

    @Operation(
        summary = "Request reset password",
        method = "POST",
        description = "Request reset password for a verified user to their email.",
        responses = [
            ApiResponse(responseCode = "202", description = "Request accepted."),
            ApiResponse(responseCode = "400", description = "Request malformed."),
            ApiResponse(responseCode = "500", description = "Unexpected server error.")
        ],
        requestBody = SwaggerRequestBody(
            required = true,
            description = "The email used to create the user.",
            content = [
                Content(
                    mediaType = MediaType.TEXT_PLAIN_VALUE,
                    schema = Schema(implementation = String::class, example = MY_EMAIL)
                )
            ]
        )
    )
    @PostMapping("request-reset")
    fun requestPasswordReset(@RequestBody email: String): ResponseEntity<Any> {
        passwordResetService.requestPasswordReset(email)
        return ResponseEntity.accepted().build()
    }

    @Operation(
        summary = "Reset password",
        method = "POST",
        description = "Reset password using a valid token.",
        responses = [
            ApiResponse(responseCode = "200", description = "Password was reset."),
            ApiResponse(responseCode = "400", description = "Invalid or expired token."),
            ApiResponse(responseCode = "500", description = "Unexpected server error.")
        ],
        requestBody = SwaggerRequestBody(
            required = true,
            description = "Reset token and new password.",
            content = [
                Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = PasswordReset::class, requiredProperties = ["token", "newPassword"]),
                    examples = [ExampleObject(MY_PASSWORD_RESET)]
                )
            ]
        )
    )
    @PostMapping("reset-password")
    fun resetPassword(@RequestBody request: PasswordReset): ResponseEntity<Any> =
        try {
            passwordResetService.resetPassword(request.token, request.newPassword)
            ResponseEntity.ok().build()
        } catch (e: IllegalStateException) {
            ResponseEntity.badRequest().build()
        }

    companion object {
        private const val MY_EMAIL = "myEmail@myProvider.ext"
        private const val MY_USER  = """{"email":"$MY_EMAIL","username":"myUser","password":"myPassword"}"""
        private const val MY_PASSWORD_RESET = """{"token":"myToken","newPassword":"myNewPassword"}"""
    }
}
