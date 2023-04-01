package org.thenuts.powerplay.opmode.tele

import com.qualcomm.robotcore.hardware.Gamepad
import org.thenuts.powerplay.subsystems.output.VerticalSlides
import org.thenuts.powerplay.subsystems.output.Output
import org.thenuts.powerplay.subsystems.October
import org.thenuts.switchboard.command.Command
import org.thenuts.switchboard.command.combinator.SlotCommand
import org.thenuts.switchboard.dsl.mkSequential
import org.thenuts.switchboard.util.Frame
import java.lang.Integer.max
import kotlin.math.roundToInt

class SoloDriver(val gamepad: Gamepad, val bot: October, val outputSlot: SlotCommand) : Command {
    override val done: Boolean = false
    val pad1 = Gamepad()
    val pad2 = Gamepad()

    val d1 = Driver1(pad1, bot)
    val d2 = Driver2(pad2, bot, outputSlot)

    override val prereqs = d1.prereqs + d2.prereqs
    override val postreqs = d1.postreqs + d2.postreqs

    override fun update(frame: Frame) {
        pad1.safeCopy(gamepad)
        pad2.safeCopy(pad1)
        if (pad1.right_trigger > 0.5) pad1.right_stick_x = 0.0f
        pad1.dpad_down = false
        pad1.dpad_up = false
        pad1.dpad_left = false
        pad1.dpad_right = false
        pad1.left_bumper = false
        pad1.right_bumper = false
        d1.update(frame)
        d2.update(frame)
    }

    override fun cleanup() {
        d1.cleanup()
        d2.cleanup()
    }
}