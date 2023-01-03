package org.thenuts.powerplay.opmode.commands

import org.thenuts.powerplay.subsystems.output.Lift
import org.thenuts.powerplay.subsystems.output.Output
import org.thenuts.switchboard.command.Command
import org.thenuts.switchboard.dsl.mkSequential
import kotlin.time.Duration.Companion.milliseconds

class OutputCommand(val manip: Output, val outputHeight: Lift.Height) : Command by mkSequential(strict = false, {
    add(LiftCommand(manip.lift, Lift.State.RunTo(Integer.max(0, outputHeight.pos - 4 * Lift.CONE_STEP))))

    task { manip.claw.state = Output.ClawState.OPEN }
    delay(2000.milliseconds)

    add(LiftCommand(manip.lift, Lift.State.RunTo(outputHeight.pos)))

    task { manip.claw.state = Output.ClawState.CLOSED }
    delay(1000.milliseconds)
}) {
    override val postreqs: List<Pair<Command, Int>> = listOf(manip.lift to 10)
}