package org.thenuts.powerplay.acme.util;

import com.acmerobotics.dashboard.canvas.Canvas;
import com.acmerobotics.roadrunner.geometry.Pose2d;
import com.acmerobotics.roadrunner.geometry.Vector2d;
import com.acmerobotics.roadrunner.path.Path;

import org.apache.commons.math3.linear.RealMatrix;

import java.util.List;

/**
 * Set of helper functions for drawing Road Runner paths and trajectories on dashboard canvases.
 */
public class DashboardUtil {
    private static final double DEFAULT_RESOLUTION = 2.0; // distance units; presumed inches
    private static final double ROBOT_RADIUS = 4; // in
    private static final int ELLIPSE_STEPS = 20;


    public static void drawPoseHistory(Canvas canvas, List<Pose2d> poseHistory) {
        double[] xPoints = new double[poseHistory.size()];
        double[] yPoints = new double[poseHistory.size()];
        for (int i = 0; i < poseHistory.size(); i++) {
            Pose2d pose = poseHistory.get(i);
            xPoints[i] = pose.getX();
            yPoints[i] = pose.getY();
        }
        canvas.strokePolyline(xPoints, yPoints);
    }

    public static void drawSampledPath(Canvas canvas, Path path, double resolution) {
        int samples = (int) Math.ceil(path.length() / resolution);
        double[] xPoints = new double[samples];
        double[] yPoints = new double[samples];
        double dx = path.length() / (samples - 1);
        for (int i = 0; i < samples; i++) {
            double displacement = i * dx;
            Pose2d pose = path.get(displacement);
            xPoints[i] = pose.getX();
            yPoints[i] = pose.getY();
        }
        canvas.strokePolyline(xPoints, yPoints);
    }

    public static void drawSampledPath(Canvas canvas, Path path) {
        drawSampledPath(canvas, path, DEFAULT_RESOLUTION);
    }

    public static void drawRobot(Canvas canvas, Pose2d pose) {
        canvas.strokeCircle(pose.getX(), pose.getY(), ROBOT_RADIUS);
        Vector2d v = pose.headingVec().times(ROBOT_RADIUS);
        double x1 = pose.getX() + v.getX() / 2, y1 = pose.getY() + v.getY() / 2;
        double x2 = pose.getX() + v.getX(), y2 = pose.getY() + v.getY();
        canvas.strokeLine(x1, y1, x2, y2);
    }

    public static void drawRobotEstimate(Canvas canvas, Pose2d pose, RealMatrix cov) {
        double a = cov.getEntry(0, 0);
        double b = cov.getEntry(0, 1);
        double c = cov.getEntry(1, 0);
        double d = cov.getEntry(1, 1);

        double r = (a + c) / 2.0;
        double s = Math.sqrt(((a - c) / 2.0) * ((a - c) / 2.0) + b * b);
        double l1 = r + s;
        double l2 = r - s;
        double rl1 = Math.sqrt(l1);
        double rl2 = Math.sqrt(l2);

        double cos;
        double sin;
        if (b == 0) {
            if (a >= c) {
                cos = 1.0;
                sin = 0.0;
            } else {
                cos = 0.0;
                sin = 1.0;
            }
        } else {
            double theta = Math.atan2(l1 - a, b);
            cos = Math.cos(theta);
            sin = Math.sin(theta);
        }

        double dt = 2.0 * Math.PI / (double) ELLIPSE_STEPS;
        double[] xPoints = new double[ELLIPSE_STEPS];
        double[] yPoints = new double[ELLIPSE_STEPS];

        for (int i = 0; i < ELLIPSE_STEPS; i++) {
            double t = (double) i * dt;
            double x = rl1 * cos * Math.cos(t) - rl2 * sin * Math.sin(t);
            double y = rl1 * sin * Math.cos(t) + rl2 * cos * Math.sin(t);

            xPoints[i] = pose.getX() + x;
            yPoints[i] = pose.getY() + y;
        }

        canvas.strokePolygon(xPoints, yPoints);

        double headingSigma = cov.getEntry(2, 2);
        double[] headings = new double[] { pose.getHeading(), pose.getHeading() - headingSigma, pose.getHeading() + headingSigma };
        double[] lengths = new double[] { ROBOT_RADIUS, ROBOT_RADIUS / 2.0, ROBOT_RADIUS / 2.0 };
        for (int j = 0; j < 3; j++) {
            Vector2d v = Vector2d.polar(lengths[j], headings[j]);
            double x1 = pose.getX() + v.getX() / 2, y1 = pose.getY() + v.getY() / 2;
            double x2 = pose.getX() + v.getX(), y2 = pose.getY() + v.getY();
            canvas.strokeLine(x1, y1, x2, y2);
        }
    }
}
