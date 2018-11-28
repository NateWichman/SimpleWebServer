import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import java.util.NoSuchElementException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.BufferedOutputStream;
import java.util.Date;

public class Server{

	/** The port number for this Server to listen on **/
	static final int PORT_NUMBER = 8080;
	static final File DIRECTORY = new File(".");
	static File[] listOfFiles;

	public static void main(String[] args){
		//Getting all Files in directory and adding them to listOfFiles
		File folder = new File(".");
	        listOfFiles = folder.listFiles();

		for(File x : listOfFiles){
			if(x.isFile())
				System.out.println("File: " + x.getName());
			else if(x.isDirectory())
				System.out.println("Directory: " + x.getName());
		}
		
		
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

	/** For receiving GET requests from the client **/
	private BufferedReader in = null;

	/** For sending the HTTP header string to the client **/
	private PrintWriter out = null;

	/** For sending file data to the client **/
	private	BufferedOutputStream fileOut = null;

	public clientThread(Socket clientSocket){
		this.clientSocket = clientSocket;
		
		try{
			//Initalizing the reader and the writer to the client
			in = new BufferedReader(new InputStreamReader(
					clientSocket.getInputStream()));
			out = new PrintWriter(clientSocket.getOutputStream());
			fileOut = new BufferedOutputStream(clientSocket.getOutputStream());


		}catch(IOException e){
			System.err.println("Error initializing reader and writer: "
					+ e.getMessage());
		}
	}

	@Override
	public void run(){
		System.out.println("A client has connected");
		
		//Holds received information
		String input = "";
		
		while(true){
			try{
				input = in.readLine();
				System.out.println("Received from Client: " + input);

				StringTokenizer parser = new StringTokenizer(input);
				String method = parser.nextToken().toUpperCase();
				System.out.println("Method: " + method);

				String requestedFile = parser.nextToken().toLowerCase();
				System.out.println("Requested File: " + requestedFile);

				if(method.equals("GET")){
					//This is a GET request, handling it
					File file = new File(Server.DIRECTORY, requestedFile);
					int fileLength = (int) file.length();

					if(file.exists()){
						System.out.println("Requested file exists");
						FileInputStream fileIn = new FileInputStream(file);
						byte[] fileData = new byte[fileLength];
						fileIn.read(fileData);
						fileIn.close();
						
						//Send file to client *Private Helper Method*
						sendFileToClient(fileData, file);

						
							
					}else{
						//Send 404 
						System.out.println("requested file not found");
						
						//Sending 404 to client
						sendFileNotFoundReply();
					}
				}
				else{
					/*Not a GET request, so we will ignore this as
					functionality other than GET is not supported
					by this program */
					System.out.println("Unsupported Method Received, Ignoring\n");

				}

			}catch(IOException e){
				System.err.println("Error in thread run: " + e.getMessage());
				break;
			}catch(NoSuchElementException e){
				System.out.println("Incorrect request format received: " + input);
				System.out.println("Ignoring incorrect request\n");
				continue;
			}
		}
		/*finally{
			try{
				//Destructing reader, writer, and socket to end the thread 
				in.close();
				out.close();
				clientSocket.close();
			}catch(IOException e){
				System.err.println("Error ending thread: " + e.getMessage());
			} 
		} */
	}

	private void sendFileToClient(byte[] fileData, File file){
		//Sending HTTP header
		out.println("HTTP/1.1 200 OK");
		out.println("Server: Datacom Web Server project, Nathan Wichman, Prof. Kalafut");
		out.println("Date: " + new Date());
		out.println("Content-type: ");
		out.println("Content-length: " + (int) file.length());
		out.println();
		out.flush();

		//Sending File
		try{
			fileOut.write(fileData, 0, (int) file.length());
			fileOut.flush();
		}catch(IOException e){
			System.err.println("Error sending file: " + e.getMessage());
		}

	}

	private void sendFileNotFoundReply(){
		out.println("HTTP/1.1 0 File Not Found");
		out.println("Server: Data Web Server project, Nathan Wichman, Prof. Kalafut");
		out.println("Date: " + new Date());
		out.println("Content-type: ");
		out.println("Content-length: ");
		out.println();
		out.flush();
	}

}
