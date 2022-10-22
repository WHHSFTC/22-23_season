package org.thenuts.powerplay;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;

@TeleOp
public class DriveTele extends OpMode {
    DcMotor rf, rb, lb, lf;
    DcMotor slides;

    @Override
    public void init() {
        rf = hardwareMap.dcMotor.get("motorRF");
        rb = hardwareMap.dcMotor.get("motorRB");
        lb = hardwareMap.dcMotor.get("motorLB");
        lf = hardwareMap.dcMotor.get("motorLF");

//        rf.setDirection(DcMotorSimple.Direction.REVERSE);
//        rb.setDirection(DcMotorSimple.Direction.REVERSE);
//        lb.setDirection(DcMotorSimple.Direction.REVERSE);
//        lf.setDirection(DcMotorSimple.Direction.REVERSE);

//        slides = hardwareMap.dcMotor.get("slides");
    }

    @Override
    public void loop() {
        double x = -gamepad1.left_stick_y;
        double y = -gamepad1.left_stick_x;
        double omega = -gamepad1.right_stick_x;

        boolean turtle = gamepad1.left_trigger > 0.5 || gamepad1.right_trigger > 0.5;
        double scalar = turtle ? 0.25 : 1.0;

        double prf = (+x +y +omega) * scalar;
        double prb = (+x -y +omega) * scalar;
        double plb = (-x -y +omega) * scalar;
        double plf = (-x +y +omega) * scalar;

        double max = Math.max(Math.max(Math.max(Math.max(prf, prb), plb), plf), 1);

        prf /= max;
        prb /= max;
        plb /= max;
        plf /= max;

        rf.setPower(prf);
        rb.setPower(prb);
        lb.setPower(plb);
        lf.setPower(plf);

        telemetry.addData("rf", prf);
        telemetry.addData("rb", prb);
        telemetry.addData("lb", plb);
        telemetry.addData("lf", plf);
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
