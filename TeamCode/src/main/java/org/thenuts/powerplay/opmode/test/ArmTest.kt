package org.thenuts.powerplay.opmode.test

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import org.thenuts.powerplay.game.Alliance
import org.thenuts.powerplay.game.Mode
import org.thenuts.powerplay.opmode.CommandLinearOpMode
import org.thenuts.powerplay.subsystems.output.Output
import org.thenuts.powerplay.subsystems.October
import org.thenuts.switchboard.command.Command
import org.thenuts.switchboard.command.CommandScheduler
import org.thenuts.switchboard.dsl.mkLinear

@Autonomous
class ArmTest : CommandLinearOpMode<October>(::October, Alliance.RED, Mode.TEST) {
    lateinit var cmd: Command
    override fun postStartHook() {
        cmd = mkLinear {
            bot.output.claw.state = Output.ClawState.NARROW
            while (true) {
                bot.output.arm.state = Output.ArmState.PASSTHRU_OUTPUT
                do yield() while (bot.output.arm.isBusy)
                bot.output.arm.state = Output.ArmState.SAMESIDE_OUTPUT
                do yield() while (bot.output.arm.isBusy)
            }
        }
        sched.addCommand(cmd)
    }

    override fun loopHook() {
        if (cmd.done) {
            stop()
        }
        log.out["COMMAND ORDER"] = sched.nodes.filterIsInstance<CommandScheduler.CommandNode>().map { it.runner.cmd::class.simpleName }
    }
}