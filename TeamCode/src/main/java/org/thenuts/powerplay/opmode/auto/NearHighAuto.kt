package org.thenuts.powerplay.opmode.auto

import com.acmerobotics.roadrunner.geometry.Pose2d
import com.acmerobotics.roadrunner.geometry.Vector2d
import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.Disabled
import org.thenuts.powerplay.game.Signal
import org.thenuts.powerplay.opmode.commands.TrajectorySequenceCommand
import org.thenuts.powerplay.opmode.commands.go
import org.thenuts.powerplay.subsystems.output.Output
import org.thenuts.powerplay.subsystems.output.VerticalSlides
import org.thenuts.switchboard.command.Command
import org.thenuts.switchboard.dsl.mkSequential
import kotlin.math.PI
import kotlin.time.Duration.Companion.milliseconds

abstract class NearHighAuto(right: Boolean): CyclingAuto(right) {
    override fun generateCommand(): Command {
        val startPose = Pose2d(0.0, 5.0, PI)
        val intakePose = Pose2d(if (right) 49.0 else 50.0, if (right) -25.5 else 24.25, if (right) PI /2.0 else -PI /2.0)
        val samesidePose = Pose2d(if (right) 50.0 else 50.0, if (right) 11.5 else -13.0, if (right) PI else PI)
        val nearHigh = Pose2d(if (right) 40.0 else 40.0, if (right) 30.0 else -30.0, if (right) 6.0*PI/8.0 else -6.0*PI/8.0)
        val intermediate = Pose2d(if (right) 50.0 else 50.0, if (right) 12.0 else -12.0, if (right) PI/2.0 else -PI/2.0)

        fun driveToOutput(): Command =
            TrajectorySequenceCommand(bot.drive, intakePose, quickExit = true) {
                setTangent((MIDDLE))
                splineTo(intermediate.vec(), MIDDLE)
                splineTo(nearHigh.vec(), nearHigh.heading)
            }
        fun driveToIntake(): Command =
            TrajectorySequenceCommand(bot.drive, nearHigh, quickExit = true) {
                setReversed(true)
                splineTo(intermediate.vec(), SIDE)
                splineTo(intakePose.vec(), SIDE)
            }

        val junctions = listOf(
            Junction(VerticalSlides.Height.HIGH.pos, ::driveToOutput, ::driveToIntake),
            Junction(VerticalSlides.Height.HIGH.pos, ::driveToOutput, ::driveToIntake),
            Junction(VerticalSlides.Height.HIGH.pos, ::driveToOutput, ::driveToIntake),
            Junction(VerticalSlides.Height.HIGH.pos, ::driveToOutput, ::driveToIntake),
        )

        bot.drive.poseEstimate = startPose

        return mkSequential {
            add(pushSignal(startPose, samesidePose, VerticalSlides.Height.HIGH.pos))
            add(samesideOutput(VerticalSlides.Height.HIGH.pos))
            add(samesideToStack(samesidePose, intakePose))

            add(cycle(5, junctions))

            task { bot.output.arm.state = Output.ArmState.INTAKE }
            task { bot.output.lift.runTo(0) }

            switch({ bot.vision?.signal?.finalTarget ?: Signal.MID }) {
                value(Signal.LEFT) {
                    go(bot.drive, nearHigh, quickExit = true) {
                        setReversed(true)
                        if (!right)
                            splineTo(Vector2d(51.0, 0.0), SIDE)
                        addDisplacementMarker {
                            bot.output.claw.state = Output.ClawState.CLOSED
                            bot.output.arm.state = Output.ArmState.CLEAR
                        }
                        splineToSplineHeading(Pose2d(51.0, 24.0, -SIDE), PI /2.0)
                    }
                }
                value(Signal.MID) { // MID or null
                    go(bot.drive, nearHigh, quickExit = true) {
                        setReversed(true)
                        splineTo(Vector2d(48.0, 0.0), SIDE)
                        splineTo(Vector2d(35.0, 0.0), PI)
                    }
                }
                value(Signal.RIGHT) {
                    go(bot.drive, nearHigh, quickExit = true) {
                        setReversed(true)
                        if (right)
                            splineTo(Vector2d(51.0, 0.0), SIDE)
                        addDisplacementMarker {
                            bot.output.claw.state = Output.ClawState.CLOSED
                            bot.output.arm.state = Output.ArmState.CLEAR
                        }
                        splineToSplineHeading(Pose2d(51.0, -24.0, -SIDE), -PI /2.0)
                    }
                }
            }
            delay(1000.milliseconds)
            task { stop() }
        }
    }
}

@Disabled
@Autonomous(preselectTeleOp = "OctoberTele", group = "_official")
class LeftNearHigh: NearHighAuto(false)

@Disabled
@Autonomous(preselectTeleOp = "OctoberTele", group = "_official")
class RightNearHigh: NearHighAuto(true)
