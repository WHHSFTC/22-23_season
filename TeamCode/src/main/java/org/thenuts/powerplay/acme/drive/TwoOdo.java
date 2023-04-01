package org.thenuts.powerplay.acme.drive;

import static org.thenuts.powerplay.acme.drive.ThreeOdo.FRONT_OFFSET;
import static org.thenuts.powerplay.acme.drive.ThreeOdo.LATERAL_DISTANCE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.roadrunner.geometry.Pose2d;
import com.acmerobotics.roadrunner.localization.TwoTrackingWheelLocalizer;
import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;
import org.thenuts.powerplay.acme.util.Encoder;

import java.util.Arrays;
import java.util.List;

/*
 * Sample tracking wheel localizer implementation assuming the standard configuration:
 *
 *    /--------------\
 *    |     ____     |
 *    |     ----     |
 *    | ||        || |
 *    | ||        || |
 *    |              |
 *    |              |
 *    \--------------/
 *
 */
@Config
//public class StandardTrackingWheelLocalizer extends ThreeTrackingWheelLocalizer {
public class TwoOdo extends TwoTrackingWheelLocalizer {
    public static double TICKS_PER_REV = 8192;
    public static double WHEEL_RADIUS = 0.6889764; // in
    public static double GEAR_RATIO = 1; // output (wheel) speed / input (encoder) speed

    public static double BACK_OFFSET = -4.55380; // in; offset of the lateral wheel

    private Encoder leftEncoder, rightEncoder, backEncoder, frontEncoder;
    private BNO055IMU imu;

    // BACK IS INTAKE, FRONT IS OUTPUT
    public TwoOdo(HardwareMap hardwareMap, BNO055IMU imu) {
        super(Arrays.asList(
//                new Pose2d(0.82497-1.0, LATERAL_DISTANCE / 2, 0), // left
                new Pose2d(0.82497-1.0, -LATERAL_DISTANCE / 2, 0), // right
//                new Pose2d(BACK_OFFSET, -0.02949, Math.toRadians(90)) // back
                new Pose2d(FRONT_OFFSET, -5.0, Math.toRadians(90)) // front
        ));

//        leftEncoder = new Encoder(hardwareMap.get(DcMotorEx.class, "motorRF"));
        rightEncoder = new Encoder(hardwareMap.get(DcMotorEx.class, "motorLF"));
//        backEncoder = new Encoder(hardwareMap.get(DcMotorEx.class, "motorRB"));
        frontEncoder = new Encoder(hardwareMap.get(DcMotorEx.class, "motorLB"));
        this.imu = imu;

        // TODO: reverse any encoders using Encoder.setDirection(Encoder.Direction.REVERSE)
    }

    public static double encoderTicksToInches(double ticks) {
        return WHEEL_RADIUS * 2 * Math.PI * GEAR_RATIO * ticks / TICKS_PER_REV;
    }

    @NonNull
    @Override
    public List<Double> getWheelPositions() {
        return Arrays.asList(
//                encoderTicksToInches(leftEncoder.getCurrentPosition()),
                encoderTicksToInches(rightEncoder.getCurrentPosition()),
//                encoderTicksToInches(backEncoder.getCurrentPosition())
                encoderTicksToInches(frontEncoder.getCurrentPosition())
        );
    }

    @NonNull
    @Override
    public List<Double> getWheelVelocities() {
        // TODO: If your encoder velocity can exceed 32767 counts / second (such as the REV Through Bore and other
        //  competing magnetic encoders), change Encoder.getRawVelocity() to Encoder.getCorrectedVelocity() to enable a
        //  compensation method

        return Arrays.asList(
//                encoderTicksToInches(leftEncoder.getCorrectedVelocity()),
                encoderTicksToInches(rightEncoder.getCorrectedVelocity()),
//                encoderTicksToInches(backEncoder.getCorrectedVelocity())
                encoderTicksToInches(frontEncoder.getCorrectedVelocity())
        );
    }

    @Override
    public double getHeading() {
        return imu.getAngularOrientation().firstAngle;
    }

    @Nullable
    @Override
    public Double getHeadingVelocity() {
        return (double) imu.getAngularVelocity().zRotationRate;
    }
}
