package org.thenuts.powerplay.opmode.tele

import com.qualcomm.robotcore.hardware.Gamepad
import org.thenuts.powerplay.subsystems.Lift
import org.thenuts.powerplay.subsystems.Manipulator
import org.thenuts.powerplay.subsystems.October
import org.thenuts.switchboard.command.Command
import org.thenuts.switchboard.util.Frame

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

        if (pad.shift()) {
            bot.manip.lift.state = Lift.State.Manual(-pad.left_stick_y.toDouble())
        } else if (prev.shift()) {
            bot.manip.lift.state = Lift.State.Manual(0.0)
        }

        val state = bot.manip.lift.state
        if (state is Lift.State.Hold && state.isManual && pad.a && !prev.a) {
            bot.manip.lift.state = bot.manip.translate(bot.manip.state.lift)
        }

        if (pad.right_bumper && !prev.right_bumper) {
            bot.manip.claw.state = Manipulator.ClawState.CLOSED
        } else if (pad.left_bumper && !prev.left_bumper) {
            bot.manip.claw.state = Manipulator.ClawState.OPEN
        }

        if (pad.b && !prev.b) {
            bot.manip.wrist.state = Manipulator.WristState.BACK
        } else if (pad.x && !prev.x) {
            bot.manip.wrist.state = Manipulator.WristState.FRONT
        }

        if (pad.y && !prev.y) {
            bot.manip.extension.state = Manipulator.ExtensionState.BACK
        } else if (pad.a && !prev.a) {
            bot.manip.extension.state = Manipulator.ExtensionState.FRONT
        }

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

        bot.log.out["lift pos"] = bot.manip.lift.encoder.position

        prev.safeCopy(pad)
    }

    companion object {
        @JvmField var SLIDES_DEADZONE = 0.05
    }
}

fun Gamepad.shift() = right_trigger > 0.5 || left_trigger > 0.5