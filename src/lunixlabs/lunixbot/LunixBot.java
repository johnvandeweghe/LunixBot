package lunixlabs.lunixbot;
import lunixlabs.lunixbot.movement.IMovement;
import lunixlabs.lunixbot.movement.VisitCountSurfer;
import lunixlabs.lunixbot.targeting.GuessFactorTargeting;
import lunixlabs.lunixbot.targeting.ITargeting;
import robocode.*;
import robocode.util.Utils;

import java.awt.*;
import java.awt.geom.Point2D;

public class LunixBot extends AdvancedRobot
{
    private static IMovement movement = new VisitCountSurfer();

    private static ITargeting targeting = new GuessFactorTargeting();

    public void run() {
		setColors(Color.pink,Color.magenta,Color.green, new Color(57, 255, 20), new Color(0,0,0,0)); // body,gun,radar
        movement.reset();
        targeting.reset();
 
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
        Point2D.Double myLocation = new Point2D.Double(getX(), getY());

        double scannedAbsoluteBearing = e.getBearingRadians() + getHeadingRadians();
 
        setTurnRadarRightRadians(Utils.normalRelativeAngle(scannedAbsoluteBearing
            - getRadarHeadingRadians()) * 2);

        movement.track(e, getVelocity(), getHeadingRadians(), myLocation, getTime());

        double goAngle = movement.suggestAngle(getVelocity(), getHeadingRadians(), myLocation, getTime());

        lunixlabs.lunixbot.movement.Utils.moveInDirection(this, goAngle);

        targeting.track(e, myLocation, getVelocity(), getHeadingRadians(), getTime());

        double power = targeting.choosePower(myLocation, getEnergy());
        double angleOffset = targeting.chooseTargetOffset(power);

//        if(missesSinceLastHit > 2) {
//            angleOffset = Math.PI*Math.random() - 1;
//            power = .1;
//            missesSinceLastHit = 0;
//            System.out.println("RANDO!");
//        }

        double gunAdjust = Utils.normalRelativeAngle(
                scannedAbsoluteBearing - getGunHeadingRadians() + angleOffset);

        setTurnGunRightRadians(gunAdjust);

        if (getGunHeat() == 0 && gunAdjust < Math.atan2(9, e.getDistance()) && setFireBullet(power) != null) {
            targeting.trackShot(power, angleOffset, getVelocity(), getHeadingRadians(), myLocation, getTime());
		}
	}

    public void onHitByBullet(HitByBulletEvent e) {
	    movement.logHit(e.getBullet(), getVelocity(), getHeadingRadians(), new Point2D.Double(getX(), getY()), getTime());
        System.out.println("Ow! " + Math.round(movement.getDodgeRate() * 10000d) / 100d + "%");
    }

    public void onBulletHitBullet(BulletHitBulletEvent e) {
        movement.logHit(e.getHitBullet(), getVelocity(), getHeadingRadians(), new Point2D.Double(getX(), getY()), getTime());
    }

    @Override
    public void onBulletHit(BulletHitEvent event) {
        movement.updateEnemyEnergy(event.getEnergy());
        targeting.trackHit(event.getBullet());
        System.out.println("Yeah! " + Math.round(targeting.getHitRate() * 10000d) / 100d + "%");
    }

    @Override
    public void onHitRobot(HitRobotEvent event) {
        movement.updateEnemyEnergy(event.getEnergy());
    }

    public void onPaint(java.awt.Graphics2D g) {
        movement.onPaint(g, getTime());

        targeting.onPaint(g, getTime());
    }

}
