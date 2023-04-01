package org.thenuts.powerplay.math

import org.apache.commons.math3.linear.ArrayRealVector
import org.apache.commons.math3.linear.RealMatrix
import org.apache.commons.math3.linear.RealVector
import java.util.*

sealed interface RandomVariable {
    val dimension: Int

    fun pdf(x: RealVector): Double
    fun cdf(x: RealVector): Double
    fun rvs(n: Int, seed: Long? = null): List<RealVector>

    fun add(other: RandomVariable): RandomVariable
    fun transform(transformation: Transformation): RandomVariable

    data class Gaussian(override val dimension: Int, val mean: RealVector, val cov: RealMatrix) : RandomVariable {
        override fun pdf(x: RealVector): Double {
            TODO("Not yet implemented")
        }

        override fun cdf(x: RealVector): Double {
            TODO("Not yet implemented")
        }

        override fun rvs(n: Int, seed: Long?): List<RealVector> {
            val rnd = seed?.let(::Random) ?: Random()
            TODO("Not yet implemented")
        }

        override fun add(other: RandomVariable): RandomVariable {
            assert(dimension == other.dimension)
            if (other is Gaussian) {
                return Gaussian(dimension, mean + other.mean, cov + other.cov)
            }
            TODO("Not yet implemented")
        }

        override fun transform(transformation: Transformation): RandomVariable {
            assert(dimension == transformation.inputDimension)
            val J = transformation.jacobian(mean)
            return Gaussian(dimension, transformation(mean), J * (cov * J.T))
        }
    }
}