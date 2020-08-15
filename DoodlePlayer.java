import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.awt.*;
import java.awt.event.*;
import java.util.TreeMap;

import javax.imageio.ImageIO;
import javax.swing.*;

/**
 * Edited by: Maria Paula Mora
 * Partner: Lizzie Hernandez
 *
 * Client-server graphical editor
 *
 * Starter code author: Chris Bailey-Kellogg and Travis Peters
 */

public class DoodlePlayer extends JFrame {
    private static String serverIP = "localhost";			// IP address of sketch server
    // "localhost" for your own machine;
    // or ask a friend for their IP address

    private static final int width = 800, height = 800;		// canvas size

    // Current settings on GUI
    public enum Mode {DRAW, MOVE, RECOLOR, DELETE}
    private Mode mode = Mode.DRAW;				// drawing/moving/recoloring/deleting objects
    private String shapeType = "ellipse";		// type of object to add
    private Color color = Color.black;			// current drawing color

    // Drawing state
    // these are remnants of my implementation; take them as possible suggestions or ignore them
    private Shape curr = null;					// current shape (if any) being drawn
    private SketchEC sketch;						// holds and handles all the completed objects
    private int movingId = -1;					// current shape id (if any; else -1) being moved
    private Point drawFrom = null;				// where the drawing started
    private Point moveFrom = null;				// where object is as it's being dragged


    // Communication
    private DoodlePlayerCommunicator comm;			// communication with the sketch server

    // FOR THE GAME
    private boolean drawAllowed = false;             // The person can only draw when they have already sent what they are drawing
    private boolean guessAllowed = false;            // The person can guess if the drawer has sent what they are drawing
    private int seconds = 50;                        // The amount of time the person has to draw
    private int score = 0;                           // Player's score
    protected Timer timer;                           // The timer for the game
    private JTextField queryF;					     // GUI text field to send what you will be drawing
    private JComponent guiButtons;                   // The buttons that give the ability to draw
    private JComponent guiGame;                      // The buttons that give the ability to play

    // booleans to handle different screens
    private boolean homeScreen = true;                // Show home screen
    private boolean instructionsScreen = false;       // Show instructions screen
    private boolean leaderBoardScreen = false;                // Show home screen
    private boolean drawingScreen = false;            // Show drawing screen
    private boolean guessingScreen = false;           // Show the guessing screen
    private String role = "";                         // is the player either a guesser or a drawer

    // variables to keep track of wrong guesses
    private boolean wrongGuess = false;
    private int wrongGuessTime;
    private ArrayList<String> winnersList;            // a list of the players and their scores

    private String roundWinner = "";                  // who won the round

    private ArrayList<String> connectedPlayers;        // all of the connected players

    public DoodlePlayer() {
        super("Doodle It");

        // start sketch, list of players in the game, and list of players in order of scores
        sketch = new SketchEC();
        connectedPlayers = new ArrayList<>();
        winnersList = new ArrayList<>();

        // Connect to server
        comm = new DoodlePlayerCommunicator(serverIP, this);
        comm.start();

        // Helpers to create the canvas and GUI (buttons, etc.)
        JComponent canvas = setupCanvas();
        guiButtons = setupGUI();
        guiButtons.setVisible(false);

        // Helpers to create the canvas and GUI (buttons, etc.)
        guiGame = setUpGameGUI();
        guiGame.setVisible(false);

        // Put the buttons and canvas together into the window
        Container cp = getContentPane();
        cp.setLayout(new BorderLayout());
        cp.add(canvas, BorderLayout.CENTER);
        cp.add(guiButtons, BorderLayout.NORTH);
        cp.add(guiGame, BorderLayout.SOUTH);

        // Usual initialization
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setVisible(true);
    }

