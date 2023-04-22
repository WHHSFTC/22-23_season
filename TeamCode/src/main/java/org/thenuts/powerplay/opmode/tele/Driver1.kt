package org.thenuts.powerplay.opmode.tele

import com.acmerobotics.dashboard.config.Config
import com.acmerobotics.roadrunner.control.PIDCoefficients
import com.acmerobotics.roadrunner.control.PIDFController
import com.acmerobotics.roadrunner.drive.DriveSignal
import com.acmerobotics.roadrunner.geometry.Pose2d
import com.acmerobotics.roadrunner.geometry.Vector2d
import com.qualcomm.robotcore.hardware.Gamepad
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import org.thenuts.powerplay.acme.drive.DriveConstants.*
import org.thenuts.powerplay.acme.drive.SampleMecanumDrive.LATERAL_MULTIPLIER
import org.thenuts.powerplay.opmode.auto.angleWrap
import org.thenuts.powerplay.opmode.commands.PassthruOutputCommand
import org.thenuts.powerplay.subsystems.October
import org.thenuts.powerplay.subsystems.TapeDetector
import org.thenuts.powerplay.subsystems.TapeDetector.Companion.STACK_HEADING_ERROR
import org.thenuts.powerplay.subsystems.output.Output
import org.thenuts.powerplay.subsystems.output.VerticalSlides
import org.thenuts.switchboard.command.Command
import org.thenuts.switchboard.command.combinator.SlotCommand
import org.thenuts.switchboard.dsl.mkSequential
import org.thenuts.switchboard.util.EPSILON
import org.thenuts.switchboard.util.Frame
import org.thenuts.switchboard.util.epsilonReduce
import kotlin.math.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

@Config
class Driver1(val gamepad: Gamepad, val bot: October, val outputSlot: SlotCommand) : Command {
    override val done: Boolean = false
    val prev = Gamepad()
    val pad = Gamepad()

    override val postreqs: List<Pair<Command, Int>> = listOf(bot.drive to 10, /* bot.intake to 10, */ /* bot.intake.slides to 10 */)

    var targetHeading = bot.drive.poseEstimate.heading
    var currentPov = 0.0

    fun accelConstraint(): Triple<ClosedRange<Double>, ClosedRange<Double>, ClosedRange<Double>> {
        val lift = bot.output.lift.getPosition()

        if (lift < VerticalSlides.INSIDE_BOT && FREE_LOW) {
            val range = Double.NEGATIVE_INFINITY..Double.POSITIVE_INFINITY
            return Triple(range, range, range)
        }

        val maxXPosAccel = (LOW_X_POS_ACCEL) - lift * (LOW_X_POS_ACCEL - HIGH_X_POS_ACCEL) / VerticalSlides.Height.HIGH.pos.toDouble()
        val maxXPosDecel = (LOW_X_POS_DECEL) - lift * (LOW_X_POS_DECEL - HIGH_X_POS_DECEL) / VerticalSlides.Height.HIGH.pos.toDouble()
        val maxXNegAccel = (LOW_X_NEG_ACCEL) - lift * (LOW_X_NEG_ACCEL - HIGH_X_NEG_ACCEL) / VerticalSlides.Height.HIGH.pos.toDouble()
        val maxXNegDecel = (LOW_X_NEG_DECEL) - lift * (LOW_X_NEG_DECEL - HIGH_X_NEG_DECEL) / VerticalSlides.Height.HIGH.pos.toDouble()

        val xRange = if (prevDriveSignal.vel.x.absoluteValue < 20.0) {
            -maxXNegAccel..maxXPosAccel
        } else if (prevDriveSignal.vel.x > 0.0) {
            -maxXPosDecel..maxXPosAccel
        } else {
            -maxXNegAccel..maxXNegDecel
        }

        val maxYAccel = LOW_Y_ACCEL - lift * (LOW_Y_ACCEL - HIGH_Y_ACCEL) / VerticalSlides.Height.HIGH.pos.toDouble()
        val maxYDecel = LOW_Y_DECEL - lift * (LOW_Y_DECEL - HIGH_Y_DECEL) / VerticalSlides.Height.HIGH.pos.toDouble()

        val yRange = if (prevDriveSignal.vel.y > 0.0) {
            -maxYDecel..maxYAccel
        } else {
            -maxYAccel..maxYDecel
        }

        val maxAngAccel = 80.0 - lift * 50.0 / 700.0
        val maxAngDecel = 30.0 - lift * 10.0 / 700.0

        val angRange = if (prevDriveSignal.vel.heading > 0.0) {
            -maxAngDecel..maxAngAccel
        } else {
            -maxAngAccel..maxAngDecel
        }

        return Triple(xRange, yRange, angRange)
    }

