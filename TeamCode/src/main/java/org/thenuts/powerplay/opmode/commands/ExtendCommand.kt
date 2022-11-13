package org.thenuts.powerplay.opmode.commands

import org.thenuts.powerplay.subsystems.Lift
import org.thenuts.powerplay.subsystems.Lift.Companion.CONE_STEP
import org.thenuts.powerplay.subsystems.Manipulator
import org.thenuts.switchboard.command.Command
import org.thenuts.switchboard.dsl.mkSequential
import kotlin.time.Duration.Companion.milliseconds

class ExtendCommand(val manip: Manipulator, val target: Lift.Height, val outputSide: Manipulator.OutputSide) : Command by mkSequential(strict = false, {
    val clearStop = target.pos < Lift.Height.MIN_CLEAR.pos && outputSide == Manipulator.OutputSide.PASSTHRU
    if (clearStop) {
        add(LiftCommand(manip.lift, Lift.State.RunTo(Lift.Height.MIN_CLEAR.pos)))
    } else {
        add(LiftCommand(manip.lift, Lift.State.RunTo(target.pos + CONE_STEP * 2)))
    }

    if (outputSide == Manipulator.OutputSide.PASSTHRU) {
        task { manip.wrist.state = Manipulator.WristState.OUTPUT }
        delay(1000.milliseconds)

        task { manip.extension.state = Manipulator.ExtensionState.OUTPUT }
        delay(1000.milliseconds)
    }

    if (clearStop) {
        add(LiftCommand(manip.lift, Lift.State.RunTo(target.pos + CONE_STEP * 2)))
    }
}) {
    override val postreqs: List<Pair<Command, Int>> = listOf(manip.lift to 10)
}