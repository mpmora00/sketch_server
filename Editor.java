import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.awt.*;
import java.awt.event.*;
import java.util.TreeMap;

import javax.swing.*;

/**
 * Edited by: Maria Paula Mora
 * Partner: Lizzie Hernandez
 *
 * Client-server graphical editor
 *
 * Starter Code author: Chris Bailey-Kellogg and Travis Peters
 */

public class Editor extends JFrame {
	private static String serverIP = "localhost";			// IP address of sketch server
	// "localhost" for your own machine;
	// or ask a friend for their IP address

	private static final int width = 800, height = 800;		// canvas size

	// Current settings on GUI
	public enum Mode {
		DRAW, MOVE, RECOLOR, DELETE
	}
	private Mode mode = Mode.DRAW;				// drawing/moving/recoloring/deleting objects
	private String shapeType = "ellipse";		// type of object to add
	private Color color = Color.black;			// current drawing color

	// Drawing state
	// these are remnants of my implementation; take them as possible suggestions or ignore them
	private Shape curr = null;					// current shape (if any) being drawn
	private Sketch sketch;						// holds and handles all the completed objects
	private int movingId = -1;					// current shape id (if any; else -1) being moved
	private Point drawFrom = null;				// where the drawing started
	private Point moveFrom = null;				// where object is as it's being dragged


	// Communication
	private EditorCommunicator comm;			// communication with the sketch server

	public Editor() {
		super("Graphical Editor");

		sketch = new Sketch();

		// Connect to server
		comm = new EditorCommunicator(serverIP, this);
		comm.start();

		// Helpers to create the canvas and GUI (buttons, etc.)
		JComponent canvas = setupCanvas();
		JComponent gui = setupGUI();

		// Put the buttons and canvas together into the window
		Container cp = getContentPane();
		cp.setLayout(new BorderLayout());
		cp.add(canvas, BorderLayout.CENTER);
		cp.add(gui, BorderLayout.NORTH);

		// Usual initialization
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		pack();
		setVisible(true);
	}

	/**
	 * Creates a component to draw into
	 */
	private JComponent setupCanvas() {
		JComponent canvas = new JComponent() {
			public void paintComponent(Graphics g) {
				super.paintComponent(g);
				drawSketch(g);
			}
		};

		canvas.setPreferredSize(new Dimension(width, height));

		canvas.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent event) {
				handlePress(event.getPoint());
			}

