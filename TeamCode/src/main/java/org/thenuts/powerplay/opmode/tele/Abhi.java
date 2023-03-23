package org.thenuts.powerplay.opmode.tele;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.DcMotor;

@TeleOp
public class Abhi extends OpMode{

    DcMotor rf;
    DcMotor lf;
    DcMotor rb;
    DcMotor lb;

    @Override
    public void init(){
        rf = hardwareMap.get(DcMotor.class, "motorRF");
        lf = hardwareMap.get(DcMotor.class, "motorLF");
        rb = hardwareMap.get(DcMotor.class, "motorRB");
        lb = hardwareMap.get(DcMotor.class, "motorLB");
    }

    public void move(){
       double y = 0; //vertical
       double x = 0; //horizontal
       double r = 0; //pivot/straif

        y = -gamepad1.left_stick_y;
        x = -gamepad1.left_stick_x;
        r = -gamepad1.right_stick_x;

        double preRF = (r+(y+x));
        double preLF = (r+(y-x));
        double preRB = (r+(-y+x));
        double preLB = (r+(-y-x));

        double max = Math.max(Math.max(Math.max(Math.max(preRF,preRB), preLF), preLB), 1);

        rf.setPower(preRF/max);
        lf.setPower(preLF/max);
        rb.setPower(preRB/max);
        lb.setPower(preLB/max);

        double postRF = preRF/max;
        double postLF = preLF/max;
        double postRB = preRB/max;
        double postLB = preLB/max;

        telemetry.addData("rf", postRF);
        telemetry.addData("lf", postLF);
        telemetry.addData("rb", postRB);
        telemetry.addData("lb", postLB);
        telemetry.update();
    }

    public void Stop(){
        super.stop();
        rf.setPower(0);
        lf.setPower(0);
        rb.setPower(0);
        lb.setPower(0);
}

    public void loop(){

    }

}
