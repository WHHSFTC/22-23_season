package org.thenuts.powerplay.opmode.auto

import com.acmerobotics.dashboard.config.Config
import com.acmerobotics.roadrunner.control.PIDCoefficients
import com.acmerobotics.roadrunner.control.PIDFController
import com.acmerobotics.roadrunner.drive.DriveSignal
import com.acmerobotics.roadrunner.geometry.Pose2d
import com.acmerobotics.roadrunner.geometry.Vector2d
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import org.thenuts.powerplay.acme.drive.DriveConstants.kV
import org.thenuts.powerplay.opmode.commands.go
import org.thenuts.powerplay.opmode.tele.toRadians
import org.thenuts.powerplay.subsystems.TapeDetector
import org.thenuts.powerplay.subsystems.output.VerticalSlides
import org.thenuts.powerplay.subsystems.output.Output
import org.thenuts.switchboard.command.Command
import org.thenuts.switchboard.dsl.mkParallel
import org.thenuts.switchboard.dsl.mkSequential
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.min
import kotlin.math.sign
import kotlin.time.Duration.Companion.milliseconds

@Config
abstract class CyclingAuto(val right: Boolean) : OctoberAuto() {
    val MIDDLE = if (right) PI/2.0 else -PI/2.0
    val SIDE = if (right) -PI/2.0 else PI/2.0

    fun pushSignal(startPose: Pose2d, endPose: Pose2d, height: Int): Command = mkSequential {
        task { bot.output.claw.state = Output.ClawState.CLOSED }

        go(bot.drive, startPose, quickExit = true) {
            setTangent(-PI / 4.0)
            splineToConstantHeading(Vector2d(3.0, startPose.y / 2.0), (-PI / 4.0))
            addDisplacementMarker {
                bot.output.arm.state = Output.ArmState.MAX_UP
            }
            splineToConstantHeading(Vector2d(8.0, 0.0), (0.0))
            splineToConstantHeading(Vector2d(40.0, -4.0), (0.0))
            addDisplacementMarker {
                bot.output.lift.runTo(height)
            }
            splineToConstantHeading(endPose.vec(), (MIDDLE))
        }
    }


