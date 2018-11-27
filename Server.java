import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;

public class Server{

	static final int PORT_NUMBER = 8080;

	public static void main(String[] args){
		try{
			ServerSocket serverSocket = new ServerSocket(PORT_NUMBER);
			
			while(true){
				System.out.println("Waiting for client connection...");

				new clientThread(serverSocket.accept()).start();
			}
			
			
		}catch(IOException e){
			System.err.println("Error in main: " + e.getMessage());
		}
	}
	
}

class clientThread extends Thread{
	private Socket clientSocket;

	public clientThread(Socket clientSocket){
		this.clientSocket = clientSocket;
	}

	@Override
	public void run(){
		System.out.println("A client has connected");
	}

}
