package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class RfCloudRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeAntennaProfiles(userId: String): Flow<List<AntennaProfileDoc>> = callbackFlow {
        val path = "antenna_profiles"
        val registration = db.collection(path)
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val items = snapshot.toObjects(
                        AntennaProfileDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                    trySend(items)
                }
            }
        awaitClose { registration.remove() }
    }

    fun observeVnaMeasurements(userId: String): Flow<List<VnaMeasurementDoc>> = callbackFlow {
        val path = "vna_measurements"
        val registration = db.collection(path)
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val items = snapshot.toObjects(
                        VnaMeasurementDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                    trySend(items)
                }
            }
        awaitClose { registration.remove() }
    }

    fun observeCustomPlugins(userId: String): Flow<List<CustomPluginDoc>> = callbackFlow {
        val path = "custom_plugins"
        val registration = db.collection(path)
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val items = snapshot.toObjects(
                        CustomPluginDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                    trySend(items)
                }
            }
        awaitClose { registration.remove() }
    }

    suspend fun createAntennaProfile(profile: AntennaProfileDoc): Result<String> {
        val uid = requireUserId()
        val docId = if (profile.id.isNotBlank()) profile.id else "prof_${UUID.randomUUID().toString().replace("-", "").take(20)}"
        val docRef = db.collection("antenna_profiles").document(docId)
        return try {
            docRef.set(profile.toCreateMap(uid)).await()
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getAntennaProfileById(profileId: String): Result<AntennaProfileDoc?> {
        val docRef = db.collection("antenna_profiles").document(profileId)
        return try {
            val snap = docRef.get().await()
            Result.success(
                snap.toObject(
                    AntennaProfileDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteAntennaProfile(profileId: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("antenna_profiles").document(profileId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createVnaMeasurement(measurement: VnaMeasurementDoc): Result<String> {
        val uid = requireUserId()
        val docId = if (measurement.id.isNotBlank()) measurement.id else "vna_${UUID.randomUUID().toString().replace("-", "").take(20)}"
        val docRef = db.collection("vna_measurements").document(docId)
        return try {
            docRef.set(measurement.toCreateMap(uid)).await()
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteVnaMeasurement(measurementId: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("vna_measurements").document(measurementId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createCustomPlugin(plugin: CustomPluginDoc): Result<String> {
        val uid = requireUserId()
        val docId = if (plugin.id.isNotBlank()) plugin.id else "plug_${UUID.randomUUID().toString().replace("-", "").take(20)}"
        val docRef = db.collection("custom_plugins").document(docId)
        return try {
            docRef.set(plugin.toCreateMap(uid)).await()
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleCustomPlugin(pluginId: String, enabled: Boolean): Result<Unit> {
        requireUserId()
        val docRef = db.collection("custom_plugins").document(pluginId)
        return try {
            docRef.update(
                mapOf(
                    "enabled" to enabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteCustomPlugin(pluginId: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("custom_plugins").document(pluginId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }
}
