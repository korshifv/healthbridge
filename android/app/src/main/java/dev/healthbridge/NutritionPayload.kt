package dev.healthbridge

import android.net.Uri
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

data class NutritionPayload(
    val id: String,
    val name: String,
    val meal: String,
    val kcal: Double,
    val protein: Double?,
    val fat: Double?,
    val carbs: Double?,
    val sugar: Double?,
    val at: Instant,
    val autocommit: Boolean,
) {
    companion object {
        private val linkRegex = Regex(
            pattern = "healthbridge://nutrition\\?[^\\s<>\\\"']+",
            option = RegexOption.IGNORE_CASE,
        )

        fun fromText(text: String): NutritionPayload? {
            val trimmed = text.trim()
            val candidate = if (trimmed.startsWith("healthbridge://nutrition", ignoreCase = true)) {
                trimmed
            } else {
                linkRegex.find(trimmed)?.value
            } ?: return null

            val cleaned = candidate.trimEnd('.', ',', ';', ':', ')', ']', '}', '`')
            return runCatching { Uri.parse(cleaned) }
                .getOrNull()
                ?.let(::fromUri)
        }

        fun fromUri(uri: Uri): NutritionPayload? {
            if (uri.scheme != "healthbridge" || uri.host != "nutrition") return null

            val name = uri.getQueryParameter("name")?.trim().orEmpty()
            val kcal = uri.getQueryParameter("kcal")?.toDoubleOrNull() ?: return null
            if (name.isBlank() || kcal < 0.0) return null

            return NutritionPayload(
                id = uri.getQueryParameter("id")?.takeIf { it.isNotBlank() }
                    ?: UUID.nameUUIDFromBytes(uri.toString().toByteArray()).toString(),
                name = name,
                meal = uri.getQueryParameter("meal")?.lowercase() ?: "unknown",
                kcal = kcal,
                protein = uri.getQueryParameter("protein")?.toDoubleOrNull(),
                fat = uri.getQueryParameter("fat")?.toDoubleOrNull(),
                carbs = uri.getQueryParameter("carbs")?.toDoubleOrNull(),
                sugar = uri.getQueryParameter("sugar")?.toDoubleOrNull(),
                at = parseInstant(uri.getQueryParameter("at")),
                autocommit = uri.getQueryParameter("autocommit") in setOf("1", "true", "yes"),
            )
        }

        private fun parseInstant(raw: String?): Instant {
            if (raw.isNullOrBlank()) return Instant.now()
            return runCatching { OffsetDateTime.parse(raw).toInstant() }
                .recoverCatching { Instant.parse(raw) }
                .getOrDefault(Instant.now())
        }
    }
}