    fun samesideOutput(height: Int): Command = mkSequential {
        task {
            bot.output.lift.runTo(height)
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

    fun singleCycle(height: Int, lastCone: Boolean, driveToOutput: () -> Command): Command = mkSequential {
        task { bot.output.claw.state = Output.ClawState.CLOSED }
        delay(450.milliseconds)

        task { bot.output.lift.runTo(height) }
        if (!lastCone)
            await { bot.output.lift.getPosition() > VerticalSlides.Height.ABOVE_STACK.pos }
        task { bot.output.arm.state = Output.ArmState.CLEAR }

        add(driveToOutput())
        add(passthruOutput())
    }

    fun passthruOutput(): Command = mkSequential {
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

    fun samesideToStack(samesidePose: Pose2d, intakePose: Pose2d, stackHeight: Int = 5): Command = mkSequential {
        go(bot.drive, samesidePose, quickExit = true) {
//                        strafeTo(intakePose.vec())
            setTangent((SIDE * 1.5))
            splineToSplineHeading(Pose2d(44.0, 0.0, (MIDDLE)), (SIDE))
            addDisplacementMarker {
                bot.output.arm.state = Output.ArmState.INTAKE
                bot.output.claw.state = Output.ClawState.WIDE
                bot.output.lift.runTo(VerticalSlides.Height.values()[stackHeight - 1].pos)
            }
//                        splineToSplineHeading(Pose2d(intakePose.x, 14.0, (MIDDLE)), (SIDE))
            splineToSplineHeading(intakePose, (SIDE))
        }
    }

    fun wrapWithTape(intakePose: Pose2d, tape: Boolean, command: Command): Command {
        if (tape) {
            return mkParallel(awaitAll = false) {
                add(intakeFromTape(intakePose))
                seq {
                    add(command)
                    add(Command.IDLE)
                }
            }
        } else {
            return command
        }
    }

    fun intakeFromTape(intakePose: Pose2d): Command = mkSequential {
        val targetHeading = if (right) PI/2.0 else -PI/2.0
        linear {
            bot.tapeDetector.reset()
            while (bot.tapeDetector.read() == TapeDetector.TapeState.MISSING) {
                if (!bot.drive.isBusy) return@linear
                yield()
            }
            bot.drive.trajectorySequenceRunner.interrupt()
            val startPose = bot.drive.poseEstimate
            val headingController = PIDFController(HEADING_PID)
            val axialController = PIDFController(DIST_PID)
            val lateralController = PIDFController(DIST_PID)
            var heading = bot.drive.externalHeading
            var dist = bot.intakeSensor.getDistance(DistanceUnit.INCH)
            do {
                if (frame.n % 4 == 0L)
                    heading = bot.drive.externalHeading
                if (frame.n % 8 == 0L || dist <= 3.0)
                    dist = bot.intakeSensor.getDistance(DistanceUnit.INCH)
                val headingError = (targetHeading - heading).angleWrap()
                val omega = headingController.update(
                    measuredPosition = -headingError,
                    measuredVelocity = bot.drive.poseVelocity?.heading
                )

//                val xError = intakePose.y - bot.drive.poseEstimate.y
                val xError = DIST_TARGET - dist
                bot.log.out["xError"] = xError

                var vx = axialController.update(-xError)

                val tapeState = bot.tapeDetector.read()

                var correction = TapeDetector.suggestedCorrection(tapeState) ?: Vector2d()
                correction /= kV
                var vy = correction.y

                bot.log.out["suggested y correction"] = correction.y

                if (headingError.absoluteValue > HEADING_THRESHOLD) {
                    vx = 0.0
                    vy = 0.0
                }

                if (vx < 0.0)
                    vx = sign(vx) * min(vx.absoluteValue, correction.x.absoluteValue)

                val vel = Pose2d(
                    Vector2d(vx, vy).rotated(headingError),
                    omega
                )

                val driveSignal = DriveSignal(vel, Pose2d())
                bot.drive.setDriveSignal(driveSignal)
                bot.log.out["drive power"] = vel
                yield()
            } while (opModeIsActive() && !(xError > -DIST_THRESHOLD && tapeState == TapeDetector.TapeState.CENTERED))
            bot.drive.setDriveSignal(DriveSignal())
            yield()
        }
    }

    fun cycle(initialStackHeight: Int, junctions: List<Junction>): Command = mkSequential {
        for (i in junctions.indices) {
            val stackHeight = initialStackHeight - i

//                task { bot.output.lift.state = VerticalSlides.State.RunTo(0) }
//                await { !bot.output.isBusy }

//                task { bot.output.arm.state = Output.ArmState.values()[stackHeight - 1] }

            if (i != 0) {
                task {
                    bot.output.arm.state = Output.ArmState.INTAKE
                    bot.output.claw.state = Output.ClawState.WIDE
//                    bot.output.lift.runTo(0)
                    bot.output.lift.runTo(VerticalSlides.Height.values()[stackHeight - 1].pos)
                }
                add(junctions[i - 1].driveToIntake())
            }

            add(singleCycle(junctions[i].height, stackHeight == 1, junctions[i].driveToOutput))
        }
    }

    companion object {
        @JvmField var HEADING_PID: PIDCoefficients = PIDCoefficients(5.0)
        @JvmField var HEADING_THRESHOLD: Double = 10.0.toRadians()

        @JvmField var DIST_PID: PIDCoefficients = PIDCoefficients(11.0, 0.0, 1.0)
        @JvmField var DIST_THRESHOLD: Double = 0.5
        @JvmField var DIST_TARGET: Double = 1.75
    }

    data class Junction(val height: Int, val driveToOutput: () -> Command, val driveToIntake: () -> Command)
}
