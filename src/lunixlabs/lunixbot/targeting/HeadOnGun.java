package lunixlabs.lunixbot.targeting;

import lunixlabs.lunixbot.BulletWave;

import java.awt.*;
import java.awt.geom.Point2D;

public class HeadOnGun extends AbstractGuessFactorGun {
    /**
     * @param bulletPower power of the bullet (0.1-3.0)
     * @param myLocation
     * @return a number in range [-1, 1] where 1 is the direction the enemy is going and -1 is the opposite
     */
    @Override
    protected double chooseGuessFactor(double bulletPower, Point2D.Double myLocation) {
        return 0;
    }

    @Override
    protected void trackPass(BulletWave bulletWave, double guessFactor) {
        //done!
    }

    @Override
    public double choosePower(Point2D.Double myLocation, double myEnergy) {
        if (enemyLastEnergy == 0)
            return 2.98643;
        if (getCollisionRate() > .8)
            return 2.9865;
        if(myLocation.distance(enemyLocation) < 150)
            return 2.9875;
        if(myEnergy < 10)
            return .1113;
        if (getHitRate() > .8)
            return 3;
        if (getHitRate() > .5)
            return 2;
        if(getHitRate() > .3)
            return 1;
        return .5;
    }

    @Override
    public void onPaint(Graphics2D g, long time) {
        g.setColor(Color.black);
        for (BulletWave w : myWaves) {
            w.onPaint(g, time);
        }
    }
}
