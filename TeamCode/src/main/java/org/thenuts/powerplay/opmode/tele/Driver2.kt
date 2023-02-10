package org.thenuts.powerplay.opmode.tele

import com.acmerobotics.dashboard.config.Config
import com.qualcomm.robotcore.hardware.Gamepad
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import org.thenuts.powerplay.subsystems.output.VerticalSlides
import org.thenuts.powerplay.subsystems.output.Output
import org.thenuts.powerplay.subsystems.October
import org.thenuts.switchboard.command.Command
import org.thenuts.switchboard.command.combinator.SlotCommand
import org.thenuts.switchboard.dsl.mkSequential
import org.thenuts.switchboard.util.Frame
import java.lang.Integer.max
import kotlin.math.roundToInt

@Config
class Driver2(val gamepad: Gamepad, val bot: October, val outputSlot: SlotCommand) : Command {
    override val done: Boolean = false
    val prev = Gamepad()
    val pad = Gamepad()

    override val postreqs = listOf(bot.output to 10, bot.output.lift to 10, outputSlot to 10)
    var intakeHeight = 1

    override fun update(frame: Frame) {
        controls(frame)
    }

    override fun cleanup() {

    }

    fun queueTo(target: Output.OutputState) {
        outputSlot.queue(bot.output.transitionCommand(bot.output.state, target))
    }

    fun interruptTo(target: Output.OutputState) {
        outputSlot.interrupt(bot.output.transitionCommand(bot.output.state, target))
    }

    fun updateHeight() {
        outputSlot.interrupt(mkSequential {
            task {
                if (bot.output.outputHeight == 0)
                    bot.output.lift.runTo(0)
                else
                    bot.output.lift.runTo(bot.output.outputHeight)
            }
            await { !bot.output.isBusy }
        })
    }

