package org.thenuts.powerplay.opmode.tele

import com.acmerobotics.dashboard.config.Config
import com.acmerobotics.roadrunner.geometry.Pose2d
import com.qualcomm.robotcore.hardware.Gamepad
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import org.thenuts.powerplay.subsystems.October
import org.thenuts.switchboard.command.Command
import org.thenuts.switchboard.command.combinator.SlotCommand
import org.thenuts.switchboard.util.Frame
import kotlin.math.min

@Config
class Driver1(val gamepad: Gamepad, val bot: October) : Command {
    override val done: Boolean = false
    val prev = Gamepad()
    val pad = Gamepad()

    val intakeSlot = SlotCommand()

    override val postreqs: List<Pair<Command, Int>> = listOf(bot.drive to 10, /* bot.intake to 10, */ /* bot.intake.slides to 10 */)

    override fun update(frame: Frame) {
        controls(frame)
        intakeSlot.update(frame)
    }

//    var stackHeight = 5
//    var prevIntake = false

    fun controls(frame: Frame) {
        pad.safeCopy(gamepad)

        var x = -pad.left_stick_y.toDouble()
        var y = -pad.left_stick_x.toDouble()
        val omega = -pad.right_stick_x.toDouble()

//        if (pad.dpad_left || pad.dpad_right || pad.dpad_up || pad.dpad_down) {
//            x = (if (pad.dpad_up) 1.0 else 0.0) - (if (pad.dpad_down) 1.0 else 0.0)
//            y = (if (pad.dpad_left) 1.0 else 0.0) - (if (pad.dpad_right) 1.0 else 0.0)
//        }

        val turtle = pad.shift() // || bot.output.lift.getPosition() > VerticalSlides.Height.LOW.pos + 200
        val scalar = if (turtle) TURTLE_POWER else 1.0

        var pow = Pose2d(x * scalar, y * scalar, omega)

        if (pad.left_bumper) {
            val dist = bot.passthru_dist.getDistance(DistanceUnit.INCH)
            if (dist > PASSTHRU_DIST)
                pow = pow.copy(x = min(PASSTHRU_POWER + PASSTHRU_SLOPE * (dist - PASSTHRU_DIST), PASSTHRU_MAX))
            else
                pow = pow.copy(x = 0.0)
        } else if (pad.right_bumper) {
            if (bot.intake_dist.getDistance(DistanceUnit.INCH) > INTAKE_DIST)
                pow = pow.copy(x = INTAKE_POWER)
            else
                pow = pow.copy(x = 0.0)
        }

        bot.drive.setWeightedDrivePower(pow)
        bot.log.out["drive power"] = pow

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

        prev.safeCopy(pad)
    }

    companion object {
        @JvmField var INTAKE_DIST: Double = 4.0
        @JvmField var INTAKE_POWER: Double = -0.5

        @JvmField var PASSTHRU_DIST: Double = 1.5
        @JvmField var PASSTHRU_MAX: Double = 0.5
        @JvmField var PASSTHRU_POWER: Double = 0.25
        @JvmField var PASSTHRU_SLOPE: Double = 0.1

        @JvmField var TURTLE_POWER: Double = 0.5
    }
}