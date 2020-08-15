import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * @Author Maria Paula Mora
 * Partner: Lizzie Hernandez
 *
 * Maintains a "master" version of the sketch
 */

public class Sketch {
    public TreeMap<Integer, Shape> shapes;     // hold all of the shapes in the word

    public Sketch() {
        shapes = new TreeMap<>();
    }

    /**
     * get the current map of the entire world
     */
    public synchronized TreeMap<Integer, Shape> getMap() {
        return shapes;
    }

    /**
     * allow the adding of new shapes into the tree map
     */
    public synchronized void addShape(Integer ID, Shape s) {
        shapes.put(ID, s);
    }

    /**
     * allow the removal of certain shapes into the tree map
     */
    public synchronized void removeShape(Integer id) {
        shapes.remove(id); }

    /**
     * allow the changing of color of certain shapes
     */
    public synchronized void setColor(Integer id, Color color) {
        if (shapes.containsKey(id)) {
            shapes.get(id).setColor(color);
        }
    }

    /**
     * allow moving of certain shapes of the tree map
     */
    public synchronized void moveShape(Integer id, int dx, int dy) {
        if (shapes.containsKey(id)) {
            shapes.get(id).moveBy(dx, dy);
        }
    }


    /**
     * draw all of the shapes of the map
     */
    public synchronized void draw(Graphics g) {

        // for every shape in the map
        for (Integer shapeID: shapes.navigableKeySet()) {

            // set the color based on the shape we are drawing
            g.setColor((shapes.get(shapeID).getColor()));

            // draw the shape
            shapes.get(shapeID).draw(g);
        }
    }

    /**
     * String the sketch
     */
    public String toString() {
        String result = "";

        // if there are no shapes in sketch
        if (shapes.size() == 0) {
            return result;
        }

        // for every shape in the sketch
        for (Integer id : shapes.descendingKeySet()) {
            // add this shape's information to the string
            result += id + " " + shapes.get(id).toString() + ",";
        }

        // remove the extra comma at the end
        return (result.substring(0, result.length()-1));
    }

    /**
     * Update a sketch based entirely from a string
     * proper format to parse "<ID> <shapeString>, <ID> <shapeString>, ..."
     * @param line      // a string description of another string
     */
    public void parseSketch(String line) {
        // if there is something on the image when the client signs in
        if (!line.equals("")) {

            // get all of the shapes
            String[] currentSketch = line.split(",");

            // for every shape
            for (String shapeString : currentSketch) {
                //get the properties of the shape
                String[] shapeData = shapeString.split(" ");

                Shape shape;

                // ID of the current shape
                int ID = Integer.parseInt(shapeData[0]);

                // if this shape is a polyline
                if (shapeData[1].equals("polyline")) {

                    // instantiate the shape to a polyline
                    shape = new Polyline();

                    // go through all of the points in the list
                    for (int i = 2; i < shapeData.length - 1; i += 2) {

                        // add it to the array list of the shape
                        ((Polyline) shape).addPoints(Integer.parseInt(shapeData[i]), Integer.parseInt(shapeData[i + 1]));
                    }

                    // set the color of the shape
                    shape.setColor(new Color(Integer.parseInt(shapeData[shapeData.length - 1])));
                }

                // if it's not a polygraph
                else {
                    // get all of the dimensions
                    int x1 = Integer.parseInt(shapeData[2]);
                    int y1 = Integer.parseInt(shapeData[3]);
                    int x2 = Integer.parseInt(shapeData[4]);
                    int y2 = Integer.parseInt(shapeData[5]);
                    Color color = new Color(Integer.parseInt(shapeData[6]));

                    // create a shape depending on the type
                    if (shapeData[1].equals("ellipse")) {
                        shape = new Ellipse(x1, y1, x2, y2, color);
                    }
                    else if (shapeData[1].equals("rectangle")) {
                        shape = new Rectangle(x1, y1, x2, y2, color);
                    }
                    else {
                        shape = new Segment(x1, y1, x2, y2, color);
                    }
                }

                // add this current shape to the editor sketch
                addShape(ID, shape);
            }
        }
    }

    /**
     * Used to interpret and handle messages
     * Updates sketch accordingly
     * strings have to be of format  "<add> <shape> <ID> " for add
     *                               "<delete> <ID>" for delete
     *                               "<color> <ID> <RGB>" for change color
     *                               "<move> <ID> <dx> <dy>" for move
     * @param line      // the message
     */
    public void handleMessage(String line) {

        String[] message = line.split(" ");
        Shape shape;
        int ID;

        // if the method is add
        if (message[0].equals("add")) {

            // if the shape is a polyline
            if (message[1].equals("polyline")) {

                // instantiate the shape
                shape = new Polyline();

                // go through all of the points in the list
                for (int i = 2; i < message.length - 2; i += 2) {

                    // add it to the array list of the shape
                    ((Polyline) shape).addPoints(Integer.parseInt(message[i]), Integer.parseInt(message[i + 1]));
                }

                // set the color of the shape
                shape.setColor(new Color(Integer.parseInt(message[message.length - 2])));

                // save the id of the shape
                ID = Integer.parseInt(message[message.length - 1]);
            }

            // if it's any other shape
            else {

                // get the dimensions
                int x1 = Integer.parseInt(message[2]);
                int y1 = Integer.parseInt(message[3]);
                int x2 = Integer.parseInt(message[4]);
                int y2 = Integer.parseInt(message[5]);
                Color color = new Color(Integer.parseInt(message[6]));
                ID = Integer.parseInt(message[7]);

                // create the shape depending on the type
                if (message[1].equals("ellipse")) {
                    shape = new Ellipse(x1, y1, x2, y2, color);
                } else if (message[1].equals("rectangle")) {
                    shape = new Rectangle(x1, y1, x2, y2, color);
                } else {
                    shape = new Segment(x1, y1, x2, y2, color);
                }
            }

            // add the shape to the map of sketch
            addShape(ID, shape);
        }
        // if the method is delete
        else if (message[0].equals("delete")) {

            // remove the shape from the sketch
            removeShape(Integer.parseInt(message[1]));
        }
        // if the method is move
        else if (message[0].equals("move")) {

            // move the shape in the sketch
            moveShape(Integer.parseInt(message[1]), Integer.parseInt(message[2]), Integer.parseInt(message[3]));
        }
        // if the recolor is move
        else if (message[0].equals("color")) {

            // recolor the shape in the sketch
            setColor(Integer.parseInt(message[1]), new Color(Integer.parseInt(message[2])));
        }
    }
}
