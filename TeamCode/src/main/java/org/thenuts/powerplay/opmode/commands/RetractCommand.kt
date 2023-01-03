package org.thenuts.powerplay.opmode.commands

import org.thenuts.powerplay.subsystems.output.Lift
import org.thenuts.powerplay.subsystems.output.Output
import org.thenuts.switchboard.command.Command
import org.thenuts.switchboard.dsl.mkSequential
import kotlin.time.Duration.Companion.milliseconds

class RetractCommand(val manip: Output, val from: Lift.Height, val outputSide: Output.OutputSide) : Command by mkSequential(strict = false, {
    val clearStop = from.pos < Lift.Height.MIN_CLEAR.pos && outputSide == Output.OutputSide.PASSTHRU
    if (clearStop) {
        add(LiftCommand(manip.lift, Lift.State.RunTo(Lift.Height.MIN_CLEAR.pos)))
    }

    if (outputSide == Output.OutputSide.PASSTHRU) {
        task { manip.extension.state = Output.ExtensionState.INTAKE }
        delay(1000.milliseconds)

        task { manip.wrist.state = Output.WristState.INTAKE }
        delay(1000.milliseconds)
    }

    add(LiftCommand(manip.lift, Lift.State.ZERO))
}) {
    override val postreqs: List<Pair<Command, Int>> = listOf(manip.lift to 10)
}