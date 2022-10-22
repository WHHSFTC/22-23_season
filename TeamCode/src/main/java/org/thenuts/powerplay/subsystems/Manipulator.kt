package org.thenuts.powerplay.subsystems

import org.thenuts.powerplay.annotations.DiffField
import org.thenuts.switchboard.core.Logger
import org.thenuts.switchboard.hardware.Configuration
import org.thenuts.switchboard.hardware.HardwareOutput
import kotlin.math.max
import kotlin.reflect.KProperty
import kotlin.time.Duration.Companion.seconds

class Manipulator(val log: Logger, config: Configuration) : Subsystem {
    enum class LiftState {
        INTAKE, CLEAR, OUTPUT
    }

    enum class ExtensionState(override val pos: Double) : StatefulServo.ServoPosition {
        BACK(0.01), FRONT(0.53)
    }

    enum class WristState(override val pos: Double) : StatefulServo.ServoPosition {
        BACK(0.85), FRONT(0.19)
    }

    enum class ClawState(override val pos: Double) : StatefulServo.ServoPosition {
        WIDE(0.65), OPEN(0.65), CLOSED(0.5)
    }

    enum class ManipulatorState(
        @DiffField val lift: LiftState,
        @DiffField val extension: ExtensionState,
        @DiffField val wrist: WristState,
        @DiffField val claw: ClawState,
    ) {
        FRONT_INTAKE(LiftState.INTAKE, ExtensionState.FRONT, WristState.FRONT, ClawState.OPEN),
        FRONT_CLOSED(LiftState.INTAKE, ExtensionState.FRONT, WristState.FRONT, ClawState.CLOSED),

        FRONT_CLEAR(LiftState.CLEAR, ExtensionState.FRONT, WristState.FRONT, ClawState.CLOSED),
        FRONT_TWIST(LiftState.CLEAR, ExtensionState.FRONT, WristState.BACK, ClawState.CLOSED),
        BACK_CLEAR(LiftState.CLEAR, ExtensionState.BACK, WristState.BACK, ClawState.CLOSED),

        BACK_OUTPUT(LiftState.OUTPUT, ExtensionState.BACK, WristState.BACK, ClawState.CLOSED),
        BACK_DROP(LiftState.OUTPUT, ExtensionState.BACK, WristState.BACK, ClawState.WIDE);
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

    var outputHeight = Lift.Height.HIGH

    val lift = Lift(log, config)
    val extension = StatefulServo(config.servos["extension"], ExtensionState.FRONT)
    val wrist = StatefulServo(config.servos["wrist"], WristState.FRONT)
    val claw = StatefulServo(config.servos["claw"], ClawState.CLOSED)

    override val outputs: List<HardwareOutput> = listOf(extension, wrist, claw) + lift.outputs

    fun translate(our: LiftState): Lift.State = when (our) {
        LiftState.INTAKE -> {
            Lift.State.ZERO
        }
        LiftState.CLEAR -> {
            Lift.State.RunTo(max(outputHeight.pos, Lift.Height.MIN_CLEAR.pos))
        }
        LiftState.OUTPUT -> {
            Lift.State.RunTo(outputHeight.pos)
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