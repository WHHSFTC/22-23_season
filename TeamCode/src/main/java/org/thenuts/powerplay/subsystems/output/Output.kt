package org.thenuts.powerplay.subsystems.output

import org.thenuts.powerplay.annotations.DiffField
import org.thenuts.powerplay.subsystems.util.StatefulServo
import org.thenuts.powerplay.subsystems.Subsystem
import org.thenuts.switchboard.core.Logger
import org.thenuts.switchboard.hardware.Configuration
import org.thenuts.switchboard.hardware.HardwareOutput
import kotlin.math.max
import kotlin.time.Duration.Companion.seconds

class Output(val log: Logger, config: Configuration) : Subsystem {
    enum class LiftState {
        INTAKE, CLEAR, OUTPUT
    }

    enum class ExtensionState(override val pos: Double) : StatefulServo.ServoPosition {
        OUTPUT(0.01), INTAKE(0.53)
    }

    enum class WristState(override val pos: Double) : StatefulServo.ServoPosition {
        OUTPUT(0.02), INTAKE(0.68)
    }

    enum class ClawState(override val pos: Double) : StatefulServo.ServoPosition {
        WIDE(0.61), OPEN(0.61), CLOSED(0.5)
    }

    enum class OutputSide {
        SAMESIDE, PASSTHRU
    }

    enum class ManipulatorState(
        @DiffField val lift: LiftState,
        @DiffField val extension: ExtensionState,
        @DiffField val wrist: WristState,
        @DiffField val claw: ClawState,
    ) {
        FRONT_INTAKE(LiftState.INTAKE, ExtensionState.INTAKE, WristState.INTAKE, ClawState.OPEN),
        FRONT_CLOSED(LiftState.INTAKE, ExtensionState.INTAKE, WristState.INTAKE, ClawState.CLOSED),

        FRONT_CLEAR(LiftState.CLEAR, ExtensionState.INTAKE, WristState.INTAKE, ClawState.CLOSED),
        FRONT_TWIST(LiftState.CLEAR, ExtensionState.INTAKE, WristState.OUTPUT, ClawState.CLOSED),
        BACK_CLEAR(LiftState.CLEAR, ExtensionState.OUTPUT, WristState.OUTPUT, ClawState.CLOSED),

        BACK_OUTPUT(LiftState.OUTPUT, ExtensionState.OUTPUT, WristState.OUTPUT, ClawState.CLOSED),
        BACK_DROP(LiftState.OUTPUT, ExtensionState.OUTPUT, WristState.OUTPUT, ClawState.WIDE);
//
//        BACK_CLOSED(LiftState.INTAKE, ExtensionState.BACK, WristState.BACK, ClawState.CLOSED),
//        BACK_INTAKE(LiftState.INTAKE, ExtensionState.BACK, WristState.BACK, ClawState.OPEN);

//        fun diff(that: ManipulatorState): Int {
////            val fields = this::class.members.filterIsInstance<KProperty<Any?>>()
////                .filter { member -> member.annotations.find { it is DiffField } != null }
////                .map { { state: ManipulatorState -> it.getter.call(state) } }
//            val fields = listOf(ManipulatorState::lift, ManipulatorState::extension, ManipulatorState::wrist, ManipulatorState::claw)
//            return fields.count { f -> f(this) != f(that) }
//        }
//
//        fun isLegal(that: ManipulatorState): Boolean = diff(that) == 1
        fun isLegal(that: ManipulatorState): Boolean {
            val moves: List<ManipulatorState> = when (this) {
                FRONT_INTAKE -> listOf(FRONT_CLOSED)
                FRONT_CLOSED -> listOf(FRONT_INTAKE, FRONT_CLEAR)
                FRONT_CLEAR -> listOf(FRONT_CLOSED, FRONT_TWIST)
                FRONT_TWIST -> listOf(FRONT_CLEAR, BACK_CLEAR)
                BACK_CLEAR -> listOf(FRONT_TWIST, BACK_OUTPUT)
                BACK_OUTPUT -> listOf(BACK_CLEAR, BACK_DROP)
                BACK_DROP -> listOf(BACK_OUTPUT)
            }
            return that in moves
        }
    }

    var outputHeight = VerticalSlides.Height.HIGH

    val lift = VerticalSlides(log, config)
    val extension = StatefulServo(config.servos["extension"], ExtensionState.INTAKE)
    val wrist = StatefulServo(config.servos["wrist"], WristState.INTAKE)
    val claw = StatefulServo(config.servos["claw"], ClawState.CLOSED)

    override val outputs: List<HardwareOutput> = listOf(extension, wrist, claw) + lift.outputs

    fun translate(our: LiftState): VerticalSlides.State = when (our) {
        LiftState.INTAKE -> {
            VerticalSlides.State.ZERO
        }
        LiftState.CLEAR -> {
            VerticalSlides.State.RunTo(max(outputHeight.pos, VerticalSlides.Height.MIN_CLEAR.pos))
        }
        LiftState.OUTPUT -> {
            VerticalSlides.State.RunTo(outputHeight.pos)
        }
    }

    var state: ManipulatorState = ManipulatorState.FRONT_CLOSED
        private set(value) {
            if (field.lift != value.lift)
                lift.state = translate(value.lift)
            extension.state = value.extension
            wrist.state = value.wrist
            claw.state = value.claw
            field = value
        }

    fun attempt(newState: ManipulatorState): Boolean {
        if (lift.isBusy || extension.isBusy || wrist.isBusy || claw.isBusy) {
            log.addMessage("busy!", 10.seconds)
            return false
        }

        if (!state.isLegal(newState)) {
            log.addMessage("illegal!", 10.seconds)
            return false
        }

        state = newState
        return true
    }

    fun prev() {
        ManipulatorState.values().getOrNull(state.ordinal - 1)?.let { attempt(it) }
    }

    fun next() {
        ManipulatorState.values().getOrNull(state.ordinal + 1)?.let { attempt(it) }
    }
}