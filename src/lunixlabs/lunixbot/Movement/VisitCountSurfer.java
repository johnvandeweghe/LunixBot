package lunixlabs.lunixbot.Movement;

import lunixlabs.lunixbot.BulletWave;
import lunixlabs.lunixbot.MathUtils;
import robocode.Bullet;
import robocode.Rules;
import robocode.util.Utils;

import java.awt.*;
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
        double offsetAngle = (MathUtils.absoluteBearing(surfWave.startLocation, targetLocation)
                - surfWave.angle);
        double factor = Utils.normalRelativeAngle(offsetAngle)
                / MathUtils.maxEscapeAngle(surfWave.getVelocity()) * surfWave.initialTargetDirection;

        return (int) MathUtils.limit(0,
                (factor * ((BINS - 1) / 2)) + ((BINS - 1) / 2),
                BINS - 1);
    }

    @Override
    public void logHit(Bullet bullet, double myVelocity, double myHeading, Point2D.Double myLocation, long time) {
        super.logHit(bullet, myVelocity, myHeading, myLocation, time);

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
                logHit(getFactorIndex(hitWave, hitBulletLocation));

                // We can remove this wave now, of course.
                enemyWaves.remove(enemyWaves.lastIndexOf(hitWave));
            }
        }
    }

    private void logHit(int visitIndex) {
        for (int x = 0; x < BINS; x++) {
            // for the spot bin that we were hit on, add 1;
            // for the bins next to it, add 1 / 2;
            // the next one, add 1 / 5; and so on...
            surfStats[x] = MathUtils.approxRollingAverage(surfStats[x], 1.0 / (Math.pow(visitIndex - x, 2) + 1));
        }
    }

    @Override
    public void onPaint(Graphics2D g, long time) {
        g.setColor(java.awt.Color.red);
        for (BulletWave w : enemyWaves) {
            w.onPaint(g, time);
        }
    }
}
