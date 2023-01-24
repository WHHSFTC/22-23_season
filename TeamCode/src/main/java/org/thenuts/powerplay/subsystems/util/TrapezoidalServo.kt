package org.thenuts.powerplay.subsystems.util

import org.thenuts.switchboard.hardware.Servo
import org.thenuts.switchboard.util.sinceJvmTime
import kotlin.time.Duration
import kotlin.time.DurationUnit

class TrapezoidalServo(val servo: Servo, val maxVel: () -> Double, val maxAccel: () -> Double) : Servo {
    override var position: Double = 0.0
        set(value) {
            startTime = Duration.sinceJvmTime()
            profile = TrapezoidalProfile(servo.position, value, maxVel(), maxAccel())
            field = value
        }

    var startTime: Duration = Duration.ZERO
    var profile: TrapezoidalProfile? = null

    val isBusy: Boolean get() = profile != null

    fun time(): Double {
        return (Duration.sinceJvmTime() - startTime).toDouble(DurationUnit.SECONDS)
    }

    override fun output(all: Boolean) {
        if (all) {
            servo.position = position
            servo.output(all = true)
            return
        }
        val profile = profile ?: return
        val t = time()
        if (t <= profile.length()) {
            servo.position = profile.position(t)
            servo.output()
        } else if (t > profile.length()) {
            servo.position = profile.end
            servo.output()
            this.profile = null
        }
    }

    override fun getWorstMean(): Duration {
        return servo.getWorstMean()
    }
}
