package lunixlabs;
import robocode.*;
import robocode.util.*;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;

// API help : http://robocode.sourceforge.net/docs/robocode/robocode/Robot.html

/**
 * LunixBot - a robot by (your name here)
 */
public class LunixBot extends AdvancedRobot
{
    private ArrayList<BulletWave> myWaves = new ArrayList<>();
    private static int BINS = 47;
    private static int AIM_BINS = 31;
	
	private static double[][][] stats = new double[19][2][AIM_BINS]; // 31 is the number of unique GuessFactors we're using
					  // Note: this must be odd number so we can get
					  // GuessFactor 0 at middle.
    private int direction = 1;

    private static double _surfStats[] = new double[BINS];
    private Point2D.Double _myLocation;     // our bot's location
    private Point2D.Double _enemyLocation;  // enemy bot's location

    private ArrayList<BulletWave> enemyWaves;
    private ArrayList<Integer> _surfDirections;
    private ArrayList<Double> _surfAbsBearings;

    private static double _oppEnergy = 100.0;

    int missesSinceLastHit = 0;
	
	/** This is a rectangle that represents an 800x600 battle field,
    * used for a simple, iterative WallSmoothing method (by PEZ).
    * If you're not familiar with WallSmoothing, the wall stick indicates
    * the amount of space we try to always have on either end of the tank
    * (extending straight out the front or back) before touching a wall.
    */
    public static Rectangle2D.Double _fieldRect
        = new java.awt.geom.Rectangle2D.Double(18, 18, 764, 564);
    public static double WALL_STICK = 160;
	
	/**
	 * run: LunixBot's default behavior
	 */
	public void run() {
		// setColors(Color.red,Color.blue,Color.green); // body,gun,radar
        enemyWaves = new ArrayList<>();
        _surfDirections = new ArrayList<>();
        _surfAbsBearings = new ArrayList<>();
        myWaves = new ArrayList<>();
        missesSinceLastHit = 0;
 
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

		// Robot main loop
		while(true) {
			turnRadarRightRadians(Double.POSITIVE_INFINITY);//keep turning radar right
		}
	}

    @Override
	public void onScannedRobot(ScannedRobotEvent e)
	{
		_myLocation = new Point2D.Double(getX(), getY());
 
        double lateralVelocity = getVelocity()*Math.sin(e.getBearingRadians());
        double scannedAbsoluteBearing = e.getBearingRadians() + getHeadingRadians();
        double enemyLateralVelocity = e.getVelocity() * Math.sin(e.getHeadingRadians() - scannedAbsoluteBearing);
 
        setTurnRadarRightRadians(Utils.normalRelativeAngle(scannedAbsoluteBearing
            - getRadarHeadingRadians()) * 2);
 
        _surfDirections.add(0,
            new Integer((lateralVelocity >= 0) ? 1 : -1));
        _surfAbsBearings.add(0, new Double(scannedAbsoluteBearing + Math.PI));
 
 
        double bulletPower = _oppEnergy - e.getEnergy();
        if (bulletPower < 3.01 && bulletPower > 0.09
            && _surfDirections.size() > 2) {
            enemyWaves.add(new BulletWave(
                    (Point2D.Double) _enemyLocation.clone(),
                getTime() - 1,
                    bulletPower,
                    _surfAbsBearings.get(2),
                    e.getDistance(),
                    _surfDirections.get(2),
                    lateralVelocity,
                    scannedAbsoluteBearing + Math.PI,
                    false
            ));
        }
 
        _oppEnergy = e.getEnergy();
 
        // update after EnemyWave detection, because that needs the previous
        // enemy location as the source of the wave
        _enemyLocation = MathUtil.project(_myLocation, scannedAbsoluteBearing, e.getDistance());

        cleanPassedWaves(enemyWaves, _myLocation, getTime());

        doSurfing();

        cleanPassedWaves(myWaves, _enemyLocation, getTime());

        double power = e.getDistance() < 100 ? 3 : 1;//MathUtil.scale(getConfidence(currentStats, bestindex), 0, 1, .1, 3);
        double angleOffset = predictAimGun(enemyLateralVelocity, e.getHeading(), scannedAbsoluteBearing, power);

        if(missesSinceLastHit > 2) {
            angleOffset = Math.PI*Math.random() - 1;
            power = .1;
            missesSinceLastHit = 0;
            System.out.println("RANDO!");
        }

        double gunAdjust = Utils.normalRelativeAngle(
                scannedAbsoluteBearing - getGunHeadingRadians() + angleOffset);

        setTurnGunRightRadians(gunAdjust);

        if (getGunHeat() == 0 && gunAdjust < Math.atan2(9, e.getDistance()) && setFireBullet(power) != null) {
            myWaves.add(new BulletWave(
                    _myLocation,
                    getTime(),
                    power,
                    angleOffset,
                    e.getDistance(),
                    (enemyLateralVelocity >= 0) ? 1 : -1,
                    enemyLateralVelocity,
                    scannedAbsoluteBearing,
                    true
            ));
		}
	}

