package lunixlabs.lunixbot.movement;

import lunixlabs.lunixbot.MathUtils;
import org.jetbrains.annotations.Contract;
import robocode.AdvancedRobot;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class Utils {
    /** This is a rectangle that represents an 800x600 battle field,
    * used for a simple, iterative WallSmoothing method (by PEZ).
    * If you're not familiar with WallSmoothing, the wall stick indicates
    * the amount of space we try to always have on either end of the tank
    * (extending straight out the front or back) before touching a wall.
    */
    private static Rectangle2D.Double _fieldRect
            = new Rectangle2D.Double(18, 18, 764, 564);
    private static double WALL_STICK = 36;

    @Contract(pure = true)
    public static double wallSmoothing(Point2D.Double botLocation, double angle, int orientation) {
        while (!_fieldRect.contains(MathUtils.project(botLocation, angle, WALL_STICK))) {
            angle += orientation*0.05;
        }
        return angle;
    }

    public static void moveInDirection(AdvancedRobot robot, double goAngle) {
        double angle =
                robocode.util.Utils.normalRelativeAngle(goAngle - robot.getHeadingRadians());
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
}
