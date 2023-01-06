package org.thenuts.powerplay.subsystems.util

import org.thenuts.switchboard.hardware.Servo
import kotlin.time.Duration
import kotlin.time.times

class LinkedServos(val first: Servo, val second: Servo, val zero: Pair<Double, Double>, val one: Pair<Double, Double>) : Servo {
    override var position: Double = 0.0
        set(value) {
            pair = zero + (one - zero) * value
            first.position = pair.first
            second.position = pair.second
            field = value
        }

    var pair: Pair<Double, Double> = zero
        private set

    override fun output(all: Boolean) {
        first.output(all)
        second.output(all)
    }

    override fun getWorstMean(): Duration {
        return 2 * super.getWorstMean()
    }

    operator fun Pair<Double, Double>.plus(that: Pair<Double, Double>) = this.first + that.first to this.second + that.second
    operator fun Pair<Double, Double>.minus(that: Pair<Double, Double>) = this.first - that.first to this.second - that.second
    operator fun Pair<Double, Double>.times(scalar: Double) = this.first * scalar to this.second * scalar
}
