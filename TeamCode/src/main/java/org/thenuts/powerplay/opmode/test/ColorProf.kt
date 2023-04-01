package org.firstinspires.ftc.teamcode.opmode.test

import com.acmerobotics.dashboard.FtcDashboard
import com.acmerobotics.dashboard.config.Config
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry
import com.qualcomm.hardware.broadcom.BroadcomColorSensorImpl
import com.qualcomm.hardware.rev.RevColorSensorV3
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.exception.RobotCoreException
import com.qualcomm.robotcore.hardware.*
import org.thenuts.switchboard.structures.FullRingBuffer
import org.thenuts.switchboard.util.sinceJvmTime
import kotlin.time.Duration
import kotlin.time.DurationUnit

//@Disabled
@TeleOp
@Config
class ColorProf : com.qualcomm.robotcore.eventloop.opmode.OpMode() {
    lateinit var colorSensor: RevColorSensorV3
    var mode = 0
    private val prev = Gamepad()
    private val pad = Gamepad()
    private var safe = true
    private lateinit var ring: FullRingBuffer<Long>

    override fun init() {
        colorSensor = hardwareMap.get(RevColorSensorV3::class.java, "rightTape")
        telemetry = MultipleTelemetry(telemetry, FtcDashboard.getInstance().telemetry)
        telemetry.addLine("initialized")
        telemetry.update()
    }

    override fun start() {
        ring = FullRingBuffer(SAMPLE) { Duration.sinceJvmTime().inWholeNanoseconds }
    }

    override fun loop() {
        safe = try {
            prev.copy(pad)
            pad.copy(gamepad1)
            true
        } catch (ex: RobotCoreException) {
            false
        }

        when {
            safe && pad.left_bumper && !prev.left_bumper -> mode = (mode - 1).coerceIn(0, 4)
            safe && pad.right_bumper && !prev.right_bumper -> mode = (mode + 1).coerceIn(0, 4)
            safe && pad.a && !prev.a -> engage()
            safe && pad.b && !prev.b -> disengage()
        }
        telemetry.addData("safe", safe)

        when (mode) {
            0 -> {
                telemetry.addData("mode", "call each")
                val red = colorSensor.red()
                val green = colorSensor.green()
                val blue = colorSensor.blue()
                val alpha = colorSensor.alpha()
                telemetry.addData("red", red)
                telemetry.addData("green", green)
                telemetry.addData("blue", blue)
                telemetry.addData("alpha", alpha)
            }
            1 -> {
                telemetry.addData("mode", "call red, ignore rest")
                val red = colorSensor.red()
                telemetry.addData("red", red)
                telemetry.addData("green", 0)
                telemetry.addData("blue", 0)
                telemetry.addData("alpha", 0)
            }
            2 -> {
                telemetry.addData("mode", "norm colors")
                val norm = colorSensor.normalizedColors
                val red = (norm.red * 256).coerceIn(0.0f, 255.0f).toInt()
                val green = (norm.green * 256).coerceIn(0.0f, 255.0f).toInt()
                val blue = (norm.blue * 256).coerceIn(0.0f, 255.0f).toInt()
                val alpha = (norm.alpha * 256).coerceIn(0.0f, 255.0f).toInt()
                telemetry.addData("red", red)
                telemetry.addData("green", green)
                telemetry.addData("blue", blue)
                telemetry.addData("alpha", alpha)
            }
            3 -> {
                telemetry.addData("mode", "just check armed")
                val armed = colorSensor.deviceClient.isArmed
                telemetry.addData("armed", armed)
            }
            4 -> {
                telemetry.addData("mode", "just check engaged")
                val engaged = (colorSensor.deviceClient as? Engagable)?.isEngaged
                telemetry.addData("engaged", engaged)
            }
        }

        val oldTime = ring.read(0)
        val now = Duration.sinceJvmTime().inWholeNanoseconds
        ring.write(now)
        val meanCycle = (now - oldTime) / SAMPLE
        val frequency = if (meanCycle == 0L) Double.NaN else Duration.convert(1.0, DurationUnit.SECONDS, DurationUnit.NANOSECONDS) / meanCycle
        telemetry.addData("mean cycle (ms)", Duration.convert(meanCycle.toDouble(), DurationUnit.NANOSECONDS, DurationUnit.MILLISECONDS))
        telemetry.addData("frequency (Hz)", frequency)

        telemetry.update()
    }

    fun engage() {
        (colorSensor.deviceClient as? Engagable)?.engage()
    }

    fun disengage() {
        (colorSensor.deviceClient as? Engagable)?.disengage()
    }

    companion object {
        val SAMPLE = 20
    }
}
