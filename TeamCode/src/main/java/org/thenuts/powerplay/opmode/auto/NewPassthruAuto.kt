package org.thenuts.powerplay.opmode.auto

import com.acmerobotics.roadrunner.geometry.Pose2d
import com.acmerobotics.roadrunner.geometry.Vector2d
import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import org.thenuts.powerplay.game.Signal
import org.thenuts.powerplay.opmode.commands.go
import org.thenuts.powerplay.subsystems.output.Output
import org.thenuts.powerplay.subsystems.output.VerticalSlides
import org.thenuts.switchboard.command.Command
import org.thenuts.switchboard.dsl.mkSequential
import kotlin.math.PI
import kotlin.time.Duration.Companion.milliseconds

abstract class NewPassthruAuto(right: Boolean): CyclingAuto(right) {
    override fun generateCommand(): Command {
        val startPose = Pose2d(0.0, 5.0, PI)
        val intakePose = Pose2d(if (right) 49.0 else 51.25, if (right) -25.5 else 24.25, if (right) PI /2.0 else -PI /2.0)
        val samesidePose = Pose2d(if (right) 50.0 else 50.0, if (right) 11.5 else -13.0, if (right) PI else PI)
        val sideHigh = Pose2d(if (right) 55.0 else 55.0, if (right) 5.0 else -8.0, if (right) PI /4.0 else -PI /4.0)

        val junctions = listOf(
            VerticalSlides.Height.HIGH.pos to sideHigh,
            VerticalSlides.Height.HIGH.pos to sideHigh,
            VerticalSlides.Height.HIGH.pos to sideHigh,
            VerticalSlides.Height.HIGH.pos to sideHigh,
            VerticalSlides.Height.HIGH.pos to sideHigh
        )

        bot.drive.poseEstimate = startPose

        return mkSequential {
            add(samesideOutput(VerticalSlides.Height.HIGH.pos, startPose, samesidePose))

            add(cycle(samesidePose, intakePose, 5, junctions))

            task { bot.output.arm.state = Output.ArmState.INTAKE }
            task { bot.output.lift.runTo(0) }

            switch({ bot.vision?.signal?.finalTarget ?: Signal.MID }) {
                value(Signal.LEFT) {
                    go(bot.drive, junctions.last().second, quickExit = true) {
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
                    go(bot.drive, junctions.last().second, quickExit = true) {
                        setReversed(true)
                        splineTo(Vector2d(48.0, 0.0), PI)
                        splineTo(Vector2d(35.0, 0.0), PI)
                    }
                }
                value(Signal.RIGHT) {
                    go(bot.drive, junctions.last().second, quickExit = true) {
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
class LeftNewPassthruAuto: NewPassthruAuto(false)

@Autonomous(preselectTeleOp = "OctoberTele", group = "_official")
class RightNewPassthruAuto: NewPassthruAuto(true)