    private double predictAimGun(double targetVelocity, double targetHeading, double scannedAbsoluteBearing, double power) {
        // don't try to figure out the direction they're moving
        // they're not moving, just use the direction we had before
        if (targetVelocity != 0)
        {
            if (targetVelocity < 0)
                direction = -1;
            else
                direction = 1;
        }

        double[] currentStats = stats[segmentVelocity(targetVelocity)][direction==-1?0:1];

        int bestindex = Math.round(AIM_BINS/2);	// initialize it to be in the middle, guessfactor 0.
        for (int i=0; i<currentStats.length; i++)
            if (currentStats[bestindex] < currentStats[i])
                bestindex = i;

        // this should do the opposite of the math in the WaveBullet:
        double guessfactor = MathUtil.scale(bestindex, 0, AIM_BINS - 1, -1, 1);
        //(double)(bestindex - (currentStats.length - 1) / 2)
        //                        / ((currentStats.length - 1) / 2);

        return direction * guessfactor * GameRulesUtil.maxEscapeAngle(Rules.getBulletSpeed(power));
    }

    private void cleanPassedWaves(
            ArrayList<BulletWave> waves, Point2D.Double targetLocation, long time
    ) {
        // Let's process the waves now:
        for (int i=0; i < waves.size(); i++)
        {
            BulletWave currentWave = waves.get(i);
            if (currentWave.getDistanceTraveled(time)
                    > currentWave.startLocation.distance(targetLocation) + 50)
            {
                onBulletMiss(currentWave, targetLocation);
                waves.remove(i);
                i--;
            }
        }
    }

    private int segmentDistance(double distance){
        return (int)Math.min(Math.round(distance / 300), 19);
    }

    private int segmentVelocity(double velocity){
        return (int)Math.round(velocity) + 8;
    }

    private double getConfidence(final double[] segment, final int bestIndex){
        double confidence;

        double value = segment[bestIndex];
        double sum = 0;
        for(double v : segment){
            sum += v;
        }

        confidence = value == 0 ? 0 : (value / sum);

        //Im skeptical
//        if(confidence < .75){
//            confidence /= 2;
//        }

        return value;
    }

    public void onHitByBullet(HitByBulletEvent e) {
        // If the _enemyWaves collection is empty, we must have missed the
        // detection of this wave somehow.
        if (!enemyWaves.isEmpty()) {
            Point2D.Double hitBulletLocation = new Point2D.Double(
                e.getBullet().getX(), e.getBullet().getY());
            BulletWave hitWave = null;
 
            // look through the EnemyWaves, and find one that could've hit us.
            for (Object _enemyWave : enemyWaves) {
                BulletWave ew = (BulletWave) _enemyWave;

                if (Math.abs(ew.getDistanceTraveled(getTime()) -
                        _myLocation.distance(ew.startLocation)) < 50
                        && Math.abs(Rules.getBulletSpeed(e.getBullet().getPower())
                        - ew.getVelocity()) < 0.001) {
                    hitWave = ew;
                    break;
                }
            }
 
            if (hitWave != null) {
                logHit(hitWave, hitBulletLocation);
 
                // We can remove this wave now, of course.
                enemyWaves.remove(enemyWaves.lastIndexOf(hitWave));
            }
        }
    }

