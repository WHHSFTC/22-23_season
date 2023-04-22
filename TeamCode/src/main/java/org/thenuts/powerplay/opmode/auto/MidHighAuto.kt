package org.thenuts.powerplay.opmode.auto

import com.acmerobotics.roadrunner.geometry.Pose2d
import com.acmerobotics.roadrunner.geometry.Vector2d
import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import org.thenuts.powerplay.game.Signal
import org.thenuts.powerplay.opmode.commands.TrajectorySequenceCommand
import org.thenuts.powerplay.opmode.commands.go
import org.thenuts.powerplay.opmode.tele.toRadians
import org.thenuts.powerplay.subsystems.output.Output
import org.thenuts.powerplay.subsystems.output.VerticalSlides
import org.thenuts.switchboard.command.Command
import org.thenuts.switchboard.dsl.mkSequential
import kotlin.math.PI
import kotlin.time.Duration.Companion.milliseconds

abstract class MidHighAuto(right: Boolean, val tape: Boolean): CyclingAuto(right) {
    override fun generateCommand(): Command {
        val startPose = Pose2d(0.0, 5.0, PI)
        val intakePose = Pose2d(if (right) 53.0 else 51.0, if (right) -22.0 else 23.75, if (right) PI /2.0 else -PI /2.0)
        val samesidePose = Pose2d(if (right) 24.0 else 49.0, if (right) 12.0 else -15.0, if (right) PI else PI)
        val sideHigh = Pose2d(if (right) 56.5 else 56.0, if (right) 10.5 else -8.0, if (right) PI /4.0 else -PI /4.0)

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
//            Junction(VerticalSlides.Height.HIGH.pos, ::driveToOutput, ::driveToIntake),
        )

        bot.drive.poseEstimate = startPose

        return mkSequential {
            task { bot.output.claw.state = Output.ClawState.CLOSED }

            go(bot.drive, startPose, quickExit = true) {
                setTangent(-PI / 4.0)
                splineToConstantHeading(Vector2d(3.0, startPose.y / 2.0), (-PI / 4.0))
                addDisplacementMarker {
                    bot.output.arm.state = Output.ArmState.MAX_UP
                }
                splineToConstantHeading(Vector2d(8.0, 0.0), (0.0))
                splineToConstantHeading(Vector2d(22.0, 2.0), PI/4.0)
                addDisplacementMarker {
                    bot.output.lift.runTo(VerticalSlides.Height.MID.pos)
                }
                splineToConstantHeading(samesidePose.vec(), (MIDDLE))
            }

            add(samesideOutput(VerticalSlides.Height.MID.pos))
            par {
                add(wrapWithTape(intakePose, tape, mkSequential {
                    go(bot.drive, samesidePose, quickExit = true) {
//                        strafeTo(intakePose.vec())
                        setTangent(MIDDLE)
                        splineToConstantHeading(Vector2d(26.0, 24.0), (SIDE))
                        splineToConstantHeading(Vector2d(40.0, 24.0), 0.0)
                        addDisplacementMarker {
                            bot.output.lift.runTo(VerticalSlides.Height.FIVE.pos)
                        }
//                        setTurnConstraint(4.0, 4.0)
//                        turn((-90.0).toRadians())
//                        resetTurnConstraint()
                        addDisplacementMarker {
                            bot.output.arm.state = Output.ArmState.INTAKE
                            bot.output.claw.state = Output.ClawState.WIDE
                        }
//                        splineToSplineHeading(Pose2d(intakePose.x, 14.0, (MIDDLE)), (SIDE))
                        setReversed(true)
                        splineTo(Vector2d(50.0, 12.0), SIDE)
                        splineTo(intakePose.vec(), SIDE)
//                        splineToSplineHeading(intakePose, (SIDE))
                    }
                }))
                seq {
                    delay(500.milliseconds)
                    task { bot.output.lift.runTo(VerticalSlides.Height.FIVE.pos) }
                }
            }

            if (tape) {
                add(cycle(5, junctions.subList(0, 4)))
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
class LeftMidHigh: MidHighAuto(false, false)

@Autonomous(preselectTeleOp = "OctoberTele", group = "_official")
class RightMidHigh: MidHighAuto(true, false)

@Autonomous(preselectTeleOp = "OctoberTele", group = "_official")
class LeftMidHighTape: MidHighAuto(false, true)

@Autonomous(preselectTeleOp = "OctoberTele", group = "_official")
class RightMidHighTape: MidHighAuto(true, true)
