import java.io.*;
import java.net.Socket;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Handles communication to/from the server for the editor
 *
 * Starter Code author: Chris Bailey-Kellogg and Travis Peters

 * @editors Maria Paula Mora
 *  Partner: Lizzie Hernandez
 */

public class DoodlePlayerCommunicator extends Thread {
    private PrintWriter out;		// to server
    private BufferedReader in;		// from server
    protected DoodlePlayer player;		// handling communication for

    /**
     * Establishes connection and in/out pair
     */
    public DoodlePlayerCommunicator(String serverIP, DoodlePlayer player) {
        this.player = player;
        System.out.println("connecting to " + serverIP + "...");
        try {
            Socket sock = new Socket(serverIP, 4242);
            out = new PrintWriter(sock.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            System.out.println("...connected");

        }
        catch (IOException e) {
            System.err.println("couldn't connect");
            System.exit(-1);
        }
    }


    /**
     * Sends message to the server
     */
    public void send(String msg) {
        out.println(msg);
    }

    /**
     * Keeps listening for and handling (your code) messages from the server
     */
    public void run() {
        try {
            // First, send name to communicator
            String hostname = "Unknown";
            // get from computer
            try {
                InetAddress addr;
                addr = InetAddress.getLocalHost();
                hostname = addr.getHostName();
                hostname = hostname.substring(0, hostname.length()-6);
            }
            catch (UnknownHostException ex) {
                System.out.println("Hostname can not be resolved");
            }
            // send name to communicator
            send(hostname);


            // Handle messages
            String line;
            // first time connected: get all player names already connected
            line = in.readLine();
            if(line!= null) {
                if(!line.equals("")) {
                    String[] names = line.split(",");
                    for (String name : names) {
                        player.addConnectedPlayer(name);
                    }
                    player.repaint();
                }


                while ((line = in.readLine()) != null) {
                    // next line receives player's role, guesser or drawer
                    // after game has started, read role (drawer, guesser)
                    if (line.startsWith("addPlayer")) {
                        player.addConnectedPlayer(line.split(" ")[1]);
                        player.repaint();
                    }
                    else if (line.startsWith("removePlayer")) {
                        player.removeConnectedPlayer(line.split(" ")[1]);
                        player.repaint();
                    }
                    else if (line.startsWith("role")) {
                        // restart settings
                        player.getSketch().restart();
                        player.setRoundWinner("");
                        player.setRole("");
                        player.restartTime();
                        // set role and start game with that role
                        String role = line.split(" ")[1];
                        player.setRole(role);
                        player.repaint();

                        player.startGame(role);
                        player.repaint();
                    }
                    else if(line.equals("instructions")){
                        player.startInstructionsScreen();
                    }
                    // when this player is allowed to guess, after answer has been sent to server
                    else if (line.equals("run timer")) {
                        player.startTimer();
                    }
                    // when this player gets an answer wrong
                    else if (line.equals("wrong")) {
                        player.setWrongGuess(true);
                        player.repaint();
                    }
                    // when this player guesses right
                    else if (line.equals("correct")) {
                        player.setRoundWinner("me");
                    }
                    // when someone other than this player guesses right
                    else if (line.startsWith("correct")) {
                        String roundWinner = line.split(" ")[1];
                        // announce player as winner to all other players
                        player.setRoundWinner(roundWinner);
                    }
                    // when the timer ends
                    else if (line.equals("time's up")) {
                        // announce player as winner to all other players
                        player.setRoundWinner("none");
                    }
                    else if(line.equals("finish")){
                        send("score " + player.getScore());
                    }
                    else if(line.startsWith("leaderboard")){
                        player.parseWinnersList(line);
                        player.startLeaderBoardScreen();
                    }
                    else if(line.equals("quit")){
                        player.quit();
                        System.exit(0);
                    }
                    // handle sketches
                    else {
                        player.getSketch().handleMessage(line);
                        player.repaint();
                    }
                }
            }

        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            System.out.println("server hung up");
        }
    }
}
