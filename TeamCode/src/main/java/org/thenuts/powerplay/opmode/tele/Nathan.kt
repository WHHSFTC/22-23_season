package org.thenuts.powerplay.opmode.tele

import com.qualcomm.robotcore.hardware.Gamepad
import org.thenuts.powerplay.subsystems.Lift
import org.thenuts.powerplay.subsystems.Manipulator
import org.thenuts.powerplay.subsystems.October
import org.thenuts.switchboard.command.Command
import org.thenuts.switchboard.util.Frame
import java.lang.Integer.max

class Nathan(val gamepad: Gamepad, val bot: October) : Command {
    override val done: Boolean = false
    val prev = Gamepad()
    val pad = Gamepad()

    override val postreqs = listOf(bot.manip to 10)

    override fun update(frame: Frame) {
        pad.safeCopy(gamepad)

//        var slidesPower = -pad.left_stick_y.toDouble()
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
            bot.manip.lift.state = Lift.State.Manual(-pad.left_stick_y.toDouble())
            bot.log.out["manual"] = -pad.left_stick_y.toDouble()
        } else if (prev.right_trigger > 0.5) {
            bot.manip.lift.state = Lift.State.Manual(0.0)
        }
//
//        val state = bot.manip.lift.state
//        if (state is Lift.State.Hold && state.isManual && pad.a && !prev.a) {
//            bot.manip.lift.state = bot.manip.translate(bot.manip.state.lift)
//        }

        if (pad.right_bumper && !prev.right_bumper) {
            bot.manip.claw.state = Manipulator.ClawState.CLOSED
        } else if (pad.left_bumper && !prev.left_bumper) {
            bot.manip.claw.state = Manipulator.ClawState.OPEN
        }

        if (pad.b && !prev.b) {
            bot.manip.wrist.state = Manipulator.WristState.OUTPUT
        } else if (pad.x && !prev.x) {
            bot.manip.wrist.state = Manipulator.WristState.INTAKE
        }

        if (pad.y && !prev.y) {
            bot.manip.extension.state = Manipulator.ExtensionState.OUTPUT
        } else if (pad.a && !prev.a) {
            bot.manip.extension.state = Manipulator.ExtensionState.INTAKE
        }

        if (pad.back && !prev.back) {
            bot.manip.lift.state = Lift.State.ZERO
        }

        if (pad.left_trigger > 0.5) {
            val liftState = bot.manip.lift.state
            val prevPos = when (liftState) {
                Lift.State.IDLE, Lift.State.ZERO, is Lift.State.Manual -> 0
                is Lift.State.RunTo -> liftState.pos
                is Lift.State.Hold -> liftState.pos
            }
            if (pad.dpad_up && !prev.dpad_up) {
                bot.manip.lift.state = Lift.State.RunTo(prevPos + Lift.CONE_STEP)
            } else if (pad.dpad_left && !prev.dpad_left || pad.dpad_right && !prev.dpad_right) {
                bot.manip.lift.state = Lift.State.RunTo(Lift.Height.ABOVE_STACK.pos)
            } else if (pad.dpad_down && !prev.dpad_down) {
                bot.manip.lift.state = Lift.State.RunTo(max(0, prevPos - Lift.CONE_STEP))
            }
        } else {
            if (pad.dpad_up && !prev.dpad_up) {
                bot.manip.lift.state = Lift.State.RunTo(Lift.Height.HIGH.pos)
//            bot.manip.outputHeight = Lift.Height.HIGH
            } else if (pad.dpad_left && !prev.dpad_left || pad.dpad_right && !prev.dpad_right) {
                bot.manip.lift.state = Lift.State.RunTo(Lift.Height.MID.pos)
//            bot.manip.outputHeight = Lift.Height.MID
            } else if (pad.dpad_down && !prev.dpad_down) {
                bot.manip.lift.state = Lift.State.RunTo(Lift.Height.LOW.pos)
//            bot.manip.outputHeight = Lift.Height.LOW
            }
        }

        bot.log.out["lift pos"] = bot.manip.lift.encoder1.position

        prev.safeCopy(pad)
    }

    companion object {
        @JvmField var SLIDES_DEADZONE = 0.05
    }
}

fun Gamepad.shift() = right_trigger > 0.5 || left_trigger > 0.5