    public void onBulletHitBullet(BulletHitBulletEvent e) {
        // If the _enemyWaves collection is empty, we must have missed the
        // detection of this wave somehow.
        if (!enemyWaves.isEmpty()) {
            Point2D.Double hitBulletLocation = new Point2D.Double(
                    e.getHitBullet().getX(), e.getHitBullet().getY());
            BulletWave hitWave = null;

            // look through the EnemyWaves, and find one that could've hit us.
            for (Object _enemyWave : enemyWaves) {
                BulletWave ew = (BulletWave) _enemyWave;

                if (Math.abs(ew.getDistanceTraveled(getTime()) -
                        hitBulletLocation.distance(ew.startLocation)) < 50
                        && Math.abs(Rules.getBulletSpeed(e.getHitBullet().getPower())
                        - ew.getVelocity()) < 0.001) {
                    hitWave = ew;
                    break;
                }
            }

            if (hitWave != null) {
                logHit(hitWave, hitBulletLocation);

                // We can remove this wave now, of course.
                enemyWaves.remove(enemyWaves.lastIndexOf(hitWave));
            }
        }
    }

    @Override
    public void onBulletHit(BulletHitEvent event) {
        _oppEnergy = event.getEnergy();
        missesSinceLastHit = 0;
    }

    @Override
    public void onHitRobot(HitRobotEvent event) {
        _oppEnergy = event.getEnergy();
    }

    public double wallSmoothing(Point2D.Double botLocation, double angle, int orientation) {
        while (!_fieldRect.contains(MathUtil.project(botLocation, angle, WALL_STICK))) {
            angle += orientation*0.05;
        }
        return angle;
    }


    public static void setBackAsFront(AdvancedRobot robot, double goAngle) {
        double angle =
            Utils.normalRelativeAngle(goAngle - robot.getHeadingRadians());
        if (Math.abs(angle) > (Math.PI/2)) {
            if (angle < 0) {
                robot.setTurnRightRadians(Math.PI + angle);
            } else {
                robot.setTurnLeftRadians(Math.PI - angle);
            }
            robot.setBack(100);
        } else {
            if (angle < 0) {
                robot.setTurnLeftRadians(-1*angle);
           } else {
                robot.setTurnRightRadians(angle);
           }
            robot.setAhead(100);
        }
    }
	
    private BulletWave getClosestSurfableWave() {
        double closestDistance = 50000; // I juse use some very big number here
        BulletWave surfWave = null;

        for (BulletWave ew : enemyWaves) {
            double distance = _myLocation.distance(ew.startLocation)
                    - ew.getDistanceTraveled(getTime());

            if (distance > ew.getVelocity() && distance < closestDistance) {
                surfWave = ew;
                closestDistance = distance;
            }
        }
 
        return surfWave;
    }
	
    // Given the EnemyWave that the bullet was on, and the point where we
    // were hit, calculate the index into our stat array for that factor.
    public static int getFactorIndex(BulletWave ew, Point2D.Double targetLocation) {
        double offsetAngle = (MathUtil.absoluteBearing(ew.startLocation, targetLocation)
            - ew.angle);
        double factor = Utils.normalRelativeAngle(offsetAngle)
            / GameRulesUtil.maxEscapeAngle(ew.getVelocity()) * ew.initialTargetDirection;
 
        return (int) MathUtil.limit(0,
            (factor * ((BINS - 1) / 2)) + ((BINS - 1) / 2),
            BINS - 1);
    }

    private void onBulletMiss(BulletWave bulletWave, Point2D.Double targetLocation) {
        if(bulletWave.isMine) {
            missesSinceLastHit++;
            double[] currentSegment = stats[segmentVelocity(bulletWave.initialTargetVelocity)][bulletWave.initialTargetDirection == -1 ? 0 : 1];

            double desiredDirection = MathUtil.absoluteBearing(bulletWave.startLocation, targetLocation);
            double angleOffset = Utils.normalRelativeAngle(desiredDirection - bulletWave.initialTargetAbsBearing);
            double guessFactor =
                    Math.max(-1, Math.min(1, angleOffset / GameRulesUtil.maxEscapeAngle(Rules.getBulletSpeed(bulletWave.bulletPower)))) * bulletWave.initialTargetDirection;

            int index = (int) MathUtil.scale(guessFactor, -1, 1, 0, AIM_BINS - 1);

            for (int x = 0; x < currentSegment.length; x++) {
                double newValue = 1.0 / (Math.pow(index - x, 2) + 1);
                currentSegment[x] = MathUtil.approxRollingAverage(currentSegment[x], newValue);
            }
        }
    }

