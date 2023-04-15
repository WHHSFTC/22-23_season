package org.firstinspires.ftc.teamcode.opmode.test

import android.graphics.Color
import com.acmerobotics.dashboard.FtcDashboard
import com.acmerobotics.dashboard.config.Config
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry
import com.qualcomm.hardware.rev.Rev2mDistanceSensor
import com.qualcomm.hardware.rev.RevColorSensorV3
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.exception.RobotCoreException
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.Gamepad
import com.qualcomm.robotcore.hardware.Servo
import com.qualcomm.robotcore.hardware.VoltageSensor
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import kotlin.math.pow
import kotlin.math.roundToInt

//@Disabled
@TeleOp
@Config
class DistTest : com.qualcomm.robotcore.eventloop.opmode.OpMode() {
    lateinit var distanceSensor: Rev2mDistanceSensor
    private val prev = Gamepad()
    private var safe = true

    override fun init() {
        telemetry = MultipleTelemetry(telemetry, FtcDashboard.getInstance().telemetry)
        telemetry.addLine("initialized")
        telemetry.update()
    }

    override fun start() {
        distanceSensor = hardwareMap["intakeDist"] as Rev2mDistanceSensor
    }

    override fun loop() {
        telemetry.addData("distance", distanceSensor.getDistance(DistanceUnit.INCH))
        telemetry.update()

        safe = try {
            prev.copy(gamepad1)
            true
        } catch (ex: RobotCoreException) {
            false
        }
    }
}