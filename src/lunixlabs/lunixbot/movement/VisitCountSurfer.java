package lunixlabs.lunixbot.movement;

import lunixlabs.lunixbot.BulletWave;
import lunixlabs.lunixbot.MathUtils;
import robocode.Bullet;
import robocode.util.Utils;

import java.awt.*;
import java.awt.geom.Point2D;

public class VisitCountSurfer extends AbstractSurfer {
    private static int BINS = 47;

    private double[] surfStats = new double[BINS];

    private int noWaveDirection = -1;

    @Override
    protected double checkDanger(BulletWave surfWave, Point2D.Double predictedPosition) {
        return surfStats[getFactorIndex(surfWave, predictedPosition)];
    }

    // Given the EnemyWave that the bullet was on, and the point where we
    // were hit, calculate the index into our stat array for that factor.
    private static int getFactorIndex(BulletWave surfWave, Point2D.Double targetLocation) {
        double offsetAngle = MathUtils.absoluteBearing(surfWave.startLocation, targetLocation)
                - surfWave.initialTargetAbsBearing;
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
            surfStats[x] = MathUtils.approxRollingAverage(surfStats[x], 1.0 / (Math.pow(index - x, 2) + 1), 100);
        }
    }

    @Override
    protected Double suggestIdleAngle(double myVelocity, double myHeading, Point2D.Double myLocation, long time) {
        noWaveDirection *= Math.random() > .8 ? -1 : 1;
        return lunixlabs.lunixbot.movement.Utils.wallSmoothing(myLocation, MathUtils.absoluteBearing(enemyLocation, myLocation) - (Math.PI/2 - .4), noWaveDirection);
    }

    @Override
    public void onPaint(Graphics2D g, long time) {
        g.setColor(java.awt.Color.red);
        for (BulletWave w : enemyWaves) {
            w.onPaint(g, time, surfStats);
        }

        for (Point2D.Double aDouble1 : _dLeft)
            if (aDouble1 != null)
                g.fillOval((int) aDouble1.x, (int) aDouble1.y, 5, 5);
        g.setColor(Color.green);
        for (Point2D.Double aDouble : _dright)
            if (aDouble != null)
                g.fillOval((int) aDouble.x, (int) aDouble.y, 5, 5);
    }
}
