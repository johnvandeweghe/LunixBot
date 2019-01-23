package lunixlabs.lunixbot.movement;

import lunixlabs.lunixbot.BulletWave;
import lunixlabs.lunixbot.MathUtils;
import org.jetbrains.annotations.Contract;
import robocode.Bullet;
import robocode.Rules;
import robocode.ScannedRobotEvent;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;

abstract class AbstractSurfer implements IMovement {
    protected ArrayList<BulletWave> enemyWaves;
    protected ArrayList<Integer> surfDirections;
    protected ArrayList<Double> surfAbsBearings;
    protected Point2D.Double enemyLocation;

    protected double enemyEnergy = 100.0;

    private int detectedBulletCount = 0;
    private int hitBulletCount = 0;

    @Override
    public void reset() {
        enemyWaves = new ArrayList<>();
        surfDirections = new ArrayList<>();
        surfAbsBearings = new ArrayList<>();
        enemyEnergy = 100.0;
    }

    @Override
    public void track(ScannedRobotEvent event, double myVelocity, double myHeading, Point2D.Double myLocation, long time) {

        double lateralVelocity = myVelocity*Math.sin(event.getBearingRadians());
        double scannedAbsoluteBearing = event.getBearingRadians() + myHeading;

        surfDirections.add(0,
                (lateralVelocity >= 0) ? 1 : -1);
        surfAbsBearings.add(0, scannedAbsoluteBearing + Math.PI);

        detectBullet(event, time, lateralVelocity, scannedAbsoluteBearing);

        enemyEnergy = event.getEnergy();

        // update after EnemyWave detection, because that needs the previous
        // enemy location as the source of the wave
        enemyLocation = MathUtils.project(myLocation, scannedAbsoluteBearing, event.getDistance());

        cleanPassedWaves(myLocation, time);
    }

    protected void detectBullet(ScannedRobotEvent event, long time, double lateralVelocity, double scannedAbsoluteBearing) {
        double bulletPower = enemyEnergy - event.getEnergy();
        if (bulletPower <= Rules.MAX_BULLET_POWER && bulletPower >= Rules.MIN_BULLET_POWER
                && surfDirections.size() > 2) {
            enemyWaves.add(new BulletWave(
                    (Point2D.Double) enemyLocation.clone(),
                    time - 1,
                    bulletPower,
                    surfAbsBearings.get(2),
                    event.getDistance(),
                    surfDirections.get(2),
                    lateralVelocity,
                    surfAbsBearings.get(2),
                    false
            ));
            detectedBulletCount++;
        }
    }

    protected void cleanPassedWaves(
            Point2D.Double targetLocation, long time
    ) {
        // Let's process the waves now:
        for (int i=0; i < enemyWaves.size(); i++)
        {
            BulletWave currentWave = enemyWaves.get(i);
            if (currentWave.getDistanceTraveled(time)
                    > currentWave.startLocation.distance(targetLocation) + 50)
            {
                enemyWaves.remove(i);
                i--;
            }
        }
    }

    @Override
    public double suggestAngle(double myVelocity, double myHeading, Point2D.Double myLocation, long time) {
        BulletWave surfWave = getClosestSurfableWave(myLocation, time);

        if (surfWave == null) {
            return Utils.wallSmoothing(myLocation, MathUtils.absoluteBearing(enemyLocation, myLocation) - (Math.PI/2 - .8), Math.random() > .5 ? 1 : -1);
        }

        return surfWave(surfWave, myVelocity, myHeading, myLocation, time);

    }

    protected double surfWave(BulletWave surfWave, double myVelocity, double myHeading, Point2D.Double myLocation, long time) {
        double goAngle = MathUtils.absoluteBearing(surfWave.startLocation, myLocation);

        double dangerLeft = checkDanger(surfWave, -1, myVelocity, myHeading, myLocation, time);
        double dangerRight = checkDanger(surfWave, 1, myVelocity, myHeading, myLocation, time);

        double distance = surfWave.startLocation.distance(myLocation);

        int direction;
        if (dangerLeft < dangerRight) {
            direction = -1;
        }
        else {
            direction = 1;
        }

        //Widen the dodge circle to make sure to be moving away, especially when close
        //Make sure that any changes to this is captured in the prediction code in danger checking
        return Utils.wallSmoothing(myLocation, goAngle - (Math.PI/2 - (distance < 100 ? .8 : .05)), direction);
    }

    /**
     * @return Get a danger value for a given direction (-1, 0, 1), bigger is more dangerous.
     */
    protected double checkDanger(BulletWave surfWave, int direction, double myVelocity, double myHeading, Point2D.Double myLocation, long time) {
        Point2D.Double predictedPosition;
        if (direction != 0) {
            predictedPosition = predictPosition(surfWave, direction, myVelocity, myHeading, myLocation, time);
        } else {
            predictedPosition = myLocation;
        }

        return checkDanger(surfWave, predictedPosition);
    }

