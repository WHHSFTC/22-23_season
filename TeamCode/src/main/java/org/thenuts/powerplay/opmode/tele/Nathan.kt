package org.thenuts.powerplay.opmode.tele

import com.qualcomm.robotcore.hardware.Gamepad
import org.thenuts.powerplay.subsystems.output.VerticalSlides
import org.thenuts.powerplay.subsystems.output.Output
import org.thenuts.powerplay.subsystems.October
import org.thenuts.switchboard.command.Command
import org.thenuts.switchboard.util.Frame
import java.lang.Integer.max

class Nathan(val gamepad: Gamepad, val bot: October) : Command {
    override val done: Boolean = false
    val prev = Gamepad()
    val pad = Gamepad()

    override val postreqs = listOf(bot.output to 10)

    override fun update(frame: Frame) {
        pad.safeCopy(gamepad)

//        var slidesPower = -pad.right_stick_y.toDouble()
//        if (slidesPower > SLIDES_DEADZONE) {
//            slidesPower = (Lift.MAX_SLIDES_UP) * (slidesPower - SLIDES_DEADZONE)
//        } else if (slidesPower < -SLIDES_DEADZONE) {
//            slidesPower = (Lift.MAX_SLIDES_DOWN) * (slidesPower + SLIDES_DEADZONE)
//        } else {
//            slidesPower = 0.0
//        }
//
//        bot.lift.motor.power = slidesPower

        if (pad.right_trigger > 0.5) {
            bot.output.lift.state = VerticalSlides.State.Manual(-pad.right_stick_y.toDouble())
            bot.log.out["manual"] = -pad.right_stick_y.toDouble()
        } else if (prev.right_trigger > 0.5) {
            bot.output.lift.state = VerticalSlides.State.RunTo(bot.output.lift.getPosition())
        }
//
//        val state = bot.manip.lift.state
//        if (state is Lift.State.Hold && state.isManual && pad.a && !prev.a) {
//            bot.manip.lift.state = bot.manip.translate(bot.manip.state.lift)
//        }

        if (pad.right_bumper && !prev.right_bumper) {
            bot.output.claw.state = Output.ClawState.CLOSED
        } else if (pad.left_bumper && !prev.left_bumper) {
            bot.output.claw.state = Output.ClawState.OPEN
        }

        if (pad.b && !prev.b) {
            bot.output.wrist.state = Output.WristState.OUTPUT
        } else if (pad.x && !prev.x) {
            bot.output.wrist.state = Output.WristState.INTAKE
        }

        if (pad.y && !prev.y) {
            bot.output.extension.state = Output.ExtensionState.OUTPUT
        } else if (pad.a && !prev.a) {
            bot.output.extension.state = Output.ExtensionState.INTAKE
        }

        if (pad.back && !prev.back) {
            bot.output.lift.state = VerticalSlides.State.ZERO
        }

        if (pad.left_trigger > 0.5) {
            val liftState = bot.output.lift.state
            val prevPos = when (liftState) {
                VerticalSlides.State.IDLE, VerticalSlides.State.ZERO, is VerticalSlides.State.Manual -> 0
                is VerticalSlides.State.RunTo -> liftState.pos
                is VerticalSlides.State.Hold -> liftState.pos
            }
            if (pad.dpad_up && !prev.dpad_up) {
                bot.output.lift.state = VerticalSlides.State.RunTo(prevPos + 2 * VerticalSlides.CONE_STEP)
            } else if (pad.dpad_down && !prev.dpad_down) {
                bot.output.lift.state = VerticalSlides.State.RunTo(max(0, prevPos - 2 * VerticalSlides.CONE_STEP))
            } else if (pad.dpad_left && !prev.dpad_left) {
                bot.output.lift.state = VerticalSlides.State.RunTo(max(0, prevPos - VerticalSlides.CONE_STEP / 2))
            } else if (pad.dpad_right && !prev.dpad_right) {
                bot.output.lift.state = VerticalSlides.State.RunTo(prevPos + VerticalSlides.CONE_STEP / 2)
            }
        } else {
            if (pad.dpad_up && !prev.dpad_up) {
                bot.output.lift.state = VerticalSlides.State.RunTo(VerticalSlides.Height.HIGH.pos)
//            bot.manip.outputHeight = Lift.Height.HIGH
            } else if (pad.dpad_left && !prev.dpad_left || pad.dpad_right && !prev.dpad_right) {
                bot.output.lift.state = VerticalSlides.State.RunTo(VerticalSlides.Height.MID.pos)
//            bot.manip.outputHeight = Lift.Height.MID
            } else if (pad.dpad_down && !prev.dpad_down) {
                bot.output.lift.state = VerticalSlides.State.RunTo(VerticalSlides.Height.LOW.pos)
//            bot.manip.outputHeight = Lift.Height.LOW
            }
        }

        bot.log.out["lift pos"] = bot.output.lift.encoder1.position

        prev.safeCopy(pad)
    }

    companion object {
        @JvmField var SLIDES_DEADZONE = 0.05
    }
}

fun Gamepad.shift() = right_trigger > 0.5 || left_trigger > 0.5