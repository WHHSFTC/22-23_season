package org.thenuts.powerplay.subsystems

import android.graphics.Color
import com.acmerobotics.dashboard.config.Config
import com.qualcomm.hardware.rev.RevColorSensorV3
import com.qualcomm.robotcore.hardware.HardwareMap
import org.thenuts.switchboard.core.Logger
import kotlin.math.max

@Config
class TapeDetector(val log: Logger?, hwMap: HardwareMap) {
    val leftSensor = hwMap.colorSensor["leftTape"] as RevColorSensorV3
    val rightSensor = hwMap.colorSensor["rightTape"] as RevColorSensorV3

    enum class TapeState {
        MISSING, CENTERED, LEFT, RIGHT, FAR_LEFT, FAR_RIGHT, DISABLED
    }

    var state: TapeState = TapeState.MISSING
        private set

    fun disable() {
        state = TapeState.DISABLED
        leftSensor.enableLed(false)
        rightSensor.enableLed(false)
    }

    fun enable() {
        if (state != TapeState.DISABLED)
            return
        state = TapeState.MISSING
        leftSensor.enableLed(true)
        rightSensor.enableLed(true)
    }

    fun isOverTape(sensorV3: RevColorSensorV3, name: String): Boolean {
        val norm = sensorV3.normalizedColors
        val hsv = FloatArray(3)
        Color.RGBToHSV(
            (norm.red * 256.0).toInt().coerceIn(0..255),
            (norm.green * 256.0).toInt().coerceIn(0..255),
            (norm.blue * 256.0).toInt().coerceIn(0..255),
            hsv
        )
//        log?.let { it.out[name] = hsv[2] }
//        return hsv[2] > SATURATION_THRESHOLD

        log?.let { it.out[name + "blue"] = norm.blue }
        log?.let { it.out[name + "red"] = norm.red }
        return norm.blue > BLUE_THRESHOLD || norm.red > RED_THRESHOLD
    }

    fun read(): TapeState {
        enable()
        val left = isOverTape(leftSensor, "leftTape")
        val right = isOverTape(rightSensor, "rightTape")
        log?.let { it.out["BLUE_THRESHOLD"] = BLUE_THRESHOLD }
        log?.let { it.out["RED_THRESHOLD"] = RED_THRESHOLD }

        state = when {
            left && right -> TapeState.CENTERED
            left && !right -> TapeState.LEFT
            !left && right -> TapeState.RIGHT
            state == TapeState.LEFT -> TapeState.FAR_LEFT
            state == TapeState.RIGHT -> TapeState.FAR_RIGHT
            state == TapeState.FAR_LEFT || state == TapeState.FAR_RIGHT -> state
            else -> TapeState.MISSING
        }

        return state
    }

    companion object {
        @JvmField var BLUE_THRESHOLD = 0.004
        @JvmField var RED_THRESHOLD = 0.003
    }
}