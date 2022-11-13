package org.thenuts.powerplay.opmode.tele

import com.acmerobotics.roadrunner.geometry.Pose2d
import com.qualcomm.robotcore.hardware.Gamepad
import org.thenuts.powerplay.subsystems.Lift
import org.thenuts.powerplay.subsystems.October
import org.thenuts.switchboard.command.Command
import org.thenuts.switchboard.util.Frame

class Luke(val gamepad: Gamepad, val bot: October) : Command {
    override val done: Boolean = false
    val prev = Gamepad()
    val pad = Gamepad()

    override val postreqs: List<Pair<Command, Int>> = listOf(bot.drive to 10)

    override fun update(frame: Frame) {
        pad.safeCopy(gamepad)

        var x = -pad.left_stick_y.toDouble()
        var y = -pad.left_stick_x.toDouble()
        val omega = -pad.right_stick_x.toDouble()

        if (pad.dpad_left || pad.dpad_right || pad.dpad_up || pad.dpad_down) {
            x = (if (pad.dpad_up) 1.0 else 0.0) - (if (pad.dpad_down) 1.0 else 0.0)
            y = (if (pad.dpad_left) 1.0 else 0.0) - (if (pad.dpad_right) 1.0 else 0.0)
        }

        val turtle = pad.left_trigger > 0.5 || pad.right_trigger > 0.5 || bot.manip.lift.getPosition() > Lift.Height.LOW.pos + 200
        val scalar = if (turtle) 0.5 else 1.0

        val pow = Pose2d(x * scalar, y * scalar, omega)

        bot.drive.setWeightedDrivePower(pow)
        bot.log.out["drive power"] = pow

        prev.safeCopy(pad)
    }
}