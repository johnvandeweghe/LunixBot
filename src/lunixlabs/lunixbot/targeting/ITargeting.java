package lunixlabs.lunixbot.targeting;

import lunixlabs.lunixbot.BulletWave;
import robocode.Bullet;
import robocode.ScannedRobotEvent;

import java.awt.geom.Point2D;

public interface ITargeting {
    void reset();

    void track(ScannedRobotEvent event, Point2D.Double myLocation, double myVelocity, double myHeading, long time);

    void trackShot(double power, double angleOffset, double myVelocity, double myHeading, Point2D.Double myLocation, long time);

    void trackBulletHitBullet(Bullet bullet);

    void trackEnemyWave(BulletWave wave);

    double choosePower(Point2D.Double myLocation, double myEnergy);

    double chooseGunAngle(double bulletPower, Point2D.Double myLocation, double absoluteBearingToEnemy);

    double getHitRate();

    void onPaint(java.awt.Graphics2D g, long time);
}
