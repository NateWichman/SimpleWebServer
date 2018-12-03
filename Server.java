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
import java.util.Vector;
import java.text.SimpleDateFormat;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.logging.Logger;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;

public class Server{

	/** The port number for this Server to listen on **/
	static int PORT_NUMBER = 3000;
	static File DIRECTORY = new File(".");
	static File[] listOfFiles;
	static SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
	static Logger logger = Logger.getLogger("log");
	static FileHandler fileHandler;

	public static void main(String[] args){
		if(args.length >= 2){
			String result;
			for(String command : args){
				if(command.startsWith("-p")){
					PORT_NUMBER = Integer.parseInt(args[1]);
				}
			}
		}
		//Creating Log
		try{
			fileHandler = new FileHandler("log.txt");
			logger.addHandler(fileHandler);
			SimpleFormatter formatter = new SimpleFormatter();
			fileHandler.setFormatter(formatter);

			logger.info("Logging has begun for this session");
		}catch(Exception e){
			System.err.println("Error setting up logger: " + e.getMessage());
		}
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
				System.out.println("logging request");
				//private helper method
				log(input);

				if(input.contains("Connection: close")){
					break;
				}

				StringTokenizer parser = new StringTokenizer(input);
				String method = parser.nextToken().toUpperCase();
				System.out.println("Method: " + method);

				String requestedFile = parser.nextToken().toLowerCase();
				System.out.println("Requested File: " + requestedFile);

				if(method.equals("GET")){
					//This is a GET request, handling it
					File file = new File(Server.DIRECTORY, requestedFile);
				
					int fileLength = (int) file.length();
					
					
					/*I dont think I need this, as I 
					believe the above code does the same thing,
					but it is just a procaution...*/
					boolean existsInDirectory = false;
					for(File x : Server.listOfFiles){
						if(file.getName().equals(x.getName())){
							existsInDirectory = true;
						}
					}

					if(file.exists() && existsInDirectory){
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
					System.out.println("Unsupported Method Received, Sending 501\n");
					sendUnsupportedMethod();

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
		try{
			out.close();
			in.close();
			clientSocket.close();
			fileOut.close();
		}catch(IOException e){
			System.err.println("Error closing connection" + e.getMessage());
		}
	}

	private void sendFileToClient(byte[] fileData, File file){
		//Sending HTTP header
		String content = getContentType(file);
		out.println("HTTP/1.1 200 OK");
		out.println("Server: Datacom Web Server project, Nathan Wichman, Prof. Kalafut");
		out.println("Date: " + new Date());
		out.println("Content-type: " + content);
		out.println("Content-length: " + (int) file.length());
		out.println("Last-Modified: " + Server.sdf.format(file.lastModified()));
		out.println();
		out.flush();

		//Sending File
		try{
			fileOut.write(fileData, 0, (int) file.length());
			fileOut.flush();
		}catch(IOException e){
			System.err.println("Error sending file: " + e.getMessage());
		}

		//Logging 
		log("HTTP/1.1 200 OK");
		log("Server: Datacom Web Server project, Nathan Wichman, Prof. Kalafut");
		log("Date: " + new Date());
		log("Content-type: " + content);
		log("Content-length: " + (int) file.length());
		log("Last-Modified: " + Server.sdf.format(file.lastModified()));

	}

	private void sendFileNotFoundReply(){
		String content = "text/html";
		File file = new File(Server.DIRECTORY, "fileNotFound.html");
		byte[] fileData = new byte[(int) file.length()];
		try{
			FileInputStream fIn = new FileInputStream(file);
			fIn.read(fileData);
			fIn.close();
		}catch(IOException e){
			System.err.println("Error gathering 404 file: " + e.getMessage());
		}

		out.println("HTTP/1.1 404 File Not Found");
		out.println("Server: Data Web Server project, Nathan Wichman, Prof. Kalafut");
		out.println("Date: " + new Date());
		out.println("Content-type: " + content);
		out.println("Content-length: " + (int) file.length());
		out.println("Last-Modified: " + Server.sdf.format(file.lastModified()));
		out.println();
		out.flush();

		try{
			fileOut.write(fileData, 0, (int) file.length());
			fileOut.flush();
		}catch(IOException e){
			System.err.println("Error sending 404 file: " + e.getMessage());
		}

		//logging
		log("HTTP/1.1 404 File Not Found");
		log("Server: Data Web Server project, Nathan Wichman, Prof. Kalafut");
		log("Date: " + new Date());
		log("Content-type: " + content);
		log("Content-length: " + (int) file.length());
		log("Last-Modified: " + Server.sdf.format(file.lastModified()));


	}

	private void sendUnsupportedMethod(){
		String content = "text/html";
		File file = new File(Server.DIRECTORY, "unsupportedMethod.html");
		byte[] fileData = new byte[(int) file.length()];
		try{
			FileInputStream fIn = new FileInputStream(file);
			fIn.read(fileData);
			fIn.close();
		}catch(IOException e){
			System.err.println("Error gathering 501 file: " + e.getMessage());
		}

		out.println("HTTP/1.1 501 Not Implemented");
		out.println("Server: Data Com Web Server project, Nathan Wichman, Prof. Kalafut");
		out.println("Date: " + new Date());
		out.println("Content-type: " + content);
		out.println("Content-length: " + (int) file.length());
		out.println("Last-Modified: " + Server.sdf.format(file.lastModified()));
		out.println();
		out.flush();

		try{
			fileOut.write(fileData, 0, (int) file.length());
			fileOut.flush();
		}catch(IOException e){
			System.err.println("Error sending 501 file: " + e.getMessage());
		}

		//logging
		log("HTTP/1.1 501 Not Implemented");
		log("Server: Data Com Web Server project, Nathan Wichman, Prof. Kalafut");
		log("Date: " + new Date());
		log("Content-type: " + content);
		log("Content-length: " + (int) file.length());
		log("Last-Modified: " + Server.sdf.format(file.lastModified()));
	}

	private String getContentType(File requestedFile){
		String file = requestedFile.getName();
		if(file.endsWith(".html") || file.endsWith(".htm")){
			return "text/html";
		}else if(file.endsWith(".txt")){
			return "text/plain";
		}else if(file.endsWith(".jpg")){
			return "image/jpeg";
		}else if(file.endsWith(".pdf")){
			return "application/pdf";
		}else{
			return null;
		}
	}

	private synchronized void log(String line){
		Server.logger.info(line);
	}

}
