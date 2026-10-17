package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.AntennaGeneticSynthesizer
import com.example.domain.AntennaPhysicsEngine
import com.example.domain.AntennaType
import com.example.domain.PdfPassportGenerator
import com.example.domain.SynthesisGoal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `verify app name and all 26 antennas and 3D CAD segments`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("RF Antenna Lab", appName)
        assertEquals(26, AntennaType.entries.size)

        AntennaType.entries.forEach { type ->
            val calc = AntennaPhysicsEngine.calculate(
                antennaType = type,
                frequencyMhz = 14.175,
                upperFreqMhz = 50.0,
                wireDiameterMm = 2.5,
                mastHeightMeters = 12.0,
                elementCount = type.defaultElements,
                velocityFactor = 0.95,
                txPowerWatts = 100.0
            )
            assertTrue("Dimensions should not be empty for ${type.code}", calc.dimensions.isNotEmpty())
            assertTrue("Polar pattern should not be empty for ${type.code}", calc.polarPattern.isNotEmpty())
            val segments3D = AntennaGeneticSynthesizer.build3DSegmentsForAntenna(calc)
            assertTrue("3D segments should not be empty for ${type.code}", segments3D.isNotEmpty())
        }
    }

    @Test
    fun `verify genetic antenna synthesizer and PDF passport generation`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        SynthesisGoal.entries.forEachIndexed { idx, goal ->
            val synth = AntennaGeneticSynthesizer.synthesizeOptimalAntenna(
                frequencyMhz = 14.175,
                maxSpanMeters = 8.5,
                mastHeightMeters = 10.0,
                goal = goal,
                seedVariation = idx + 1
            )
            assertTrue(synth.segments3D.isNotEmpty())
            assertTrue(synth.dimensions.isNotEmpty())
            assertTrue(synth.predictedSwr50 >= 1.0)
        }

        val sampleCalc = AntennaPhysicsEngine.calculate(
            antennaType = AntennaType.YAGI,
            frequencyMhz = 145.500,
            upperFreqMhz = 435.0,
            wireDiameterMm = 6.0,
            mastHeightMeters = 10.0,
            elementCount = 5,
            velocityFactor = 0.96,
            txPowerWatts = 50.0
        )
        val vnaOpt = com.example.domain.VnaOptimizerEngine.optimizeFromVna(
            baseCalc = sampleCalc,
            measuredResonanceMhz = 142.800,
            measuredRAtTarget = 38.0,
            measuredXAtTarget = 16.5
        )
        assertTrue(vnaOpt.predictedSwrAfterTuning < vnaOpt.measuredSwrAtTarget)
    }
}