    @Contract(pure = true)
    protected static Point2D.Double predictPosition(BulletWave surfWave, int direction, double myVelocity, double myHeading, Point2D.Double myLocation, long time) {
        Point2D.Double predictedPosition = (Point2D.Double)myLocation.clone();
        double predictedVelocity = myVelocity;
        double predictedHeading = myHeading;
        double maxTurning, moveAngle, moveDir;

        int counter = 0; // number of ticks in the future
        boolean intercepted = false;

        do {    // the rest of these code comments are rozu's
            double distance = surfWave.startLocation.distance(predictedPosition);

            //This should match the surfWave method to ensure correctly predicted movement
            moveAngle =
                    Utils.wallSmoothing(predictedPosition, MathUtils.absoluteBearing(surfWave.startLocation,
                            predictedPosition) + (direction * (Math.PI/2 - (distance < 100 ? .8 : .05))), direction)
                            - predictedHeading;

            moveDir = 1;

            if(Math.cos(moveAngle) < 0) {
                moveAngle += Math.PI;
                moveDir = -1;
            }

            moveAngle = robocode.util.Utils.normalRelativeAngle(moveAngle);

            // maxTurning is built in like this, you can't turn more then this in one tick
            maxTurning = MathUtils.getMaxTurning(predictedVelocity);
            predictedHeading = robocode.util.Utils.normalRelativeAngle(predictedHeading
                    + MathUtils.limit(-maxTurning, moveAngle, maxTurning));

            // this one is nice ;). if predictedVelocity and moveDir have
            // different signs you want to breack down
            // otherwise you want to accelerate (look at the factor "2")
            predictedVelocity +=
                    (predictedVelocity * moveDir < 0 ? 2*moveDir : moveDir);
            predictedVelocity = MathUtils.limit(-8, predictedVelocity, 8);

            // calculate the new predicted position
            predictedPosition = MathUtils.project(predictedPosition, predictedHeading,
                    predictedVelocity);

            counter++;

            if (predictedPosition.distance(surfWave.startLocation) <
                    surfWave.getDistanceTraveled(time) + (counter * surfWave.getVelocity())
                            + surfWave.getVelocity()) {
                intercepted = true;
            }
        } while(!intercepted && counter < 500);

        return predictedPosition;
    }

    protected abstract double checkDanger(BulletWave surfWave, Point2D.Double predictedPosition);

    private BulletWave getClosestSurfableWave(Point2D.Double myLocation, long time) {
        double closestDistance = 50000; // I juse use some very big number here
        BulletWave surfWave = null;

        for (BulletWave ew : enemyWaves) {
            double distance = myLocation.distance(ew.startLocation)
                    - ew.getDistanceTraveled(time);

            if (distance > ew.getVelocity() && distance < closestDistance) {
                surfWave = ew;
                closestDistance = distance;
            }
        }

        return surfWave;
    }

    @Override
    public void logHit(Bullet bullet, double myVelocity, double myHeading, Point2D.Double myLocation, long time) {
        hitBulletCount++;

        // If the _enemyWaves collection is empty, we must have missed the
        // detection of this wave somehow.
        if (!enemyWaves.isEmpty()) {
            Point2D.Double hitBulletLocation = new Point2D.Double(
                    bullet.getX(), bullet.getY());
            BulletWave hitWave = null;

            // look through the EnemyWaves, and find one that could've hit us.
            for (Object _enemyWave : enemyWaves) {
                BulletWave ew = (BulletWave) _enemyWave;

                if (Math.abs(ew.getDistanceTraveled(time) -
                        hitBulletLocation.distance(ew.startLocation)) < 50
                        && Math.abs(Rules.getBulletSpeed(bullet.getPower())
                        - ew.getVelocity()) < 0.001) {
                    hitWave = ew;
                    break;
                }
            }

            if (hitWave != null) {
                logHit(hitWave, bullet, myVelocity, myHeading, myLocation, time);

                // We can remove this wave now, of course.
                enemyWaves.remove(enemyWaves.lastIndexOf(hitWave));
            }
        }
    }

    protected abstract void logHit(BulletWave bulletWave, Bullet bullet, double myVelocity, double myHeading, Point2D.Double myLocation, long time);

    @Override
    public double getDodgeRate() {
        if (detectedBulletCount == 0) {
            return 0;
        } else {
            return 1.0 - hitBulletCount / (double) detectedBulletCount;
        }
    }

    @Override
    public void updateEnemyEnergy(double energy) {
        enemyEnergy = energy;
    }

    @Override
    public void onPaint(Graphics2D g, long time) {
        g.setColor(java.awt.Color.red);
        for (BulletWave w : enemyWaves) {
            w.onPaint(g, time);
        }
    }
}
