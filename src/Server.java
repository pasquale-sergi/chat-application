import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private ServerSocket serverSocket; //responsible for incoming clients

    //making the constructor
    public Server(ServerSocket serverSocket){
        this.serverSocket = serverSocket;
    }

    public void startServer( ){
        try{
            while(!serverSocket.isClosed()){
                //waiting for someone to connect
                Socket socket = serverSocket.accept();
                System.out.println("A new client has connected!");
                ClientHandler client = new ClientHandler(socket);

                //we need a new thread cause this gotta be a multithread operation otherwise can't connect multiple clients
                Thread thread = new Thread(client);
                thread.start();
            }
        }catch (IOException e){

        }
    }
    //if error occurs we shut down the server
    public void closeServerSocket(){
        try {
            if(serverSocket != null){
                serverSocket.close();
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(1234);
        Server server = new Server(serverSocket);
        server.startServer();
    }

}
