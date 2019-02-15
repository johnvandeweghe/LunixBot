package lunixlabs.lunixbot.targeting;

import lunixlabs.lunixbot.BulletWave;
import lunixlabs.lunixbot.MathUtils;
import robocode.Rules;

import java.awt.*;
import java.awt.geom.Point2D;

public class VisitCountGun extends AbstractGuessFactorGun {
    public static final int VELOCITY_BINS = 19;
    public static final int BULLETTIME_BINS = 11;
    private final static int AIM_BINS = 31;
    // 31 is the number of unique GuessFactors we're using
    // Note: this must be odd number so we can get
    // GuessFactor 0 at middle.

    //The inner most array is visit count data from
    // 0 -          The opposite direction they are going at MEA
    // AIM_BINS/2 - Where they are
    // AIM_BINS-1 - The direction they are going at MEA
    private double[][] stats = new double[VELOCITY_BINS][AIM_BINS];

    protected void trackPass(BulletWave bulletWave, double guessFactor) {
//        int yIndex = segmentBulletTime(bulletWave.getBulletTimeToTarget());
        int yIndex = segmentVelocity(bulletWave.initialTargetVelocity);
        int xIndex = (int) MathUtils.scale(guessFactor, -1, 1, 0, AIM_BINS - 1);

        for (int y = 0; y < stats.length; y++) {
            for (int x = 0; x < stats[y].length; x++) {
                double newValue = 1.0 / (Math.pow(xIndex - x, 2) + Math.pow(yIndex - y, 2) + 1);
                stats[y][x] = MathUtils.approxRollingAverage(stats[y][x], newValue, 100);
            }
        }
    }

    @Override
    protected double chooseGuessFactor(double bulletPower, Point2D.Double myLocation) {
        //If they are disabled just shoot them
        if(enemyLastEnergy == 0) {
            return 0;
        }

        //If they are bullet shielding shoot off center by a small amount
        if(getCollisionRate() > .8) {
            return .01;
        }

        double distance = enemyLocation.distance(myLocation);
        //double[] currentStats = stats[segmentBulletTime(distance / Rules.getBulletSpeed(bulletPower))];
        double[] currentStats = stats[segmentVelocity(enemyLateralVelocity)];

//        double[] prettyStats = Arrays.stream(currentStats).map(d -> Math.round(d * 10000d) / 1000d).toArray();
//        System.out.println(segmentVelocity(enemyLateralVelocity) + "::" + Arrays.toString(prettyStats));

        double robotWidthAngle = Math.atan2(36.0, distance)*2;
        double maxEscapeAngle = MathUtils.maxEscapeAngle(Rules.getBulletSpeed(bulletPower));

        double anglePerIndex = maxEscapeAngle * 2 / (double)AIM_BINS;

        int botIndexes = (int)Math.round(robotWidthAngle / anglePerIndex);

        if (botIndexes % 2 == 0) botIndexes--;

        botIndexes = Math.max(1, botIndexes);

        int halfBot = (botIndexes-1)/2;

        int bestindex = (int)Math.round(AIM_BINS/2.0);	// initialize it to be in the middle, guessfactor 0.
        double currentMax = 0;
        for (int i=halfBot; i<currentStats.length-halfBot; i++) {
            double clusterStats = 0;
            for(int o = i - halfBot; o < i + halfBot; o++) {
                clusterStats += currentStats[o];
            }
            if (currentMax < clusterStats) {
                currentMax = clusterStats;
                bestindex = i;
            }
        }

        return MathUtils.scale(bestindex, 0, AIM_BINS - 1, -1, 1);
    }

    private int segmentVelocity(double velocity){
        return (int)Math.round(velocity) + 8;
    }

    private int segmentBulletTime(double bullettime){
        return (int)MathUtils.scale(bullettime, 0, 53, 0, 11);
    }

    @Override
    public double choosePower(Point2D.Double myLocation, double myEnergy) {
        if (enemyLastEnergy == 0) return 2.98643;
        if (getCollisionRate() > .8) return 2.9865;
        if(myLocation.distance(enemyLocation) < 150) return 2.9875;
        if(myEnergy < 10) return .1113;
        if(getHitRate() > .5) return 2.95;
        if(getHitRate() < .1) return .1111;
        return MathUtils.scale(getHitRate(), .1, .5, .5, enemyLastBulletPower - .01);
    }

    @Override
    public void onPaint(Graphics2D g, long time) {
        g.setColor(Color.green);
        for (BulletWave w : myWaves) {
            w.onPaint(g, time, stats[segmentVelocity(w.initialTargetVelocity)]);
        }
//        for (int y = 0; y < stats.length; y++) {
//            for (int x = 0; x < stats[y].length; x++) {
//                int intensity = (int)MathUtils.scale(stats[y][x], 0, .2, 0, 255);
//                g.setColor(new Color(intensity, 255 - intensity, 0));
//                g.fillRect(x*10,y*10,10,10);
//            }
//        }
    }
}