    override fun update(frame: Frame) {
        pad.safeCopy(gamepad)
        controls(frame)
        if (pad.shift())
            output(frame)
        prev.safeCopy(pad)
    }

    fun output(frame: Frame) {
        if (pad.dpad_up && !prev.dpad_up) {
            outputSlot.interrupt(PassthruOutputCommand(bot, VerticalSlides.Height.HIGH.pos))
        } else if (pad.dpad_left && !prev.dpad_left) {
            outputSlot.interrupt(PassthruOutputCommand(bot, VerticalSlides.Height.MID.pos))
        } else if (pad.dpad_right && !prev.dpad_right) {
            outputSlot.interrupt(PassthruOutputCommand(bot, VerticalSlides.Height.MID.pos))
        } else if (pad.dpad_down && !prev.dpad_down) {
            outputSlot.interrupt(PassthruOutputCommand(bot, VerticalSlides.Height.LOW.pos))
        }
    }

    //    var stackHeight = 5
//    var prevIntake = false
    var lockTime: Duration? = null

    val axialVeloPID = PIDFController(
        AXIAL_VELO_PID
    )

    val lateralVeloPID = PIDFController(
        LATERAL_VELO_PID
    )

    val headingPID = PIDFController(
        HEADING_PID
    )

    var prevDriveSignal = DriveSignal()

