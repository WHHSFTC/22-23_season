package org.thenuts.powerplay.subsystems.util

import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sqrt

class TrapezoidalProfile(val start: Double, val end: Double, val maxVel: Double, val maxAccel: Double) {
    val theoreticalRampTime = maxVel / maxAccel
    val sign = (end - start).sign
    val distance = abs(end - start)
    val rampTime = min(sqrt(distance / maxAccel), theoreticalRampTime)
    val rampDistance = 0.5 * maxAccel * rampTime * rampTime
    val plateauDistance = distance - 2.0 * rampDistance
    val plateauTime = plateauDistance / maxVel
    val plateauVel = rampTime * maxAccel

    fun acceleration(t: Double): Double {
        if (t < 0.0) return 0.0
        if (t < rampTime) return maxAccel
        if (t < rampTime + plateauTime) return 0.0
        if (t < rampTime + plateauTime + rampTime) return -maxAccel
        return 0.0
    }

    fun velocity(t: Double): Double {
        if (t < 0.0) return 0.0
        if (t < rampTime) return t * maxAccel * sign
        if (t < rampTime + plateauTime) return plateauVel * sign
        if (t < rampTime + plateauTime + rampTime) return (plateauVel - (t - rampTime - plateauTime) * maxAccel) * sign
        return 0.0
    }

    fun position(t: Double): Double {
        if (t < 0.0) return start
        if (t < rampTime) return start + 0.5 * maxAccel * t * t * sign
        if (t < rampTime + plateauTime) return start + (rampDistance + plateauVel * (t - rampTime)) * sign
        if (t < rampTime + plateauTime + rampTime) return start + (rampDistance + plateauDistance + plateauVel * (t - rampTime - plateauTime) - 0.5 * maxAccel * (t - rampTime - plateauTime) * (t - rampTime - plateauTime)) * sign
        return end
    }

    fun length(): Double {
        return rampTime + plateauTime + rampTime
    }
}
