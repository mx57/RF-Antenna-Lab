package com.example.data

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(exception: Exception, operationType: OperationType, path: String?): String {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", exception.message ?: exception.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

@IgnoreExtraProperties
data class AntennaProfileDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val antennaType: String = "DIPOLE",
    val frequencyMhz: Double = 14.150,
    val wireDiameterMm: Double = 2.0,
    val mastHeightM: Double = 12.0,
    val elementCount: Long = 1L,
    val velocityFactor: Double = 0.95,
    val calculatedSwr: Double = 1.12,
    val gainDbi: Double = 2.15,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(ownerUid: String): Map<String, Any> {
        return mapOf(
            "userId" to ownerUid,
            "name" to name.take(120).ifBlank { "Antenna Profile" },
            "antennaType" to antennaType.take(40).ifBlank { "DIPOLE" },
            "frequencyMhz" to frequencyMhz.coerceIn(0.000001, 6000.0),
            "wireDiameterMm" to wireDiameterMm.coerceIn(0.01, 500.0),
            "mastHeightM" to mastHeightM.coerceIn(0.1, 500.0),
            "elementCount" to elementCount.coerceIn(1L, 64L),
            "velocityFactor" to velocityFactor.coerceIn(0.1, 1.0),
            "calculatedSwr" to calculatedSwr.coerceIn(1.0, 100.0),
            "gainDbi" to gainDbi.coerceIn(-50.0, 60.0),
            "notes" to notes.take(2000),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}

@IgnoreExtraProperties
data class VnaMeasurementDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val deviceName: String = "Arinst V2D",
    val connectionType: String = "USB_OTG",
    val targetFreqMhz: Double = 145.500,
    val measuredResonanceMhz: Double = 143.850,
    val measuredSwr: Double = 1.42,
    val measuredR: Double = 46.2,
    val measuredX: Double = 12.4,
    val recommendation: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(ownerUid: String): Map<String, Any> {
        return mapOf(
            "userId" to ownerUid,
            "deviceName" to deviceName.take(100).ifBlank { "Arinst V2D" },
            "connectionType" to connectionType.take(40).ifBlank { "USB_OTG" },
            "targetFreqMhz" to targetFreqMhz.coerceIn(0.000001, 6000.0),
            "measuredResonanceMhz" to measuredResonanceMhz.coerceIn(0.000001, 6000.0),
            "measuredSwr" to measuredSwr.coerceIn(1.0, 100.0),
            "measuredR" to measuredR.coerceIn(0.0, 10000.0),
            "measuredX" to measuredX.coerceIn(-10000.0, 10000.0),
            "recommendation" to recommendation.take(2000).ifBlank { "Resonance check completed" },
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}

@IgnoreExtraProperties
data class CustomPluginDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val author: String = "",
    val version: String = "1.0.0",
    val category: String = "CABLE_LOSS",
    val description: String = "",
    val formulaExpression: String = "",
    val inputSchemaJson: String = "",
    val enabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(ownerUid: String): Map<String, Any> {
        return mapOf(
            "userId" to ownerUid,
            "name" to name.take(120).ifBlank { "Custom Plugin" },
            "author" to author.take(80).ifBlank { "Operator" },
            "version" to version.take(30).ifBlank { "1.0.0" },
            "category" to category.take(40).ifBlank { "CABLE_LOSS" },
            "description" to description.take(1500).ifBlank { "Custom RF calculation extension" },
            "formulaExpression" to formulaExpression.take(2000).ifBlank { "k1 * sqrt(freqMhz) * lengthM / 100" },
            "inputSchemaJson" to inputSchemaJson.take(2000),
            "enabled" to enabled,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}
