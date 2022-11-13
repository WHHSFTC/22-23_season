package org.thenuts.powerplay.opmode.commands

import org.thenuts.powerplay.subsystems.Lift
import org.thenuts.powerplay.subsystems.Manipulator
import org.thenuts.switchboard.command.Command
import org.thenuts.switchboard.dsl.mkSequential
import kotlin.time.Duration.Companion.milliseconds

class RetractCommand(val manip: Manipulator, val from: Lift.Height, val outputSide: Manipulator.OutputSide) : Command by mkSequential(strict = false, {
    val clearStop = from.pos < Lift.Height.MIN_CLEAR.pos && outputSide == Manipulator.OutputSide.PASSTHRU
    if (clearStop) {
        add(LiftCommand(manip.lift, Lift.State.RunTo(Lift.Height.MIN_CLEAR.pos)))
    }

    if (outputSide == Manipulator.OutputSide.PASSTHRU) {
        task { manip.extension.state = Manipulator.ExtensionState.INTAKE }
        delay(1000.milliseconds)

        task { manip.wrist.state = Manipulator.WristState.INTAKE }
        delay(1000.milliseconds)
    }

    add(LiftCommand(manip.lift, Lift.State.ZERO))
}) {
    override val postreqs: List<Pair<Command, Int>> = listOf(manip.lift to 10)
}