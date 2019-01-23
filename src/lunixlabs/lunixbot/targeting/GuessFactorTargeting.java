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

public class GuessFactorTargeting implements ITargeting {
    private ArrayList<BulletWave> myWaves = new ArrayList<>();
    private final static int AIM_BINS = 31;
    // 31 is the number of unique GuessFactors we're using
    // Note: this must be odd number so we can get
    // GuessFactor 0 at middle.

    private double[][] stats = new double[19][AIM_BINS];

    private int shotsFired = 0;
    private int shotsHit = 0;

    private Point2D.Double enemyLocation;
    private double enemyLateralVelocity;
    private int enemyDirection;

    @Override
    public void reset() {
        myWaves = new ArrayList<>();
        enemyLocation = new Point2D.Double(0,0);
        enemyLateralVelocity = 0;
        enemyDirection = 1;
    }

    @Override
    public void track(ScannedRobotEvent event, Point2D.Double myLocation, double myVelocity, double myHeading, long time) {
        double scannedAbsoluteBearing = event.getBearingRadians() + myHeading;

        enemyLocation = MathUtils.project(myLocation, scannedAbsoluteBearing, event.getDistance());
        enemyLateralVelocity = event.getVelocity() * Math.sin(event.getHeadingRadians() - scannedAbsoluteBearing);

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
                    > currentWave.startLocation.distance(targetLocation) + 50)
            {
                trackPass(currentWave, targetLocation);
                myWaves.remove(i);
                i--;
            }
        }
    }

    private void trackPass(BulletWave bulletWave, Point2D.Double targetLocation) {
        double[] currentSegment = stats[segmentVelocity(bulletWave.initialTargetVelocity)];

        double desiredDirection = MathUtils.absoluteBearing(bulletWave.startLocation, targetLocation);
        double angleOffset = Utils.normalRelativeAngle(desiredDirection - bulletWave.initialTargetAbsBearing);
        double guessFactor =
                Math.max(-1, Math.min(1, angleOffset / MathUtils.maxEscapeAngle(bulletWave.getVelocity()))) * bulletWave.initialTargetDirection;

        int index = (int) MathUtils.scale(guessFactor, -1, 1, 0, AIM_BINS - 1);

        for (int x = 0; x < currentSegment.length; x++) {
            double newValue = 1.0 / (Math.pow(index - x, 2) + 1);
            currentSegment[x] = MathUtils.approxRollingAverage(currentSegment[x], newValue);
        }
    }

    @Override
    public void trackShot(double power, double angleOffset, double myVelocity, double myHeading, Point2D.Double myLocation, long time) {
        Point2D.Double myNewLocation = MathUtils.project(myLocation, myHeading, myVelocity);
        Point2D.Double enemyNewLocation = MathUtils.project(
                enemyLocation,
                MathUtils.absoluteBearing(myNewLocation, enemyLocation) + (Math.PI/2) * enemyDirection,
                enemyLateralVelocity
        );
        shotsFired++;
        myWaves.add(new BulletWave(
                myNewLocation,
                time + 1,
                power,
                angleOffset,
                myNewLocation.distance(enemyNewLocation),
                (enemyLateralVelocity >= 0) ? 1 : -1,
                enemyLateralVelocity,
                MathUtils.absoluteBearing(myNewLocation, enemyNewLocation),
                true
        ));
    }

    @Override
    public void trackHit(Bullet bullet) {
        shotsHit++;
    }

    @Override
    public double choosePower(Point2D.Double myLocation, double myEnergy) {
        return myLocation.distance(enemyLocation) < 100 ? 3 : 1;
    }

    @Override
    public double chooseTargetOffset(double bulletPower) {
        // don't try to figure out the direction they're moving
        // they're not moving, just use the direction we had before
        if (enemyLateralVelocity != 0)
        {
            if (enemyLateralVelocity < 0)
                enemyDirection = -1;
            else
                enemyDirection = 1;
        }

        double[] currentStats = stats[segmentVelocity(enemyLateralVelocity)];

        int bestindex = Math.round(AIM_BINS/2);	// initialize it to be in the middle, guessfactor 0.
        for (int i=0; i<currentStats.length; i++)
            if (currentStats[bestindex] < currentStats[i])
                bestindex = i;

        // this should do the opposite of the math in the WaveBullet:
        double guessfactor = MathUtils.scale(bestindex, 0, AIM_BINS - 1, -1, 1);

        return enemyDirection * guessfactor * MathUtils.maxEscapeAngle(Rules.getBulletSpeed(bulletPower));
    }

    private int segmentVelocity(double velocity){
        return (int)Math.round(velocity) + 8;
    }

    @Override
    public double getHitRate() {
        return shotsFired == 0 ? 0 : shotsHit / (double)shotsFired;
    }

    @Override
    public void onPaint(Graphics2D g, long time) {
        g.setColor(java.awt.Color.blue);
        for (BulletWave w : myWaves) {
            w.onPaint(g, time);
        }
    }
}
