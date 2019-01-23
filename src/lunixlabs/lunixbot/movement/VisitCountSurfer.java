package lunixlabs.lunixbot.movement;

import lunixlabs.lunixbot.BulletWave;
import lunixlabs.lunixbot.MathUtils;
import robocode.Bullet;
import robocode.util.Utils;

import java.awt.geom.Point2D;

public class VisitCountSurfer extends AbstractSurfer {
    private static int BINS = 47;

    private double surfStats[] = new double[BINS];

    @Override
    protected double checkDanger(BulletWave surfWave, Point2D.Double predictedPosition) {
        return surfStats[getFactorIndex(surfWave, predictedPosition)];
    }

    // Given the EnemyWave that the bullet was on, and the point where we
    // were hit, calculate the index into our stat array for that factor.
    private static int getFactorIndex(BulletWave surfWave, Point2D.Double targetLocation) {
        double offsetAngle = MathUtils.absoluteBearing(surfWave.startLocation, targetLocation)
                - surfWave.angle;
        double factor = Utils.normalRelativeAngle(offsetAngle)
                / MathUtils.maxEscapeAngle(surfWave.getVelocity()) * surfWave.initialTargetDirection;

        return (int) MathUtils.limit(0,
                (factor * ((BINS - 1) / 2)) + ((BINS - 1) / 2),
                BINS - 1);
    }

    protected void logHit(BulletWave bulletWave, Bullet bullet, double myVelocity, double myHeading, Point2D.Double myLocation, long time) {
        int index = getFactorIndex(bulletWave, new Point2D.Double(bullet.getX(), bullet.getY()));
        for (int x = 0; x < BINS; x++) {
            // for the spot bin that we were hit on, add 1;
            // for the bins next to it, add 1 / 2;
            // the next one, add 1 / 5; and so on...
            surfStats[x] = MathUtils.approxRollingAverage(surfStats[x], 1.0 / (Math.pow(index - x, 2) + 1));
        }
    }
}