    fun controls(frame: Frame) {
//        val limits = accelConstraint()

        val dt = frame.step.toDouble(DurationUnit.SECONDS).coerceAtLeast(EPSILON)
//        val xRange = limits.first * dt + prevDriveSignal.vel.x
//        val yRange = limits.second * dt + prevDriveSignal.vel.y
//        val turningRange = limits.third * dt + prevDriveSignal.vel.heading

        val x = -pad.left_stick_y.toDouble()
        val y = -pad.left_stick_x.toDouble()

//        if (pad.dpad_left || pad.dpad_right || pad.dpad_up || pad.dpad_down) {
//            x = (if (pad.dpad_up) 1.0 else 0.0) - (if (pad.dpad_down) 1.0 else 0.0)
//            y = (if (pad.dpad_left) 1.0 else 0.0) - (if (pad.dpad_right) 1.0 else 0.0)
//        }

        val turtle = pad.left_trigger > 0.5 // || bot.output.lift.getPosition() > VerticalSlides.Height.LOW.pos + 200
        val scalar = if (turtle) TURTLE_POWER else 1.0
        var translation = Vector2d(x, y) * scalar

        val manualTurn = pad.right_stick_x.absoluteValue > TURN_DEADZONE
        val prevManualTurn = prev.right_stick_x.absoluteValue > TURN_DEADZONE

        if (!manualTurn && prevManualTurn) {
            lockTime = frame.clock + LOCK_TIMEOUT.seconds
        }

        if (lockTime?.let { it < frame.clock } == true) {
            lockTime = null
            targetHeading = bot.drive.poseEstimate.heading.angleWrap()
        }

//        if (pad.dpad_up && !prev.dpad_up) {
//            targetHeading = 0.0
//        } else if (pad.dpad_down && !prev.dpad_down) {
//            targetHeading = PI
//        } else if (pad.dpad_left && !prev.dpad_left) {
//            targetHeading = PI/2.0
//        } else if (pad.dpad_right && !prev.dpad_right) {
//            targetHeading = -PI/2.0
//        }

        if (pad.x) {
            targetHeading = -PI/2.0
        } else if (pad.b) {
            targetHeading = PI/2.0
        }

//        val manualCentric = pad.right_trigger > 0.5
//        val prevManualCentric = prev.right_trigger > 0.5
//
//        if (manualCentric) {
//            if (!prevManualCentric) {
//                currentPov = bot.drive.poseEstimate.heading.angleWrap()
//            } else {
//                translation = translation.rotated(currentPov-bot.drive.poseEstimate.heading)
//            }
//        }

        var omega = -pad.right_stick_x.toDouble().smooth(min = MIN_TURN, max = MAX_TURN)//.coerceIn(turningRange)
        if (!manualTurn) {
            var error = (targetHeading - bot.drive.poseEstimate.heading).angleWrap()
            if (lockTime != null)
                error = 0.0
            if (LOCK_CENTRIC)// && !manualCentric)
                translation = translation.rotated(error)
            omega = headingPID.update(-error.epsilonReduce(HEADING_TOLERANCE), bot.drive.poseVelocity?.heading)

//            if (pad.a) {
//                val dist = bot. intakeSensor.getDistance(DistanceUnit.INCH)
//                bot.log.out["intake dist"] = dist
//                val pow = STACK_APPROACH_MIN + dist.coerceIn(STACK_DIST, 18.0) / (18.0 - STACK_DIST) * (STACK_APPROACH_MAX - STACK_APPROACH_MIN)
//                translation = if (dist < STACK_DIST)
//                    translation
//                else
//                    Vector2d(-pow, 0.0)
//            }
            if ((pad.x || pad.b || pad.a) && error.absoluteValue < STACK_HEADING_ERROR) {
                val dist = bot.intakeSensor.getDistance(DistanceUnit.INCH)
                bot.log.out["intake dist"] = dist
                var pow = STACK_APPROACH_MIN + dist.coerceIn(STACK_DIST, 18.0) / (18.0 - STACK_DIST) * (STACK_APPROACH_MAX - STACK_APPROACH_MIN)
                if (dist > STACK_DIST) {
                    val correct = TapeDetector.suggestedCorrection(bot.tapeDetector.read())
                    if (correct != null) {
                        if (correct.y == 0.0) {
                            translation = Vector2d(-pow, 0.0)
                        } else {
                            translation = Vector2d(-min(pow, correct.x.absoluteValue), correct.y)
                        }
                    }
                }
            }
        }

        if (!(pad.x || pad.b || pad.a) && (prev.x || prev.b || pad.a))
            bot.tapeDetector.disable()

        // map unit circle onto ellipse, preserving angle
        val maxNorm = MAX_X * MAX_Y / (sqrt((MAX_X * sin(translation.angle())).pow(2) + (MAX_Y * cos(translation.angle())).pow(2)))
        val minNorm = MIN_X * MIN_Y / (sqrt((MIN_X * sin(translation.angle())).pow(2) + (MIN_Y * cos(translation.angle())).pow(2)))

        val scaledTranslation = Vector2d.polar(
            translation.norm().smooth(min = minNorm, max = maxNorm),
            translation.angle()
        )

        // limit local acceleration
        val limitedTranslation = Vector2d(
            scaledTranslation.x,//.coerceIn(xRange),
            scaledTranslation.y//.coerceIn(yRange)
        )

val vel = Pose2d(
            limitedTranslation,
            omega
        )
        bot.log.out["x input"] = scaledTranslation.x
        bot.log.out["x output"] = limitedTranslation.x

//        if (pad.left_bumper) {
//            val dist = bot.passthru_dist.getDistance(DistanceUnit.INCH)
//            if (dist > PASSTHRU_DIST)
//                pow = pow.copy(x = min(PASSTHRU_POWER + PASSTHRU_SLOPE * (dist - PASSTHRU_DIST), PASSTHRU_MAX))
//            else
//                pow = pow.copy(x = 0.0)
//        } else if (pad.right_bumper) {
//            if (bot.intake_dist.getDistance(DistanceUnit.INCH) > INTAKE_DIST)
//                pow = pow.copy(x = INTAKE_POWER)
//            else
//                pow = pow.copy(x = 0.0)
//        }

        val driveSignal = DriveSignal(vel, Pose2d())
        bot.drive.setDriveSignal(driveSignal)
        bot.log.out["drive power"] = vel
        prevDriveSignal = driveSignal

        when {
//            pad.dpad_left && !prev.dpad_left -> stackHeight = (stackHeight - 1).coerceIn(1..5)
//            pad.dpad_right && !prev.dpad_right -> stackHeight = (stackHeight + 1).coerceIn(1..5)
            // pad.dpad_up && !prev.dpad_up -> bot.intake.arm.state = Intake.ArmState.CLEAR
            // pad.dpad_down && !prev.dpad_down -> bot.intake.arm.state = Intake.ArmState.values()[stackHeight - 1]
            // pad.left_bumper && !prev.left_bumper -> bot.intake.claw.state = Intake.ClawState.OPEN
            // pad.right_bumper && !prev.right_bumper -> bot.intake.claw.state = Intake.ClawState.CLOSED
//            pad.back && !prev.back -> {
//                if (bot.output.state == Output.OutputState.GROUND || bot.output.state == Output.OutputState.INTAKE) {
//                    bot.intake.arm.state = Intake.ArmState.STORE
//                }
//            }
        }

//        val stick = pad.right_stick_y.toDouble()
//        if (stick.absoluteValue >= 0.2 && pad.shift()) {
            // map [0.2, 1.0] on stick to [0.0, 1.0] powers
//            val slidesPower = (stick.absoluteValue - 0.2) / 0.8 * stick.sign
//            bot.intake.slides.machine.switch(LinkageSlides.State.Manual(slidesPower))
//            prevIntake = true
//        } else {
//            if (prevIntake) {
//                bot.intake.slides.machine.switch(LinkageSlides.State.IDLE)
//            }
//            prevIntake = false

//            when {
//                pad.a && !prev.a -> intakeSlot.interrupt(mkSequential {
//                    task { bot.intake.arm.state = Intake.ArmState.CLEAR }
//                    delay(500.milliseconds)
//                    task { bot.intake.slides.machine.switch(LinkageSlides.State.RunTo(24.0)) }
//                    await { !bot.intake.slides.isBusy }
//                })
//                pad.y && !prev.y -> intakeSlot.interrupt(mkSequential {
//                    task { bot.intake.arm.state = Intake.ArmState.CLEAR }
//                    delay(500.milliseconds)
//                    task { bot.intake.slides.machine.switch(LinkageSlides.State.RunTo(10.0)) }
//                    await { !bot.intake.slides.isBusy }
//                    task { bot.intake.arm.state = Intake.ArmState.TRANSFER }
//                     wait for output to be ready
//                    par(awaitAll = true) {
//                        await { !bot.output.isBusy && bot.output.state == Output.OutputState.GROUND }
//                        delay(500.milliseconds)
//                    }
//
//                    task { bot.intake.slides.machine.switch(LinkageSlides.State.RunTo(0.0)) }
//                    await { !bot.intake.slides.isBusy }
//                    task { bot.intake.claw.state = Intake.ClawState.OPEN }
//                    delay(500.milliseconds)
//
//                     get out of output's way:
//                    task { bot.intake.arm.state = Intake.ArmState.CLEAR }
//                    task { bot.intake.slides.machine.switch(LinkageSlides.State.RunTo(10.0)) }
//                    par(awaitAll = true) {
//                        delay(500.milliseconds)
//                        await { !bot.intake.slides.isBusy }
//                    }
//                })
//            }
//        }
//
//        bot.log.out["intake distance"] = ticksToInches(bot.intake.slides.encoder.position.toDouble())
    }

