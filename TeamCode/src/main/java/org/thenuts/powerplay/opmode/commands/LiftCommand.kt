package org.thenuts.powerplay.opmode.commands

import org.thenuts.powerplay.subsystems.Lift
import org.thenuts.switchboard.command.Command
import org.thenuts.switchboard.dsl.mkSequential

class LiftCommand(val lift: Lift, val state: Lift.State) : Command by mkSequential(strict = false, {
    task {
        lift.state = state
    }

    await { !lift.isBusy }
}) {
    override val postreqs: List<Pair<Command, Int>> = listOf(lift to 10)
}
