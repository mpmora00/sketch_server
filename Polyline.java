import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Edited by: Maria Paula Mora
 * Partner: Lizzie Hernandez
 *
 * A multi-segment Shape, with straight lines connecting "joint" points -- (x1,y1) to (x2,y2) to (x3,y3) ...
 *
 * Starter Code author: Chris Bailey-Kellogg
 */
public class Polyline implements Shape {

	private ArrayList<Point> joints; 		// a list of all of the points of the polyline
	private Color color;										// the color of the shape

	/**
	 * An "empty" Polyline, with no points set so far
	 */
	public Polyline() {
		joints = new ArrayList<>();
	}

	/**
	 * An "empty" Polyline, with only one point set so far
	 */
	public Polyline (int x1, int y1, Color color) {
		joints = new ArrayList<>();
		joints.add(new Point(x1, y1));
		this.color = color;
	}

	/**
	 * Move the Polyline joints
	 */
	@Override
	public void moveBy(int dx, int dy) {
		// for every joint that the polyline has
		for (Point p : joints) {
			p.x = (int) (p.getX() + dx);
			p.y = (int) (p.getY() + dy);
		}
	}

	/**
	 * add more points to the Polyline joints
	 */
	public void addPoints(int x, int y) {
		// add the new point to the list of joints
		joints.add(new Point(x, y));
	}

	/**
	 * Change/Get the color of a shape
	 */
	@Override
	public Color getColor() { return color; }

	@Override
	public void setColor(Color color) { this.color = color; }

	/**
	 * Check if a point is contained by the Polyline
	 */
	@Override
	public boolean contains(int x, int y) {

		// for every joint that the polyline has
		for (int i = 0; i < joints.size() - 1; i++) {
			// find the segments of the polyline
			Point p1 = joints.get(i);
			Point p2 = joints.get(i+1);

			// calculate the distance between the point and the segments
			double distance = Segment.pointToSegmentDistance(x, y, (int) p1.getX(), (int) p1.getY(), (int) p2.getX(), (int) p2.getY());

			// if the distance is less than 5 (it contains the point)
			// if it's 0 the precision is too small (difficult to find), so we chose less than 5
			if (distance < 5) {
				return true;
			}
		}

		// not found in any segment
		return false;
	}

	/**
	 * Draw the shape
	 */
	@Override
	public void draw(Graphics g) {
		g.setColor(color);

		// for every joint
		for (int i = 0; i < joints.size() - 1; i++) {
			Point p1 = joints.get(i);
			Point p2 = joints.get(i + 1);

			// draw a line between them
			g.drawLine((int) p1.getX(), (int) p1.getY(), (int) p2.getX(), (int) p2.getY());
		}
	}

	/**
	 * Return a string for the Polyline
	 */
	@Override
	public String toString() {
		String result = "polyline ";

		// for every joint in the Polyline
		for (Point p : joints) {
			result += (int) p.getX() + " " + (int) p.getY() + " ";
		}
		result += color.getRGB();
		return result;
	}
}
