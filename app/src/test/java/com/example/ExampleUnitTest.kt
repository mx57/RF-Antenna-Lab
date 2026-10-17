package com.example

import com.example.domain.AntennaPhysicsEngine
import com.example.domain.AntennaType
import com.example.domain.BUILTIN_COAX_CABLES
import com.example.domain.PluginEngine
import com.example.domain.VnaOptimizerEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun antennaPhysicsEngine_calculatesFrom1HzTo6GHzAndAllWave() {
        // 1 Hz ELF Magnetic Loop
        val elfCalc = AntennaPhysicsEngine.calculate(
            antennaType = AntennaType.MAG_LOOP,
            frequencyMhz = 0.000001 // 1 Hz
        )
        assertTrue(elfCalc.dimensions.isNotEmpty())
        assertTrue(elfCalc.wavelengthMeters > 100000.0)

        // 14.175 MHz 5-element Yagi
        val hfYagi = AntennaPhysicsEngine.calculate(
            antennaType = AntennaType.YAGI,
            frequencyMhz = 14.175,
            elementCount = 5
        )
        assertEquals(6, hfYagi.dimensions.size) // REF + DRV + 3 Directors + BOOM
        assertTrue(hfYagi.gainDbi > 7.0)

        // 5800 MHz (5.8 GHz) Helical SHF Antenna
        val shfHelix = AntennaPhysicsEngine.calculate(
            antennaType = AntennaType.HELICAL_PATCH,
            frequencyMhz = 5800.0,
            elementCount = 8
        )
        assertTrue(shfHelix.gainDbi > 10.0)
        assertEquals(41, shfHelix.sweepPoints.size)

        // All-Wave Discone
        val discone = AntennaPhysicsEngine.calculate(
            antennaType = AntennaType.DISCONE,
            frequencyMhz = 145.0
        )
        assertTrue(discone.antennaType.isAllWave)
        assertTrue(discone.centerSwr50 < 1.5)
    }

    @Test
    fun vnaOptimizerEngine_reducesSwrAndSynthesizesLNetwork() {
        val base = AntennaPhysicsEngine.calculate(
            antennaType = AntennaType.DIPOLE,
            frequencyMhz = 14.175
        )
        val opt = VnaOptimizerEngine.optimizeFromVna(
            baseCalc = base,
            measuredResonanceMhz = 13.850,
            measuredRAtTarget = 42.0,
            measuredXAtTarget = 24.0
        )
        assertTrue(opt.predictedSwrAfterTuning < opt.measuredSwrAtTarget)
        assertTrue(opt.lengthDeltaMeters < 0.0) // 13.85 MHz < 14.175 MHz -> needs shortening
        assertTrue(opt.lNetworkMatch.capacitancePf > 0.0)
    }

    @Test
    fun pluginEngine_calculatesCoaxLossAndCustomFormula() {
        val lmr400 = BUILTIN_COAX_CABLES.first { it.id == "LMR400" }
        val loss = PluginEngine.calculateCoaxCableLoss(
            cable = lmr400,
            freqMhz = 145.5,
            lengthMeters = 30.0,
            loadSwr = 1.5,
            txPowerWatts = 100.0
        )
        assertTrue(loss.totalLossDb > 0.5 && loss.totalLossDb < 4.0)
        assertTrue(loss.powerAtAntennaWatts < 100.0 && loss.powerAtAntennaWatts > 50.0)

        val customExec = PluginEngine.executePluginFormula(
            pluginName = "Test Coax Loss",
            formulaExpression = "(k1 * sqrt(freqMhz) + k2 * freqMhz) * (lengthM / 100)",
            inputSchemaJson = "lengthM=30,k1=0.39,k2=0.0008",
            freqMhz = 144.0,
            swr = 1.2,
            powerW = 100.0
        )
        assertTrue(customExec.primaryValue > 1.0)
    }
}
