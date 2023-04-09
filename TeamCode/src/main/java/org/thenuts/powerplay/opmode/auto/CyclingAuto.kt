package org.thenuts.powerplay.opmode.auto

import com.acmerobotics.roadrunner.geometry.Pose2d
import com.acmerobotics.roadrunner.geometry.Vector2d
import org.thenuts.powerplay.game.Signal
import org.thenuts.powerplay.opmode.commands.go
import org.thenuts.powerplay.subsystems.output.VerticalSlides
import org.thenuts.powerplay.subsystems.output.Output
import org.thenuts.switchboard.command.Command
import org.thenuts.switchboard.dsl.mkSequential
import kotlin.math.PI
import kotlin.time.Duration.Companion.milliseconds

abstract class CyclingAuto(val right: Boolean) : OctoberAuto() {
    val MIDDLE = if (right) PI/2.0 else -PI/2.0
    val SIDE = if (right) -PI/2.0 else PI/2.0

    fun samesideOutput(height: Int, startPose: Pose2d, samesidePose: Pose2d): Command = mkSequential {
        task { bot.output.claw.state = Output.ClawState.CLOSED }

        go(bot.drive, startPose, quickExit = true) {
            setTangent(-PI/4.0)
            splineToConstantHeading(Vector2d(3.0, startPose.y/2.0), (-PI/4.0))
            addDisplacementMarker {
                bot.output.arm.state = Output.ArmState.MAX_UP
            }
            splineToConstantHeading(Vector2d(8.0, 0.0), (0.0))
            splineToConstantHeading(Vector2d(42.0, -4.0), (0.0))
            addDisplacementMarker {
                bot.output.lift.state =
                    VerticalSlides.State.RunTo(VerticalSlides.Height.HIGH.pos)
            }
            splineToConstantHeading(samesidePose.vec(), (MIDDLE))
        }
        task {
            bot.output.lift.state =
                VerticalSlides.State.RunTo(height)
        }
        task { bot.output.arm.state = Output.ArmState.SAMESIDE_HOVER }
        await { !bot.output.isBusy }
//                delay(1000.milliseconds)
        task { bot.output.arm.state = Output.ArmState.SAMESIDE_OUTPUT }
        delay(300.milliseconds)
//                task {
//                    bot.output.lift.state =
//                        VerticalSlides.State.RunTo(VerticalSlides.Height.MID.pos)
//                }
//                await { !bot.output.lift.isBusy }
        task { bot.output.claw.state = Output.ClawState.WIDE }
        delay(100.milliseconds)
        task { bot.output.arm.state = Output.ArmState.CLEAR }
//                delay(200.milliseconds)
    }

    fun passthruOutput(height: Int, lastCone: Boolean, intakePose: Pose2d, outputPose: Pose2d): Command = mkSequential {
        task { bot.output.claw.state = Output.ClawState.CLOSED }
        delay(450.milliseconds)

        task { bot.output.lift.state = VerticalSlides.State.RunTo(height) }
        if (!lastCone)
            await { bot.output.lift.getPosition() > VerticalSlides.Height.ABOVE_STACK.pos }
        task { bot.output.arm.state = Output.ArmState.CLEAR }


        go(bot.drive, intakePose, quickExit = true) {
//                    lineToLinearHeading(outputPose)
            setTangent((MIDDLE))
            splineTo(outputPose.vec(), outputPose.heading)
//                    turnWrap(passthruPose.heading - intakePose.heading)
        }

//                task {
//                    bot.output.lift.state =
//                        VerticalSlides.State.RunTo(height)
//                }
        task { bot.output.arm.state = Output.ArmState.PASSTHRU_OUTPUT }
//                await { !bot.output.lift.isBusy }
//                delay(1000.milliseconds)
//                delay(150.milliseconds)
//                task {
//                    bot.output.lift.state =
//                        VerticalSlides.State.RunTo(VerticalSlides.Height.MID.pos)
//                }
//                await { !bot.output.lift.isBusy }
//                task { bot.output.arm.state = Output.ArmState.PASSTHRU_OUTPUT }
        delay(400.milliseconds)
        task { bot.output.claw.state = Output.ClawState.NARROW }
//                delay(300.milliseconds)
//                task { bot.output.claw.state = Output.ClawState.CLOSED }
        delay(200.milliseconds)
        task { bot.output.arm.state = Output.ArmState.INTAKE }
        delay(300.milliseconds)
    }

    fun cycle(samesidePose: Pose2d, intakePose: Pose2d, initialStackHeight: Int, junctions: List<Pair<Int, Pose2d>>): Command = mkSequential {
        for (i in junctions.indices) {
            val stackHeight = initialStackHeight - i

//                task { bot.output.lift.state = VerticalSlides.State.RunTo(0) }
//                await { !bot.output.isBusy }

            task { bot.output.lift.state = VerticalSlides.State.RunTo(VerticalSlides.Height.values()[stackHeight - 1].pos) }
//                task { bot.output.arm.state = Output.ArmState.values()[stackHeight - 1] }

            if (i == 0) {
                go(bot.drive, samesidePose, quickExit = true) {
//                        strafeTo(intakePose.vec())
                    setTangent((SIDE * 1.5))
                    splineToSplineHeading(Pose2d(44.0, 0.0, (MIDDLE)), (SIDE))
                    addDisplacementMarker {
                        bot.output.arm.state = Output.ArmState.INTAKE
                        bot.output.claw.state = Output.ClawState.WIDE
                    }
//                        splineToSplineHeading(Pose2d(intakePose.x, 14.0, (MIDDLE)), (SIDE))
                    splineToSplineHeading(intakePose, (SIDE))
                }
            } else {
                task {
                    bot.output.arm.state = Output.ArmState.INTAKE
                    bot.output.claw.state = Output.ClawState.WIDE
                }
                go(bot.drive, junctions[i - 1].second, quickExit = true) {
                    setReversed(true)
                    splineTo(intakePose.vec(), (SIDE))
                }
            }

            add(passthruOutput(junctions[i].first, stackHeight != 1, intakePose, junctions[i].second))
        }
    }

}
