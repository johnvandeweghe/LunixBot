package lunixlabs.lunixbot.movement;

import lunixlabs.lunixbot.BulletWave;
import robocode.Bullet;
import robocode.ScannedRobotEvent;

import java.awt.geom.Point2D;

public interface IMovement {
    void reset();

    BulletWave track(ScannedRobotEvent event, double myVelocity, double myHeading, Point2D.Double myLocation, long time);

    void updateEnemyEnergy(double energy);

    double suggestAngle(double myVelocity, double myHeading, Point2D.Double myLocation, long time);

    void logHit(Bullet bullet, double myVelocity, double myHeading, Point2D.Double myLocation, long time);

    double getDodgeRate();

    void onPaint(java.awt.Graphics2D g, long time);
}
