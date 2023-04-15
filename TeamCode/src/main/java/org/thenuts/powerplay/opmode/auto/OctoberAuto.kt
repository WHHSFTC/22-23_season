package org.thenuts.powerplay.opmode.auto

import org.thenuts.powerplay.game.Alliance
import org.thenuts.powerplay.game.Mode
import org.thenuts.powerplay.opmode.CommandLinearOpMode
import org.thenuts.powerplay.subsystems.GlobalState
import org.thenuts.powerplay.subsystems.localization.KalmanLocalizer
import org.thenuts.powerplay.subsystems.October
import org.thenuts.switchboard.command.Command
import org.thenuts.switchboard.command.CommandScheduler
import org.thenuts.switchboard.util.sinceJvmTime
import kotlin.math.PI
import kotlin.time.Duration

abstract class OctoberAuto : CommandLinearOpMode<October>(::October, Alliance.RED, Mode.AUTO) {
    private lateinit var cmd: Command

    abstract fun generateCommand(): Command

    override fun postInitHook() {
        bot.vision?.front?.startDebug()
        bot.vision?.signal?.enable()
        bot.vision?.gamepad = gamepad1

        bot.output.lift.zeroPosition()

        bot.drive.externalHeading = PI
        KalmanLocalizer.TELE_HEADING_OFFSET = (bot.drive.rawExternalHeading - bot.drive.externalHeading).angleWrap()

        cmd = generateCommand()
    }

    override fun stopHook() {
        bot.vision?.front?.stopDebug()
        bot.vision?.signal?.disable()
        GlobalState.poseEstimate = bot.drive.poseEstimate
    }

    override fun initLoopHook() {
        log.out["signal"] = bot.vision?.signal?.finalTarget
    }

    override fun postStartHook() {
        bot.vision?.front?.stopDebug()
        bot.vision?.signal?.disable()
        bot.vision?.gamepad = null
        GlobalState.autoStartTime = Duration.sinceJvmTime()
        sched.addCommand(cmd)
    }

    override fun loopHook() {
        log.out["COMMAND ORDER"] = sched.nodes.filterIsInstance<CommandScheduler.CommandNode>().map { it.runner.cmd::class.simpleName }
    }
}
