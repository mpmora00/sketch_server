import java.awt.*;
import java.io.*;
import java.net.Socket;

/**
 * Edited by: Maria Paula Mora
 * Partner: Lizzie Hernandez
 *
 * Handles communication to/from the server for the editor
 *
 * Starter Code author: Chris Bailey-Kellogg and Travis Peters
 */

public class EditorCommunicator extends Thread {
	private PrintWriter out;		// to server
	private BufferedReader in;		// from server
	protected Editor editor;		// handling communication for

	/**
	 * Establishes connection and in/out pair
	 */
	public EditorCommunicator(String serverIP, Editor editor) {
		this.editor = editor;
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
			// Handle messages
			String line = in.readLine();
			editor.getSketch().parseSketch(line);

			// keep reading new updates
			while ((line = in.readLine()) != null) {

				// handle the messages given
				editor.getSketch().handleMessage(line);

				// repaint the editor to get the state of the world
				editor.repaint();
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
