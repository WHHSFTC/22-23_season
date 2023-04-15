package org.thenuts.powerplay.opmode.commands

import org.thenuts.powerplay.subsystems.October
import org.thenuts.powerplay.subsystems.output.Output
import org.thenuts.switchboard.command.Command
import org.thenuts.switchboard.dsl.mkSequential
import kotlin.time.Duration.Companion.milliseconds

class PassthruOutputCommand(bot: October, height: Int): Command by mkSequential(strict = false, {
    task { bot.output.lift.runTo(height) }
    task { bot.output.arm.state = Output.ArmState.PASSTHRU_OUTPUT }
//    delay(400.milliseconds)
    await { !bot.output.isBusy }

    task { bot.output.claw.state = Output.ClawState.NARROW }
    task { bot.output.arm.state = Output.ArmState.INTAKE }
    await { bot.output.profiledServo.servo.position > Output.ArmState.CLEAR.pos }

    task { bot.output.lift.runTo(0) }
})