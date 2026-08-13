package dev.healthbridge

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.MealType
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.grams
import androidx.health.connect.client.units.kilocalories
import java.time.ZoneId

class HealthConnectWriter(context: Context) {
    private val client = HealthConnectClient.getOrCreate(context)

    val writePermission: String = HealthPermission.getWritePermission(NutritionRecord::class)

    suspend fun hasWritePermission(): Boolean =
        writePermission in client.permissionController.getGrantedPermissions()

    suspend fun insert(payload: NutritionPayload) {
        val zoneOffset = ZoneId.systemDefault().rules.getOffset(payload.at)

        val record = NutritionRecord(
            name = payload.name,
            mealType = mealType(payload.meal),
            energy = payload.kcal.kilocalories,
            protein = payload.protein?.grams,
            totalFat = payload.fat?.grams,
            totalCarbohydrate = payload.carbs?.grams,
            sugar = payload.sugar?.grams,
            startTime = payload.at,
            endTime = payload.at.plusSeconds(60),
            startZoneOffset = zoneOffset,
            endZoneOffset = zoneOffset,
            metadata = Metadata.manualEntry(clientRecordId = payload.id),
        )

        client.insertRecords(listOf(record))
    }

    private fun mealType(value: String): Int = when (value.lowercase()) {
        "breakfast", "завтрак" -> MealType.MEAL_TYPE_BREAKFAST
        "lunch", "обед" -> MealType.MEAL_TYPE_LUNCH
        "dinner", "ужин" -> MealType.MEAL_TYPE_DINNER
        "snack", "перекус" -> MealType.MEAL_TYPE_SNACK
        else -> MealType.MEAL_TYPE_UNKNOWN
    }
}
