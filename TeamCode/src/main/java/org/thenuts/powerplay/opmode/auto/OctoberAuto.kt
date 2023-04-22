package org.thenuts.powerplay.opmode.auto

import com.acmerobotics.roadrunner.geometry.Pose2d
import com.qualcomm.robotcore.hardware.DcMotor
import org.thenuts.powerplay.game.Alliance
import org.thenuts.powerplay.game.Mode
import org.thenuts.powerplay.opmode.CommandLinearOpMode
import org.thenuts.powerplay.subsystems.GlobalState
import org.thenuts.powerplay.subsystems.localization.KalmanLocalizer
import org.thenuts.powerplay.subsystems.October
import org.thenuts.switchboard.command.Command
import org.thenuts.switchboard.command.CommandScheduler
import org.thenuts.switchboard.dsl.mkSequential
import org.thenuts.switchboard.util.sinceJvmTime
import kotlin.math.PI
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

abstract class OctoberAuto : CommandLinearOpMode<October>(::October, Alliance.RED, Mode.AUTO) {
    private lateinit var cmd: Command
    var preAutoDelay = 0

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
        GlobalState.autoStopTime = Duration.sinceJvmTime()
        bot.drive.setWeightedDrivePower(Pose2d())
        bot.drive.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE)
    }

    override fun initLoopHook() {
        if (gamepad2.b) preAutoDelay += 10
        if (gamepad2.a && preAutoDelay > 0) preAutoDelay -= 10

        log.out["@DELAY"] = preAutoDelay.toDouble() / 1000.0
        log.out["signal"] = bot.vision?.signal?.finalTarget
    }

    override fun postStartHook() {
        bot.vision?.front?.stopDebug()
        bot.vision?.signal?.disable()
        bot.vision?.gamepad = null
        GlobalState.autoStartTime = Duration.sinceJvmTime()
        sched.addCommand(mkSequential {
            delay(preAutoDelay.milliseconds)
            add(cmd)
        })
    }

    override fun loopHook() {
        log.out["COMMAND ORDER"] = sched.nodes.filterIsInstance<CommandScheduler.CommandNode>().map { it.runner.cmd::class.simpleName }
    }
}
