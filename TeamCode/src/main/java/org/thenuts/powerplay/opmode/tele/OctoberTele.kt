package org.thenuts.powerplay.opmode.tele

import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.thenuts.powerplay.game.Alliance
import org.thenuts.powerplay.game.Mode
import org.thenuts.powerplay.opmode.CommandLinearOpMode
import org.thenuts.powerplay.subsystems.October
import org.thenuts.switchboard.command.CommandScheduler

@TeleOp
class OctoberTele : CommandLinearOpMode<October>(::October, Alliance.RED, Mode.TELE) {
    override fun postStartHook() {
        sched.addCommand(Driver1(gamepad1, bot))
        sched.addCommand(Driver2(gamepad2, bot))
    }

    override fun loopHook() {
        log.out["COMMAND ORDER"] = sched.nodes.filterIsInstance<CommandScheduler.CommandNode>().map { it.runner.cmd::class.simpleName }
    }
}