package org.thenuts.powerplay.subsystems.intake

import com.acmerobotics.dashboard.config.Config
import com.acmerobotics.roadrunner.control.PIDCoefficients
import com.acmerobotics.roadrunner.control.PIDFController
import org.thenuts.powerplay.subsystems.util.StateMachine
import org.thenuts.powerplay.subsystems.Subsystem
import org.thenuts.switchboard.core.Logger
import org.thenuts.switchboard.hardware.Configuration
import org.thenuts.switchboard.hardware.HardwareOutput
import org.thenuts.switchboard.util.Frame
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.sin

@Config
class LinkageSlides(val log: Logger, config: Configuration) : Subsystem {
    val motor = config.motors["linkage"]
    val encoder = config.encoders["linkage"]

    override fun start(frame: Frame) {
        encoder.stopAndReset()
    }

    override val outputs: List<HardwareOutput> = listOf(motor)

    sealed class State: StateMachine.State<LinkageSlides, State> {
        override fun toString(): String {
            return javaClass.simpleName
        }

        object IDLE: State() {
            override fun update(mech: LinkageSlides, frame: Frame) { }
        }

        data class Manual(val pow: Double) : State() {
            override fun update(mech: LinkageSlides, frame: Frame) {
                mech.motor.power = pow
            }
        }

        data class RunTo(val inches: Double) : State() {
            val controller = PIDFController(LINKAGE_PID, kV, kA, kStatic)

            init {
//                controller.targetPosition = inchesToTicks(inches)
                controller.targetPosition = inches
            }

            override fun update(mech: LinkageSlides, frame: Frame) {
//                val currentPosition = mech.encoder.position.toDouble()
                val currentPosition = ticksToInches(mech.encoder.position.toDouble())
                val power = controller.update(currentPosition)
                mech.motor.power = power
            }
        }
    }

    val machine = StateMachine<LinkageSlides, State>(State.IDLE, this)

    val isBusy: Boolean
        get() = machine.state.let { when (it) {
            State.IDLE, is State.Manual -> false
            is State.RunTo -> it.controller.lastError > 2.0
        } }

    override fun update(frame: Frame) {
        log.out["STATE linkage"] = machine.state
        machine.state.update(this, frame)
    }

    companion object {
        val GEAR_RATIO = (((1.0+(46.0/17.0))) * (1.0+(46.0/11.0))) // 19.2
        val TICKS_PER_REV = 28.0 * GEAR_RATIO

        // lengths of hypothetical arms in a 2-bar linkage
        val LEADER_LENGTH = 17.15199 // inches
        val FOLLOWER_LENGTH = 17.15199 // inches

        val INIT_ANGLE = 80.75 / 180.0 * PI // radians
        val MIN_DIST = 5.5 // inches

        fun ticksToInches(ticks: Double): Double {
            // f is the angle opposite of FOLLOWER, and l is the angle opposite of LEADER
            val f = INIT_ANGLE - ticks / TICKS_PER_REV * 2.0 * PI
            // law of sines: sin(f)/F = sin(l)/L = sin(PI - f - l)/distance
            val l = asin(sin(f) / FOLLOWER_LENGTH * LEADER_LENGTH)
            return FOLLOWER_LENGTH / sin(f) * sin(PI - f - l) - MIN_DIST
        }

        fun inchesToTicks(inches: Double): Double {
            val inches = inches.coerceIn(0.0..28.0) + MIN_DIST

            // law of cosines
            val f = acos((LEADER_LENGTH * LEADER_LENGTH + inches * inches - FOLLOWER_LENGTH * FOLLOWER_LENGTH) / (2.0 * inches * LEADER_LENGTH))

            return (INIT_ANGLE - f) / (2.0 * PI) * TICKS_PER_REV
        }

        @JvmField var LINKAGE_PID = PIDCoefficients(
            0.0,
            0.0,
            0.0
        )
        @JvmField var kV = 0.0
        @JvmField var kA = 0.0
        @JvmField var kStatic = 0.0
    }
}