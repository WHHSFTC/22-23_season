package org.thenuts.powerplay.subsystems.output

import com.acmerobotics.dashboard.config.Config
import org.thenuts.powerplay.annotations.DiffField
import org.thenuts.powerplay.subsystems.util.StatefulServo
import org.thenuts.powerplay.subsystems.Subsystem
import org.thenuts.powerplay.subsystems.util.LinkedServos
import org.thenuts.switchboard.command.Command
import org.thenuts.switchboard.core.Logger
import org.thenuts.switchboard.dsl.mkSequential
import org.thenuts.switchboard.hardware.Configuration
import org.thenuts.switchboard.hardware.HardwareOutput
import java.util.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.time.Duration.Companion.seconds

@Config
class Output(val log: Logger, config: Configuration) : Subsystem {
    enum class ArmState(override val pos: Double) : StatefulServo.ServoPosition {
        INTAKE(0.66), TWO(0.60), THREE(0.60), FOUR(0.60), FIVE(0.60),
        PASSTHRU_OUTPUT(0.43), CLEAR(0.18), SAMESIDE_OUTPUT(0.43),
        MAX_UP(0.1),
        HORIZONTAL(0.6);

        fun offset(): Double = cos((pos - HORIZONTAL.pos) * SERVO_RANGE) * ARM_LENGTH

        companion object {
            val SERVO_RANGE = 160.0/180.0 * PI
            val ARM_LENGTH = 11.12205
        }
    }

    enum class ClawState(override val pos: Double) : StatefulServo.ServoPosition {
        WIDE(1.0), OPEN(1.0), CLOSED(0.5)
    }

    enum class LiftState {
        INTAKE, CLEAR, OUTPUT
    }

    enum class OutputSide(vararg states: OutputState) {
        SAMESIDE(
            OutputState.GROUND,
            OutputState.INTAKE,
            OutputState.CLEAR,
            OutputState.S_OUTPUT,
            OutputState.S_LOWER,
            OutputState.S_DROP
        ),

        PASSTHRU(
            OutputState.GROUND,
            OutputState.INTAKE,
            OutputState.CLEAR,
            OutputState.P_OUTPUT,
            OutputState.P_LOWER,
            OutputState.P_DROP
        );

        val states = states.toList()
    }

    enum class OutputState(
        @DiffField val lift: LiftState,
        @DiffField val arm: ArmState,
        @DiffField val claw: ClawState,
    ) {
        GROUND(LiftState.INTAKE, ArmState.INTAKE, ClawState.WIDE),
        INTAKE(LiftState.INTAKE, ArmState.INTAKE, ClawState.CLOSED),

        CLEAR(LiftState.CLEAR, ArmState.CLEAR, ClawState.CLOSED),
        CLEAR_OPEN(LiftState.CLEAR, ArmState.CLEAR, ClawState.OPEN),

        S_OUTPUT(LiftState.OUTPUT, ArmState.CLEAR, ClawState.CLOSED),
        S_LOWER(LiftState.OUTPUT, ArmState.SAMESIDE_OUTPUT, ClawState.CLOSED),
        S_DROP(LiftState.OUTPUT, ArmState.SAMESIDE_OUTPUT, ClawState.OPEN),

        P_OUTPUT(LiftState.OUTPUT, ArmState.CLEAR, ClawState.CLOSED),
        P_LOWER(LiftState.OUTPUT, ArmState.PASSTHRU_OUTPUT, ClawState.CLOSED),
        P_DROP(LiftState.OUTPUT, ArmState.PASSTHRU_OUTPUT, ClawState.OPEN);
//
//        BACK_CLOSED(LiftState.INTAKE, ExtensionState.BACK, ClawState.CLOSED),
//        BACK_INTAKE(LiftState.INTAKE, ExtensionState.BACK, ClawState.OPEN);

//        fun diff(that: ManipulatorState): Int {
////            val fields = this::class.members.filterIsInstance<KProperty<Any?>>()
////                .filter { member -> member.annotations.find { it is DiffField } != null }
////                .map { { state: ManipulatorState -> it.getter.call(state) } }
//            val fields = listOf(ManipulatorState::lift, ManipulatorState::extension, ManipulatorState::wrist, ManipulatorState::claw)
//            return fields.count { f -> f(this) != f(that) }
//        }
//
//        fun isLegal(that: ManipulatorState): Boolean = diff(that) == 1


        // return legal moves in order of increasing cost
        fun legalMoves(): List<OutputState> {
            return when (this) {
                GROUND -> listOf(INTAKE)
                INTAKE -> listOf(GROUND, CLEAR, S_OUTPUT, P_OUTPUT)

                CLEAR -> listOf(INTAKE, S_OUTPUT, P_OUTPUT, CLEAR_OPEN)
                CLEAR_OPEN -> listOf(CLEAR)

                S_OUTPUT -> listOf(CLEAR, S_LOWER, INTAKE)
                S_LOWER -> listOf(S_OUTPUT, S_DROP, INTAKE)
                S_DROP -> listOf(S_LOWER)

                P_OUTPUT -> listOf(CLEAR, P_LOWER, INTAKE)
                P_LOWER -> listOf(P_OUTPUT, P_DROP, INTAKE)
                P_DROP -> listOf(P_LOWER)
            }
        }

        fun bfs(target: OutputState): List<OutputState>? {
            if (this == target)
                return listOf()

            val seen = mutableSetOf(this)
            val stems = LinkedList<List<OutputState>>()
            stems.add(listOf(this))

            while (stems.isNotEmpty()) {
                val stem = stems.pop()
                val end = stem.last()
                if (end == target)
                    return stem.subList(1, stem.size)
                end.legalMoves().filter { it !in seen }.forEach {
                    seen.add(it)
                    stems.add(stem + it)
                }
            }

            return null
        }
    }

