package org.thenuts.powerplay.subsystems.util

import com.acmerobotics.roadrunner.profile.*
import org.thenuts.switchboard.hardware.Servo
import org.thenuts.switchboard.util.sinceJvmTime
import kotlin.time.Duration
import kotlin.time.DurationUnit

class TrapezoidalServo(val servo: Servo, val maxVel: VelocityConstraint, val maxAccel: AccelerationConstraint) : Servo {
    override var position: Double = 0.0
        set(value) {
            startTime = Duration.sinceJvmTime()
            profile = MotionProfileGenerator.generateMotionProfile(
                start = MotionState(servo.position, profile?.get(time())?.v ?: 0.0),
                goal = MotionState(value, 0.0),
                velocityConstraint = maxVel,
                accelerationConstraint = maxAccel,
                resolution = 0.01
                //value, maxVel(), maxAccel())
            )
            field = value
        }

    var startTime: Duration = Duration.ZERO
    var profile: MotionProfile? = null

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
        if (t <= profile.duration()) {
            servo.position = profile[t].x
            servo.output()
        } else if (t > profile.duration()) {
            servo.position = profile.end().x
            servo.output()
            this.profile = null
        }
    }

    override fun getWorstMean(): Duration {
        return servo.getWorstMean()
    }
}
