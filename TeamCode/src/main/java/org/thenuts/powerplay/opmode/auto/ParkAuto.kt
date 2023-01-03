package org.thenuts.powerplay.opmode.auto

import com.acmerobotics.roadrunner.geometry.Pose2d
import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import org.thenuts.powerplay.game.Alliance
import org.thenuts.powerplay.game.Mode
import org.thenuts.powerplay.opmode.CommandLinearOpMode
import org.thenuts.powerplay.subsystems.output.Output
import org.thenuts.powerplay.subsystems.October
import org.thenuts.switchboard.command.Command
import org.thenuts.switchboard.command.CommandScheduler
import org.thenuts.switchboard.dsl.mkSequential
import kotlin.time.Duration.Companion.milliseconds

@Autonomous(preselectTeleOp = "OctoberTele")
class ParkAuto : CommandLinearOpMode<October>(::October, Alliance.RED, Mode.AUTO) {
    lateinit var cmd: Command
    override fun postStartHook() {
        cmd = mkSequential {
            task {
                bot.drive.setWeightedDrivePower(Pose2d(0.5, 0.0, 0.0))
            }
            delay(800.milliseconds)
            task {
                bot.drive.setWeightedDrivePower(Pose2d())
                bot.output.claw.state = Output.ClawState.OPEN
            }
            delay(2000.milliseconds)
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