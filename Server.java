import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.util.StringTokenizer;


public class Server{

	/** The port number for this Server to listen on **/
	static final int PORT_NUMBER = 8080;

	public static void main(String[] args){
		try{
			//Socket to accept incomming clients
			ServerSocket serverSocket = new ServerSocket(PORT_NUMBER);

			//Infinite while loop to accept multiple clients
			while(true){
				System.out.println("Waiting for client connection...");

				//If a client connects, then a thread handles it 
				//and the loop restarts to wait for more clients.
				new clientThread(serverSocket.accept()).start();
			}
			
		}catch(IOException e){
			System.err.println("Error in main: " + e.getMessage());
		}
	}
	
}

class clientThread extends Thread{
	
	/** A socket for the client this thread is handling **/
	private Socket clientSocket;

	/** For receiving information from the client **/
	private BufferedReader in = null;

	/** For sending information to the client **/
	private PrintWriter out = null;

	public clientThread(Socket clientSocket){
		this.clientSocket = clientSocket;
		
		try{
			//Initalizing the reader and the writer to the client
			in = new BufferedReader(new InputStreamReader(
					clientSocket.getInputStream()));
			out = new PrintWriter(clientSocket.getOutputStream());

		}catch(IOException e){
			System.err.println("Error initializing reader and writer: "
					+ e.getMessage());
		}
	}

	@Override
	public void run(){
		System.out.println("A client has connected");
		
		//Holds received information
		String input;
		
		try{
			input = in.readLine();
			System.out.println("Received from Client: " + input);

			StringTokenizer parser = new StringTokenizer(input);
			String method = parser.nextToken().toUpperCase();
			System.out.println("Method: " + method);

			String requestedFile = parser.nextToken().toLowerCase();
			System.out.println("Requested File: " + requestedFile);

		}catch(IOException e){
			System.err.println("Error in thread run: " + e.getMessage());
		}finally{
			try{
				//Destructing reader, writer, and socket to end the thread 
				in.close();
				out.close();
				clientSocket.close();
			}catch(IOException e){
				System.err.println("Error ending thread: " + e.getMessage());
			}
		}
	}

}
