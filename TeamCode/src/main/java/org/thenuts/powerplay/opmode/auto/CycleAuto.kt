package org.thenuts.powerplay.opmode.auto

import com.acmerobotics.roadrunner.geometry.Pose2d
import com.acmerobotics.roadrunner.geometry.Vector2d
import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import org.thenuts.powerplay.acme.drive.DriveConstants
import org.thenuts.powerplay.acme.trajectorysequence.TrajectorySequenceBuilder
import org.thenuts.powerplay.game.Alliance
import org.thenuts.powerplay.game.Mode
import org.thenuts.powerplay.game.Signal
import org.thenuts.powerplay.opmode.CommandLinearOpMode
import org.thenuts.powerplay.opmode.commands.OutputCommand
import org.thenuts.powerplay.opmode.commands.go
import org.thenuts.powerplay.subsystems.output.VerticalSlides
import org.thenuts.powerplay.subsystems.October
import org.thenuts.powerplay.subsystems.intake.LinkageSlides
import org.thenuts.powerplay.subsystems.output.Output
import org.thenuts.powerplay.subsystems.output.VerticalSlides.Companion.CONE_STEP
import org.thenuts.switchboard.command.Command
import org.thenuts.switchboard.command.CommandScheduler
import org.thenuts.switchboard.command.combinator.SlotCommand
import org.thenuts.switchboard.dsl.mkSequential
import kotlin.math.PI
import kotlin.time.Duration.Companion.milliseconds

abstract class CycleAuto(val right: Boolean) : CommandLinearOpMode<October>(::October, Alliance.RED, Mode.AUTO) {
    lateinit var cmd: Command

    override fun postInitHook() {
        bot.vision?.front?.startDebug()
        bot.vision?.signal?.enable()
        bot.vision?.gamepad = gamepad1
    }

    override fun stopHook() {
        bot.vision?.front?.stopDebug()
        bot.vision?.signal?.disable()
    }

    override fun initLoopHook() {
        log.out["signal"] = bot.vision?.signal?.finalTarget
    }

    override fun postStartHook() {
        bot.vision?.front?.stopDebug()
        bot.vision?.signal?.disable()
        bot.vision?.gamepad = null

        val reference = Pose2d(if (right) -36.0 else 36.0, 72.0, -PI/2.0)

        fun vec2d(x: Double, y: Double): Vector2d {
            if (REFERENCE)
                return Vector2d(x, y).rotated(reference.heading) + reference.vec()
            else return Vector2d(x, y)
        }

        fun pose2d(x: Double, y: Double, heading: Double): Pose2d {
            if (REFERENCE)
                return Pose2d(vec2d(x, y), heading + reference.heading)
            else return Pose2d(x, y, heading)
        }

        val offset = 5.0
        val startPose = pose2d(0.0, offset, PI)
        val intakePose = pose2d(52.5, if (right) -19.0 else 19.0, if (right) PI/2.0 else -PI/2.0)
        val outputPose = pose2d(if (right) 48.5 else 47.5, if (right) 8.0 else -10.0, if (right) PI else PI)

        bot.drive.poseEstimate = startPose
        cmd = mkSequential {
//            add(ExtendCommand(bot.output, scoreHeight, Output.OutputSide.SAMESIDE))
            task { bot.output.claw.state = Output.ClawState.CLOSED }
            delay(500.milliseconds)
            task { bot.output.arm.state = Output.ArmState.MAX_UP }
//            delay(500.milliseconds)

            go(bot.drive, startPose) {
                back(3.0)
                if (right) {
                    strafeRight(24.0 - offset)
                    back(48.0)
                } else {
                    strafeLeft(24.0 + offset)
                    back(48.0)
                }
                strafeTo(outputPose.vec())
            }

            fun output(height: Int) {
                task {
                    bot.output.lift.state =
                        VerticalSlides.State.RunTo(VerticalSlides.Height.HIGH.pos)
                }
                await { !bot.output.lift.isBusy }
//                delay(1000.milliseconds)
                task { bot.output.arm.state = Output.ArmState.SAMESIDE_OUTPUT }
                delay(1000.milliseconds)
                task {
                    bot.output.lift.state =
                        VerticalSlides.State.RunTo(VerticalSlides.Height.MID.pos)
                }
                await { !bot.output.lift.isBusy }
                task { bot.output.claw.state = Output.ClawState.OPEN }
                delay(200.milliseconds)
                task { bot.output.arm.state = Output.ArmState.MAX_UP }
                delay(1000.milliseconds)
                task { bot.output.lift.state = VerticalSlides.State.RunTo(0) }
            }

            output(VerticalSlides.Height.HIGH.pos)

            repeat(0) { i ->
                val stackHeight = 5 - i

                go(bot.drive, outputPose) {
                    turnWrap(intakePose.heading - outputPose.heading)
                }

                task { bot.output.arm.state = Output.ArmState.values()[stackHeight - 1] }

                await { !bot.output.isBusy }

                go(bot.drive, outputPose.copy(heading = intakePose.heading)) {
                    lineTo(intakePose.vec())
                }

                task { bot.output.claw.state = Output.ClawState.CLOSED }
                delay(500.milliseconds)

                task { bot.output.lift.state = VerticalSlides.State.RunTo(VerticalSlides.Height.MID.pos) }
                await { !bot.output.lift.isBusy }

                task { bot.output.arm.state = Output.ArmState.MAX_UP }
//                delay(1000.milliseconds)

                go(bot.drive, intakePose) {
                    lineTo(outputPose.vec())
                    turnWrap(outputPose.heading - intakePose.heading)
                }

                output(VerticalSlides.Height.HIGH.pos)
            }

            switch({ bot.vision!!.signal.finalTarget }) {
                value(Signal.LEFT) {
                    go(bot.drive, outputPose) {
                        strafeTo(vec2d(51.0, 0.0))
                        forward(24.0)
                        strafeRight(24.0)
                    }
                }
                value(Signal.MID) {
                    go(bot.drive, outputPose) {
                        strafeTo(vec2d(51.0, 0.0))
                        forward(12.0)
                    }
//                    go(bot.drive, Pose2d(26.0, 0.0, PI)) {
//                        turnWrap(-PI)
//                    }
                }
                value(Signal.RIGHT) {
                    go(bot.drive, outputPose) {
                        strafeTo(vec2d(51.0, 0.0))
                        forward(24.0)
                        strafeLeft(24.0)
                    }
                }
            }
            delay(1000.milliseconds)
            task { stop() }
        }
        sched.addCommand(cmd)
    }

    override fun loopHook() {
        log.out["COMMAND ORDER"] = sched.nodes.filterIsInstance<CommandScheduler.CommandNode>().map { it.runner.cmd::class.simpleName }
    }

    companion object {
        @JvmField var REFERENCE = false
    }
}

fun Double.angleWrap(): Double {
    var x = this;
    while (x >= PI) {
        x -= 2 * PI
    }
    while (x <= -PI) {
        x += 2 * PI
    }
    return x
}

fun TrajectorySequenceBuilder.turnWrap(angle: Double) = turn(angle.angleWrap())

@Autonomous(preselectTeleOp = "OctoberTele", group = "_official")
class LeftCycleAuto : CycleAuto(false)

@Autonomous(preselectTeleOp = "OctoberTele", group = "_official")
class RightCycleAuto : CycleAuto(true)
