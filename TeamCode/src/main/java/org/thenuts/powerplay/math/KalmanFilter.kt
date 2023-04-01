package org.thenuts.powerplay.math

import org.apache.commons.math3.linear.*

class KalmanFilter(
    val dimension: Int,
    x0: RealVector,
    P0: RealMatrix,
    var F: RealMatrix, // transition matrix
    var B: RealMatrix, // control matrix
    var Q: RealMatrix, // process covariance
    var H: RealMatrix, // real -> measurement transformation
    var R: RealMatrix  // measurement covariance
) {
    var x: RealVector = x0 // filter mean
    var P: RealMatrix = P0 // filter covariance

    var y: RealVector = ArrayRealVector(dimension) // residual
    var K: RealMatrix = Array2DRowRealMatrix(dimension, dimension) // kalman gain

    val I: RealMatrix = MatrixUtils.createRealIdentityMatrix(dimension) // identity matrix

    val mean: RealVector get() = x
    val cov: RealMatrix get() = P

    init {
        assert(dimension == x0.dimension) { "x0 has wrong dimension" }
        assert(dimension == P0.rowDimension && dimension == P0.columnDimension) { "P0 has wrong dimensions" }
        assert(dimension == F.rowDimension && dimension == F.columnDimension) { "F has wrong dimensions" }
        assert(dimension == B.rowDimension && dimension == B.columnDimension) { "B has wrong dimensions" }
        assert(dimension == Q.rowDimension && dimension == Q.columnDimension) { "Q has wrong dimensions" }
        assert(dimension == H.rowDimension && dimension == H.columnDimension) { "H has wrong dimensions" }
        assert(dimension == R.rowDimension && dimension == R.columnDimension) { "R has wrong dimensions" }
    }

    fun processCov(Q: RealMatrix): KalmanFilter {
        this.Q = Q
        return this
    }

    fun processVar(r: RealVector): KalmanFilter = processCov(MatrixUtils.createRealDiagonalMatrix(r.toArray()))
    fun processVar(vararg r: Double): KalmanFilter = processCov(MatrixUtils.createRealDiagonalMatrix(r))

    fun controlMat(B: RealMatrix): KalmanFilter {
        this.B = B
        return this
    }

    fun predict(u: RealVector): KalmanFilter {
        // no assertion for dimensions of u at runtime
        x = F(x) + B(u)
        P = F * (P * F.T) + Q
        return this
    }
    fun predict(vararg u: Double): KalmanFilter = predict(ArrayRealVector(u, true))

//    fun observationBuilder(): ObservationBuilder {
//        return ObservationBuilder()
//    }

//    inner class ObservationBuilder {
//        private var observation_mean: RealVector? = null
//        private var observation_covariance: RealMatrix? = null
//
//        fun mean(mean: RealVector): ObservationBuilder {
//            this.observation_mean = mean
//            return this
//        }
//
//        fun mean(vararg mean: Double): ObservationBuilder = mean(rv(mean))
//
//        fun covariance(covariance: RealMatrix): ObservationBuilder {
//            this.observation_covariance = covariance
//            return this
//        }
//
//        fun variance(variance: RealVector): ObservationBuilder {
//            this.observation_covariance = rm.diag(variance)
//            return this
//        }
//
//        fun variance(vararg variance: Double): ObservationBuilder = variance(rv(variance))
//
//        fun std(std: RealVector): ObservationBuilder {
//            this.observation_covariance = rm.diag(std.map { it * it })
//            return this
//        }
//
//        fun std(vararg std: Double): ObservationBuilder = std(rv(std))
//
//        fun predict() {
//            val u = observation_mean ?: rv.zero(B.columnDimension)
//            val Q = observation_covariance ?: Q
//
//            x = F(x) + B(u)
//            P = F * (P * F.T) + Q
//        }
//
//        fun update() {
//            val z = observation_mean ?: rv.zero(H.rowDimension)
//            val R = observation_covariance ?: R
//
//            y = z - H(x)
//            K = P * H.T * (H * P * H.T + R).inv
//            x = x + K(y)
//            P = (I - K * H) * P
//        }
//    }

    fun measurementCov(R: RealMatrix): KalmanFilter {
        this.R = R
        return this
    }

    fun measurementVar(r: RealVector): KalmanFilter = measurementCov(MatrixUtils.createRealDiagonalMatrix(r.toArray()))
    fun measurementVar(vararg r: Double): KalmanFilter = measurementCov(MatrixUtils.createRealDiagonalMatrix(r))

    fun update(z: RealVector): KalmanFilter {
        y = z - H(x)
        K = P * H.T * (H * P * H.T + R).inv
        x = x + K(y)
        P = (I - K * H) * P
        return this
    }
    fun update(vararg z: Double): KalmanFilter = update(ArrayRealVector(z, true))
}