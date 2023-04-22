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

abstract class SideHighAuto(right: Boolean, val tape: Boolean, val n: Int): CyclingAuto(right) {
    override fun generateCommand(): Command {
        val startPose = Pose2d(0.0, 5.0, PI)
        val intakePose = Pose2d(if (right) 47.5 else 51.0, if (right) -26.0 else 23.75, if (right) PI /2.0 else -PI /2.0)
        val samesidePose = Pose2d(if (right) 50.0 else 49.0, if (right) 12.0 else -15.0, if (right) PI else PI)
        val sideHigh = Pose2d(if (right) 55.5 else 56.0, if (right) 4.5 else -8.0, if (right) PI /4.0 else -PI /4.0)

        fun driveToOutput(): Command =
            TrajectorySequenceCommand(bot.drive, intakePose, quickExit = true) {
                setTangent((MIDDLE))
                splineTo(sideHigh.vec(), sideHigh.heading)
            }
        fun driveToIntake(): Command = wrapWithTape(intakePose, tape,
            TrajectorySequenceCommand(bot.drive, sideHigh, quickExit = true) {
                setReversed(true)
                splineTo(intakePose.vec(), SIDE)
            })


        val junctions = listOf(
            Junction(VerticalSlides.Height.HIGH.pos, ::driveToOutput, ::driveToIntake),
            Junction(VerticalSlides.Height.HIGH.pos, ::driveToOutput, ::driveToIntake),
            Junction(VerticalSlides.Height.HIGH.pos, ::driveToOutput, ::driveToIntake),
            Junction(VerticalSlides.Height.HIGH.pos, ::driveToOutput, ::driveToIntake),
            Junction(VerticalSlides.Height.HIGH.pos, ::driveToOutput, ::driveToIntake),
        )

        bot.drive.poseEstimate = startPose

        return mkSequential {
            add(pushSignal(startPose, samesidePose, VerticalSlides.Height.HIGH.pos))
            add(samesideOutput(VerticalSlides.Height.HIGH.pos))
            par {
                add(wrapWithTape(intakePose, tape, samesideToStack(samesidePose, intakePose)))
                seq {
                    delay(500.milliseconds)
                    task { bot.output.lift.runTo(VerticalSlides.Height.FIVE.pos) }
                }
            }

            if (n < 5) {
                add(cycle(5, junctions.subList(0, n)))
            } else {
                add(cycle(5, junctions))
            }

            task { bot.output.arm.state = Output.ArmState.INTAKE }
            task { bot.output.lift.runTo(0) }

            switch({ bot.vision?.signal?.finalTarget ?: Signal.MID }) {
                value(Signal.LEFT) {
                    if (right)
                        go(bot.drive, sideHigh, quickExit = true) {
                            setReversed(true)
                            splineTo(Vector2d(48.0, -6.0), SIDE)
                            addDisplacementMarker {
                                bot.output.claw.state = Output.ClawState.CLOSED
                                bot.output.arm.state = Output.ArmState.PARK
                            }
                            splineToSplineHeading(Pose2d(48.0, 20.0, -SIDE), PI/2.0)
                        }
                    else
                        go(bot.drive, sideHigh, quickExit = true) {
                            setReversed(true)
                            splineTo(Vector2d(49.0, 2.0), SIDE)
                            addDisplacementMarker {
                                bot.output.claw.state = Output.ClawState.CLOSED
                                bot.output.arm.state = Output.ArmState.PARK
                            }
//                            splineToSplineHeading(Pose2d(49.0, 24.0, -SIDE), PI /2.0)
                            splineTo(Vector2d(49.0, 24.0), PI /2.0)
                            back(3.0)
                        }
                }
                value(Signal.MID) { // MID or null
                    go(bot.drive, sideHigh, quickExit = true) {
                        setReversed(true)
                        splineTo(Vector2d(48.0, 0.0), PI)
                        splineTo(Vector2d(35.0, 0.0), PI)
                    }
                }
                value(Signal.RIGHT) {
                    if (right)
                        go(bot.drive, sideHigh, quickExit = true) {
                            setReversed(true)
                            splineTo(Vector2d(49.0, -6.0), SIDE)
                            addDisplacementMarker {
                                bot.output.claw.state = Output.ClawState.CLOSED
                                bot.output.arm.state = Output.ArmState.PARK
                            }
                            splineToSplineHeading(Pose2d(49.0, -28.0, -SIDE), -PI /2.0)
                        }
                    else
                        go(bot.drive, sideHigh, quickExit = true) {
                            setReversed(true)
                            splineTo(Vector2d(48.0, 6.0), SIDE)
                            addDisplacementMarker {
                                bot.output.claw.state = Output.ClawState.CLOSED
                                bot.output.arm.state = Output.ArmState.PARK
                            }
                            splineToSplineHeading(Pose2d(48.0, -21.5, -SIDE), -PI/2.0)
                        }
                }
            }
            delay(1000.milliseconds)
            task { stop() }
        }
    }
}

@Autonomous(preselectTeleOp = "OctoberTele", group = "_official")
class LeftSideHigh: SideHighAuto(false, false, 5)

@Autonomous(preselectTeleOp = "OctoberTele", group = "_official")
class RightSideHigh: SideHighAuto(true, false, 5)

@Autonomous(preselectTeleOp = "OctoberTele", group = "_official")
class LeftSideHigh4: SideHighAuto(false, false, 4)

@Autonomous(preselectTeleOp = "OctoberTele", group = "_official")
class RightSideHigh4: SideHighAuto(true, false, 4)

@Autonomous(preselectTeleOp = "OctoberTele", group = "_official")
class LeftSideHighTape: SideHighAuto(false, true, 4)

@Autonomous(preselectTeleOp = "OctoberTele", group = "_official")
class RightSideHighTape: SideHighAuto(true, true, 4)

@Autonomous(preselectTeleOp = "OctoberTele", group = "_official")
class LeftSideHighTape3: SideHighAuto(false, true, 3)

@Autonomous(preselectTeleOp = "OctoberTele", group = "_official")
class RightSideHighTape3: SideHighAuto(true, true, 3)
