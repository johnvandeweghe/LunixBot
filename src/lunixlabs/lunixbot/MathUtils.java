package lunixlabs.lunixbot;

import org.jetbrains.annotations.Contract;

import java.awt.geom.Point2D;

public final class MathUtils {
    @Contract(pure = true)
    static double scale(final double valueIn, final double baseMin, final double baseMax, final double limitMin, final double limitMax) {
        return ((limitMax - limitMin) * (valueIn - baseMin) / (baseMax - baseMin)) + limitMin;
    }

    @Contract(pure = true)
    public static Point2D.Double project(Point2D.Double sourceLocation,
                                  double angle, double length) {
        return new Point2D.Double(sourceLocation.x + Math.sin(angle) * length,
            sourceLocation.y + Math.cos(angle) * length);
    }

    @Contract(pure = true)
    public static double absoluteBearing(Point2D.Double source, Point2D.Double target) {
        return Math.atan2(target.x - source.x, target.y - source.y);
    }

    @Contract(pure = true)
    public static double limit(double min, double value, double max) {
        return Math.max(min, Math.min(value, max));
    }

    @Contract(pure = true)
    static double rollingAvg(double value, double newEntry, double n, double weighting ) {
        return (value * n + newEntry * weighting)/(n + weighting);
    }

    @Contract(pure = true)
    public static double approxRollingAverage(double avg, double new_sample) {
        avg -= avg / 100;
        avg += new_sample / 100;

        return avg;
    }

    @Contract(pure = true)
    public static double maxEscapeAngle(double velocity) {
        return Math.asin(8.0/velocity);
    }

    @Contract(pure = true)
    public static double getMaxTurning(double velocity) {
        return Math.PI/720d*(40d - 3d*Math.abs(velocity));
    }
}
