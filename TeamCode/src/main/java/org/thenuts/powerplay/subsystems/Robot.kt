package org.thenuts.powerplay.subsystems

import org.thenuts.switchboard.command.Command
import org.thenuts.switchboard.scheduler.HardwareCallable

abstract class Robot {
    abstract val initCommands: List<Command>
    abstract val startCommands: List<Command>

    abstract val hardwareScheduler: HardwareCallable
}