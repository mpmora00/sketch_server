import java.awt.*;
import java.io.*;
import java.net.Socket;

/**
 * Edited by: Maria Paula Mora
 * Partner: Lizzie Hernandez
 *
 * Handles communication between the server and one client, for SketchServer
 *
 * Starter Code author: Chris Bailey-Kellogg
 */
public class SketchServerCommunicator extends Thread {
	private Socket sock;					// to talk with client
	private BufferedReader in;				// from client
	private PrintWriter out;				// to client
	private SketchServer server;			// handling communication for

	public SketchServerCommunicator(Socket sock, SketchServer server) {
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

	/**
	 * Keeps listening for and handling (your code) messages from the client
	 */


	public void run() {
		try {
			System.out.println("someone connected");

			// Communication channel
			in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			out = new PrintWriter(sock.getOutputStream(), true);

			// Tell the client the current state of the world
			send(server.getSketch().toString());


			// Keep getting and handling messages from the client

			String line;

			while ((line = in.readLine()) != null) {

				System.out.println("recieved: " + line);

				/**
				 * modify the state of the server world while notifying other comms of changes
				 */

				// if the method is add
				if (line.startsWith("add")) {

					// get an ID
					int ID = server.getCurrID();

					// modify current state of the world based on message
					server.getSketch().handleMessage(line + " " + ID);

					// tell all of the clients that shape has been added
					server.broadcast(line + " " + ID);

				}

				else {
					// modify current state of the world based on message
					server.getSketch().handleMessage(line);

					// let every client know of this change
					server.broadcast(line);
				}
			}

			// Clean up -- note that also remove self from server's list so it doesn't broadcast here
			server.removeCommunicator(this);
			out.close();
			in.close();
			sock.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
}
