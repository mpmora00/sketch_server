import java.awt.*;

/**
 * @author Maria Paula Mora
 * Partner: Lizzie Hernandez
 *
 * An Triangle Shape
 */
public class Triangle implements Shape {
    private Polygon triangle;           // the shape to be drawn

    private int[] y = new int[3];       // an array of all of the x values
    private int[] x = new int[3];       // an array of all of the y values

    private int h;                      // the height of the triangle
    private Color color;                // the color of the triangle

    /**
     * An "empty" triangle, with only one point set so far
     */
    public Triangle(int x1, int y1, Color color) {
        // all of the x's will be equal to this value
        x[0] = x1;
        x[1] = x1;
        x[2] = x1;

        // all of these y's will be equal to this value
        y[0] = y1;
        y[1] = y1;
        y[2] = y1;

        this.color = color;
    }

    public Triangle(int x1, int x2, int y, int h,  Color color) {
        // all of the x's will be equal to this value
        this.x[0] = x1;
        this.x[1] = x2;
        this.x[2] = (x1 + ((x2 - x1)/2));

        // all of these y's will be equal to this value
        this.y[0] = y;
        this.y[1] = y;
        this.y[2] = y + h;

        this.color = color;
    }

    /**
     * Add the last boundary of the shape
     */
    public void setBoundaries(int x2, int y2) {

        // add the second x point
        x[1] = x2;

        // edit the triangle height
        this.h = y2 - y[0];

        // add the third point based on where the mouse is right now
        x[2] = (x[0] + (x[1] - x[0]) / 2);
        y[2] = (y[0] + h);
    }

    /**
     * Check if a point is contained by the Triangle
     */
    @Override
    public boolean contains(int x, int y) {

        // create a polygon with this values
        triangle = new Polygon(this.x, this.y, 3);

        // return if the shape contains these values
        return triangle.contains(new Point(x, y));
    }

    /**
     * Move the points of the triangle
     */
    @Override
    public void moveBy(int dx, int dy) {
        // edit all of the points based on where the mouse is
        x[0] += dx; y[0] += dy;
        x[1] += dx; y[1] += dy;
        x[2] += dx; y[2] += dy;
    }

    /**
     * Change/Get the color of a shape
     */
    @Override
    public Color getColor() {
        return color;
    }
    @Override
    public void setColor(Color color) {

        this.color = color;
    }

    /**
     * Draw the shape
     */
    @Override
    public void draw(Graphics g) {
        g.setColor(color);
        triangle = new Polygon(this.x, this.y, 3);
        g.fillPolygon(triangle);
    }

    /**
     * Return a string for the Polyline
     */
    @Override
    public String toString() {
        return "triangle "+x[0]+" "+x[1]+" " +y[0] +" "+h+" "+color.getRGB();
    }
}
