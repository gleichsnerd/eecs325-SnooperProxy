import java.io.*;
import java.lang.reflect.Array;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;

public class proxyd {
	
	
	int port;
	
	public proxyd(String[] args) throws IOException {
		
		for(int i = 0; i < args.length; i++) {
			if(args[i].equals("-port") && i + 1 < args.length)
				this.port = Integer.parseInt(args[i + 1]);
		}
		
		proxyd.runServer(this.port);
		
	}

	public static ArrayList<String> readAllLines(BufferedReader reader) throws IOException {
		ArrayList<String> returnList = new ArrayList<String>();
		String line = reader.readLine();
		
		while(line != null && !(line.length() == 0)){
			returnList.add(line);
			System.out.println(line);
			line = reader.readLine();
		}
		System.out.println("Retrieved List");
		return returnList;
	}
	
	public static void runServer(int port) throws IOException {
		
        ServerSocket welcomeSocket = new ServerSocket(port);
        System.out.println("Server initialized");
        
        while(true)
        {
        	System.out.println("Loop");
           Socket connectionSocket = welcomeSocket.accept();
           BufferedReader inFromClient =
              new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
           DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
           ArrayList<String> clientList = proxyd.readAllLines(inFromClient);
           if (clientList.size() > 0) {
	           String hostname = clientList.get(1).split("Host: ")[1];
	           System.out.println(hostname);
	           
	           InetAddress target = InetAddress.getByName(hostname);
	           Socket helloSocket = new Socket(target.getHostAddress(), 80);   
	           OutputStream toTarget= new DataOutputStream(helloSocket.getOutputStream());
	           for(String line: clientList) {
	        	   toTarget.write((line + '\n').getBytes());
	           }
	           toTarget.write(("\r\n").getBytes());
	           BufferedReader inFromServer = new BufferedReader(new InputStreamReader(helloSocket.getInputStream()));
	           InputStream rawServerStream = helloSocket.getInputStream();
	           String inLine = inFromServer.readLine();
	           int inSize = 0;
	           while (inLine.length() != 0) {
	        	   System.out.println(inLine);
	        	   outToClient.write((inLine + "\r\n").getBytes());
	        	   if (inLine.contains("Content-Length")) {
	        		   inSize = Integer.parseInt((inLine.split("Content-Length: ")[1]));
	        	   }
	        	   inLine = inFromServer.readLine();
	           }
//	           toTarget.write(("\r\n").getBytes());
	           byte[] foo = new byte[inSize];
	           rawServerStream.read(foo, 0, inSize);
	           for (int i = 0; i < foo.length; i++) {
	        	   outToClient.write(foo[i]);
	           }
	           outToClient.flush();
	           helloSocket.close();
	           connectionSocket.close();
	           
	           
           }
//           Socket connection = new Socket(inFromClient.)
//           Socket 
//           String sentence;
//           String modifiedSentence;
//           BufferedReader inFromUser = new BufferedReader( new InputStreamReader(System.in));
//           Socket clientSocket = new Socket("localhost", 6789);
//           DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
//           BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
//           sentence = inFromUser.readLine();
//           outToServer.writeBytes(sentence + '\n');
//           modifiedSentence = inFromServer.readLine();
//           System.out.println("FROM SERVER: " + modifiedSentence);
//           clientSocket.close();
//           
           
//           capitalizedSentence = clientSentence.toUpperCase() + '\n';
//           outToClient.writeBytes(capitalizedSentence);
        }
  	
//		try
//	    {
//	        ServerSocket listenSocket = new ServerSocket(port);
//	        while(true)
//	        {
//	            System.out.println("wait");
//	            Socket acceptSocket = listenSocket.accept(); // blocking call until it receives a connection
//	            System.out.println("Accepted");
//	            System.out.println(acceptSocket.getOutputStream());
////	            myThread thr = new myThread(acceptsocket);
////	            thr.start();
//	        }
//	        
//	    }
//	    catch(IOException e)
//	    {
//	        System.err.println(">>>>" + e.getMessage() );
//	        e.printStackTrace();
//	    }
//		
	}
	
	
	public static void main(String[] args) throws IOException {
			proxyd proxy = new proxyd(new String[]{"-port", "50025"});
			
	}
		
	
}
