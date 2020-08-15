import java.net.*;
import java.util.*;
import java.io.*;

/** @author Maria Paula Mora
 * Partner: Lizzie Hernandez
 */
public class DoodleGameServer {
    private ServerSocket listen;						            // for accepting connections
    private ArrayList<DoodleGameServerCommunicator> comms;	        // all the connections with clients
    private Map<DoodleGameServerCommunicator, String> roles;
    private Map<DoodleGameServerCommunicator, String> names;        //
    private SketchEC sketch;								            // the state of the world
    private int currentID = 0;                                      // shape id
    private int numOfPlayers = 0;                                   // number of people who have connected
    private ArrayList<DoodleGameServerCommunicator> leaderBoard;
    private int indexOfDrawer = -1;
    private boolean gameStarted = false;
    private String answer = "";
    private int rounds = 0;

    public DoodleGameServer(ServerSocket listen) {
        this.listen = listen;
        sketch = new SketchEC();
        roles = new HashMap<>();
        names = new HashMap<>();

        comms = new ArrayList<>();
        leaderBoard = new ArrayList<>();
    }

    public SketchEC getSketch() {
        return sketch;
    }

    /**
     * @return assigned integer as an ID for a shape in the world
     */
    public int getCurrentID() {
        currentID += 1;		// all IDs are different
        return currentID;
    }

    public int getRounds(){return rounds;}

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getAnswer(){
        return answer;
    }

    public int getNumOfPlayers(){return numOfPlayers;}

    /**
     * @return a String with all player names separated by commas
     */
    public String getAllPlayers(){
        String allPlayers = "";
        for(DoodleGameServerCommunicator comm: names.keySet()){
            allPlayers += names.get(comm) + ",";
        }
        return allPlayers;
    }

    public synchronized void setRoles(){
        indexOfDrawer = (indexOfDrawer+1)%numOfPlayers;
        rounds += 1;
        roles = new HashMap<>();

        int idx = 0;
        for(DoodleGameServerCommunicator comm: comms){
            if(idx == indexOfDrawer){
                roles.put(comm, "drawer");
            }
            else{
                roles.put(comm, "guesser");
            }
            idx++;
        }
    }


    /**
     * @param gameStarted state of the game, decides whether or not to take further connections
     */
    public void setGameStarted(boolean gameStarted){
        this.gameStarted = gameStarted;
    }

    /**
     * The usual loop of accepting connections and firing off new threads to handle them
     */
    public void getConnections() throws IOException {
        System.out.println("server ready for connections");
        while (!gameStarted) {
            DoodleGameServerCommunicator comm = new DoodleGameServerCommunicator(listen.accept(), this);
            comm.setDaemon(true);
            comm.start();
            addCommunicator(comm);
        }
    }

    public synchronized void addPlayer(DoodleGameServerCommunicator comm, String name){
        names.put(comm, name);
        numOfPlayers++;
    }

    public synchronized void addResult(DoodleGameServerCommunicator comm){
        leaderBoard.add(comm);

        // once all communicators have sent their results
        if(leaderBoard.size() == comms.size()){
            sortAndSendLeaderBoard();
        }
    }

    public void sortAndSendLeaderBoard(){
        // sort the leader board
        leaderBoard.sort(new Comparator<DoodleGameServerCommunicator>() {
            @Override
            public int compare(DoodleGameServerCommunicator o1, DoodleGameServerCommunicator o2) {
                return o2.getScore()-o1.getScore();
            }
        });

        // start a string to broadcast
        String broadcast = "leaderboard,";
        for(DoodleGameServerCommunicator communicator: leaderBoard){
            broadcast += communicator.getPlayerName() +" " +communicator.getScore() + ",";
        }

        broadcast(broadcast);
    }

    /**
     * Adds the communicator to the list of current communicators
     */
    public synchronized void addCommunicator(DoodleGameServerCommunicator comm) {
        comms.add(comm);
    }

    /**
     * Removes the communicator from the list of current communicators
     */
    public synchronized void removeCommunicator(DoodleGameServerCommunicator comm) {
        comms.remove(comm);
        roles.remove(comm);
        names.remove(comm);
        numOfPlayers --;
    }

    /**
     * Sends the message from the one communicator to all (including the originator)
     */
    public synchronized void broadcast(String msg) {
        for (DoodleGameServerCommunicator comm : comms) {
            comm.send(msg);
        }
    }

    /**
     * Sends the message from the one communicator to everyone except the originator
     */
    public synchronized void broadcastExcept(DoodleGameServerCommunicator comm, String msg) {
        for (DoodleGameServerCommunicator otherComm : comms) {
            System.out.println(comm);
            if(comm != otherComm) {
                otherComm.send(msg);
            }
        }
    }

    public void removeAllCommunicators(){
        for (DoodleGameServerCommunicator comm : comms) {
            comms.remove(comm);
        }
    }

    public synchronized void broadcastRoles(){
        for(DoodleGameServerCommunicator comm : comms){
            String role = roles.get(comm);
            comm.send("role " + role);
        }
    }

    public static void main(String[] args) throws Exception {
        new DoodleGameServer(new ServerSocket(4242)).getConnections();
    }
}