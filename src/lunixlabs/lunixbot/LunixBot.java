package lunixlabs.lunixbot;
import lunixlabs.lunixbot.movement.IMovement;
import lunixlabs.lunixbot.movement.VisitCountSurfer;
import lunixlabs.lunixbot.targeting.HeadOnGun;
import lunixlabs.lunixbot.targeting.RandomGun;
import lunixlabs.lunixbot.targeting.VisitCountGun;
import lunixlabs.lunixbot.targeting.ITargeting;
import robocode.*;
import robocode.util.Utils;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.Arrays;

/**
 * TODO:
 * - Bullet Shadows
 * - DC-PIF Gun
 * - PIF surfing
 */
public class LunixBot extends AdvancedRobot
{
    private static IMovement movement = new VisitCountSurfer();

    private static ITargeting[] guns = new ITargeting[]{
            new VisitCountGun(),
            new RandomGun(),
            new HeadOnGun()
    };

    private static int currentGun = 0;

    public void run() {
		setColors(Color.pink,Color.magenta,Color.green, new Color(57, 255, 20), new Color(0,0,0,0)); // body,gun,radar
        movement.reset();

        for(ITargeting gun : guns)
            System.out.println(gun.getClass().getName() + ": " + gun.getHitRate() + "%");

        for(ITargeting gun : guns)
            gun.reset();
 
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

        BulletWave enemyWave = movement.track(e, getVelocity(), getHeadingRadians(), myLocation, getTime());
        if(enemyWave != null) {
            for(ITargeting gun : guns)
                gun.trackEnemyWave(enemyWave);
        }

        double goAngle = movement.suggestAngle(getVelocity(), getHeadingRadians(), myLocation, getTime());

        lunixlabs.lunixbot.movement.Utils.moveInDirection(this, goAngle);

        for(ITargeting gun : guns)
            gun.track(e, myLocation, getVelocity(), getHeadingRadians(), getTime());


        double[] powers = new double[guns.length];
        double[] angleOffsets = new double[guns.length];

        for (int i = 0; i < guns.length; i++) {
            ITargeting gun = guns[i];
            powers[i] = gun.choosePower(myLocation, getEnergy());
            angleOffsets[i] = gun.chooseTargetOffset(powers[i], myLocation);
        }

//        if(missesSinceLastHit > 2) {
//            angleOffset = Math.PI*Math.random() - 1;
//            power = .1;
//            missesSinceLastHit = 0;
//            System.out.println("RANDO!");
//        }

        double gunAdjust = Utils.normalRelativeAngle(
                scannedAbsoluteBearing - getGunHeadingRadians() + angleOffsets[currentGun]);

        setTurnGunRightRadians(gunAdjust);

        if (getGunHeat() == 0 && gunAdjust < Math.atan2(9, e.getDistance()) && setFireBullet(powers[currentGun]) != null) {
            for (int i = 0; i < guns.length; i++) {
                ITargeting gun = guns[i];
                gun.trackShot(powers[i], angleOffsets[i], getVelocity(), getHeadingRadians(), myLocation, getTime());
            }
		}
	}

    public void onHitByBullet(HitByBulletEvent e) {
	    movement.logHit(e.getBullet(), getVelocity(), getHeadingRadians(), new Point2D.Double(getX(), getY()), getTime());
        System.out.println("Ow! " + Math.round(movement.getDodgeRate() * 10000d) / 100d + "%");
    }

    public void onBulletHitBullet(BulletHitBulletEvent e) {
        for(ITargeting gun : guns)
            gun.trackBulletHitBullet(e.getHitBullet());
        movement.logHit(e.getHitBullet(), getVelocity(), getHeadingRadians(), new Point2D.Double(getX(), getY()), getTime());
    }

    @Override
    public void onBulletHit(BulletHitEvent event) {
        movement.updateEnemyEnergy(event.getEnergy());
        //guns[currentGun].trackHit(event.getBullet());
        System.out.println("Yeah! " + Math.round(guns[currentGun].getHitRate() * 10000d) / 100d + "%");
    }

    @Override
    public void onHitRobot(HitRobotEvent event) {
        movement.updateEnemyEnergy(event.getEnergy());
    }

    public void onPaint(java.awt.Graphics2D g) {
        movement.onPaint(g, getTime());

        guns[currentGun].onPaint(g, getTime());
    }

}
