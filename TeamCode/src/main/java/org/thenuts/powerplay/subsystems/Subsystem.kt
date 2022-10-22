package org.thenuts.powerplay.subsystems

import org.thenuts.switchboard.command.Command
import org.thenuts.switchboard.hardware.HardwareOutput

interface Subsystem : Command {
    override val done: Boolean get() = false
    val outputs: List<HardwareOutput> get() = listOf()
    val children: List<Subsystem> get() = listOf()
}