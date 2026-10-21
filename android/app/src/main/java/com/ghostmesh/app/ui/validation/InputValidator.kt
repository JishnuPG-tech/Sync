package com.ghostmesh.app.ui.validation

import java.util.regex.Pattern

sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val errorMessage: String) : ValidationResult()
}

object InputValidator {
    // Official Instagram handle regex: 1-30 chars, no consecutive dots, cannot start/end with dot
    private val INSTAGRAM_REGEX = Pattern.compile("^(?!.*\\.\\.)(?!^\\.)[a-zA-Z0-9._]{1,30}(?<!\\.)$")

    fun validateInstagramUsername(input: String): ValidationResult {
        val cleaned = input.trim().removePrefix("@")

        if (cleaned.isEmpty()) {
            return ValidationResult.Invalid("Instagram username cannot be empty")
        }

        if (cleaned.contains(" ")) {
            return ValidationResult.Invalid("Instagram username cannot contain spaces")
        }

        if (cleaned.length > 30) {
            return ValidationResult.Invalid("Instagram username cannot exceed 30 characters")
        }

        if (cleaned.startsWith(".")) {
            return ValidationResult.Invalid("Username cannot start with a period (.)")
        }

        if (cleaned.endsWith(".")) {
            return ValidationResult.Invalid("Username cannot end with a period (.)")
        }

        if (cleaned.contains("..")) {
            return ValidationResult.Invalid("Username cannot contain consecutive periods (..)")
        }

        if (!INSTAGRAM_REGEX.matcher(cleaned).matches()) {
            return ValidationResult.Invalid("Only letters, numbers, periods (.), and underscores (_) allowed")
        }

        return ValidationResult.Valid
    }

    fun validateDisplayName(input: String): ValidationResult {
        val cleaned = input.trim()
        if (cleaned.isEmpty()) {
            return ValidationResult.Invalid("Display name cannot be empty")
        }
        if (cleaned.length < 2) {
            return ValidationResult.Invalid("Display name must be at least 2 characters")
        }
        if (cleaned.length > 50) {
            return ValidationResult.Invalid("Display name cannot exceed 50 characters")
        }
        return ValidationResult.Valid
    }
}