    /**
     * Creates a component to draw into, includes mouse events
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
        // Put all the stuff into a panel
        JComponent gui = new JPanel();

        // drop-down menu with shapes
        String[] shapes = {"ellipse", "freehand", "rectangle", "segment", "triangle"};
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
                e -> {
                    color = colorChooser.getColor();
                    colorL.setBackground(color);
                },  // OK button
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

        // add all functions to gui
        gui.setLayout(new FlowLayout());
        gui.add(shapeB);
        gui.add(chooseColorB);
        gui.add(colorL);
        gui.add(modesP);
        gui.setVisible(false);
        return gui;
    }


    /**
     * Creates a panel for telling what the drawing is, and sending the message
     * For the players
     */
    private JComponent setUpGameGUI() {
        JComponent gui = new JPanel();

        // start the timer
        timer = new Timer(1000, new AbstractAction("update") {
            public void actionPerformed(ActionEvent e) {
                // if the player is a drawer and he has started drawing
                if (role.equals("drawer") && drawAllowed) {
                    // if the timer hasn't ended, decrease the time
                    if (seconds > 0) { seconds -= 1; }
                    else {
                        // drawing time has ended
                        drawAllowed = false;

                        // restart the time
                        seconds = 50;

                        // let the server know that the time is up
                        comm.send("time's up");
                    }
                }

                //if the player is a guesser and the drawer has decide what he is drawing
                else if(role.equals("guesser") && guessAllowed) {
                    // if the timer hasn't ended, decrease the time
                    if (seconds > 0) { seconds -= 1; }
                    // if the timer ended
                    else {
                        // restart the time
                        guessAllowed = false;
                        seconds = 50;
                    }
                }
                // repaint the screen to show the timer
                repaint();
            }
        });

        // Button to move to the next round
        JButton nextRound = new JButton("Next Round");
        nextRound.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(!roundWinner.equals("")){
                    comm.send("next");
                }
            }
        });

        // Search button fires off the game
        JButton send = new JButton("Send");
        send.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {

                // will add code here to fire off the game
                // depending on the role of the player
                if (role.equals("drawer")) {
                    comm.send("drawing " + queryF.getText());
                }

                else if(role.equals("guesser")&&guessAllowed) {
                    comm.send("guessing " + queryF.getText());
                }
            }
        });

        // text field for the search query
        queryF = new JTextField(20);
        queryF.setBounds(width, height, 5, 5);

        // Put all the stuff into a panel
        gui.setLayout(new FlowLayout());
        gui.add(queryF);
        gui.add(send);
        gui.add(nextRound);

        return gui;
    }


    /*
     * Getters, Setters, and Helper Methods
     */


    /**
     * Getter for the sketch instance variable
     */
    public SketchEC getSketch() {
        return sketch;
    }

    /**
     * To decide the role of the player (guesser, drawer)
     * @param role     the role of the player
     */
    public void setRole(String role) {
        this.role = role;
    }

    /**
     * @return player's current core
     */
    public int getScore(){return score;}

    /**
     * restart time on the clock
     */
    public void restartTime() {
        seconds = 50;
    }

    /**
     * Start the timer running
     * called when the drawer send what they are drawing
     */
    public void startTimer() {

        // if the player is a drawer the drawer
        if(role.equals("drawer")){
            // drawing is allowed
            guessAllowed = false;
            drawAllowed = true;
        }
        // if the player is a guesser
        else if(role.equals("guesser")){
            // guessing is allowed
            drawAllowed = false;
            guessAllowed = true;
        }

        timer.start();
    }

    /**
     * Start the display of instructions
     */
    public void startInstructionsScreen(){
        instructionsScreen = true;
        homeScreen = false;

        repaint();
    }

    /**
     * Change the type of screen to show -- guesser or setter
     */
    public void startGame(String type) {
        // if the game has started stop showing the home screen
        instructionsScreen = false;

        // if the player is a drawer
        if (type.equals("drawer")) {

            // show the drawing screen
            drawingScreen = true;
            guessingScreen = false;

            // show the buttons to draw
            guiButtons.setVisible(true);
        }
        // if the player is a guesser
        if (type.equals("guesser")) {

            // show only the guessing screen
            drawingScreen = false;
            guessingScreen = true;

            // do not show the buttons to draw
            guiButtons.setVisible(false);
        }

        // the search bar at the bottom
        guiGame.setVisible(true);
        repaint();
    }


    /**
     * Start the display of leader board
     */
    public void startLeaderBoardScreen(){
        leaderBoardScreen = true;
        guessingScreen = false;
        drawingScreen = false;
        guiButtons.setVisible(false);
        guiGame.setVisible(false);

        repaint();
    }

    /**
     * The person guessed wrong
     * @param wrongGuess    did the person guess wrong
     */
    public void setWrongGuess(boolean wrongGuess){
        this.wrongGuess = wrongGuess;
        wrongGuessTime = seconds;
    }

    /**
     * @param winner name of first person to guess right on this round
     */
    public void setRoundWinner(String winner) {
        // if I won the round
        if(winner.equals("me")){
            // increase my score
            score += seconds;
        }
        // reset the guess and draw booleans
        guessAllowed = false;
        drawAllowed = false;

        // I am the round winner
        this.roundWinner = winner;

        repaint();
    }


    /**
     * add this player to the connected player list
     * @param name   // the name of the player we want to add
     */
    public void addConnectedPlayer(String name){
        connectedPlayers.add(name);
    }

    /**
     * remove this player from the connected player list
     * @param name   // the name of the player we want to remove
     */
    public void removeConnectedPlayer(String name) {
        // for every player in the game
        for (int i = 0; i < connectedPlayers.size(); i ++) {

            // if the player is the same as the one we want to remove
            if (connectedPlayers.get(i).equals(name)) {

                // erase it
                connectedPlayers.remove(i);

                // stop looking for the player
                break;
            }
        }
    }

    /**
     * Get a string, convert it into an array list of the winners and their scores
     */
    public void parseWinnersList(String list) {
        String[] playerScore = list.split(",");

        for (int i = 1; i<playerScore.length; i++) {
            winnersList.add(playerScore[i]);
        }
    }

    /**
     * load the background image
     * @param filename  the name of the file
     * @return          the image
     */
    public static BufferedImage loadImage(String filename) {
        BufferedImage image = null;
        try {
            image = ImageIO.read(new File(filename));
        }
        catch (Exception e) {
            System.err.println("Couldn't load image from `"+filename+"' -- make sure the file exists in that folder");
            System.exit(-1);
        }
        return image;
    }



    /*
     *  Drawing methods based on screen type (Home Screen, Leader Board screen, Instructions Screen, etc.)
     */


    /**
     * draw the beginning homeScreen with the connections
     */
    public void drawHomeScreen(Graphics g) {

        // draw the background image
        g.drawImage(loadImage("SketchServer/doodle.jpg"), 0, 0, null);

        // draw the game title
        g.setColor(new Color(13, 76, 108));             // rectangle behind title
        g.fillRect(40, 40, width - 80, 100);

        // draw the border for the title
        g.setColor(Color.black);
        ((Graphics2D)g).setStroke(new BasicStroke(5));     // make the border thicker
        g.drawRect(40, 40, width - 80, 100); // draw the rectangle border

        // draw the game title string
        String gameTitle = "Doodle It";                           // title
        Font boldTitle = new Font("Futura", Font.BOLD, 70);
        g.setColor(Color.white);
        g.setFont(boldTitle);
        g.drawString(gameTitle, (width - g.getFontMetrics().stringWidth(gameTitle)) / 2, (140 + g.getFontMetrics().getHeight())/2);

        // draw  buttons
        Font options = new Font("Helvetica Nue", Font.BOLD, 30);
        g.setFont(options);
        int buttonWidth = 200;
        int buttonHeight = 50;

        // Start game button
        g.setColor(new Color(13, 76, 108));
        g.fillRect((width/2) - 300, height - buttonHeight * 2 + 20, buttonWidth, buttonHeight);

        // quit button
        g.setColor(Color.gray);
        g.fillRect((width/2) + 100, height - buttonHeight * 2 + 20, buttonWidth, buttonHeight);

        // write the strings for the buttons
        g.setColor(Color.white);
        g.drawString("Start Game", (width/2) - 300 + 10, height - buttonHeight * 2 + g.getFontMetrics().getHeight() + 20);
        g.drawString("Quit Game", (width/2) + 100 + 15, height - buttonHeight * 2 + g.getFontMetrics().getHeight() + 20);

        // set the font details
        Font simpleText = new Font("Helvetica Nue", Font.BOLD, 20);
        g.setFont(simpleText);

        // draw connected players
        g.setColor(Color.white);
        g.fillRect(40, 160, width - 80, 540);         // white box
        g.setColor(Color.black);
        g.drawRect(40, 160, width - 80, 540);        // box border

        // the waiting to connect box
        g.setColor(Color.gray);
        g.fillRect(60, 180,width-120, 30);
        g.setColor(Color.black);
        g.drawString("Waiting for connections... ", 70, 200);

        // draw the string and boxes for all of the players
        int yStart = 220;       // where to draw it
        int player = 1;         // player number

        // for every player connected
        for(String name: connectedPlayers){
            g.setColor(Color.lightGray);

            // draw the boxes
            g.fillRect(60, yStart,width-120, 30);
            g.setColor(Color.black);

            // draw the two strings
            g.drawString("Player "+ player, 70, yStart + 23);
            g.drawString(name, 300,yStart + 23);

            // increase the y location and the player number
            yStart = yStart + 40;
            player++;
        }
    }

    /**
     * draw the Instructions Screen with the connections
     */
    public void drawInstructionsScreen(Graphics g) {

        // draw the background image
        g.drawImage(loadImage("SketchServer/doodle.jpg"), 0, 0, null);

        // draw the game title
        g.setColor(new Color(13, 76, 108));             // rectangle behind title
        g.fillRect(40, 40, width - 80, 100);

        // draw the border for the title
        g.setColor(Color.black);
        ((Graphics2D)g).setStroke(new BasicStroke(5));     // make the border thicker
        g.drawRect(40, 40, width - 80, 100); // draw the rectangle border

        // draw the game title string
        String gameTitle = "Doodle It";                           // title
        Font boldTitle = new Font("Futura", Font.BOLD, 70);
        g.setColor(Color.white);
        g.setFont(boldTitle);
        g.drawString(gameTitle, (width - g.getFontMetrics().stringWidth(gameTitle)) / 2, (140 + g.getFontMetrics().getHeight())/2);

        // draw  buttons
        Font options = new Font("Helvetica Nue", Font.BOLD, 30);
        g.setFont(options);
        int buttonWidth = 200;
        int buttonHeight = 50;

        // Next game button (to continue to the game after reading the instructions
        g.setColor(new Color(13, 76, 108));
        g.fillRect((width/2) - 300, height - buttonHeight * 2 + 20, buttonWidth, buttonHeight);

        // quit button
        g.setColor(Color.gray);
        g.fillRect((width/2) + 100, height - buttonHeight * 2 + 20, buttonWidth, buttonHeight);

        // write the strings for the buttons
        g.setColor(Color.white);
        g.drawString("Next", (width/2) - 300 + 10, height - buttonHeight * 2 + g.getFontMetrics().getHeight() + 20);
        g.drawString("Quit Game", (width/2) + 100 + 15, height - buttonHeight * 2 + g.getFontMetrics().getHeight() + 20);

        // draw instructions
        g.setColor(Color.white);
        g.fillRect(40, 160, width - 80, 540);         // white box
        g.setColor(Color.black);
        g.drawRect(40, 160, width - 80, 540);         // box border

        // set the font details
        Font instructionsFont = new Font("Helvetica Nue", Font.BOLD, 30);
        Font textFont = new Font("Helvetica Nue", Font.BOLD, 20);

        // the string of instructions
        g.setColor(Color.black);

        String instructions = "Instructions:";
        String instructions1 = "One person will be chosen to be the drawer";
        String instructions2 = "while all others are guessers." ;

        String instructions3 = "Drawer, choose what to draw";
        String instructions4 = "before starting.";

        String instructions5 = "Write it down and start the";
        String instructions6 = "timer";

        String instructions7 = "You'll have 50 seconds to get";
        String instructions8 = "someone to guess right.";

        String instructions9 = "Guessers, first one to guess";
        String instructions10 = "right wins more points.";

        // Instructions title
        g.setFont(instructionsFont);
        g.drawString(instructions, (width - g.getFontMetrics().stringWidth(instructions)) / 2, 240);

        // drawer instructions
        g.setFont(textFont);
        g.drawString(instructions1, (width - g.getFontMetrics().stringWidth(instructions1)) / 2, 280);
        g.drawString(instructions2, (width - g.getFontMetrics().stringWidth(instructions2)) / 2, 310);

        g.drawString(instructions3, (width/2 - g.getFontMetrics().stringWidth(instructions3) - 20), 390);
        g.drawString(instructions4, (width/2 - g.getFontMetrics().stringWidth(instructions3) - 20), 420);

        g.drawString(instructions5, (width/2 - g.getFontMetrics().stringWidth(instructions3) - 20), 490);
        g.drawString(instructions6, (width/2 - g.getFontMetrics().stringWidth(instructions3) - 20), 530);

        g.drawString(instructions7, (width/2 - g.getFontMetrics().stringWidth(instructions3) - 20), 580);
        g.drawString(instructions8, (width/2 - g.getFontMetrics().stringWidth(instructions3) - 20), 620);

        // Guesser instructions
        g.drawString(instructions9, (width/2 + 20), 390);
        g.drawString(instructions10, (width/2 + 20), 420);

    }

    /**
     * Draw the leader's board screen, after the game has ended
     * Display the player's and their scores
     */
    public void drawWinnersScreen(Graphics g) {

        // draw the background image
        g.drawImage(loadImage("SketchServer/doodle.jpg"), 0, 0, null);

        // draw the game title
        g.setColor(new Color(13, 76, 108));             // rectangle behind title
        g.fillRect(40, 40, width - 80, 100);

        // draw the border for the title
        g.setColor(Color.black);
        ((Graphics2D)g).setStroke(new BasicStroke(5));     // make the border thicker
        g.drawRect(40, 40, width - 80, 100); // draw the rectangle border

        // draw the game title string
        String gameTitle = "Doodle It";                           // title
        Font boldTitle = new Font("Futura", Font.BOLD, 70);
        g.setColor(Color.white);
        g.setFont(boldTitle);
        g.drawString(gameTitle, (width - g.getFontMetrics().stringWidth(gameTitle)) / 2, (140 + g.getFontMetrics().getHeight())/2);

        // draw  buttons
        Font options = new Font("Helvetica Nue", Font.BOLD, 30);
        g.setFont(options);
        int buttonWidth = 200;
        int buttonHeight = 50;

        // quit button
        g.setColor(Color.gray);
        g.fillRect((width/2) + 100, height - buttonHeight * 2 + 20, buttonWidth, buttonHeight);

        // write the strings for the buttons
        g.setColor(Color.white);
        g.drawString("Quit Game", (width/2) + 100 + 15, height - buttonHeight * 2 + g.getFontMetrics().getHeight() + 20);


        // draw connected players
        g.setColor(Color.white);
        g.fillRect(40, 160, width - 80, 540);         // white box
        g.setColor(Color.black);
        g.drawRect(40, 160, width - 80, 540);        // box border

        // set the font details
        Font simpleText = new Font("Helvetica Nue", Font.BOLD, 30);
        g.setFont(simpleText);

        // Leader's Board box
        g.setColor(Color.gray);
        g.fillRect(60, 175,width-110, 45);
        g.setColor(Color.black);
        g.drawString("Leader's Board", width/2 - g.getFontMetrics().stringWidth("Leader's Board")/2, 210);

        // set the font details
        simpleText = new Font("Helvetica Nue", Font.BOLD, 20);
        g.setFont(simpleText);

        // draw the string and boxes for all of the players
        int yStart = 230;       // where to draw it
        int player = 1;         // player score (1 = winner)

        // for every player connected
        for(String name: winnersList){

            String[] playerScore = name.split(" ");

            g.setColor(Color.lightGray);

            // draw the boxes
            g.fillRect(60, yStart,width-120, 30);
            g.setColor(Color.black);

            // draw the strings
            g.drawString(Integer.toString(player), 70, yStart + 23);
            g.drawString(playerScore[0], (width - g.getFontMetrics().stringWidth(playerScore[0]))/2,yStart + 23);
            g.drawString(playerScore[1], width - 100, yStart + 23);

            // increase the y location and the player number
            yStart = yStart + 40;
            player++;
        }
    }


    /**
     * Draws all the shapes in the sketch,
     * along with the object currently being drawn in this editor (not yet part of the sketch)
     */
    public void drawSketch(Graphics g) {

        // if we are on the home screen
        if (homeScreen) {
            drawHomeScreen(g);
        }

        // if we are on the instructions screen
        else if(instructionsScreen){
            drawInstructionsScreen(g);
        }

        // if we are on the leader boards screen
        else if(leaderBoardScreen){
            drawWinnersScreen(g);
        }

        // if it's either the drawing screen or the guessing screen, draw the shapes of the drawing
        if (drawingScreen || guessingScreen) {
            // draw all of the shapes of the sketch
            sketch.draw(g);
            // draw the current shape
            if (curr != null) {
                // get the current shapes color
                g.setColor(curr.getColor());
                curr.draw(g);
            }
        }

        // depending on the screen, draw the timer and score
        if (drawingScreen) {
            // timer box
            g.setColor(new Color(13, 76, 108));
            g.fillRect(width/2 - 180, 50, 160, 50);
            // Player role
            g.setColor(new Color(70,130,180));
            g.fillRect(width/2 + 20, 50, 160, 50);

            // player role string
            String typePlayer = role.substring(1);
            String firstLetter = Character.toString(role.charAt(0)).toUpperCase();
            typePlayer = firstLetter + typePlayer;

            // font characteristics
            g.setFont(new Font("Futura", Font.BOLD, 20));
            g.setColor(Color.white);
            g.drawString(typePlayer, width/2 + 20 + 40, 80);
            g.drawString("Time: " + seconds, (width/2 - 180 + 40), 80);


            // announce winner
            if(!roundWinner.equals("")){
                // If time runs out before someone wins
                if(roundWinner.equals("none")){
                    g.setColor(Color.black);
                    String s = "No winners. Move to next round.";
                    g.drawString(s, (width - g.getFontMetrics().stringWidth(s)) / 2, 700);
                }
                else {
                    g.setColor(Color.black);
                    String s = roundWinner + " guessed right first.";
                    g.drawString(s, (width - g.getFontMetrics().stringWidth(s)) / 2, 700);
                }
            }
        }
        // draw the timer for the guessing screen
        else if (guessingScreen) {

            // Player role
            g.setColor(new Color(70,130,180));
            g.fillRect(width/2 - 280, 50, 160, 50);
            // timer box
            g.setColor(new Color(13, 76, 108));
            g.fillRect(width/2 - 160/2, 50, 160, 50);
            // score box
            g.fillRect(width/2 + 120, 50, 160, 50);

            // write the text
            g.setFont(new Font("Futura", Font.BOLD, 20));
            g.setColor(Color.white);


            g.drawString("Time: " + seconds, width/2 - 160/2 + 40, 80);
            g.drawString("Score: "+ score, width/2 + 120 + 40, 80);

            // player roll string
            if(!role.equals("")) {
                String typePlayer = role.substring(1);
                String firstLetter = Character.toString(role.charAt(0)).toUpperCase();
                typePlayer = firstLetter + typePlayer;
                g.drawString(typePlayer, width / 2 - 280 + 40, 80);
            }

            // announce winner
            if(!roundWinner.equals("")){
                // If I'm the winner
                if(roundWinner.equals("me")){
                    // let me know I won
                    g.setColor(Color.GREEN);
                    g.drawString("Correct.", (width-g.getFontMetrics().stringWidth("Correct."))/2, 700);
                }
                // If time runs out before someone wins
                else if(roundWinner.equals("none")){
                    g.setColor(Color.black);
                    String s = "No winners. Move to next round.";
                    g.drawString(s, (width - g.getFontMetrics().stringWidth(s)) / 2, 700);
                }
                // if I am not the winner
                else{
                    // let me know someone else won
                    g.setColor(Color.black);
                    String s = roundWinner + " guessed right first.";
                    g.drawString(s, (width-g.getFontMetrics().stringWidth(s))/2, 700);
                }
            }
            // If I guessed incorrectly
            else if(wrongGuess){

                // let me know I was wrong
                g.setColor(Color.RED);
                g.drawString("Try Again.", (width-g.getFontMetrics().stringWidth("Try Again."))/2, 700);

                // 5 seconds after they send the message or before the timer runs out
                if(wrongGuessTime == seconds + 4 || seconds == 1){
                    // stop displaying the message
                    wrongGuess = false;
                }
            }
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

        // if we are in the home screen
        if (homeScreen||instructionsScreen|| leaderBoardScreen) {
            int buttonWidth = 200;
            int buttonHeight = 50;

            // handle pressing the start and quit buttons
            if (p.getY() > (height - buttonHeight * 2 + 20 ) && p.getY() < (height - buttonHeight * 2 + buttonHeight + 20)) {

                // if the quit button was pressed
                if (p.getX() > ((width / 2.0) + 100) && p.getX() < ((width / 2.0) + 100) + buttonWidth) {
                    System.exit(0);
                }

                if (!leaderBoardScreen) {
                    // if the start button was pressed
                    if (p.getX() > ((width / 2.0) - 300) && p.getX() < ((width / 2.0) - 300) + buttonWidth) {
                        if (homeScreen) {
                            // let the server know the game has started
                            comm.send("start");
                        } else {
                            comm.send("next");
                        }
                    }
                }
            }
        }

        // if the player has decide what he is drawing
        if (drawAllowed) {

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
                else if (shapeType.equals("triangle")) {
                    curr = new Triangle((int) drawFrom.getX(), (int) drawFrom.getY(), color);
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
    }


    /**
     * Helper method for drag to new point
     * In drawing mode, update the other corner of the object;
     * in moving mode, (request to) drag the object
     */
    private void handleDrag(Point p) {

        // if the drawer has chosen what he is drawing
        if (drawAllowed) {
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
                        ((Segment) curr).setEnd((int) p.getX(), (int) p.getY());
                    }
                    else if (shapeType.equals("triangle")) {
                        ((Triangle) curr).setBoundaries((int) p.getX(), (int) p.getY());
                    }
                }

            }

            // In moving mode, shift the object and keep track of where next step is from
            if (mode == Mode.MOVE && moveFrom != null & movingId != -1) {

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
    }

    /**
     * Helper method for release
     * In drawing mode, pass the add new object request on to the server;
     * in moving mode, release it
     */
    private void handleRelease() {

        // if the drawer has chosen what he is drawing
        if (drawAllowed) {
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
    }

    /**
     * Quitting a game, all players quit when one player quits
     */
    public void quit(){
        System.exit(0);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new DoodlePlayer();
                new DoodlePlayer();
            }
        });
    }
}