    fun Double.smooth(inputDeadzone: Double = INPUT_DEADZONE, exponent: Double = EXPONENT, min: Double = 0.0, max: Double = 1.0): Double {
        return (((this.absoluteValue - inputDeadzone) / (1.0 - inputDeadzone)).pow(EXPONENT) * (max - min) + min) * sign(this)
    }

    companion object {
        @JvmField var INTAKE_DIST: Double = 4.0
        @JvmField var INTAKE_POWER: Double = -0.5

        @JvmField var PASSTHRU_DIST: Double = 1.5
        @JvmField var PASSTHRU_MAX: Double = 0.5
        @JvmField var PASSTHRU_POWER: Double = 0.25
        @JvmField var PASSTHRU_SLOPE: Double = 0.1

        @JvmField var TURTLE_POWER: Double = 0.5

        @JvmField var INPUT_DEADZONE: Double = 0.05

        @JvmField var FLICK_P: Double = 8.0
        @JvmField var TURN_DEADZONE: Double = 0.05
        @JvmField var LOCK_CENTRIC: Boolean = true
        @JvmField var LOCK_TIMEOUT: Double = 0.5

        @JvmField var EXPONENT: Double = 2.0

        @JvmField var AXIAL_VELO_PID: PIDCoefficients = PIDCoefficients(1.0)
        @JvmField var LATERAL_VELO_PID: PIDCoefficients = PIDCoefficients(1.0)
        @JvmField var HEADING_PID: PIDCoefficients = PIDCoefficients(5.0)
        @JvmField var HEADING_TOLERANCE: Double = 5.0.toRadians()

        @JvmField var MAX_X = 1.0/kV
        @JvmField var MAX_Y = 1.0/kV/LATERAL_MULTIPLIER
        @JvmField var MAX_TURN = 1.0/kV/((TRACK_WIDTH + WHEEL_BASE)/2.0)

        @JvmField var MIN_X = MAX_X * 0.2
        @JvmField var MIN_Y = MAX_Y * 0.1
        @JvmField var MIN_TURN = 0.1 * MAX_TURN

        @JvmField var HIGH_X_POS_ACCEL = 1000.0
        @JvmField var HIGH_X_POS_DECEL = 200.0

        @JvmField var LOW_X_POS_ACCEL = 200.0
        @JvmField var LOW_X_POS_DECEL = 200.0

        @JvmField var HIGH_X_NEG_ACCEL = 200.0
        @JvmField var HIGH_X_NEG_DECEL = 1000.0

        @JvmField var LOW_X_NEG_ACCEL = 200.0
        @JvmField var LOW_X_NEG_DECEL = 200.0

        @JvmField var HIGH_Y_ACCEL = 80.0
        @JvmField var HIGH_Y_DECEL = 30.0

        @JvmField var LOW_Y_ACCEL = 200.0
        @JvmField var LOW_Y_DECEL = 200.0

        @JvmField var FREE_LOW = true

        @JvmField var STACK_APPROACH_MAX = 0.5
        @JvmField var STACK_APPROACH_MIN = 0.2
        @JvmField var STACK_DIST = 2.0

        @JvmField var D1_OUTPUT = false
    }
}

fun Double.plusOrMinus(that: Double) = (this - that) .. (this + that)
operator fun ClosedRange<Double>.plus(that: Double) = (that + start) .. (that + endInclusive)
operator fun ClosedRange<Double>.times(that: Double) = (that * start) .. (that * endInclusive)
fun Double.toRadians() = this / 180.0 * PI
