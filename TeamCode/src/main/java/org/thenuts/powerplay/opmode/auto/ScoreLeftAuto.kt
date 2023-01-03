package org.thenuts.powerplay.opmode.auto

import com.acmerobotics.roadrunner.geometry.Pose2d
import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import org.thenuts.powerplay.game.Alliance
import org.thenuts.powerplay.game.Mode
import org.thenuts.powerplay.game.Signal
import org.thenuts.powerplay.opmode.CommandLinearOpMode
import org.thenuts.powerplay.opmode.commands.ExtendCommand
import org.thenuts.powerplay.opmode.commands.RetractCommand
import org.thenuts.powerplay.opmode.commands.OutputCommand
import org.thenuts.powerplay.opmode.commands.go
import org.thenuts.powerplay.subsystems.output.Lift
import org.thenuts.powerplay.subsystems.output.Output
import org.thenuts.powerplay.subsystems.October
import org.thenuts.switchboard.command.Command
import org.thenuts.switchboard.command.CommandScheduler
import org.thenuts.switchboard.dsl.mkSequential
import kotlin.math.PI
import kotlin.time.Duration.Companion.milliseconds

//@Autonomous(preselectTeleOp = "OctoberTele")
abstract class ScoreLeftAuto(val scoreHeight: Lift.Height) : CommandLinearOpMode<October>(::October, Alliance.RED, Mode.AUTO) {
    lateinit var cmd: Command
    override fun postInitHook() {
        bot.vision?.front?.startDebug()
        bot.vision?.signal?.enable()
        bot.vision?.gamepad = gamepad1
    }

    override fun stopHook() {
        bot.vision?.front?.stopDebug()
        bot.vision?.signal?.disable()
    }

    override fun initLoopHook() {
        log.out["signal"] = bot.vision?.signal?.finalTarget
    }
    override fun postStartHook() {
        bot.vision?.front?.stopDebug()
        bot.vision?.signal?.disable()
        bot.vision?.gamepad = null
        bot.drive.poseEstimate = Pose2d(0.0, -7.0, PI)
        cmd = mkSequential {
            add(ExtendCommand(bot.output, scoreHeight, Output.OutputSide.SAMESIDE))
            go(bot.drive, Pose2d(0.0, -7.0, PI)) {
                strafeRight(7.0)
                back(50.0)
                forward(22.0)
            }
            go(bot.drive, Pose2d(28.0, 0.0, PI)) {
                strafeRight(16.0)
                back(6.0)
            }
            add(OutputCommand(bot.output, scoreHeight))
            go(bot.drive, Pose2d(34.0, 16.0, PI)) {
                forward(8.0)
                strafeLeft(16.0)
            }
            add(RetractCommand(bot.output, scoreHeight, Output.OutputSide.SAMESIDE))
            switch({ bot.vision!!.signal.finalTarget }) {
                value(Signal.LEFT) {
                    go(bot.drive, Pose2d(26.0, 0.0, PI)) {
                        strafeRight(27.0)
//                        turn(-PI)
                    }
                }
                value(Signal.MID) {
//                    go(bot.drive, Pose2d(26.0, 0.0, PI)) {
//                        turn(-PI)
//                    }
                }
                value(Signal.RIGHT) {
                    go(bot.drive, Pose2d(26.0, 0.0, PI)) {
                        strafeLeft(30.0)
//                        turn(-PI)
                    }
                }
            }
            delay(1000.milliseconds)
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

@Autonomous(preselectTeleOp = "OctoberTele", group = "_official")
class LeftAuto : ScoreLeftAuto(Lift.Height.LOW)

@Autonomous(preselectTeleOp = "OctoberTele", group = "_official")
class RightAuto : ScoreLeftAuto(Lift.Height.MID)