    var outputSide = OutputSide.PASSTHRU
    var outputHeight = VerticalSlides.Height.HIGH.pos
        set(value) {
            field = value
            if (state.lift != LiftState.INTAKE) {
                lift.state = translate(state.lift)
            }
        }

    val lift = VerticalSlides(log, config)
    val claw = StatefulServo(config.servos["claw_output"], ClawState.WIDE)

    val leftArm = config.servos["left_output"]
    val rightArm = config.servos["right_output"]

    val arm = StatefulServo(LinkedServos(leftArm, rightArm, { LEFT_BACK to RIGHT_BACK } , { LEFT_DOWN to RIGHT_DOWN }), ArmState.INTAKE)

    override val outputs: List<HardwareOutput> = listOf(claw, leftArm, rightArm) + lift.outputs

    fun translate(our: LiftState): VerticalSlides.State = when (our) {
        LiftState.INTAKE -> {
//            VerticalSlides.State.ZERO
            VerticalSlides.State.RunTo(0)
        }
        LiftState.CLEAR -> {
            VerticalSlides.State.RunTo(max(outputHeight, VerticalSlides.Height.MIN_CLEAR.pos))
        }
        LiftState.OUTPUT -> {
            VerticalSlides.State.RunTo(outputHeight)
        }
    }

    var state: OutputState = OutputState.GROUND
        private set(value) {
            if (field.lift != value.lift)
                lift.state = translate(value.lift)
            arm.state = value.arm
            claw.state = value.claw
            field = value
        }

    val isBusy: Boolean
        get() = lift.isBusy || arm.isBusy // || claw.isBusy

    fun transitionCommand(from: OutputState, to: OutputState): Command {
        val path = from.bfs(to)!!
        return mkSequential {
            for (step in path) {
                task { state = step }
                await { !isBusy }
            }
        }
    }

    fun attempt(newState: OutputState): Boolean {
        if (isBusy) {
            log.addMessage("busy!", 10.seconds)
            return false
        }

//        if (!state.isLegal(newState)) {
//            log.addMessage("illegal!", 10.seconds)
//            return false
//        }

        state = newState
        return true
    }

    fun prev() {
        if (state in outputSide.states)
            outputSide.states.getOrNull(outputSide.states.indexOf(state) - 1)?.let { attempt(it) }
    }

    fun next() {
        if (state in outputSide.states)
            outputSide.states.getOrNull(outputSide.states.indexOf(state) + 1)?.let { attempt(it) }
    }

    companion object {
        @JvmField var LEFT_BACK = 0.0
        @JvmField var LEFT_DOWN = 0.96
        @JvmField var RIGHT_BACK = 0.96
        @JvmField var RIGHT_DOWN = 0.0

        @JvmField var STEP = 0.05
    }
}