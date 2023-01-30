package org.thenuts.powerplay.opmode.tele

import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.thenuts.powerplay.game.Alliance
import org.thenuts.powerplay.game.Mode
import org.thenuts.powerplay.opmode.CommandLinearOpMode
import org.thenuts.powerplay.subsystems.October
import org.thenuts.switchboard.command.CommandScheduler
import org.thenuts.switchboard.command.combinator.SlotCommand

@TeleOp
class SoloTele : CommandLinearOpMode<October>(::October, Alliance.RED, Mode.TELE) {
    lateinit var outputSlot: SlotCommand

    override fun postStartHook() {
        outputSlot = SlotCommand(postreqs = listOf(bot.output to 10, bot.output.lift to 10))

        sched.addCommand(outputSlot)
        sched.addCommand(SoloDriver(gamepad1, bot, outputSlot))
//        sched.addCommand(SoloDriver(gamepad2, bot, outputSlot))
    }

    override fun loopHook() {
        log.out["COMMAND ORDER"] = sched.nodes.filterIsInstance<CommandScheduler.CommandNode>().map { it.runner.cmd::class.simpleName }
    }
}