package org.thenuts.powerplay.subsystems

import com.qualcomm.hardware.rev.Rev2mDistanceSensor
import com.qualcomm.hardware.rev.RevColorSensorV3
import org.firstinspires.ftc.teamcode.opmode.vision.Vision
import org.thenuts.powerplay.acme.drive.SampleMecanumDrive
import org.thenuts.powerplay.game.Alliance
import org.thenuts.powerplay.game.Mode
import org.thenuts.powerplay.subsystems.output.Output
import org.thenuts.switchboard.command.Command
import org.thenuts.switchboard.core.Logger
import org.thenuts.switchboard.hardware.Configuration
import org.thenuts.switchboard.scheduler.HardwareCallable
import org.thenuts.switchboard.scheduler.*
import kotlin.time.Duration.Companion.milliseconds

class October(val log: Logger, val config: Configuration, val alliance: Alliance, val mode: Mode) : Robot() {
    val drive = SampleMecanumDrive(config.hwMap)
    val output = Output(log, config)
//    val intake = Intake(log, config)
//    val intake_dist = config.hwMap.get(Rev2mDistanceSensor::class.java, "intake_dist")
//    val passthru_dist = config.hwMap.get(Rev2mDistanceSensor::class.java, "passthru_dist")
    val intakeSensor = config.hwMap["intakeDist"] as Rev2mDistanceSensor
    val tapeDetector = TapeDetector(log, config.hwMap)
    val vision: Vision? = if (mode == Mode.AUTO) Vision(log, config, alliance) else null

    override val initCommands = listOf<Command>()
    override val startCommands = listOf<Command>(drive, output, output.lift /*, intake, intake.slides */)

    override val hardwareScheduler: HardwareCallable = bucket(20.milliseconds,
        listOf( // on ones
            // roadrunner drivetrain not managed through switchboard
            rot(2.milliseconds, output.arm, output.claw),
//            rot(2.milliseconds, intake.leftArm, intake.rightArm, intake.claw),
        ),
        listOf( // on twos
            all(output.lift.motor1, output.lift.motor2),
//            intake.slides.motor
        ),
        listOf(

        )
    )
}