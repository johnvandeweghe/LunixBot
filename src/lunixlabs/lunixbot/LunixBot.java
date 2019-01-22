package lunixlabs.lunixbot;
import lunixlabs.lunixbot.Movement.IMovement;
import lunixlabs.lunixbot.Movement.VisitCountSurfer;
import robocode.*;
import robocode.util.Utils;

import java.awt.geom.Point2D;
import java.util.ArrayList;

public class LunixBot extends AdvancedRobot
{
    private ArrayList<BulletWave> myWaves = new ArrayList<>();
    private static int AIM_BINS = 31;
	
	private static double[][][] stats = new double[19][2][AIM_BINS]; // 31 is the number of unique GuessFactors we're using
					  // Note: this must be odd number so we can get
					  // GuessFactor 0 at middle.
    private int direction = 1;

    private int missesSinceLastHit = 0;

    private static IMovement movement = new VisitCountSurfer();

    /**
	 * run: LunixBot's default behavior
	 */
	public void run() {
		// setColors(Color.red,Color.blue,Color.green); // body,gun,radar
        movement.reset();
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
        // our bot's location
        Point2D.Double _myLocation = new Point2D.Double(getX(), getY());

        double scannedAbsoluteBearing = e.getBearingRadians() + getHeadingRadians();
        double enemyLateralVelocity = e.getVelocity() * Math.sin(e.getHeadingRadians() - scannedAbsoluteBearing);
 
        setTurnRadarRightRadians(Utils.normalRelativeAngle(scannedAbsoluteBearing
            - getRadarHeadingRadians()) * 2);

        movement.track(e, getVelocity(), getHeadingRadians(), _myLocation, getTime());

        double goAngle = movement.suggestAngle(getVelocity(), getHeadingRadians(), _myLocation, getTime());

        lunixlabs.lunixbot.Movement.Utils.moveInDirection(this, goAngle);

        cleanPassedWaves(myWaves, MathUtils.project(_myLocation, scannedAbsoluteBearing, e.getDistance()), getTime());

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
        double guessfactor = MathUtils.scale(bestindex, 0, AIM_BINS - 1, -1, 1);
        //(double)(bestindex - (currentStats.length - 1) / 2)
        //                        / ((currentStats.length - 1) / 2);

        return direction * guessfactor * MathUtils.maxEscapeAngle(Rules.getBulletSpeed(power));
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

    public void onHitByBullet(HitByBulletEvent e) {
	    movement.logHit(e.getBullet(), getVelocity(), getHeadingRadians(), new Point2D.Double(getX(), getY()), getTime());
    }

    public void onBulletHitBullet(BulletHitBulletEvent e) {
        movement.logHit(e.getHitBullet(), getVelocity(), getHeadingRadians(), new Point2D.Double(getX(), getY()), getTime());
    }

    @Override
    public void onBulletHit(BulletHitEvent event) {
        movement.updateEnemyEnergy(event.getEnergy());
        missesSinceLastHit = 0;
    }

    @Override
    public void onHitRobot(HitRobotEvent event) {
        movement.updateEnemyEnergy(event.getEnergy());
    }

    private void onBulletMiss(BulletWave bulletWave, Point2D.Double targetLocation) {
        if(bulletWave.isMine) {
            missesSinceLastHit++;
            double[] currentSegment = stats[segmentVelocity(bulletWave.initialTargetVelocity)][bulletWave.initialTargetDirection == -1 ? 0 : 1];

            double desiredDirection = MathUtils.absoluteBearing(bulletWave.startLocation, targetLocation);
            double angleOffset = Utils.normalRelativeAngle(desiredDirection - bulletWave.initialTargetAbsBearing);
            double guessFactor =
                    Math.max(-1, Math.min(1, angleOffset / MathUtils.maxEscapeAngle(Rules.getBulletSpeed(bulletWave.bulletPower)))) * bulletWave.initialTargetDirection;

            int index = (int) MathUtils.scale(guessFactor, -1, 1, 0, AIM_BINS - 1);

            for (int x = 0; x < currentSegment.length; x++) {
                double newValue = 1.0 / (Math.pow(index - x, 2) + 1);
                currentSegment[x] = MathUtils.approxRollingAverage(currentSegment[x], newValue);
            }
        }
    }

    public void onPaint(java.awt.Graphics2D g) {
        movement.onPaint(g, getTime());

        g.setColor(java.awt.Color.blue);
        for (BulletWave w : myWaves) {
            w.onPaint(g, getTime());
        }
    }

}
