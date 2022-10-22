package org.thenuts.powerplay.subsystems

import org.thenuts.powerplay.acme.drive.SampleMecanumDrive
import org.thenuts.switchboard.command.Command
import org.thenuts.switchboard.core.Logger
import org.thenuts.switchboard.hardware.Configuration
import org.thenuts.switchboard.scheduler.HardwareScheduler
import org.thenuts.switchboard.scheduler.*
import kotlin.time.Duration.Companion.milliseconds

class October(val log: Logger, val config: Configuration) : Robot() {
    val drive = SampleMecanumDrive(config.hwMap)
    val manip = Manipulator(log, config)

    override val initCommands = listOf<Command>()
    override val startCommands = listOf<Command>(drive, manip, manip.lift)

    override val hardwareScheduler: HardwareScheduler = bucket(20.milliseconds,
        listOf( // on ones
            // roadrunner drivetrain not managed through switchboard
        ),
        listOf( // on twos
            manip.lift.motor,
            rot(4.milliseconds, manip.extension, manip.wrist, manip.claw)
        ),
        listOf(

        )
    )
}