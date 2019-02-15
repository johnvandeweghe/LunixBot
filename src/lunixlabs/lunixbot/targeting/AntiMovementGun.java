package lunixlabs.lunixbot.targeting;

import lunixlabs.lunixbot.BulletWave;
import lunixlabs.lunixbot.MathUtils;
import lunixlabs.lunixbot.movement.IMovement;
import lunixlabs.lunixbot.movement.VisitCountSurfer;
import robocode.Bullet;
import robocode.Rules;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

public class AntiMovementGun extends AbstractGuessFactorGun {

    private IMovement movement;
    private double lastEnergy = 100.0;
    private long lastTime = 0;
    private double enemyLastVelocity = 0;
    private double enemyLastHeading = 0;

    protected Point2D.Double[] _predictions;

    protected double _inverseScannedAbsBearing;

    public AntiMovementGun(IMovement movement) {
        this.movement = movement;
    }

    @Override
    public void reset() {
        super.reset();
        movement.reset();
        lastEnergy = 100.0;
        lastTime = 0;
    }

    @Override
    public void track(ScannedRobotEvent event, Point2D.Double myLocation, double myVelocity, double myHeading, long time) {
        super.track(event, myLocation, myVelocity, myHeading, time);

        enemyLastVelocity = event.getVelocity();
        enemyLastHeading = event.getHeadingRadians();
        lastTime = time;

        double scannedAbsoluteBearing = Utils.normalAbsoluteAngle(event.getBearingRadians() + myHeading + Math.PI);

        _inverseScannedAbsBearing = scannedAbsoluteBearing;
        movement.track(new ScannedRobotEvent(
                "fake",
                lastEnergy,
                Utils.normalRelativeAngle(scannedAbsoluteBearing - event.getHeadingRadians()),
                event.getDistance(),
                myHeading,
                myVelocity,
                false
            ),
            event.getVelocity(),
            event.getHeadingRadians(),
            enemyLocation,
            time
        );
    }

    @Override
    public double chooseGunAngle(double bulletPower, Point2D.Double myLocation, double absoluteBearingToEnemy) {
        Point2D.Double predictedPosition = (Point2D.Double)enemyLocation.clone();
        double predictedVelocity = enemyLastVelocity;
        double predictedHeading = enemyLastHeading;
        double maxTurning, moveDir;
        Double moveAngle;

        this._predictions = new Point2D.Double[500];
        int counter = 0; // number of ticks in the future

        do {    // the rest of these code comments are rozu's
            //This should match the surfWave method to ensure correctly predicted movement
            _predictions[counter] = predictedPosition;
            moveAngle = movement.suggestAngle(
                    predictedVelocity, predictedHeading, predictedPosition, lastTime + counter
            );

            if(moveAngle == null) {
                break;
            }

            moveAngle -= predictedHeading;

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
        } while(counter < myLocation.distance(enemyLocation) / Rules.getBulletSpeed(bulletPower) && counter < 500);

        return MathUtils.absoluteBearing(myLocation, predictedPosition);
    }

    /**
     * @param bulletPower power of the bullet (0.1-3.0)
     * @param myLocation
     * @return a number in range [-1, 1] where 1 is the direction the enemy is going and -1 is the opposite
     */
    @Override
    protected double chooseGuessFactor(double bulletPower, Point2D.Double myLocation) {
        //TODO: Not a guess factor gun. Change base class.
        return 0;
    }

    @Override
    protected void trackPass(BulletWave bulletWave, double guessFactor) {
        //
    }

    @Override
    protected void trackHit(BulletWave bulletWave) {
        super.trackHit(bulletWave);
        //long time = bulletWave.fireTime + (long)bulletWave.getBulletTimeToTarget();
        Point2D.Double pos = MathUtils.project(bulletWave.startLocation, bulletWave.angle, bulletWave.startLocation.distance(enemyLocation)-15);
        movement.logHit(
                new Bullet(bulletWave.angle, pos.x, pos.y, bulletWave.bulletPower, "blah", "blah", true, (int)(Math.random() * 10000)),
                enemyLastVelocity,
                enemyLastHeading,
                enemyLocation,
                lastTime
        );
    }

    @Override
    public double choosePower(Point2D.Double myLocation, double myEnergy) {
        lastEnergy = myEnergy;
        return .1;
    }

    @Override
    public void onPaint(Graphics2D g, long time) {
        super.onPaint(g, time);
        g.setColor(Color.orange);
        movement.onPaint(g, time);
        Point2D.Double lineend = MathUtils.project(enemyLocation, _inverseScannedAbsBearing, 800);
        g.draw(new Line2D.Double(enemyLocation, lineend));
        for (Point2D.Double aDouble1 : _predictions)
            if (aDouble1 != null)
                g.fillOval((int) aDouble1.x, (int) aDouble1.y, 5, 5);
    }
}
