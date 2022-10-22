package org.thenuts.powerplay.subsystems

import com.acmerobotics.dashboard.config.Config
import com.acmerobotics.roadrunner.control.PIDCoefficients
import com.acmerobotics.roadrunner.control.PIDFController
import org.thenuts.switchboard.core.Logger
import org.thenuts.switchboard.hardware.Configuration
import org.thenuts.switchboard.hardware.Motor
import org.thenuts.switchboard.util.Frame
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.time.Duration.Companion.seconds

@Config
class Lift(val log: Logger, config: Configuration) : Subsystem {
    val encoder = config.encoders["slides"].also {
        it.stopAndReset()
    }

    val motor = config.motors["slides"].also {
        it.zpb = Motor.ZeroPowerBehavior.BRAKE
    }

    val leftSwitch = config.touchSensors["leftLimit"]
    val rightSwitch = config.touchSensors["rightLimit"]

    override val outputs = listOf(motor)

    var state: State = State.IDLE
        set(value) {
            runToController.reset()
            field = value
        }

    sealed class State {
        object IDLE : State()
        object ZERO : State()
        data class RunTo(val pos: Int): State()
        data class Hold(val pos: Int, val isManual: Boolean): State()
        data class Manual(val velocity: Double): State()
    }

    enum class Height(val pos: Int) {
        INTAKE(0), MIN_CLEAR(2300),

        TERMINAL(100), GROUND(100),
        LOW(1300), MID(2300), HIGH(3000);
    }

    val isBusy: Boolean
        get() {
            val state = state
            return when (state) {
                State.ZERO -> true
                is State.RunTo -> true
                is State.Manual -> true

                is State.Hold -> state.isManual

                State.IDLE -> false
            }
        }

    val runToController = PIDFController(
        LIFT_RUN_TO_PID,
        LIFT_KV,
        LIFT_KA,
        LIFT_KS
    )

    override fun update(frame: Frame) {
        var state = state
        log.out["lift state"] = state
        state = when (state) {
            State.ZERO, is State.Manual -> {
                if (leftSwitch.pressed || rightSwitch.pressed) {
                    encoder.stopAndReset()
                    motor.zpb = Motor.ZeroPowerBehavior.BRAKE
                    motor.power = 0.0
                    State.IDLE
                } else {
                    state
                }
            }

            is State.RunTo -> {
                if ((encoder.position - state.pos).absoluteValue < TOLERANCE) {
                    log.addMessage("reached ${state.pos}", 10.seconds)
                    State.IDLE
                } else {
                    state
                }
            }

            else -> state
        }

        when (state) {
            State.ZERO -> {
                if (encoder.position > BRAKE_HEIGHT) {
                    motor.zpb = Motor.ZeroPowerBehavior.BRAKE
                    motor.power = 0.0
                } else {
                    motor.zpb = Motor.ZeroPowerBehavior.FLOAT
                    motor.power = DROP_POWER
                }
            }
            is State.Hold -> {
                motor.zpb = Motor.ZeroPowerBehavior.BRAKE
                if (state.pos > BRAKE_HEIGHT) {
                    motor.power = 0.0
                } else {
                    motor.power = max(0.0, LIFT_KB + LIFT_KH * state.pos)
                }
            }
            is State.RunTo -> {
                motor.zpb = Motor.ZeroPowerBehavior.BRAKE
                runToController.targetPosition = state.pos.toDouble()
                runToController.targetVelocity = 0.0
                runToController.targetAcceleration = 0.0
                val output = runToController.update(
                    measuredPosition = encoder.position.toDouble(),
                )
                log.out["PID output"] = output
                log.out["static term"] = LIFT_KB + LIFT_KH * state.pos
                motor.power = max(0.0, output + LIFT_KB + LIFT_KH * state.pos)
            }
            is State.Manual -> {
                motor.zpb = Motor.ZeroPowerBehavior.FLOAT
                motor.power = max(0.0, state.velocity + LIFT_KB + LIFT_KH * encoder.position)
            }
        }

        if (this.state != state)
            this.state = state
    }

    override fun cleanup() {
        motor.zpb = Motor.ZeroPowerBehavior.BRAKE
        motor.power = 0.0
        state = State.IDLE
    }

    companion object {
        @JvmField var MAX_SLIDES_UP = 1.0
        @JvmField var MAX_SLIDES_DOWN = 0.1

        @JvmField var LIFT_RUN_TO_PID = PIDCoefficients(
            kP = 0.004
        )
        @JvmField var LIFT_KV = 1.0
        @JvmField var LIFT_KA = 0.0
        @JvmField var LIFT_KS = 0.0
        @JvmField var LIFT_KB = 0.0
        @JvmField var LIFT_KH = 0.0

        @JvmField var TOLERANCE = 200

        @JvmField var BRAKE_HEIGHT = 0
        @JvmField var DROP_POWER = 0.0
    }
}