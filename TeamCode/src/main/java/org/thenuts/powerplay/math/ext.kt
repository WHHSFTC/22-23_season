package org.thenuts.powerplay.math

import org.apache.commons.math3.linear.*
import kotlin.math.cos
import kotlin.math.sin

val Double.cos: Double get() = cos(this)
val Double.sin: Double get() = sin(this)

operator fun RealMatrix.invoke(v: RealVector): RealVector = this.operate(v)
operator fun RealMatrix.times(m: RealMatrix): RealMatrix = this.multiply(m)
operator fun RealMatrix.plus(m: RealMatrix): RealMatrix = this.add(m)
operator fun RealMatrix.minus(m: RealMatrix): RealMatrix = this.subtract(m)
val RealMatrix.inv: RealMatrix get() = MatrixUtils.inverse(this)
val RealMatrix.T: RealMatrix get() = this.transpose()

operator fun RealVector.plus(v: RealVector): RealVector = this.add(v)
operator fun RealVector.minus(v: RealVector): RealVector = this.subtract(v)
operator fun RealVector.times(s: Double): RealVector = this.mapMultiply(s)
operator fun RealVector.get(i: Int): Double = this.getEntry(i)
operator fun RealVector.set(i: Int, v: Double) = this.setEntry(i, v)

object rv {
    operator fun get(vararg elements: Double): RealVector = ArrayRealVector(elements, false)
    operator fun invoke(elements: DoubleArray): RealVector = ArrayRealVector(elements, false)
    fun full(size: Int, preset: Double): RealVector = ArrayRealVector(size, preset)
    fun zero(size: Int): RealVector = full(size, 0.0)
    fun one(size: Int): RealVector = full(size, 1.0)
}
object rm {
    operator fun get(vararg elements: RealVector): RealMatrix = Array2DRowRealMatrix(elements.map { it.toArray() }.toTypedArray())
    operator fun invoke(elements: Array<RealVector>): RealMatrix = Array2DRowRealMatrix(elements.map { it.toArray() }.toTypedArray())
    object diag {
        operator fun get(vararg elements: Double): RealMatrix = MatrixUtils.createRealDiagonalMatrix(elements)
        operator fun invoke(v: RealVector): RealMatrix = MatrixUtils.createRealDiagonalMatrix(v.toArray())
    }
    fun id(n: Int): RealMatrix = MatrixUtils.createRealIdentityMatrix(n)
    fun zero(m: Int, n: Int): RealMatrix = MatrixUtils.createRealMatrix(m, n)
    fun rotate(theta: Double): RealMatrix = this[
            rv[theta.cos, -theta.sin],
            rv[theta.sin,  theta.cos]
    ]
    fun rotatePoseDelta(theta: Double): RealMatrix = this[
            rv[theta.cos, -theta.sin, 0.0],
            rv[theta.sin,  theta.cos, 0.0],
            rv[0.0,        0.0,       1.0]
    ]
}