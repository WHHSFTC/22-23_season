package org.thenuts.powerplay.opmode.tele

import com.acmerobotics.dashboard.config.Config
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.thenuts.powerplay.game.Alliance
import org.thenuts.powerplay.game.Mode
import org.thenuts.powerplay.opmode.CommandLinearOpMode
import org.thenuts.powerplay.subsystems.GlobalState
import org.thenuts.powerplay.subsystems.localization.KalmanLocalizer
import org.thenuts.powerplay.subsystems.October
import org.thenuts.powerplay.subsystems.output.Output
import org.thenuts.switchboard.command.CommandScheduler
import org.thenuts.switchboard.command.combinator.SlotCommand
import org.thenuts.switchboard.util.sinceJvmTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@TeleOp
@Config
class OctoberTele : CommandLinearOpMode<October>(::October, Alliance.RED, Mode.TELE) {
    lateinit var outputSlot: SlotCommand

    override fun postInitHook() {
        bot.output.claw.state = Output.ClawState.CLOSED
        bot.output.arm.state = Output.ArmState.CLEAR
    }

    override fun postStartHook() {
        bot.drive.localizer = KalmanLocalizer(bot.drive.localizer, bot.drive.imu)
        bot.drive.poseEstimate = GlobalState.poseEstimate
        outputSlot = SlotCommand(postreqs = listOf(bot.output to 10, bot.output.lift to 10))

        sched.addCommand(outputSlot)
        sched.addCommand(Driver1(gamepad1, bot, outputSlot))
        sched.addCommand(Driver2(gamepad2, bot, outputSlot))

        while (WAIT && Duration.sinceJvmTime() < GlobalState.autoStartTime + WAIT_TIME.seconds && !gamepad1.start && !isStopRequested) {
            idle()
        }
    }

    override fun loopHook() {
        log.out["COMMAND ORDER"] = sched.nodes.filterIsInstance<CommandScheduler.CommandNode>().map { it.runner.cmd::class.simpleName }
    }

    companion object {
        @JvmField var WAIT_TIME = 41.5
        @JvmField var WAIT = false
    }
}