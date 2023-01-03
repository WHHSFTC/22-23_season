package org.thenuts.powerplay.subsystems.intake

import org.thenuts.powerplay.subsystems.Subsystem
import org.thenuts.switchboard.core.Logger
import org.thenuts.switchboard.hardware.Configuration

/**
 * Class controlling all intake subsystems (slides, arm, claw)
 */
class Intake(val log: Logger, config: Configuration) : Subsystem {
    val slides = LinkageSlides(log, config)

    override val children: List<Subsystem> = listOf(slides)
}