    // Given the EnemyWave that the bullet was on, and the point where we
    // were hit, update our stat array to reflect the danger in that area.
    public void logHit(BulletWave ew, Point2D.Double targetLocation) {
        int index = getFactorIndex(ew, targetLocation);

        for (int x = 0; x < BINS; x++) {
            // for the spot bin that we were hit on, add 1;
            // for the bins next to it, add 1 / 2;
            // the next one, add 1 / 5; and so on...
            _surfStats[x] = MathUtil.approxRollingAverage(_surfStats[x], 1.0 / (Math.pow(index - x, 2) + 1));
        }
    }
	
    public Point2D.Double predictPosition(BulletWave surfWave, int direction) {
        Point2D.Double predictedPosition = (Point2D.Double)_myLocation.clone();
        double predictedVelocity = getVelocity();
        double predictedHeading = getHeadingRadians();
        double maxTurning, moveAngle, moveDir;
 
        int counter = 0; // number of ticks in the future
        boolean intercepted = false;
 
        do {    // the rest of these code comments are rozu's
            double distance = surfWave.startLocation.distance(predictedPosition);
            moveAngle =
                wallSmoothing(predictedPosition, MathUtil.absoluteBearing(surfWave.startLocation,
                predictedPosition) + (direction * (Math.PI/2 - (distance < 100 ? .8 : .05))), direction)
                - predictedHeading;

            moveDir = 1;

            if(Math.cos(moveAngle) < 0) {
                moveAngle += Math.PI;
                moveDir = -1;
            }
 
            moveAngle = Utils.normalRelativeAngle(moveAngle);

            // maxTurning is built in like this, you can't turn more then this in one tick
            maxTurning = Math.PI/720d*(40d - 3d*Math.abs(predictedVelocity));
            predictedHeading = Utils.normalRelativeAngle(predictedHeading
                + MathUtil.limit(-maxTurning, moveAngle, maxTurning));

            // this one is nice ;). if predictedVelocity and moveDir have
            // different signs you want to breack down
            // otherwise you want to accelerate (look at the factor "2")
            predictedVelocity +=
                (predictedVelocity * moveDir < 0 ? 2*moveDir : moveDir);
            predictedVelocity = MathUtil.limit(-8, predictedVelocity, 8);

            // calculate the new predicted position
            predictedPosition = MathUtil.project(predictedPosition, predictedHeading,
                predictedVelocity);
 
            counter++;
 
            if (predictedPosition.distance(surfWave.startLocation) <
                surfWave.getDistanceTraveled(getTime()) + (counter * surfWave.getVelocity())
                + surfWave.getVelocity()) {
                intercepted = true;
            }
        } while(!intercepted && counter < 500);
 
        return predictedPosition;
    }
	
    public double checkDanger(BulletWave surfWave, int direction) {
        int index = getFactorIndex(surfWave,
            direction != 0 ? predictPosition(surfWave, direction) : _myLocation);
 
        return _surfStats[index];
    }
 
    public void doSurfing() {
        BulletWave surfWave = getClosestSurfableWave();
 
        if (surfWave == null) { return; }
 
        double dangerLeft = checkDanger(surfWave, -1);
        double dangerCenter = checkDanger(surfWave, 0);
        double dangerRight = checkDanger(surfWave, 1);
 
        double goAngle = MathUtil.absoluteBearing(surfWave.startLocation, _myLocation);

        double distance = surfWave.startLocation.distance(_myLocation);

        int direction;
        if (dangerLeft < dangerRight) {
            if(dangerCenter < dangerLeft) {
                return;
            } else {
                direction = -1;
            }
        }
        else {
            if(dangerCenter < dangerRight) {
                return;
            } else {
                direction = 1;
            }
        }

        goAngle = wallSmoothing(_myLocation, goAngle - (Math.PI/2 - (distance < 100 ? .8 : .05)), direction);

        setBackAsFront(this, goAngle);
    }

