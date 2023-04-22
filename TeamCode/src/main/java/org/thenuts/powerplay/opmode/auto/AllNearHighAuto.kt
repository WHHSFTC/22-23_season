package org.thenuts.powerplay.opmode.auto

import com.acmerobotics.roadrunner.control.PIDFController
import com.acmerobotics.roadrunner.geometry.Pose2d
import com.acmerobotics.roadrunner.geometry.Vector2d
import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
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

abstract class AllNearHighAuto(right: Boolean, val restSide: Boolean = false): CyclingAuto(right) {
    override fun generateCommand(): Command {
        val startPose = Pose2d(0.0, 5.0, PI)
        val backupVec = Vector2d(if (right) 50.0 else 50.0, if (right) 26.0 else -24.0)
        val nearHigh = Pose2d(if (right) 40.0 else 40.0, if (right) 33.75 else -30.0, if (right) 6.0*PI/8.0 else -6.0*PI/8.0)
        val intakePose = Pose2d(if (right) 52.5 else 50.0, if (right) -21.0 else 24.25, if (right) PI /2.0 else -PI /2.0)
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
        fun aroundSignal(): Command = mkSequential {
            task { bot.output.claw.state = Output.ClawState.CLOSED }
//            delay(400.milliseconds)
//            task { bot.output.arm.state = Output.ArmState.MAX_UP }

//            delay(200.milliseconds)
            go(bot.drive, startPose, quickExit = true) {
                if (right) {
                    setTangent(PI / 4.0)
                    splineToConstantHeading(Vector2d(0.5, startPose.y * 1.5), (PI / 2.0))
                } else {
                    setTangent(-PI / 4.0)
                    splineToConstantHeading(Vector2d(0.5, startPose.y / 2.0), (-PI / 2.0))
                }
                splineToConstantHeading(Vector2d(4.0, if (right) 22.0 else -24.0), if (right) PI/4.0 else -PI/4.0)
                splineToConstantHeading(Vector2d(8.0, if (right) 25.0 else -28.0), (0.0))
                addDisplacementMarker {
                    bot.output.arm.state = Output.ArmState.MAX_UP
                    bot.output.lift.runTo(VerticalSlides.Height.HIGH.pos)
                }
                splineToConstantHeading(backupVec, 0.0)
                setReversed(false)
                splineTo(nearHigh.vec(), nearHigh.heading)
            }
        }

        val junctions = listOf(
                Junction(VerticalSlides.Height.HIGH.pos, ::driveToOutput, ::driveToIntake),
                Junction(VerticalSlides.Height.HIGH.pos, ::driveToOutput, ::driveToIntake),
                Junction(VerticalSlides.Height.HIGH.pos, ::driveToOutput, ::driveToIntake),
                Junction(VerticalSlides.Height.HIGH.pos, ::driveToOutput, ::driveToIntake),
            )

        bot.drive.poseEstimate = startPose
        return mkSequential {
            add(aroundSignal())
            add(passthruOutput())

            task {
                bot.output.arm.state = Output.ArmState.INTAKE
                bot.output.claw.state = Output.ClawState.WIDE
                bot.output.lift.runTo(VerticalSlides.Height.FIVE.pos)
            }
            add(driveToIntake())

            add(cycle(5, junctions, delayTime = 800.milliseconds))

            task { bot.output.arm.state = Output.ArmState.MAX_UP }
            task { bot.output.lift.runTo(0) }

            switch({ bot.vision?.signal?.finalTarget ?: Signal.MID }) {
                value(Signal.LEFT) {
                    go(bot.drive, nearHigh, quickExit = true) {
                        setReversed(true)
                        if (!right)
                            splineTo(Vector2d(51.0, 0.0), SIDE)
                        addDisplacementMarker {
                            bot.output.claw.state = Output.ClawState.CLOSED
                            bot.output.arm.state = Output.ArmState.PARK
                        }
                        splineToSplineHeading(Pose2d(48.0, 25.0, -SIDE), PI /2.0)
//                        splineToSplineHeading(Pose2d(30.0, 25.0, PI), PI)
                    }
//                    linear {
//                        val headingController = PIDFController(HEADING_PID)
//                        val targetHeading = -PI/2.0
//                        while (opModeIsActive()) {
//                            val heading = bot.drive.externalHeading
//                            val headingError = (targetHeading - heading).angleWrap()
//                        }
//                    }
                }
                value(Signal.MID) { // MID or null
                    go(bot.drive, nearHigh, quickExit = true) {
                        setReversed(true)
                        addDisplacementMarker {
                            bot.output.claw.state = Output.ClawState.CLOSED
                            bot.output.arm.state = Output.ArmState.PARK
                        }
                        splineTo(Vector2d(48.0, 0.0), SIDE)
//                        turn(90.0.toRadians())
                    }
                }
                value(Signal.RIGHT) {
                    if (right)
                        go(bot.drive, nearHigh, quickExit = true) {
                            setReversed(true)
                            addDisplacementMarker {
                                bot.output.claw.state = Output.ClawState.CLOSED
                                bot.output.arm.state = Output.ArmState.PARK
                            }
                            splineTo(Vector2d(48.0, 0.0), SIDE)
                            splineToSplineHeading(Pose2d(48.0, -30.0, -SIDE), -PI /2.0)
                        }
                    else
                        go(bot.drive, nearHigh, quickExit = true) {
                            setReversed(true)
                            addDisplacementMarker {
                                bot.output.claw.state = Output.ClawState.CLOSED
                                bot.output.arm.state = Output.ArmState.PARK
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
class LeftAllNearHigh: AllNearHighAuto(false)

@Autonomous(preselectTeleOp = "OctoberTele", group = "_official")
class RightAllNearHigh: AllNearHighAuto(true)
