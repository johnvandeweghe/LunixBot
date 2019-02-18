package lunixlabs.lunixbot;
import lunixlabs.lunixbot.movement.BasicSurfer;
import lunixlabs.lunixbot.movement.IMovement;
import lunixlabs.lunixbot.movement.VisitCountSurfer;
import lunixlabs.lunixbot.targeting.*;
import robocode.*;
import robocode.util.Utils;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

/**
 * TODO:
 * - Bullet Shadows in VC gun
 * - DC-PIF Gun (Anti movement gun has PIF, should be a good base)
 * - PIF surfing
 * - Anti gun movement (with Basic VC gun dodger, should be very good)
 */
public class LunixBot extends AdvancedRobot
{
    private static IMovement movement = new VisitCountSurfer();

    private static ITargeting[] guns = new ITargeting[]{
            new VisitCountGun(),
            new RandomGun(),
            new HeadOnGun(),
            new AntiMovementGun(new BasicSurfer())
    };

    private static int currentGun = 3;

    private double _goAngle;

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

        Double goAngle = movement.suggestAngle(getVelocity(), getHeadingRadians(), myLocation, getTime());

        if(goAngle != null) {
            this._goAngle = goAngle;
            lunixlabs.lunixbot.movement.Utils.moveInDirection(this, goAngle);
        }

        for(ITargeting gun : guns)
            gun.track(e, myLocation, getVelocity(), getHeadingRadians(), getTime());

        double[] powers = new double[guns.length];
        double[] angleOffsets = new double[guns.length];

        for (int i = 0; i < guns.length; i++) {
            ITargeting gun = guns[i];
            powers[i] = gun.choosePower(myLocation, getEnergy());
            angleOffsets[i] = gun.chooseGunAngle(powers[i], myLocation, scannedAbsoluteBearing);
        }

//        if(getEnergy() > 90 || guns[currentGun].getHitRate() < .05) {
//            if(currentGun != 1) {
//                System.out.println("Switching to Random gun.");
//            }
//            currentGun = 1;
//        } else {
//            for (int i = 0; i < guns.length; i++) {
//                if (guns[i].getHitRate() > guns[currentGun].getHitRate()) {
//                    currentGun = i;
//                    for(ITargeting gun : guns)
//                        System.out.println(gun.getClass().getName() + ": " + gun.getHitRate() + "%");
//                    System.out.println("Switching to gun #" + i);
//                }
//            }
//        }

        double gunAdjust = Utils.normalRelativeAngle(
                angleOffsets[currentGun] - getGunHeadingRadians());

        setTurnGunRightRadians(gunAdjust);

        if (getGunHeat() == 0 && powers[currentGun] < getEnergy() && gunAdjust < Math.atan2(9, e.getDistance()) && setFireBullet(powers[currentGun]) != null) {
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
        //guns[3].onPaint(g, getTime());

        Point2D.Double wallStick = MathUtils.project(new Point2D.Double(getX(), getY()), _goAngle, 160);

        g.draw(new Line2D.Double(getX(), getY(), wallStick.x, wallStick.y));
    }

}
