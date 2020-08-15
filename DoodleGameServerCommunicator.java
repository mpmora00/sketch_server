import java.io.*;
import java.net.Socket;

/** @author Maria Paula Mora
 * Partner: Lizzie Hernandez
 */
public class DoodleGameServerCommunicator extends Thread {
    private Socket sock;					// to talk with client
    private BufferedReader in;				// from client
    private PrintWriter out;				// to client
    private DoodleGameServer server;			// handling communication for
    private String playerName;
    private int score;

    public DoodleGameServerCommunicator(Socket sock, DoodleGameServer server) {
        this.sock = sock;
        this.server = server;
    }

    /**
     * Sends a message to the client
     * @param msg
     */
    public void send(String msg) {
        out.println(msg);
    }

    public int getScore(){return score;}

    public String getPlayerName(){return playerName;}
    /**
     * Keeps listening for and handling (your code) messages from the client
     */
    public void run() {
        try {

            // Communication channel
            in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            out = new PrintWriter(sock.getOutputStream(), true);

            // notify client of all current connections
            send(server.getAllPlayers());

            /*
             * first line receives the user's name
             */
            String line = in.readLine();            // read name
            playerName = line;
            server.addPlayer(this, line);                 // add to player list
            server.broadcast("addPlayer "+ line);                 // let all comms know the player has joined


            // Keep getting and handling messages from the client

            while ((line = in.readLine()) != null) {
                System.out.println("received:" + line);
                // first time someone sends start
                if((line).equals("start")){
                    // let the server know someone sent start
                    server.setGameStarted(true);
                    // once someone sends start, let everyone know the game has started, move to next phase
                    server.broadcast("instructions");
                }
                else if(line.equals("next")){
                    if(server.getRounds() == server.getNumOfPlayers()){
                        server.broadcast("finish");
                    }
                    else {
                        server.setRoles();
                        server.getSketch().restart();
                        server.broadcastRoles();
                    }
                }
                else if(line.startsWith("drawing")){
                    server.setAnswer(line.split(" ")[1].toLowerCase());
                    server.broadcast("run timer");
                }
                else if(line.startsWith("guessing")){
                    String guess = line.split(" ")[1].toLowerCase();
                    if(guess.equals(server.getAnswer())){
                        // let everyone know who won, except the winner
                        server.broadcastExcept(this, "correct " + playerName);
                        // let the winner know they won
                        send("correct");
                    }
                    else{
                        send("wrong");
                    }
                }
                else if(line.equals("time's up")){
                    // let all players know round has ended, no right guesses
                    server.broadcast("time's up");
                }
                else if(line.startsWith("score")){
                    score = Integer.parseInt(line.split(" ")[1]);
                    server.addResult(this);
                }
                /*
                 * modify the state of the server world while notifying other comms of changes
                 */
                else if(line.startsWith("add")){
                    // assign an ID to the new shape
                    int ID = server.getCurrentID();
                    // modify current state of the world based on message
                    server.getSketch().handleMessage(line + " " + ID);
                    // notify editors of the change
                    server.broadcast(line + " " + ID);
                }
                else{
                    // modify current state of the world based on message
                    server.getSketch().handleMessage(line);
                    // notify editors of the change
                    server.broadcast(line);
                }
            }


            // Clean up -- note that also remove self from server's list so it doesn't broadcast here
            server.broadcast("quit");
            server.removeAllCommunicators();
            out.close();
            in.close();
            sock.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}