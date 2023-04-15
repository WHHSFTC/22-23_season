package org.thenuts.powerplay.opmode.auto

import com.acmerobotics.roadrunner.geometry.Pose2d
import com.acmerobotics.roadrunner.geometry.Vector2d
import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import org.thenuts.powerplay.game.Signal
import org.thenuts.powerplay.opmode.commands.TrajectorySequenceCommand
import org.thenuts.powerplay.opmode.commands.go
import org.thenuts.powerplay.subsystems.output.Output
import org.thenuts.powerplay.subsystems.output.VerticalSlides
import org.thenuts.switchboard.command.Command
import org.thenuts.switchboard.dsl.mkSequential
import kotlin.math.PI
import kotlin.time.Duration.Companion.milliseconds

abstract class SideMidAuto(right: Boolean): CyclingAuto(right) {
    override fun generateCommand(): Command {
        val startPose = Pose2d(0.0, 5.0, PI)
        val intakePose = Pose2d(if (right) 49.0 else 50.0, if (right) -25.5 else 24.25, if (right) PI/2.0 else -PI/2.0)
        val samesidePose = Pose2d(if (right) 50.0 else 50.0, if (right) 11.5 else -13.0, if (right) PI else PI)
        val sideMid = Pose2d(if (right) 43.0 else 43.0, if (right) 5.0 else -7.0, if (right) PI * 3.0/4.0 else -PI * 3.0/4.0)

        fun driveToOutput(): Command =
            TrajectorySequenceCommand(bot.drive, intakePose, quickExit = true) {
                setTangent((MIDDLE))
                splineTo(sideMid.vec(), sideMid.heading)
            }
        fun driveToIntake(): Command =
            TrajectorySequenceCommand(bot.drive, sideMid, quickExit = true) {
                setReversed(true)
                splineTo(intakePose.vec(), SIDE)
            }

        val junctions = listOf(
            Junction(VerticalSlides.Height.MID.pos, ::driveToOutput, ::driveToIntake),
            Junction(VerticalSlides.Height.MID.pos, ::driveToOutput, ::driveToIntake),
            Junction(VerticalSlides.Height.MID.pos, ::driveToOutput, ::driveToIntake),
            Junction(VerticalSlides.Height.MID.pos, ::driveToOutput, ::driveToIntake),
            Junction(VerticalSlides.Height.MID.pos, ::driveToOutput, ::driveToIntake),
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
                    go(bot.drive, sideMid, quickExit = true) {
                        setReversed(true)
                        splineTo(Vector2d(51.0, 0.0), SIDE)
                        addDisplacementMarker {
                            bot.output.claw.state = Output.ClawState.CLOSED
                            bot.output.arm.state = Output.ArmState.CLEAR
                        }
                        splineToSplineHeading(Pose2d(51.0, 24.0, -SIDE), PI /2.0)
                    }
                }
                value(Signal.MID) { // MID or null
                    go(bot.drive, sideMid, quickExit = true) {
                        setReversed(true)
                        splineTo(Vector2d(48.0, 0.0), PI)
                        splineTo(Vector2d(35.0, 0.0), PI)
                    }
                }
                value(Signal.RIGHT) {
                    go(bot.drive, sideMid, quickExit = true) {
                        setReversed(true)
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

@Autonomous(preselectTeleOp = "OctoberTele", group = "_official")
class LeftSideMid: SideMidAuto(false)

@Autonomous(preselectTeleOp = "OctoberTele", group = "_official")
class RightSideMid: SideMidAuto(true)