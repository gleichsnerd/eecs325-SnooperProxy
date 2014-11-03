import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;

/**
 * ProxyThread class for proxyd
 * @author Adam Gleichsner (amg188)
 * eecs325 Project 1
 */
public class ProxyThread extends Thread {

	//Class variables to hold our client and server sockets
	private Socket clientSocket;
	private Socket serverSocket;
	//Permanent values defining server socket timeout and the body read buffer size
	private static final int TIMEOUT = 10 * 1000;
	private static final int BUFFER_SIZE = 8192;

	
	/**
	 * Constructor to initialize the thread with the given client socket
	 * @param fromSocket - Client Socket
	 */
	public ProxyThread(Socket fromSocket) {
		super("SendThread");
		this.clientSocket = fromSocket;
		this.serverSocket = null;
	}
	

	/**
	 * Overridden run method, effectively parses a request in two parts:
	 * Part 1 - grab the request from the client, parse it for corrections and host name,
	 * open up a socket to the server, and send the request
	 * Part 2 - grab the return data from the server, parse the header for Content-Length
	 * (if available), parse the body via a byte buffer, and send the response to the client
	 */
	@Override
	public void run() {
		
		//Byte arrays to hold the request and response to said request
		byte[] clientRequest;
		byte[] serverResponse;
		
		//Buffered input and output streams going to and from the client and server
		BufferedInputStream inFromClient, inFromServer; 
		BufferedOutputStream outToClient, outToServer;
		
		//ByteArrayOutputStream to hold all parsed data going from and to the client
		ByteArrayOutputStream request, response;
		
		//Variables for setting up the server socket
		String hostname = "";
		InetAddress serverAddress;
		
		//Variable for parsing the response body
		int bodySize = 0;
		
		try {
			//Setup in/out to the client socket
			inFromClient = new BufferedInputStream(clientSocket.getInputStream());
			outToClient = new BufferedOutputStream(clientSocket.getOutputStream());
			request = new ByteArrayOutputStream();
			
			//Parse the header for the hostname and the entire request
			hostname = parseHeader(inFromClient, request).toString();
			clientRequest = request.toByteArray();

			//Define the address via hostname and setup the server socket
			serverAddress = InetAddress.getByName(hostname);
			this.serverSocket = new Socket(serverAddress.getHostAddress(), 80);
			this.serverSocket.setSoTimeout(TIMEOUT);
				
			//Setup in/out to server socket
			outToServer = new BufferedOutputStream(serverSocket.getOutputStream());
			inFromServer = new BufferedInputStream(serverSocket.getInputStream());
			response = new ByteArrayOutputStream();
			
			//Send the request to the server
			outToServer.write(clientRequest);
			outToServer.flush();
		
			//Parse the response header and grab the content-length field
			String clField = parseHeader(inFromServer, response).toString();
			//If there actually was a content-length field, get the number
			if (!clField.isEmpty())
				bodySize = Integer.parseInt(clField);
			
			//Parse the response body by appending it to the byte array stream
			parseBody(inFromServer, response, bodySize);
			
			//Send the server response to the client
			serverResponse = response.toByteArray();
			outToClient.write(serverResponse);
			outToClient.flush();
			
			//Close all streams and sockets for safety and good practice
			request.close();
			response.close();
			inFromClient.close();
			outToClient.close();
			inFromServer.close();
			outToServer.close();

			serverSocket.close();
			clientSocket.close();	
			
		} catch(IOException e) {
        	//For debugging purposes, uncomment this line
            //System.out.println("IOException: " + e.getMessage());
        }
		
	}
	
	//Private methods
	
	/**
	 * Helper method that reads through the HTTP header
	 * Checks for and returns host or content-length, depending on
	 * whether it's a request or a response
	 * @param in - Input stream
	 * @param out - Output stream, usually a ByteArrayOutputStream
	 * @return StringBuffer - the value for either host or content-length
	 */
	private StringBuffer parseHeader (InputStream in, OutputStream out) {
		
		//Buffer to append \r\n to the request lines, otherwise the request is invalid
		StringBuffer header = new StringBuffer("");
		//Buffer to grab the value for host or content-length
		StringBuffer returnField = new StringBuffer("");
		//Variable for grabbing each line of data in the header 
		String line = "";
		//Variable used in grabbing the host or content-length
		String[] splitLine;
		
		try {
			//While there is valid data and we aren't at the end of the header
			while ((line = readLine(in)) != null && !line.isEmpty())
			{
				//Fix the request
				header.append(line + "\r\n");
				//For debugging and seeing every header, uncomment the following line
				//System.out.println(data.toString());
				
				//If we're parsing a request, we need the hostname
				if (line.contains("Host: ")) {
					splitLine = line.split("Host: ");
					returnField.append(splitLine[1]);
				//Else, if this is a response, we want the content-length
				} else if (line.contains("Content-Length: ")) {
					splitLine = line.split("Content-Length: ");
					returnField.append(splitLine[1]);
				}
			}
			//Add the terminating line and write it to the output stream
			header.append("\r\n");
			out.write(header.toString().getBytes(), 0, header.length());
		} catch (Exception e) {
			//For debugging, uncomment the following line
			//System.err.println("Error parsing header: " + e);
		}
		
		return returnField;
	}

	/**
	 * Helper method to parse the body of a response separately from the header.
	 * @param in - input stream
	 * @param out - output stream, usually a ByteArrayOutputStream
	 * @param bodySize - Number of bytes we should be parsing, 0 if content-length field no provided
	 */
	private void parseBody (InputStream in, OutputStream out, int bodySize) {
		//Keep track of all bytes processed
		int byteCount = 0;
		//Naturally assume that we want to stay open until the server closes the connection
		boolean waitForServer = true;
			
		//If we already know the size of the body, then we don't have to wait for the server
		if (bodySize > 0)
			waitForServer = false;

		try {
			//Create a buffer to continuously read chunks from the body
			byte[] buffer = new byte[BUFFER_SIZE];
			int b = 0;
			//While we still need more data and are still receiving data
			while ((waitForServer || byteCount < bodySize) && (b = in.read(buffer)) >= 0) {
				out.write(buffer, 0 , b);
				byteCount += b;
			}
			//Push all pending data into the output stream
			out.flush();
		}  catch (Exception e)  {
			//For debugging, uncomment the following line
			//System.out.println("Error parsing body: " + e);
		}

	}

	/**
	 * Helper method that will read one line of data at a time,
	 * useful for parsing headers
	 * @param in - input stream
	 * @return String - stringified line of the header
	 */
	private String readLine (InputStream in)
	{
		//Buffer to read bytes b into
		StringBuffer line = new StringBuffer("");
		int b;
		
		try {
			//If there isn't another line, then we should return null
			//Otherwise, reset it to the beginning so we can process 
			//in the while loop
			in.mark(1);
			if (in.read() == -1)
				return null;
			else
				in.reset();
			
			//While we are still getting valid data and haven't hit an end of line character
			while ((b = in.read()) > 0 && b != '\r' && b != '\n') {
				line.append((char)b);
			}
			
			//We need to check for \r\n and wind back if it's not the end of line
			if (b == '\r') {
				in.mark(1);
				//If we aren't dealing with the expected CRLF, reset the byte we just read
				if (!((b = in.read()) == '\n'))
					in.reset();
			}
		}  catch (Exception e) {
			//For debugging, uncomment the following line
			//System.out.println("Error getting data line: " + e);
		}
		
		return line.toString();
	}
	
	
}
