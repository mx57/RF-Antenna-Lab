package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class RfCloudRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createAndObserveAntennaProfile_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = RfCloudRepository(firestore, auth)

        val profile = AntennaProfileDoc(
            name = "14.15 MHz 3-el Yagi",
            antennaType = "YAGI",
            frequencyMhz = 14.15,
            wireDiameterMm = 12.0,
            mastHeightM = 15.0,
            elementCount = 3L,
            velocityFactor = 0.96,
            calculatedSwr = 1.08,
            gainDbi = 8.15,
            notes = "Boom 4.2m"
        )

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createAntennaProfile(profile)
        }
        assertTrue(createResult.isSuccess)
        val docId = createResult.getOrThrow()

        val emitted = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeAntennaProfiles(uid).first { list -> list.any { it.id == docId } }
        }
        assertTrue(emitted.any { it.id == docId && it.name == "14.15 MHz 3-el Yagi" })
    }

    @Test
    fun getAntennaProfile_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = RfCloudRepository(firestore, auth)
        val docId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createAntennaProfile(
                AntennaProfileDoc(name = "Alice Private Loop", antennaType = "MAG_LOOP")
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = RfCloudRepository(firestore, auth)
        val bobRead = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getAntennaProfileById(docId)
        }
        assertTrue(bobRead.isFailure)
        val exception = bobRead.exceptionOrNull() as? FirebaseFirestoreException
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, exception?.code)
    }

    @Test
    fun observeAntennaProfiles_unauthenticated_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = RfCloudRepository(firestore, auth)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeAntennaProfiles("unauth_user").first()
            }
            fail("Expected FirebaseFirestoreException PERMISSION_DENIED")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }

    @Test
    fun createVnaAndPlugin_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = RfCloudRepository(firestore, auth)

        val vnaRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createVnaMeasurement(
                VnaMeasurementDoc(
                    deviceName = "Arinst V2D",
                    connectionType = "USB_OTG",
                    targetFreqMhz = 145.5,
                    measuredResonanceMhz = 144.8,
                    measuredSwr = 1.25,
                    measuredR = 48.0,
                    measuredX = 6.5,
                    recommendation = "Trim 3.2 mm"
                )
            )
        }
        assertTrue(vnaRes.isSuccess)

        val plugRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createCustomPlugin(
                CustomPluginDoc(
                    name = "Heliax Loss Calc",
                    author = "RA3AAA",
                    version = "1.2.0",
                    category = "CABLE_LOSS",
                    description = "Coax loss at UHF",
                    formulaExpression = "k1 * sqrt(freqMhz) * lengthM / 100",
                    inputSchemaJson = "lengthM=25,k1=0.18",
                    enabled = true
                )
            )
        }
        assertTrue(plugRes.isSuccess)
        val plugId = plugRes.getOrThrow()

        val plugins = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeCustomPlugins(uid).first { list -> list.any { it.id == plugId } }
        }
        assertTrue(plugins.any { it.id == plugId })
    }

    private companion object {
        const val ALICE_EMAIL = "alice@rf-lab.test"
        const val BOB_EMAIL = "bob@rf-lab.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
