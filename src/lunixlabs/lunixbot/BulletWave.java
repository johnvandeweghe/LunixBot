package lunixlabs.lunixbot;

import org.jetbrains.annotations.Contract;
import robocode.Rules;
import robocode.util.Utils;

import java.awt.*;
import java.awt.geom.Point2D;

public class BulletWave {
    //Bullet Data
    public final Point2D.Double startLocation;
    final long fireTime;
    final double bulletPower;
    public final double angle;
    //Initial Target Data
    final double initialTargetDistance;
    public final int initialTargetDirection;
    final double initialTargetVelocity;
    final double initialTargetAbsBearing;
    final boolean isMine;

    public BulletWave(Point2D.Double startLocation, long fireTime, double bulletPower, double angle, double initialTargetDistance, int initialTargetDirection, double initialTargetVelocity, double initialTargetAbsBearing, boolean isMine) {
        this.startLocation = startLocation;
        this.fireTime = fireTime;
        this.bulletPower = bulletPower;
        this.angle = angle;
        this.initialTargetDistance = initialTargetDistance;
        this.initialTargetDirection = initialTargetDirection;
        this.initialTargetVelocity = initialTargetVelocity;
        this.initialTargetAbsBearing = initialTargetAbsBearing;
        this.isMine = isMine;
    }

    @Contract(pure = true)
    public final double getVelocity() {
        return Rules.getBulletSpeed(bulletPower);
    }

    @Contract(pure = true)
    public final double getDistanceTraveled(long time) {
        return getVelocity() * (time - fireTime);
    }

    public void onPaint(Graphics2D g, long time) {
        Point2D.Double velocityEndpoint = MathUtils.project(startLocation, Utils.normalAbsoluteAngle(angle+initialTargetDirection*Math.PI/2), initialTargetVelocity);
        g.drawLine((int)startLocation.x, (int)startLocation.y, (int)velocityEndpoint.x, (int)velocityEndpoint.y);

//            Point2D.Double endpoint = MathUtil.project(startLocation, initialTargetAbsBearing, initialTargetDistance);
//            g.drawLine((int)startLocation.x, (int)startLocation.y, (int)endpoint.x, (int)endpoint.y);

        Point2D.Double endpoint1 = MathUtils.project(startLocation, initialTargetAbsBearing - MathUtils.maxEscapeAngle(getVelocity())/2, initialTargetDistance);
        g.drawLine((int)startLocation.x, (int)startLocation.y, (int)endpoint1.x, (int)endpoint1.y);

        Point2D.Double endpoint2 = MathUtils.project(startLocation, initialTargetAbsBearing + MathUtils.maxEscapeAngle(getVelocity())/2, initialTargetDistance);
        g.drawLine((int)startLocation.x, (int)startLocation.y, (int)endpoint2.x, (int)endpoint2.y);

        int radius = (int)getDistanceTraveled(time);

        if(radius - 40 < initialTargetDistance)
            g.drawOval((int)(startLocation.x - radius ), (int)(startLocation.y - radius), radius*2, radius*2);
    }
}
