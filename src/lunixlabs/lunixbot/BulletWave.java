package lunixlabs.lunixbot;

import org.jetbrains.annotations.Contract;
import robocode.Rules;
import robocode.util.Utils;

import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.DoubleSummaryStatistics;

public class BulletWave {
    //Bullet Data
    public final Point2D.Double startLocation;
    public final long fireTime;
    public final double bulletPower;
    public final double angle;
    //Initial Target Data
    public final double initialTargetDistance;
    public final int initialTargetDirection;
    public final double initialTargetVelocity;
    public final double initialTargetAbsBearing;

    public BulletWave(Point2D.Double startLocation, long fireTime, double bulletPower, double angle, double initialTargetDistance, int initialTargetDirection, double initialTargetVelocity, double initialTargetAbsBearing) {
        this.startLocation = startLocation;
        this.fireTime = fireTime;
        this.bulletPower = bulletPower;
        this.angle = angle;
        this.initialTargetDistance = initialTargetDistance;
        this.initialTargetDirection = initialTargetDirection;
        this.initialTargetVelocity = initialTargetVelocity;
        this.initialTargetAbsBearing = initialTargetAbsBearing;
    }

    @Contract(pure = true)
    public final double getVelocity() {
        return Rules.getBulletSpeed(bulletPower);
    }

    @Contract(pure = true)
    public final double getDistanceTraveled(long time) {
        return getVelocity() * (time - fireTime);
    }

    public final double getBulletTimeToTarget() {
        return initialTargetDistance / getVelocity();
    }

    public final boolean checkAngleCollides(Point2D.Double targetLocation) {
        double currentTargetAbsBearing = MathUtils.absoluteBearing(startLocation, targetLocation);
        double currentAngle = Utils.normalRelativeAngle(currentTargetAbsBearing - initialTargetAbsBearing);

        //Half of one bot in radians at the current distance (target location is center of bot)
        double toleranceAngle = MathUtils.getBotWidthInRadians(startLocation.distance(targetLocation))/2;

        return Math.abs(Utils.normalRelativeAngle(currentAngle - angle)) < toleranceAngle;
    }

    public void onPaint(Graphics2D g, long time) {
//            Point2D.Double endpoint = MathUtil.project(startLocation, initialTargetAbsBearing, initialTargetDistance);
//            g.drawLine((int)startLocation.x, (int)startLocation.y, (int)endpoint.x, (int)endpoint.y);

//        Point2D.Double endpoint1 = MathUtils.project(startLocation, initialTargetAbsBearing - MathUtils.maxEscapeAngle(getVelocity()), initialTargetDistance);
//        g.drawLine((int)startLocation.x, (int)startLocation.y, (int)endpoint1.x, (int)endpoint1.y);
//
//        Point2D.Double endpoint2 = MathUtils.project(startLocation, initialTargetAbsBearing + MathUtils.maxEscapeAngle(getVelocity()), initialTargetDistance);
//        g.drawLine((int)startLocation.x, (int)startLocation.y, (int)endpoint2.x, (int)endpoint2.y);

        int radius = (int)getDistanceTraveled(time);

        if(radius - 40 < initialTargetDistance)
            g.drawArc((int)(startLocation.x - radius ), (int)(startLocation.y - radius), radius*2, radius*2,
                    (int)Math.toDegrees(initialTargetAbsBearing - MathUtils.maxEscapeAngle(getVelocity())),
                    (int)Math.toDegrees(MathUtils.maxEscapeAngle(getVelocity())*2)
                    );
    }

    public void onPaint(Graphics2D g, long time, double[] visitCounts) {
//        Point2D.Double endpoint = MathUtils.project(startLocation, Utils.normalAbsoluteAngle(initialTargetAbsBearing + angle), initialTargetDistance);
//        g.drawLine((int)startLocation.x, (int)startLocation.y, (int)endpoint.x, (int)endpoint.y);

        int radius = (int)getDistanceTraveled(time);
        double size = Math.toDegrees(MathUtils.maxEscapeAngle(getVelocity())*2 / visitCounts.length);

        DoubleSummaryStatistics stat = Arrays.stream(visitCounts).summaryStatistics();
        double max = stat.getMax();

        if(radius - 40 < initialTargetDistance) {
            Color color = g.getColor();
            double waveStartingAngle = Math.toDegrees(initialTargetAbsBearing - initialTargetDirection*MathUtils.maxEscapeAngle(getVelocity())) - 90;
            for(int i = 0; i < visitCounts.length; i++) {
                int intensity = (int)MathUtils.scale(visitCounts[i], 0, max, 0, 255);
                g.setColor(new Color(intensity, 255-intensity, 0));
                g.draw(new Arc2D.Double(
                         (startLocation.x - radius), (startLocation.y - radius), radius * 2, radius * 2,
                        (waveStartingAngle + i*size*initialTargetDirection),
                        size, Arc2D.CHORD
                ));
            }
            g.setColor(color);
        }
    }
}
