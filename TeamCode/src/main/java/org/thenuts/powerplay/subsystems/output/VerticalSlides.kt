package org.thenuts.powerplay.subsystems.output

import com.acmerobotics.dashboard.config.Config
import com.acmerobotics.roadrunner.control.PIDCoefficients
import com.acmerobotics.roadrunner.control.PIDFController
import com.qualcomm.robotcore.hardware.DcMotorSimple
import org.thenuts.powerplay.subsystems.Subsystem
import org.thenuts.powerplay.subsystems.util.TrapezoidalProfile
import org.thenuts.switchboard.core.Logger
import org.thenuts.switchboard.hardware.Configuration
import org.thenuts.switchboard.hardware.Motor
import org.thenuts.switchboard.hardware.MotorImpl
import org.thenuts.switchboard.util.Frame
import org.thenuts.switchboard.util.sinceJvmTime
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.DurationUnit

@Config
class VerticalSlides(val log: Logger, config: Configuration) : Subsystem {
    val encoder1 = config.encoders["slides1"].also {
//        it.stopAndReset()
    }

    val encoder2 = config.encoders["slides2"].also {
//        it.stopAndReset()
    }

    fun zeroPosition() {
        encoder1.stopAndReset()
        encoder2.stopAndReset()
    }

    fun getPosition(): Int {
        val pos1 = encoder1.position
        val pos2 = encoder2.position
        log.out["pos1"] = pos1
        log.out["pos2"] = pos2

        return pos2
//        return if ((pos1 - pos2).absoluteValue < MAX_DIFF) {
//            (pos1 + pos2) / 2
//        } else if (encoder1.velocity == 0.0) {
//            pos2
//        } else {
//            pos1
//        }
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
        (it as? MotorImpl)?.m?.direction = DcMotorSimple.Direction.REVERSE
    }
    val motor2 = config.motors["slides2"].also {
        it.zpb = Motor.ZeroPowerBehavior.BRAKE
//        (it as? MotorImpl)?.m?.direction = DcMotorSimple.Direction.REVERSE
    }

    val leftSwitch = config.touchSensors["leftLimit"]
    val rightSwitch = config.touchSensors["rightLimit"]

    override val outputs = listOf(motor1, motor2)

    var state: State = State.IDLE
        set(value) {
            resetController()
            field = value
        }

    sealed class State {
        object IDLE : State()
        object ZERO : State()
        object FIND_EDGE : State()
        sealed class PID: State() {
            abstract val pos: Int
            abstract val vel: Double
            abstract val accel: Double
        }
        data class RunTo(override val pos: Int): PID() {
            override val vel: Double = 0.0
            override val accel: Double = 0.0
        }
        class Profiled(val profile: TrapezoidalProfile): PID() {
            private var startTime: Duration? = null
            val t: Double get() {
                val now = Duration.sinceJvmTime()
                val s = startTime
                return if (s == null) {
                    startTime = now
                    0.0
                } else {
                    (now - s).toDouble(DurationUnit.SECONDS)
                }
            }
            override val pos: Int
                get() = profile.position(t).roundToInt()
            override val vel: Double
                get() = profile.velocity(t)
            override val accel: Double
                get() = profile.acceleration(t)
        }
        data class Hold(val pos: Int, val isManual: Boolean): State()
        data class Manual(val velocity: Double): State()
    }

    fun profileTo(target: Int) {
        val profile = TrapezoidalProfile(getPosition().toDouble(), target.toDouble(), MAX_VEL, MAX_ACCEL)
        log.out["slides profile length"] = profile.length()
        state = State.Profiled(profile)
    }

    fun runTo(target: Int) {
        if (PROFILED)
            profileTo(target)
        else
            state = State.RunTo(target)
    }

    @Config
    enum class Height(@JvmField var pos: Int) {
        INTAKE(0),  TWO(50), THREE(90), FOUR(140), FIVE(180),

        MIN_CLEAR(0), ABOVE_STACK(250),

        TERMINAL(38), GROUND(38),
        LOW(0), MID(335), HIGH(660);
    }

    val isBusy: Boolean
        get() {
            val state = state
            return when (state) {
                State.ZERO -> true
                State.FIND_EDGE -> true
                is State.Manual -> true
                is State.RunTo -> (state.pos - getPosition()).absoluteValue > BUSY_THRESHOLD
                is State.Profiled -> (state.profile.end - getPosition()).absoluteValue > BUSY_THRESHOLD

                is State.Hold -> state.isManual

                State.IDLE -> false
            }
        }

