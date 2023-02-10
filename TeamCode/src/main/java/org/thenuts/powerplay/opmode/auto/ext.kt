package org.thenuts.powerplay.opmode.auto

import org.thenuts.powerplay.acme.trajectorysequence.TrajectorySequenceBuilder
import kotlin.math.PI


fun Double.angleWrap(): Double {
    var x = this;
    while (x > PI) {
        x -= 2 * PI
    }
    while (x <= -PI) {
        x += 2 * PI
    }
    return x
}

fun TrajectorySequenceBuilder.turnWrap(angle: Double) = turn(angle.angleWrap())
