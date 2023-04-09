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

    class Composition(val left: Transformation, val right: Transformation): Transformation {
        override val inputDimension: Int = right.inputDimension
        override val outputDimension: Int = left.outputDimension

        init {
            assert(left.inputDimension == right.outputDimension)
        }

        override fun invoke(x: RealVector): RealVector {
            return left(right(x))
        }

        override fun jacobian(x: RealVector): RealMatrix {
            return left.jacobian(right(x)) * right.jacobian(x)
        }
    }
}

operator fun Transformation.times(other: Transformation) = Transformation.Composition(this, other)