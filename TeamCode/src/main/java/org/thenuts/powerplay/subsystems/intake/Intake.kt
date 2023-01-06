package org.thenuts.powerplay.subsystems.intake

import org.thenuts.powerplay.subsystems.Subsystem
import org.thenuts.powerplay.subsystems.output.Output
import org.thenuts.powerplay.subsystems.util.LinkedServos
import org.thenuts.powerplay.subsystems.util.StatefulServo
import org.thenuts.switchboard.core.Logger
import org.thenuts.switchboard.hardware.Configuration
import org.thenuts.switchboard.hardware.HardwareOutput

/**
 * Class controlling all intake subsystems (slides, arm, claw)
 */
class Intake(val log: Logger, config: Configuration) : Subsystem {
    val slides = LinkageSlides(log, config)

    enum class ClawState(override val pos: Double) : StatefulServo.ServoPosition {
        OPEN(0.5), CLOSED(0.0)
    }

    enum class ArmState(override val pos: Double) : StatefulServo.ServoPosition {
        ONE(0.0), TWO(0.0), THREE(0.0), FOUR(0.0), FIVE(0.0),
        CLEAR(0.0), TRANSFER(0.0), STORE(0.0), INIT(0.0)
    }

    val claw = StatefulServo(config.servos["claw_intake"], ClawState.OPEN)

    val leftArm = config.servos["left_intake"]
    val rightArm = config.servos["right_intake"]

    val arm = StatefulServo(LinkedServos(leftArm, rightArm, 0.0 to 1.0, 1.0 to 0.0), ArmState.INIT)

    override val children: List<Subsystem> = listOf(slides)
    override val outputs: List<HardwareOutput> = listOf(claw, leftArm, rightArm)
}