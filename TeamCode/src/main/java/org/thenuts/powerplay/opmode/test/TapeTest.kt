package org.firstinspires.ftc.teamcode.opmode.test

import com.acmerobotics.dashboard.FtcDashboard
import com.acmerobotics.dashboard.config.Config
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.hardware.Gamepad
import org.thenuts.powerplay.subsystems.TapeDetector
import org.thenuts.switchboard.core.Logger

//@Disabled
@TeleOp
@Config
class TapeTest : com.qualcomm.robotcore.eventloop.opmode.OpMode() {
    lateinit var tapeDetector: TapeDetector
    lateinit var log: Logger
    private val prev = Gamepad()

    override fun init() {
        telemetry = MultipleTelemetry(telemetry, FtcDashboard.getInstance().telemetry)
        telemetry.addLine("initialized")
        telemetry.update()
    }

    override fun start() {
        log = Logger(telemetry)
        tapeDetector = TapeDetector(log, hardwareMap)
        tapeDetector.enable()
    }

    override fun loop() {
        if (gamepad1.a && !prev.a) {
            tapeDetector.disable()
        } else if (gamepad1.b && !prev.b) {
            tapeDetector.enable()
        }
        if (tapeDetector.state != TapeDetector.TapeState.DISABLED)
            tapeDetector.read()
        gamepad1.copy(prev)
        log.out["state"] = tapeDetector.state
        log.out["SATURATION_THRESHOLD"] = TapeDetector.BLUE_THRESHOLD
        log.update()
    }
}