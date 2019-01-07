package lunixlabs;

import java.awt.geom.*;
import robocode.util.Utils;
 
public class WaveBullet
{
	private final int MAX_READINGS = 10;
	private Point2D.Double start;
	private double startBearing, power;
	private long   fireTime;
	private int    direction;
	private double[]  returnSegment;
	private static int readings = 0;
 
	public WaveBullet(double x, double y, double bearing, double power,
			int direction, long time, double[] segment)
	{
		start = new Point2D.Double(x, y);
		startBearing   = bearing;
		this.power     = power;
		this.direction = direction;
		fireTime       = time;
		returnSegment  = segment;
	}

	public double getBulletSpeed()
	{
		return LunixBot.GameRulesUtil.bulletVelocity(power);
	}

	public double maxEscapeAngle()
	{
		return LunixBot.GameRulesUtil.maxEscapeAngle(getBulletSpeed());
	}
	public boolean checkHit(double enemyX, double enemyY, long currentTime)
	{
		// if the distance from the wave origin to our enemy has passed
		// the distance the bullet would have traveled...
		if (Point2D.distance(start.x, start.y, enemyX, enemyY) <=
				(currentTime - fireTime) * LunixBot.GameRulesUtil.bulletVelocity(power))
		{
			double desiredDirection = Math.atan2(enemyX - start.x, enemyY - start.y);
			double angleOffset = Utils.normalRelativeAngle(desiredDirection - startBearing);
			double guessFactor =
				Math.max(-1, Math.min(1, angleOffset / maxEscapeAngle())) * direction;
			int index = (int) Math.round((returnSegment.length - 1) /2 * (guessFactor + 1));
			readings++;
			//System.out.println(readings);
			for (int x = 0; x < returnSegment.length; x++) {
				returnSegment[x] = LunixBot.MathUtil.rollingAvg(returnSegment[x], 1.0 / (Math.pow(index - x, 2) + 1), Math.min(readings, this.MAX_READINGS), 1) ;
			}
			return true;
		}
		return false;
	}

	public void onPaint(java.awt.Graphics2D g, long currentTime) {

		//int radius = (int)(w.distanceTraveled + w.bulletVelocity);
		//hack to make waves line up visually, due to execution sequence in robocode engine
		//use this only if you advance waves in the event handlers (eg. in onScannedRobot())
		//NB! above hack is now only necessary for robocode versions before 1.4.2
		//otherwise use:
		int radius = (int)((currentTime - fireTime) * LunixBot.GameRulesUtil.bulletVelocity(power));

		//Point2D.Double center = w.fireLocation;
		if(radius - 40 < 1000)
			g.drawOval((int)(start.x - radius ), (int)(start.y - radius), radius*2, radius*2);
	}
} // end WaveBullet class
