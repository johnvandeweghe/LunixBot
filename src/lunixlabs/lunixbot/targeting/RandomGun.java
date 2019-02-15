package lunixlabs.lunixbot.targeting;

import lunixlabs.lunixbot.BulletWave;

import java.awt.*;
import java.awt.geom.Point2D;

public class RandomGun extends AbstractGuessFactorGun {
    /**
     * @param bulletPower power of the bullet (0.1-3.0)
     * @param myLocation
     * @return a number in range [-1, 1] where 1 is the direction the enemy is going and -1 is the opposite
     */
    @Override
    protected double chooseGuessFactor(double bulletPower, Point2D.Double myLocation) {
        return Math.random() * 2 - 1;
    }

    @Override
    protected void trackPass(BulletWave bulletWave, double guessFactor) {
        //done!
    }

    @Override
    public double choosePower(Point2D.Double myLocation, double myEnergy) {
        //This is guaranteed to not be very good, so lets not waste energy.
        return .1;
    }

    @Override
    public void onPaint(Graphics2D g, long time) {
        g.setColor(Color.yellow);
        for (BulletWave w : myWaves) {
            w.onPaint(g, time);
        }
    }
}
