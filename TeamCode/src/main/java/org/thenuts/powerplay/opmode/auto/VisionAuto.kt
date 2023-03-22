package org.thenuts.powerplay.opmode.auto

import com.acmerobotics.roadrunner.geometry.Pose2d
import com.acmerobotics.roadrunner.geometry.Vector2d
import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.Disabled
import org.thenuts.powerplay.game.Alliance
import org.thenuts.powerplay.game.Mode
import org.thenuts.powerplay.game.Signal
import org.thenuts.powerplay.opmode.CommandLinearOpMode
import org.thenuts.powerplay.opmode.commands.go
import org.thenuts.powerplay.subsystems.output.VerticalSlides
import org.thenuts.powerplay.subsystems.October
import org.thenuts.powerplay.subsystems.output.Output
import org.thenuts.switchboard.command.Command
import org.thenuts.switchboard.command.CommandScheduler
import org.thenuts.switchboard.dsl.mkSequential
import kotlin.math.PI
import kotlin.time.Duration.Companion.milliseconds

abstract class VisionAuto(val right: Boolean) : CommandLinearOpMode<October>(::October, Alliance.RED, Mode.AUTO) {
    lateinit var cmd: Command

    override fun postInitHook() {
        bot.vision?.front?.startDebug()
        bot.vision?.signal?.enable()
        bot.vision?.gamepad = gamepad1

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

        fun heading(heading: Double): Double {
            if (REFERENCE)
                return heading + reference.heading
            else return heading
        }

        val offset = 5.0
        val startPose = pose2d(0.0, offset, PI)
        var intakePose = pose2d(if (right) 51.5 else 50.0, if (right) -20.25 else 21.5, if (right) PI/2.0 else -PI/2.0)
        var samesidePose = pose2d(if (right) 48.5 else 48.5, if (right) 11.0 else -12.75, if (right) PI else PI)
        var passthruPose = pose2d(if (right) 48.5 else 57.0, if (right) 11.0 else -6.0, if (right) PI else -PI/4.0)
        val MIDDLE = if (right) PI/2.0 else -PI/2.0
        val SIDE = if (right) -PI/2.0 else PI/2.0
        val cycleOffset = if (right)
            pose2d(0.0, 0.0, 0.0)
        else
            pose2d(-0.75, 0.0, 0.0)

        bot.drive.poseEstimate = startPose
        cmd = mkSequential {
//            add(ExtendCommand(bot.output, scoreHeight, Output.OutputSide.SAMESIDE))
            task { bot.output.claw.state = Output.ClawState.CLOSED }
//            delay(500.milliseconds)

            switch({ bot.vision!!.signal.finalTarget }) {
                value(Signal.LEFT) {
                    go(bot.drive, startPose, quickExit = true) {
                        back(4.0)
                        strafeLeft(6.0)
                        back(24.0)
                        strafeRight(24.0)
                    }
                }
                value(Signal.MID) {
                    go(bot.drive, startPose, quickExit = true) {
                        back(4.0)
                        strafeLeft(6.0)
                        back(24.0)
                    }
                }
                value(Signal.RIGHT) {
                    go(bot.drive, startPose, quickExit = true) {
                        back(4.0)
                        strafeLeft(6.0)
                        back(24.0)
                        strafeLeft(24.0)
                    }
                }
            }
            delay(1000.milliseconds)
            task { stop() }
        }
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
        sched.addCommand(cmd)
    }

    override fun loopHook() {
        log.out["COMMAND ORDER"] = sched.nodes.filterIsInstance<CommandScheduler.CommandNode>().map { it.runner.cmd::class.simpleName }
    }

    companion object {
        @JvmField var REFERENCE = false
    }
}

@Autonomous(preselectTeleOp = "OctoberTele", group = "_official")
class TheVisionAuto : VisionAuto(false)
