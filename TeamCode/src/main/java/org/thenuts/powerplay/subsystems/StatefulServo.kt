package org.thenuts.powerplay.subsystems

import org.thenuts.switchboard.hardware.HardwareOutput
import org.thenuts.switchboard.hardware.Servo
import org.thenuts.switchboard.util.sinceJvmTime
import kotlin.math.absoluteValue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class StatefulServo<T: StatefulServo.ServoPosition>(val servo: Servo, initial: T, val speed: Double = 0.5) : HardwareOutput {
    val isBusy get() = Duration.sinceJvmTime(Duration.ZERO) < deadline

    private var deadline = Duration.ZERO

    init {
        servo.position = initial.pos
    }

    override fun output(all: Boolean) {
        servo.output(all)
    }

//    constructor(servo: Servo, initial: T): this(servo as ServoImplEx, initial)
    var state: T = initial
        set(value) {
//            when {
//                value.en && !servo.isPwmEnabled -> {
//                    servo.setPwmEnable()
//                }
//                !value.en && servo.isPwmEnabled -> {
//                    servo.setPwmDisable()
//                }
//            }

            val delta = value.pos - servo.position
            deadline = Duration.sinceJvmTime(Duration.ZERO) + (delta.absoluteValue / speed).seconds

            servo.position = value.pos

            field = value
        }

    interface ServoPosition {
        val pos: Double
//        val en: Boolean
    }
}