			public void mouseReleased(MouseEvent event) {
				handleRelease();
			}
		});

		canvas.addMouseMotionListener(new MouseAdapter() {
			public void mouseDragged(MouseEvent event) {
				handleDrag(event.getPoint());
			}
		});

		return canvas;
	}

	/**
	 * Creates a panel with all the buttons
	 */
	private JComponent setupGUI() {
		// Select type of shape
		String[] shapes = {"ellipse", "freehand", "rectangle", "segment"};
		JComboBox<String> shapeB = new JComboBox<String>(shapes);
		shapeB.addActionListener(e -> shapeType = (String) ((JComboBox<String>) e.getSource()).getSelectedItem());

		// Select drawing/recoloring color
		// Following Oracle example
		JButton chooseColorB = new JButton("choose color");
		JColorChooser colorChooser = new JColorChooser();
		JLabel colorL = new JLabel();
		colorL.setBackground(Color.black);
		colorL.setOpaque(true);
		colorL.setBorder(BorderFactory.createLineBorder(Color.black));
		colorL.setPreferredSize(new Dimension(25, 25));
		JDialog colorDialog = JColorChooser.createDialog(chooseColorB,
				"Pick a Color",
				true,  //modal
				colorChooser,
				e -> { color = colorChooser.getColor(); colorL.setBackground(color); },  // OK button
				null); // no CANCEL button handler
		chooseColorB.addActionListener(e -> colorDialog.setVisible(true));

		// Mode: draw, move, recolor, or delete
		JRadioButton drawB = new JRadioButton("draw");
		drawB.addActionListener(e -> mode = Mode.DRAW);
		drawB.setSelected(true);
		JRadioButton moveB = new JRadioButton("move");
		moveB.addActionListener(e -> mode = Mode.MOVE);
		JRadioButton recolorB = new JRadioButton("recolor");
		recolorB.addActionListener(e -> mode = Mode.RECOLOR);
		JRadioButton deleteB = new JRadioButton("delete");
		deleteB.addActionListener(e -> mode = Mode.DELETE);
		ButtonGroup modes = new ButtonGroup(); // make them act as radios -- only one selected
		modes.add(drawB);
		modes.add(moveB);
		modes.add(recolorB);
		modes.add(deleteB);
		JPanel modesP = new JPanel(new GridLayout(1, 0)); // group them on the GUI
		modesP.add(drawB);
		modesP.add(moveB);
		modesP.add(recolorB);
		modesP.add(deleteB);

		// Put all the stuff into a panel
		JComponent gui = new JPanel();
		gui.setLayout(new FlowLayout());
		gui.add(shapeB);
		gui.add(chooseColorB);
		gui.add(colorL);
		gui.add(modesP);
		return gui;
	}

	/**
	 * Getter for the sketch instance variable
	 */
	public Sketch getSketch() {
		return sketch;
	}

	/**
	 * Draws all the shapes in the sketch,
	 * along with the object currently being drawn in this editor (not yet part of the sketch)
	 */
	public void drawSketch(Graphics g) {

		// draw all of the shapes of the sketch
		sketch.draw(g);

		// draw the current shape
		if (curr != null) {

			// get the current shapes color
			g.setColor(curr.getColor());
			curr.draw(g);
		}
	}

	// Helpers for event handlers

	/**
	 * Helper method for press at point
	 * In drawing mode, start a new object;
	 * in moving mode, (request to) start dragging if clicked in a shape;
	 * in recoloring mode, (request to) change clicked shape's color
	 * in deleting mode, (request to) delete clicked shape
	 */
	private void handlePress(Point p) {

		// handle the draw method
		if (mode == Mode.DRAW) {

			// update the location where we should begin drawing
			drawFrom = p;

			// start a new object depending on the shape
			if (shapeType.equals("ellipse")) {
				curr = new Ellipse((int) drawFrom.getX(), (int) drawFrom.getY(), color);
			}
			else if (shapeType.equals("rectangle")) {
				curr = new Rectangle((int) drawFrom.getX(), (int) drawFrom.getY(), color);
			}
			else if (shapeType.equals("freehand")) {
				curr = new Polyline((int) drawFrom.getX(), (int) drawFrom.getY(), color);
			}
			else if (shapeType.equals("segment")) {
				curr = new Segment((int) drawFrom.getX(), (int) drawFrom.getY(), color);
			}

			// Refresh the canvas since the appearance has changed
			repaint();
		}
		// handle other methods
		else {
			// the map of all of the shapes and their id
			TreeMap<Integer, Shape> shapeMap = sketch.getMap();

			// For every shape we have in this map
			for (int id : shapeMap.descendingKeySet()) {

				// If this shape is being clicked on
				if (shapeMap.get(id).contains((int) p.getX(), (int) p.getY())) {

					// mark it as the current one being moved
					movingId = id;

					// begin the move process
					if (mode == Mode.MOVE && moveFrom == null) {
						moveFrom = p;
					}

					else if (mode == Mode.RECOLOR) {
						// let the server know we wish to recolor
						comm.send("color " + movingId + " " + color.getRGB());
					}

					else {
						// let the server know we wish to delete
						comm.send("delete " + movingId);
					}

					// once we have found the top-most item, end the loop
					break;
				}
			}
		}
	}


	/**
	 * Helper method for drag to new point
	 * In drawing mode, update the other corner of the object;
	 * in moving mode, (request to) drag the object
	 */
	private void handleDrag(Point p) {

		// if the shape exists
		if (curr != null && drawFrom != null) {

			// In drawing mode, revise the shape as it is moves
			if (mode == Mode.DRAW) {

				if (shapeType.equals("ellipse")) {
					((Ellipse) curr).setCorners((int) drawFrom.getX(), (int) drawFrom.getY(), (int) p.getX(), (int) p.getY());
				}
				else if (shapeType.equals("rectangle")) {
					((Rectangle) curr).setEndCorners((int) p.getX(), (int) p.getY());
				}

				else if (shapeType.equals("freehand")) {
					((Polyline) curr).addPoints((int) p.getX(), (int) p.getY());
				}
				else if (shapeType.equals("segment")) {
					((Segment)curr).setEnd((int) p.getX(), (int) p.getY());
				}
			}

		}

		// In moving mode, shift the object and keep track of where next step is from
		if (mode == Mode.MOVE && moveFrom != null && movingId != -1) {

			// how much should we move the item
			int dx = (int) (p.getX() - moveFrom.getX());
			int dy = (int) (p.getY() - moveFrom.getY());

			//send the message that the object has been moved
			comm.send("move " + movingId + " " + dx + " " + dy);

			// update the move from position
			moveFrom = (p);
		}

		// Refresh the canvas since the appearance has changed
		repaint();
	}

	/**
	 * Helper method for release
	 * In drawing mode, pass the add new object request on to the server;
	 * in moving mode, release it
	 */
	private void handleRelease() {

		// if the shape exists
		if (curr != null) {

			// once we finally release the mouse, let the server know we created an object
			if (mode == Mode.DRAW && drawFrom != null) {

				// send the message that the shape has been added
				comm.send("add " + curr.toString());

				// we have finished this shape so reset current
				curr = null;
			}
		}

		// for the move method, let the server know we stopped moving
		else if (mode == Mode.MOVE && movingId != -1) {

			//send the message that the object has stopped being moved
			comm.send("move " + movingId + " " + 0 + " " + 0);

			// reset the variables
			moveFrom = null;
			movingId = -1;
		}
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				new Editor();
				new Editor();
			}
		});
	}
}
