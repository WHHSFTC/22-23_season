package org.thenuts.powerplay.opmode.test;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;

@Autonomous(group = "test")
public class DriveMotorTest extends OpMode {
    DcMotorEx rf, rb, lb, lf;

    @Override
    public void init() {
        telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());

        rf = (DcMotorEx) hardwareMap.dcMotor.get("motorRF");
        rb = (DcMotorEx) hardwareMap.dcMotor.get("motorRB");
        lb = (DcMotorEx) hardwareMap.dcMotor.get("motorLB");
        lf = (DcMotorEx) hardwareMap.dcMotor.get("motorLF");

        lf.setDirection(DcMotorSimple.Direction.REVERSE);
        lb.setDirection(DcMotorSimple.Direction.REVERSE);
//        rf.setDirection(DcMotorSimple.Direction.REVERSE);
//        rb.setDirection(DcMotorSimple.Direction.REVERSE);
    }

    @Override
    public void loop() {
        double x = -gamepad1.left_stick_y;

        rf.setPower(x);
        rb.setPower(x);
        lb.setPower(x);
        lf.setPower(x);

        telemetry.addData("rf", rf.getVelocity(AngleUnit.DEGREES) / 60);
        telemetry.addData("rb", rb.getVelocity(AngleUnit.DEGREES) / 60);
        telemetry.addData("lb", lb.getVelocity(AngleUnit.DEGREES) / 60);
        telemetry.addData("lf", lf.getVelocity(AngleUnit.DEGREES) / 60);
        telemetry.update();
    }

    @Override
    public void stop() {
        super.stop();
        rf.setPower(0);
        rb.setPower(0);
        lb.setPower(0);
        lf.setPower(0);
    }
}
