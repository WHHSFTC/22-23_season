package org.thenuts.powerplay.subsystems

import com.acmerobotics.dashboard.config.Config
import com.acmerobotics.roadrunner.control.PIDCoefficients
import com.acmerobotics.roadrunner.control.PIDFController
import com.qualcomm.robotcore.hardware.DcMotor.ZeroPowerBehavior
import com.qualcomm.robotcore.hardware.DcMotorSimple
import org.thenuts.switchboard.core.Logger
import org.thenuts.switchboard.hardware.Configuration
import org.thenuts.switchboard.hardware.Motor
import org.thenuts.switchboard.hardware.MotorImpl
import org.thenuts.switchboard.util.Frame
import kotlin.math.absoluteValue
import kotlin.math.max

@Config
class Lift(val log: Logger, config: Configuration) : Subsystem {
    val encoder1 = config.encoders["slides1"].also {
        it.stopAndReset()
    }

    val encoder2 = config.encoders["slides2"].also {
        it.stopAndReset()
    }

    fun getPosition(): Int {
        val pos1 = encoder1.position
        val pos2 = encoder2.position

        return if ((pos1 - pos2).absoluteValue < MAX_DIFF) {
            (pos1 + pos2) / 2
        } else if (encoder1.velocity == 0.0) {
            pos2
        } else {
            pos1
        }
    }

    fun setPower(pow: Double) {
        motor1.power = pow
        motor2.power = pow
    }

    fun setZpb(zpb: Motor.ZeroPowerBehavior) {
        motor1.zpb = zpb
        motor2.zpb = zpb
    }

    val motor1 = config.motors["slides1"].also {
        it.zpb = Motor.ZeroPowerBehavior.BRAKE
        (it as MotorImpl).m.direction = DcMotorSimple.Direction.REVERSE
    }
    val motor2 = config.motors["slides2"].also {
        it.zpb = Motor.ZeroPowerBehavior.BRAKE
        (it as MotorImpl).m.direction = DcMotorSimple.Direction.REVERSE
    }

    val leftSwitch = config.touchSensors["leftLimit"]
    val rightSwitch = config.touchSensors["rightLimit"]

    override val outputs = listOf(motor1, motor2)

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

    @Config
    enum class Height(@JvmField var pos: Int) {
        INTAKE(0), MIN_CLEAR(2300), ABOVE_STACK(1300),

        TERMINAL(100), GROUND(100),
        LOW(1300), MID(2400), HIGH(3500);
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
                    encoder1.stopAndReset()
                    encoder2.stopAndReset()
                    setZpb(Motor.ZeroPowerBehavior.BRAKE)
                    setPower(0.0)
                    if (state == State.ZERO) {
                        State.IDLE
                    } else {
                        state
                    }
                } else {
                    state
                }
            }

//            is State.RunTo -> {
//                if ((encoder.position - state.pos).absoluteValue < TOLERANCE) {
//                    log.addMessage("reached ${state.pos}", 10.seconds)
//                    State.IDLE
//                } else {
//                    state
//                }
//            }

            else -> state
        }

        when (state) {
            State.ZERO -> {
//                if (getPosition() > BRAKE_HEIGHT) {
//                    setZpb(Motor.ZeroPowerBehavior.BRAKE)
//                    setPower(0.0)
//                } else {
                setZpb(Motor.ZeroPowerBehavior.FLOAT)
                setPower(DROP_POWER)
//                }
            }
            is State.Hold -> {
                setZpb(Motor.ZeroPowerBehavior.BRAKE)
                if (state.pos > BRAKE_HEIGHT) {
                    setPower(0.0)
                } else {
                    setPower(max(0.0, LIFT_KB + LIFT_KH * state.pos))
                }
            }
            is State.RunTo -> {
                setZpb(Motor.ZeroPowerBehavior.BRAKE)
                runToController.targetPosition = state.pos.toDouble()
                runToController.targetVelocity = 0.0
                runToController.targetAcceleration = 0.0
                val output = runToController.update(
                    measuredPosition = getPosition().toDouble(),
                )
                log.out["PID output"] = output
                log.out["static term"] = LIFT_KB + LIFT_KH * state.pos
                setPower(max(DROP_POWER, output + LIFT_KB + LIFT_KH * state.pos))
            }
            is State.Manual -> {
                setZpb(Motor.ZeroPowerBehavior.FLOAT)
                setPower(max(DROP_POWER, state.velocity + LIFT_KB + LIFT_KH * getPosition()))
            }
        }

        if (this.state != state)
            this.state = state
    }

    override fun cleanup() {
        setZpb(Motor.ZeroPowerBehavior.BRAKE)
        setPower(0.0)
        state = State.IDLE
    }

    companion object {
//        @JvmField var MAX_SLIDES_UP = 1.0
//        @JvmField var MAX_SLIDES_DOWN = 0.1

        @JvmField var LIFT_RUN_TO_PID = PIDCoefficients(
            kP = 0.002,
        )
        @JvmField var LIFT_KV = 1.0
        @JvmField var LIFT_KA = 0.0

        @JvmField var LIFT_KS = 0.0
        @JvmField var LIFT_KB = 0.0
        @JvmField var LIFT_KH = 0.0

        @JvmField var TOLERANCE = 200

        @JvmField var BRAKE_HEIGHT = 0
        @JvmField var DROP_POWER = -0.5

        @JvmField var MAX_DIFF = 200

        @JvmField var CONE_STEP = 200
    }
}