package lunixlabs.lunixbot.targeting;

import lunixlabs.lunixbot.BulletWave;
import lunixlabs.lunixbot.MathUtils;
import robocode.Bullet;
import robocode.Rules;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;

abstract class AbstractGuessFactorGun implements ITargeting {
    protected ArrayList<BulletWave> myWaves = new ArrayList<>();
    //TODO: decay this somehow so we can see how well we are doing locally, not overall
    //We learn, which means this is only for a while, we can keep both, but should also have a per round accuracy, or just a rolling avg
    private int shotsFired = 0;
    private int shotsHit = 0;
    private int shotsFiredRound = 0;
    private int shotsHitRound = 0;
    private int shotsCollided = 0;

    protected Point2D.Double enemyLocation;
    protected double enemyLateralVelocity;
    protected int enemyDirection;
    protected double enemyLastBulletPower;
    protected double enemyLastEnergy;


    @Override
    public void reset() {
        myWaves = new ArrayList<>();
        enemyLocation = new Point2D.Double(0,0);
        enemyLateralVelocity = 0;
        enemyDirection = 1;
        enemyLastBulletPower = .1;
        enemyLastEnergy = 100;
        shotsFiredRound = 0;
        shotsHitRound = 0;
        shotsCollided = 0;
    }

    @Override
    public void track(ScannedRobotEvent event, Point2D.Double myLocation, double myVelocity, double myHeading, long time) {
        double scannedAbsoluteBearing = event.getBearingRadians() + myHeading;

        enemyLocation = MathUtils.project(myLocation, scannedAbsoluteBearing, event.getDistance());
        enemyLateralVelocity = event.getVelocity() * Math.sin(event.getHeadingRadians() - scannedAbsoluteBearing);
        enemyLastEnergy = event.getEnergy();

        cleanPassedWaves(enemyLocation, time);
    }

    private void cleanPassedWaves(
            Point2D.Double targetLocation, long time
    ) {
        // Let's process the waves now:
        for (int i=0; i < myWaves.size(); i++)
        {
            BulletWave currentWave = myWaves.get(i);
            if (currentWave.getDistanceTraveled(time)
                    > currentWave.startLocation.distance(targetLocation) + 10)
            {
                trackPass(currentWave, targetLocation);
                myWaves.remove(i);
                i--;
            }
        }
    }

    private void trackPass(BulletWave bulletWave, Point2D.Double targetLocation) {
        double desiredDirection = MathUtils.absoluteBearing(bulletWave.startLocation, targetLocation);
        double angleOffset = Utils.normalRelativeAngle(desiredDirection - bulletWave.initialTargetAbsBearing);

        //half of one bot
        double toleranceAngle = Math.atan2(36.0, bulletWave.startLocation.distance(targetLocation));

        if(Math.abs(Utils.normalRelativeAngle(angleOffset - bulletWave.angle)) < toleranceAngle) {
            trackHit(bulletWave);
        }



        double guessFactor =
                Math.max(-1, Math.min(1,
                        angleOffset / MathUtils.maxEscapeAngle(bulletWave.getVelocity()))
                ) * bulletWave.initialTargetDirection;

        trackPass(bulletWave, guessFactor);
    }

    @Override
    public void trackShot(double power, double angleOffset, double myVelocity, double myHeading, Point2D.Double myLocation, long time) {
        shotsFired++;
        shotsFiredRound++;

        double initialTargetAbsBearing = MathUtils.absoluteBearing(myLocation, enemyLocation);
        myWaves.add(new BulletWave(
                myLocation,
                time,
                power,
                Utils.normalRelativeAngle(angleOffset - initialTargetAbsBearing),
                myLocation.distance(enemyLocation),
                (enemyLateralVelocity >= 0) ? 1 : -1,
                enemyLateralVelocity,
                initialTargetAbsBearing
        ));
    }

    protected void trackHit(BulletWave bulletWave) {
        shotsHit++;
        shotsHitRound++;
    }

    @Override
    public void trackEnemyWave(BulletWave wave) {
        enemyLastBulletPower = wave.bulletPower;
    }

    @Override
    public double chooseGunAngle(double bulletPower, Point2D.Double myLocation, double absoluteBearingToEnemy) {
        // don't try to figure out the direction they're moving
        // they're not moving, just use the direction we had before
        if (enemyLateralVelocity != 0)
        {
            if (enemyLateralVelocity < 0)
                enemyDirection = -1;
            else
                enemyDirection = 1;
        }

        double guessfactor = chooseGuessFactor(bulletPower, myLocation);

        return Utils.normalAbsoluteAngle(absoluteBearingToEnemy + enemyDirection * guessfactor * MathUtils.maxEscapeAngle(Rules.getBulletSpeed(bulletPower)));
    }

    @Override
    public void trackBulletHitBullet(Bullet bullet) {
        shotsCollided++;
    }

    protected double getCollisionRate() {
        return shotsFiredRound == 0 ? 0 : shotsCollided / (double)shotsFiredRound;
    }

    /**
     * @param bulletPower power of the bullet (0.1-3.0)
     * @param myLocation
     * @return a number in range [-1, 1] where 1 is the direction the enemy is going and -1 is the opposite
     */
    protected abstract double chooseGuessFactor(double bulletPower, Point2D.Double myLocation);

    protected abstract void trackPass(BulletWave bulletWave, double guessFactor);

    @Override
    public double getHitRate() {
        return shotsFiredRound == 0 ? 0 : shotsHitRound / (double)shotsFiredRound;
        //return shotsFired == 0 ? 0 : shotsHit / (double)shotsFired;
    }

    @Override
    public void onPaint(Graphics2D g, long time) {
        g.setColor(Color.green);
        for (BulletWave w : myWaves) {
            w.onPaint(g, time);
        }
    }
}
