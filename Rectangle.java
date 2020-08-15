import java.awt.Color;
import java.awt.Graphics;

/**
 * Edited by: Maria Paula Mora
 * Partner: Lizzie Hernandez
 *
 * A rectangle-shaped Shape
 * Defined by an upper-left corner (x1,y1) and a lower-right corner (x2,y2)
 * with x1<=x2 and y1<=y2
 *
 * Starter Code author: Chris Bailey-Kellogg
 */
public class Rectangle implements Shape {

    private int x1, y1, x2, y2;		// upper left and lower right
    private Color color;            // color of the shape

    /**
     * An "empty" rectangle, with only one point set so far
     */
    public Rectangle(int x1, int y1, Color color) {
        this.x1 = x1; this.x2 = x1;
        this.y1 = y1; this.y2 = y1;
        this.color = color;
    }

    /**
     * An rectangle defined by two corners
     */
    public Rectangle(int x1, int y1, int x2, int y2, Color color) {
        this.x1 = x1; this.x2 = x2;
        this.y1 = y1; this.y2 = y2;
        this.color = color;
    }

    /**
     * define the two end corners
     */
    public void setEndCorners(int x2, int y2) {
        this.x2 = x2;
        this.y2 = y2;
    }

    /**
     * Move the rectangle
     */
	@Override
	public void moveBy(int dx, int dy) {
        x1 += dx; y1 += dy;
        x2 += dx; y2 += dy;
	}

    /**
     * Change/Get the color of a shape
     */
	@Override
	public Color getColor() { return color; }

	@Override
	public void setColor(Color color) { this.color = color; }

    /**
     * Check if a point is contained by the rectangle
     */
	@Override
	public boolean contains(int x, int y) {
        // if it the x and y values are inside the square
	    return (x >= x1 && x <= x2 && y >= y1 && y <= y2);
	}

    /**
     * Draw the shape
     */
	@Override
	public void draw(Graphics g) {
	    g.setColor(color);
	    g.fillRect(x1, y1, x2-x1, y2-y1);
	}

    /**
     * Return a string for the Polyline
     */
	public String toString() {
        return "rectangle "+x1+" "+y1+" "+x2+" "+y2+" "+color.getRGB();
	}
}
