package org.thenuts.powerplay.math

import org.apache.commons.math3.linear.RealMatrix
import org.apache.commons.math3.linear.RealVector

sealed interface Transformation {
    operator fun invoke(x: RealVector): RealVector

    fun jacobian(x: RealVector): RealMatrix

    val inputDimension: Int
    val outputDimension: Int

    class Linear(val matrix: RealMatrix) : Transformation {
        override val inputDimension: Int = matrix.columnDimension
        override val outputDimension: Int = matrix.rowDimension

        override fun invoke(x: RealVector): RealVector {
            return matrix(x)
        }

        override fun jacobian(x: RealVector): RealMatrix {
            return matrix
        }
    }
}