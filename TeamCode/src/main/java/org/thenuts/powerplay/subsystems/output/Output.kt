package org.thenuts.powerplay.subsystems.output

import com.acmerobotics.dashboard.config.Config
import com.qualcomm.robotcore.hardware.PwmControl
import com.qualcomm.robotcore.hardware.ServoImplEx
import org.thenuts.powerplay.annotations.DiffField
import org.thenuts.powerplay.subsystems.util.StatefulServo
import org.thenuts.powerplay.subsystems.Subsystem
import org.thenuts.powerplay.subsystems.util.LinkedServos
import org.thenuts.powerplay.subsystems.util.TrapezoidalServo
import org.thenuts.switchboard.command.Command
import org.thenuts.switchboard.core.Logger
import org.thenuts.switchboard.dsl.mkSequential
import org.thenuts.switchboard.hardware.Configuration
import org.thenuts.switchboard.hardware.HardwareOutput
import org.thenuts.switchboard.hardware.ServoImpl
import org.thenuts.switchboard.util.Frame
import java.util.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.time.Duration.Companion.seconds

@Config
class Output(val log: Logger, config: Configuration) : Subsystem {
    enum class ArmState(override val pos: Double) : StatefulServo.ServoPosition {
        // first five positions in order for stack intake
        INTAKE(1.0), TWO(0.972), THREE(0.944), FOUR(0.916), FIVE(0.888),
        GROUND(0.972),
        PASSTHRU_OUTPUT(0.23), PASSTHRU_HOVER(0.35),
        CLEAR(0.55),
        SAMESIDE_HOVER(0.68), SAMESIDE_OUTPUT(0.84),
        MAX_UP(0.60),
        PARK(0.45),
        HORIZONTAL(0.85);

        fun offset(): Double = cos((pos - HORIZONTAL.pos) * SERVO_RANGE) * ARM_LENGTH

        companion object {
            val SERVO_RANGE = 300.0/180.0 * PI
            val ARM_LENGTH = 11.12205
        }
    }

    enum class ClawState(override val pos: Double) : StatefulServo.ServoPosition {
        WIDE(0.88), NARROW(0.73), CLOSED(0.55), JUNCTION(0.74), INIT(0.93)
    }

    enum class LiftState {
        INTAKE, CLEAR, OUTPUT
    }

    enum class OutputSide(vararg states: OutputState) {
        SAMESIDE(
            OutputState.GROUND,
            OutputState.INTAKE,
            OutputState.CLEAR,
            OutputState.S_HOVER,
            OutputState.S_LOWER,
            OutputState.S_DROP
        ),

        PASSTHRU(
            OutputState.GROUND,
            OutputState.INTAKE,
            OutputState.CLEAR,
            OutputState.P_HOVER,
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

        CLEAR_LOW(LiftState.INTAKE, ArmState.CLEAR, ClawState.CLOSED),

        CLEAR(LiftState.CLEAR, ArmState.CLEAR, ClawState.CLOSED),
        CLEAR_OPEN(LiftState.CLEAR, ArmState.CLEAR, ClawState.NARROW),

        S_HOVER(LiftState.OUTPUT, ArmState.SAMESIDE_HOVER, ClawState.CLOSED),
        S_LOWER(LiftState.OUTPUT, ArmState.SAMESIDE_OUTPUT, ClawState.CLOSED),
        S_DROP(LiftState.OUTPUT, ArmState.SAMESIDE_OUTPUT, ClawState.WIDE),

        P_HOVER(LiftState.OUTPUT, ArmState.PASSTHRU_HOVER, ClawState.CLOSED),
        P_LOWER(LiftState.OUTPUT, ArmState.PASSTHRU_OUTPUT, ClawState.CLOSED),
        P_DROP(LiftState.OUTPUT, ArmState.PASSTHRU_OUTPUT, ClawState.NARROW),
        P_JUNCTION(LiftState.OUTPUT, ArmState.PASSTHRU_OUTPUT, ClawState.JUNCTION);
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
                INTAKE -> listOf(GROUND, CLEAR_LOW)

                CLEAR_LOW -> listOf(INTAKE, CLEAR)

                CLEAR -> listOf(CLEAR_LOW, S_HOVER, P_HOVER, CLEAR_OPEN)
                CLEAR_OPEN -> listOf(CLEAR, CLEAR_LOW)

                S_HOVER -> listOf(CLEAR, S_LOWER, INTAKE)
                S_LOWER -> listOf(S_HOVER, S_DROP, INTAKE)
                S_DROP -> listOf(CLEAR_OPEN, CLEAR, S_LOWER)

                P_HOVER -> listOf(CLEAR, P_LOWER, INTAKE)
                P_LOWER -> listOf(P_HOVER, P_DROP, INTAKE)
                P_DROP -> listOf(P_LOWER, P_JUNCTION)
                P_JUNCTION -> listOf(CLEAR_OPEN)
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
    val claw = StatefulServo(config.servos["claw_output"], ClawState.INIT)

    val leftArm = config.servos["left_output"].also {
        ((it as? ServoImpl)?.s as? ServoImplEx)?.pwmRange =PwmControl.PwmRange(500.0,2500.0)
    }
    val rightArm = config.servos["right_output"].also {
        ((it as? ServoImpl)?.s as? ServoImplEx)?.pwmRange =PwmControl.PwmRange(500.0,2500.0)
    }

    val linkedServos = LinkedServos(leftArm, rightArm, { LEFT_BACK to RIGHT_BACK } , { LEFT_DOWN to RIGHT_DOWN }).also {
        it.position = ArmState.INTAKE.pos
    }
    val profiledServo = TrapezoidalServo(linkedServos, { MAX_VEL }, { MAX_ACCEL })
    val arm = StatefulServo(profiledServo, ArmState.INTAKE)

    override val outputs: List<HardwareOutput> = listOf(claw, leftArm, rightArm) + lift.outputs

    override fun update(frame: Frame) {
        log.out["arm target"] = profiledServo.profile?.get(profiledServo.time())?.x
        log.out["arm position"] = profiledServo.servo.position
    }

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
        get() = lift.isBusy || profiledServo.isBusy // || claw.isBusy

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
        // goBILDA Torque
//        @JvmField var LEFT_BACK = 0.00
//        @JvmField var LEFT_DOWN = 1.0
//        @JvmField var RIGHT_BACK = 1.0
//        @JvmField var RIGHT_DOWN = 0.00

        @JvmField var LEFT_BACK = 1.0
        @JvmField var LEFT_DOWN = 0.18
        @JvmField var RIGHT_BACK = 0.0
        @JvmField var RIGHT_DOWN = 0.82
        @JvmField var STEP = 0.028

//        @JvmField var MAX_ACCEL = 5.0
//        @JvmField var MAX_VEL = 15.0

        @JvmField var MAX_ACCEL = 2.5
        @JvmField var MAX_VEL = 2.0
    }
}