    fun controls(frame: Frame) {
        pad.safeCopy(gamepad)

//        var slidesPower = -pad.right_stick_y.toDouble()
//        if (slidesPower > SLIDES_DEADZONE) {
//            slidesPower = (Lift.MAX_SLIDES_UP) * (slidesPower - SLIDES_DEADZONE)
//        } else if (slidesPower < -SLIDES_DEADZONE) {
//            slidesPower = (Lift.MAX_SLIDES_DOWN) * (slidesPower + SLIDES_DEADZONE)
//        } else {
//            slidesPower = 0.0
//        }
//
//        bot.lift.motor.power = slidesPower

        if (pad.right_trigger > 0.5) {
            bot.output.lift.state = VerticalSlides.State.Manual(-pad.right_stick_y.toDouble())
            bot.log.out["manual"] = -pad.right_stick_y.toDouble()
        } else if (prev.right_trigger > 0.5) {
            bot.output.lift.runTo(bot.output.lift.getPosition())
        }
//
//        val state = bot.manip.lift.state
//        if (state is Lift.State.Hold && state.isManual && pad.a && !prev.a) {
//            bot.manip.lift.state = bot.manip.translate(bot.manip.state.lift)
//        }

        if (pad.right_bumper && !prev.right_bumper) {
            // close claw
            outputSlot.interrupt(mkSequential {
                task { bot.output.claw.state = Output.ClawState.CLOSED }
                await { !bot.output.isBusy }
            })
/*
            when(bot.output.state) {
                Output.OutputState.GROUND -> {
                    interruptTo(Output.OutputState.INTAKE)
                }

                Output.OutputState.CLEAR_OPEN -> {
                    interruptTo(Output.OutputState.CLEAR)
                }

                Output.OutputState.S_DROP -> {
                    interruptTo(Output.OutputState.S_OUTPUT)
                }

                Output.OutputState.P_DROP -> {
                    interruptTo(Output.OutputState.P_OUTPUT)
                }
            }
*/
        } else if (pad.left_bumper && !prev.left_bumper) {
            // open claw
            outputSlot.interrupt(mkSequential {

                task { bot.output.claw.state = if (bot.output.claw.state == Output.ClawState.OPEN) Output.ClawState.WIDE
                else Output.ClawState.OPEN }
                await { !bot.output.isBusy }
            })
/*
            when(bot.output.state) {
                Output.OutputState.INTAKE -> {
                    interruptTo(Output.OutputState.GROUND)
                }

                Output.OutputState.CLEAR -> {
                    interruptTo(Output.OutputState.CLEAR_OPEN)
                }

                Output.OutputState.S_OUTPUT, Output.OutputState.S_LOWER -> {
                    interruptTo(Output.OutputState.S_DROP)
                }

                Output.OutputState.P_OUTPUT, Output.OutputState.P_LOWER -> {
                    interruptTo(Output.OutputState.P_DROP)
                }
            }
*/
        }

        if (pad.start && !prev.start) {
            bot.output.arm.servo.position = (bot.output.arm.servo.position - Output.STEP).coerceIn(0.01..0.99)
        } else if (pad.back && !prev.back) {
            bot.output.arm.servo.position = (bot.output.arm.servo.position + Output.STEP).coerceIn(0.01..0.99)
        }

        bot.log.out["arm position"] = bot.output.arm.servo.position

        if (pad.left_trigger > 0.5) {
            val liftState = bot.output.lift.state
            val prevPos = when (liftState) {
                VerticalSlides.State.IDLE, VerticalSlides.State.ZERO, VerticalSlides.State.FIND_EDGE, is VerticalSlides.State.Manual -> 0
                is VerticalSlides.State.RunTo -> liftState.pos
                is VerticalSlides.State.Hold -> liftState.pos
                is VerticalSlides.State.Profiled -> liftState.profile.end.roundToInt()
            }
            if (pad.dpad_up && !prev.dpad_up) {
                bot.output.lift.runTo(prevPos + VerticalSlides.CONE_STEP)
            } else if (pad.dpad_down && !prev.dpad_down) {
                bot.output.lift.runTo(max(0, prevPos - VerticalSlides.CONE_STEP))
            } else if (pad.dpad_left && !prev.dpad_left) {
                intakeHeight = (intakeHeight - 1).coerceIn(1..5)
                bot.output.arm.state = Output.ArmState.values()[intakeHeight - 1]
            } else if (pad.dpad_right && !prev.dpad_right) {
                intakeHeight = (intakeHeight + 1).coerceIn(1..5)
                bot.output.arm.state = Output.ArmState.values()[intakeHeight - 1]
            }
        } else {
            if (pad.dpad_up && !prev.dpad_up) {
                bot.output.outputHeight = VerticalSlides.Height.HIGH.pos
//                interruptTo(Output.OutputState.CLEAR)
                updateHeight()
                bot.output.arm.state = Output.ArmState.CLEAR
//                bot.output.lift.state = VerticalSlides.State.RunTo(VerticalSlides.Height.HIGH.pos)
            } else if (pad.dpad_left && !prev.dpad_left) {
                outputSlot.interrupt(mkSequential {
                    task { bot.output.lift.runTo(0) }
                    await { !bot.output.isBusy }
                    task { bot.output.arm.state = Output.ArmState.values()[intakeHeight - 1] }
                })
            } else if (pad.dpad_right && !prev.dpad_right) {
                bot.output.outputHeight = VerticalSlides.Height.MID.pos
//                interruptTo(Output.OutputState.CLEAR)
                updateHeight()
                bot.output.arm.state = Output.ArmState.CLEAR
//                bot.output.lift.state = VerticalSlides.State.RunTo(VerticalSlides.Height.MID.pos)
            } else if (pad.dpad_down && !prev.dpad_down) {
                bot.output.outputHeight = VerticalSlides.Height.LOW.pos
//                interruptTo(Output.OutputState.CLEAR)
                updateHeight()
                bot.output.arm.state = Output.ArmState.CLEAR
//                bot.output.lift.state = VerticalSlides.State.RunTo(VerticalSlides.Height.LOW.pos)
            }
        }

        if (pad.b && !prev.b) {
//            interruptTo(Output.OutputState.P_LOWER)
            outputSlot.interrupt(mkSequential {
                task { bot.output.arm.state = Output.ArmState.SAMESIDE_OUTPUT }
                await { !bot.output.isBusy }
            })
        } else if (pad.x && !prev.x) {
            interruptTo(Output.OutputState.S_LOWER)
            outputSlot.interrupt(mkSequential {
                task {
                    bot.output.arm.state =
                            if (bot.output.arm.state == Output.ArmState.PASSTHRU_HOVER) Output.ArmState.PASSTHRU_OUTPUT
                            else Output.ArmState.PASSTHRU_HOVER
                }
                await { !bot.output.isBusy }
            })
            /*if ((!prev.x || frame.n % 4 == 0L) && bot.output.claw.state != Output.ClawState.CLOSED) {
                val dist = bot.intake_dist.getDistance(DistanceUnit.INCH)
                bot.log.out["intake_dist"] = dist
                if (dist < CLAW_DIST)
                    bot.output.claw.state = Output.ClawState.CLOSED
//                    outputSlot.interrupt(mkSequential {
//                        task { bot.output.arm.state = Output.ArmState.CLEAR }
//                        await { !bot.output.isBusy }
//                    })
            }*/
        } else if (pad.y && !prev.y) {
//            interruptTo(Output.OutputState.CLEAR)
            outputSlot.interrupt(mkSequential {
                task { bot.output.arm.state = Output.ArmState.CLEAR }
                await { !bot.output.isBusy }
            })
        } else if (pad.a && !prev.a) {
//            interruptTo(Output.OutputState.GROUND)
            outputSlot.interrupt(mkSequential {
                if (bot.output.arm.state.pos < Output.ArmState.CLEAR.pos) {
                    task { bot.output.arm.state = Output.ArmState.CLEAR }
                    await { !bot.output.isBusy }
                }
                task { bot.output.lift.runTo(0) }
                await { !bot.output.isBusy }
                task { bot.output.arm.state = Output.ArmState.values()[intakeHeight - 1] }
                task { bot.output.claw.state = Output.ClawState.WIDE }
            })
        }

        bot.log.out["lift pos"] = bot.output.lift.encoder1.position

        prev.safeCopy(pad)
    }

    companion object {
        @JvmField var SLIDES_DEADZONE = 0.05
        @JvmField var CLAW_DIST = 3.75
    }
}