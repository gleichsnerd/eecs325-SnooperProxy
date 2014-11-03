import java.io.*;
import java.net.*;
import java.util.ArrayList;

/**
 * proxyd class - A lightweight snooping proxy
 * @author Adam Gleichsner
 * eecs325 Project 1
 */
public class proxyd {
	
	//ArrayList to hold all created threads
	private static ArrayList<ProxyThread> threads;
	
	/**
	 * Method to actively start a ProxyThread for every connection on the port
	 * provided.
	 * @param port - Port to grab requests from
	 * @throws IOException
	 */
	private static void runServer(int port) throws IOException {
		
		//Create the socket to always listen on
        ServerSocket welcomeSocket = new ServerSocket(port);
        System.out.println("Proxy initialized on port " + port);
        System.out.println("Press CTRL+C to quit");
        
        //Remember, you're here forever
        while(true) {
        	//For each new thread, add it to our arraylist and start it
        	//The arraylist is really only there to prevent overwriting old threads
        	//as well as setting up the groundwork for future resource management
			threads.add(new ProxyThread(welcomeSocket.accept()));
			threads.get(threads.size() - 1).run();
        }
	}
	
	/**
	 * main method to grab all input commands
	 * Defauls the proxy to port 5019 in the case of no input port
	 * Accepts one argument: -port 1234
	 * @param args - Array of all input arguments
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		int port = 5019;
		threads = new ArrayList<ProxyThread>();
		
		//If no inputs, default to 5019
		if (args.length == 0) {
			System.out.println("No input port given: defaulting to port 5019");
			proxyd.runServer(port);
		//Else, make sure that -port is given with a port number and in the correct order
		} else if (args.length == 2) {
			for(int i = 0; i < args.length; i++) {
				if(args[i].equals("-port") && i + 1 < args.length)
					port = Integer.parseInt(args[i + 1]);
				else if (args[i].equals("-port"))
					System.err.println("Argument Error: Accepted argument -port should be followed by a port number");
			}
			proxyd.runServer(port);
		//Otherwise, err out
		} else {
			System.err.println("Argument Error: Invalid number of arguments");
		}
	}
}
