package org.thenuts.powerplay.opmode.tele

import android.util.Log
import com.qualcomm.robotcore.exception.RobotCoreException
import com.qualcomm.robotcore.hardware.Gamepad

fun Gamepad.safeCopy(that: Gamepad) {
    try {
        this.copy(that)
    } catch (ex: RobotCoreException) {
        Log.e("TELE", "Gamepad copy resulted in error ${ex.localizedMessage}")
    }
}
