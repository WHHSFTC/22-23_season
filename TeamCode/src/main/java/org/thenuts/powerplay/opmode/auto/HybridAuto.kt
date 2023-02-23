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

abstract class HybridAuto(val right: Boolean) : CommandLinearOpMode<October>(::October, Alliance.RED, Mode.AUTO) {
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
        var passthruPose = pose2d(if (right) 48.5 else 43.0, if (right) 11.0 else -30.0, if (right) PI else -3.0*PI/4.0)
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

            go(bot.drive, startPose, quickExit = true) {
                setTangent(-PI/4.0)
                splineToConstantHeading(vec2d(3.0, offset/2.0), heading(-PI/4.0))
//                back(3.0)
                addDisplacementMarker {
                    bot.output.arm.state = Output.ArmState.MAX_UP
                }
//                if (right) {
//                    strafeRight(24.0 - offset)
//                    back(48.0)
//                } else {
//                    strafeLeft(24.0 + offset)
//                    back(48.0)
//                }
//                strafeLeft(offset)
                splineToConstantHeading(vec2d(8.0, 0.0), heading(0.0))
                splineToConstantHeading(vec2d(52.0, 0.0), heading(0.0))
                addDisplacementMarker {
                    bot.output.lift.state =
                        VerticalSlides.State.RunTo(VerticalSlides.Height.HIGH.pos)
                }
                splineToConstantHeading(samesidePose.vec(), heading(MIDDLE))
            }

            fun samesideOutput(height: Int) {
                task {
                    bot.output.lift.state =
                        VerticalSlides.State.RunTo(height)
                }
                task { bot.output.arm.state = Output.ArmState.CLEAR }
                await { !bot.output.isBusy }
//                delay(1000.milliseconds)
                task { bot.output.arm.state = Output.ArmState.SAMESIDE_OUTPUT }
                delay(200.milliseconds)
//                task {
//                    bot.output.lift.state =
//                        VerticalSlides.State.RunTo(VerticalSlides.Height.MID.pos)
//                }
//                await { !bot.output.lift.isBusy }
                task { bot.output.claw.state = Output.ClawState.WIDE }
                delay(100.milliseconds)
                task { bot.output.arm.state = Output.ArmState.CLEAR }
                delay(200.milliseconds)
            }

            fun passthruOutput(height: Int) {
                task {
                    bot.output.lift.state =
                        VerticalSlides.State.RunTo(height)
                }
                task { bot.output.arm.state = Output.ArmState.PASSTHRU_HOVER }
                await { !bot.output.isBusy }
//                delay(1000.milliseconds)
                delay(300.milliseconds)
//                task {
//                    bot.output.lift.state =
//                        VerticalSlides.State.RunTo(VerticalSlides.Height.MID.pos)
//                }
//                await { !bot.output.lift.isBusy }
                task { bot.output.claw.state = Output.ClawState.OPEN }
//                delay(300.milliseconds)
//                task { bot.output.claw.state = Output.ClawState.CLOSED }
                delay(100.milliseconds)
                task { bot.output.arm.state = Output.ArmState.CLEAR }
                delay(400.milliseconds)
            }

            samesideOutput(VerticalSlides.Height.HIGH.pos)

            repeat(3) { i ->
                val stackHeight = 5 - i

                if (i == 0) {
                    go(bot.drive, samesidePose, quickExit = true) {
                        turnWrap(intakePose.heading - samesidePose.heading)
                    }
                }

                task { bot.output.lift.state = VerticalSlides.State.RunTo(0) }
//                await { !bot.output.isBusy }

                task { bot.output.arm.state = Output.ArmState.values()[stackHeight - 1] }
                task { bot.output.claw.state = Output.ClawState.WIDE }
//                await { !bot.output.isBusy }

                if (i == 0) {
                    go(bot.drive, samesidePose.copy(heading = intakePose.heading), quickExit = true) {
//                        setTangent((passthruPose.heading + PI).angleWrap())
                        strafeTo(intakePose.vec())
                    }
                } else {
                    go(bot.drive, passthruPose, quickExit = true) {
//                        setTangent((passthruPose.heading + PI).angleWrap())
                        setReversed(true)
                        splineTo(intakePose.vec(), heading(SIDE))
                    }
                }

                task { bot.output.claw.state = Output.ClawState.CLOSED }
                delay(300.milliseconds)

                task { bot.output.lift.state = VerticalSlides.State.RunTo(VerticalSlides.Height.ABOVE_STACK.pos) }
                await { !bot.output.lift.isBusy }
                task { bot.output.arm.state = Output.ArmState.CLEAR }


                go(bot.drive, intakePose, quickExit = true) {
//                    lineToLinearHeading(outputPose)
                    setTangent(heading(MIDDLE))
                    splineTo(passthruPose.vec(), passthruPose.heading)
//                    turnWrap(passthruPose.heading - intakePose.heading)
                }

                intakePose += cycleOffset
                passthruPose += cycleOffset
                samesidePose += cycleOffset

                passthruOutput(VerticalSlides.Height.HIGH.pos)
            }

            switch({ bot.vision!!.signal.finalTarget }) {
                value(Signal.LEFT) {
                    go(bot.drive, passthruPose, quickExit = true) {
                        setReversed(true)
                        splineToConstantHeading(vec2d(51.0, 0.0), SIDE)
                        addDisplacementMarker { bot.output.lift.runTo(VerticalSlides.Height.MID.pos) }
                        splineToConstantHeading(vec2d(32.0, 0.0), PI)
                        splineToConstantHeading(vec2d(32.0, 23.0), PI/2.0)
                        splineToConstantHeading(vec2d(35.0, 23.0), 0.0)
                    }
                }
                value(Signal.MID) {
                    go(bot.drive, passthruPose, quickExit = true) {
                        setReversed(true)
                        splineToConstantHeading(vec2d(51.0, 0.0), SIDE)
                        addDisplacementMarker { bot.output.lift.runTo(VerticalSlides.Height.MID.pos) }
                        splineToConstantHeading(vec2d(35.0, 0.0), PI)
                    }
                }
                value(Signal.RIGHT) {
                    go(bot.drive, passthruPose, quickExit = true) {
                        setReversed(true)
                        splineToConstantHeading(vec2d(51.0, 0.0), SIDE)
                        addDisplacementMarker { bot.output.lift.runTo(VerticalSlides.Height.MID.pos) }
                        splineToConstantHeading(vec2d(32.0, 0.0), PI)
                        splineToConstantHeading(vec2d(32.0, -23.0), -PI/2.0)
                        splineToConstantHeading(vec2d(35.0, -23.0), 0.0)
                    }
                }
            }
            task { bot.output.lift.runTo(0) }
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
class LeftHybridAuto : HybridAuto(false)

@Autonomous(preselectTeleOp = "OctoberTele", group = "_official")
class RightHybridAuto : HybridAuto(true)
