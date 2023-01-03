package org.thenuts.powerplay.opmode.commands

import org.thenuts.powerplay.subsystems.output.VerticalSlides
import org.thenuts.switchboard.command.Command
import org.thenuts.switchboard.dsl.mkSequential

class LiftCommand(val lift: VerticalSlides, val state: VerticalSlides.State) : Command by mkSequential(strict = false, {
    task {
        lift.state = state
    }

    await { !lift.isBusy }
}) {
    override val postreqs: List<Pair<Command, Int>> = listOf(lift to 10)
}