    var runToController = PIDFController(
        LIFT_RUN_TO_PID,
        LIFT_KV,
        LIFT_KA,
        LIFT_KS
    )

    fun resetController() {
        runToController = PIDFController(
            LIFT_RUN_TO_PID,
            LIFT_KV,
            LIFT_KA,
            LIFT_KS
        )
        runToController.reset()
    }

    override fun update(frame: Frame) {
        var state = state
        log.out["lift state"] = state
        state = when (state) {
            State.ZERO, is State.Manual -> {
                if (leftSwitch.pressed || rightSwitch.pressed) {
                    setZpb(Motor.ZeroPowerBehavior.BRAKE)
                    setPower(0.0)
                    State.FIND_EDGE
                } else {
                    state
                }
            }

            State.FIND_EDGE -> {
                if (!leftSwitch.pressed && !rightSwitch.pressed) {
                    encoder1.stopAndReset()
                    encoder2.stopAndReset()
                    setZpb(Motor.ZeroPowerBehavior.BRAKE)
                    setPower(0.0)
                    State.IDLE
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

        val position = getPosition().toDouble()
        val velocity = encoder2.velocity
        log.out["slides measured position"] = position
        log.out["slides measured velocity"] = velocity
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
            State.FIND_EDGE -> {
                setZpb(Motor.ZeroPowerBehavior.BRAKE)
                setPower(EDGE_POWER)
            }
            is State.Hold -> {
                setZpb(Motor.ZeroPowerBehavior.BRAKE)
                if (state.pos > BRAKE_HEIGHT) {
                    setPower(0.0)
                } else {
                    setPower(max(0.0, LIFT_KB + LIFT_KH * state.pos))
                }
            }
            is State.PID -> {
                setZpb(Motor.ZeroPowerBehavior.BRAKE)
                runToController.targetPosition = state.pos.toDouble()
                runToController.targetVelocity = state.vel
                runToController.targetAcceleration = state.accel
                var roundedPosition = position
                var roundedVelocity = velocity
                val error = position - state.pos.toDouble()
                if (position < INSIDE_BOT && error.absoluteValue < SAME_THRESHOLD) {
                    roundedPosition = state.pos.toDouble()
                }
                if (error < NO_VELO_TRESHOLD) {
                    roundedVelocity = 0.0
                }
                val output = runToController.update(
                    measuredPosition = roundedPosition,
                    measuredVelocity = roundedVelocity,
                )
                log.out["slides target position"] = state.pos
                log.out["slides target velocity"] = state.vel
                log.out["slides PID output"] = output
                var static = LIFT_KB + LIFT_KH * state.pos
                if (output.absoluteValue < MIN_STATIC) {
                    static *= output.absoluteValue / MIN_STATIC
                }
                log.out["slides static term"] = static
                var power = max(DROP_POWER, output + static)
//                if (position < INSIDE_BOT) {
//                    if (power > 0.0)
//                        power += BOT_FRICTION
//                    else {
//                        power = max(INSIDE_DROP_POWER, power)
//                        //setZpb(INSIDE_DROP_MODE)
//                    }
//                }
                setPower(power)
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
            kP = 0.008,
            kD = 0.0004
        )
        @JvmField var LIFT_KV = 0.0008
        @JvmField var LIFT_KA = 0.00007

        @JvmField var LIFT_KS = 0.1
        @JvmField var LIFT_KB = 0.1
        @JvmField var LIFT_KH = 0.0000

        @JvmField var SAME_THRESHOLD = 10
        @JvmField var NO_VELO_TRESHOLD = 50

        @JvmField var BRAKE_HEIGHT = 0
        @JvmField var DROP_POWER = -1.0
        @JvmField var EDGE_POWER = 0.25

        @JvmField var MAX_DIFF = 200

        @JvmField var CONE_STEP = 38

        @JvmField var BUSY_THRESHOLD = 150

        @JvmField var INSIDE_BOT = 260
        @JvmField var INSIDE_DROP_POWER = -1.0
        //@JvmField var INSIDE_DROP_MODE = Motor.ZeroPowerBehavior.BRAKE
        @JvmField var BOT_FRICTION = 0.0

        @JvmField var VELO_THRESHOLD = 10.0

        @JvmField var MAX_ACCEL = 6000.0
        @JvmField var MAX_VEL = 1500.0

        @JvmField var PROFILED = true

        @JvmField var MIN_STATIC = 0.1
    }
}