    public void onPaint(java.awt.Graphics2D g) {
        g.setColor(java.awt.Color.red);
        for (BulletWave w : enemyWaves) {
            w.onPaint(g);
        }

        g.setColor(java.awt.Color.blue);
        for (BulletWave w : myWaves) {
            w.onPaint(g);
        }
    }

    class BulletWave {
	    //Bullet Data
        final Point2D.Double startLocation;
        final long fireTime;
        final double bulletPower, angle;
        //Initial Target Data
        final double initialTargetDistance;
        final int initialTargetDirection;
        final double initialTargetVelocity;
        final double initialTargetAbsBearing;
        final boolean isMine;

        BulletWave(Point2D.Double startLocation, long fireTime, double bulletPower, double angle, double initialTargetDistance, int initialTargetDirection, double initialTargetVelocity, double initialTargetAbsBearing, boolean isMine) {
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

        final double getVelocity() {
            return Rules.getBulletSpeed(bulletPower);
        }

        final double getDistanceTraveled(long time) {
            return getVelocity() * (time - fireTime);
        }

        void onPaint(Graphics2D g) {
            Point2D.Double velocityEndpoint = MathUtil.project(startLocation, Utils.normalAbsoluteAngle(angle+direction*Math.PI/2), initialTargetVelocity);
            g.drawLine((int)startLocation.x, (int)startLocation.y, (int)velocityEndpoint.x, (int)velocityEndpoint.y);

//            Point2D.Double endpoint = MathUtil.project(startLocation, initialTargetAbsBearing, initialTargetDistance);
//            g.drawLine((int)startLocation.x, (int)startLocation.y, (int)endpoint.x, (int)endpoint.y);

            Point2D.Double endpoint1 = MathUtil.project(startLocation, initialTargetAbsBearing - GameRulesUtil.maxEscapeAngle(getVelocity())/2, initialTargetDistance);
            g.drawLine((int)startLocation.x, (int)startLocation.y, (int)endpoint1.x, (int)endpoint1.y);

            Point2D.Double endpoint2 = MathUtil.project(startLocation, initialTargetAbsBearing + GameRulesUtil.maxEscapeAngle(getVelocity())/2, initialTargetDistance);
            g.drawLine((int)startLocation.x, (int)startLocation.y, (int)endpoint2.x, (int)endpoint2.y);

            int radius = (int)getDistanceTraveled(getTime());

            if(radius - 40 < initialTargetDistance)
                g.drawOval((int)(startLocation.x - radius ), (int)(startLocation.y - radius), radius*2, radius*2);
        }
    }

    final static class GameRulesUtil {
        static double maxEscapeAngle(double velocity) {
            return Math.asin(8.0/velocity);
        }
    }

    final static class MathUtil {
        public static double scale(final double valueIn, final double baseMin, final double baseMax, final double limitMin, final double limitMax) {
            return ((limitMax - limitMin) * (valueIn - baseMin) / (baseMax - baseMin)) + limitMin;
        }

        public static Point2D.Double project(Point2D.Double sourceLocation,
                                                 double angle, double length) {
            return new Point2D.Double(sourceLocation.x + Math.sin(angle) * length,
                sourceLocation.y + Math.cos(angle) * length);
        }

        public static double absoluteBearing(Point2D.Double source, Point2D.Double target) {
            return Math.atan2(target.x - source.x, target.y - source.y);
        }

        public static double limit(double min, double value, double max) {
            return Math.max(min, Math.min(value, max));
        }

        public static double rollingAvg(double value, double newEntry, double n, double weighting ) {
            return (value * n + newEntry * weighting)/(n + weighting);
        }

        static double approxRollingAverage (double avg, double new_sample) {
            avg -= avg / 100;
            avg += new_sample / 100;

            return avg;
        }
    }
}
