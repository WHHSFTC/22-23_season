package org.firstinspires.ftc.teamcode.opmode.test

import android.graphics.Color
import com.acmerobotics.dashboard.FtcDashboard
import com.acmerobotics.dashboard.config.Config
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry
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
class ColorTest : com.qualcomm.robotcore.eventloop.opmode.OpMode() {
    lateinit var colorSensor: RevColorSensorV3
    lateinit var motor: DcMotorEx
    private val prev = Gamepad()
    private var safe = true

    override fun init() {
        telemetry = MultipleTelemetry(telemetry, FtcDashboard.getInstance().telemetry)
        telemetry.addLine("initialized")
        telemetry.update()
    }

    override fun start() {
        colorSensor = hardwareMap.get(RevColorSensorV3::class.java, "rightTape")
        colorSensor.enableLed(true)
    }

    override fun loop() {
        val hsv = FloatArray(3)
        val red = colorSensor.red()
        val green = colorSensor.green()
        val blue = colorSensor.blue()
        val alpha = colorSensor.alpha()
        val s = 256.0 / 2.0.pow(16)
        Color.RGBToHSV(
            (red * s).roundToInt(),
            (green * s).roundToInt(),
            (blue * s).roundToInt(),
            hsv
        )
        telemetry.addData("red", red)
        telemetry.addData("green", green)
        telemetry.addData("blue", blue)
        telemetry.addData("alpha", alpha)
        telemetry.addData("hue", hsv[0])
        telemetry.addData("saturation", hsv[1])
        telemetry.addData("value", hsv[2])
        val distance = colorSensor.getDistance(DistanceUnit.INCH)
        telemetry.addData("distance", distance)
        telemetry.addData("normalized alpha / 255", alpha / 255.0)
        val norm = colorSensor.normalizedColors
        telemetry.addData("normalized colors", "%f %f %f %f", norm.red, norm.green, norm.blue, norm.alpha)

        telemetry.update()

        safe = try {
            prev.copy(gamepad1)
            true
        } catch (ex: RobotCoreException) {
            false
        }
    }
}