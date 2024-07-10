import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class ClientHandler implements Runnable{

    private BufferedReader bufferedReader; //read the message from clients
    private BufferedWriter bufferedWriter; //send messages to clients
    private String clientUsername;
    private static ArrayList<ClientHandler> connections = new ArrayList<>();
    private Socket socket; //socket passed by the server in order to stabilish the connection
    public ClientHandler(Socket socket){
        try{
            this.socket = socket;

            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            //reading the line to take the username to assign to the client
            this.clientUsername = bufferedReader.readLine();
            //this rapresent the client handler user
            connections.add(this);

            //now we send the entered chat message
            broadcastMessage(clientUsername+" joined the chat!");

        }catch (IOException e){
            closeEverything(socket, bufferedReader, bufferedWriter);
        }

    }

    @Override
    public void run() {
        //we need different threads to handle sending and receiving messages without blocking the whole flow
        String messageFromClient;

        while(socket.isConnected()){
            try {
                //we read the message from client
                messageFromClient = bufferedReader.readLine();
                if(messageFromClient.equalsIgnoreCase("/quit")){
                    closeEverything(socket, bufferedReader, bufferedWriter);
                    break;
                }
                broadcastMessage(messageFromClient);
            }catch (IOException e ){
                closeEverything(socket, bufferedReader, bufferedWriter);
                break;
            }
        }

    }

    public void broadcastMessage(String message) {
        synchronized (connections) {
            for (ClientHandler ch : connections) {
                try {

                    if (!ch.clientUsername.equals(clientUsername)) {
                        ch.bufferedWriter.write(message);
                        //this means "im done with sending data"
                        ch.bufferedWriter.newLine();
                        //we flush
                        ch.bufferedWriter.flush();
                    }

                } catch (IOException e) {
                    closeEverything(socket, bufferedReader, bufferedWriter);
                }
            }
        }
    }

    public void removeClientHandler() {
        synchronized (connections) {
            connections.remove(this);
            System.out.println(clientUsername + " has left the chat!");
            broadcastMessage(clientUsername+" has left the chat");
        }
    }

    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter){
        removeClientHandler();
        try {
            if(bufferedReader != null){
                bufferedReader.close();
            }
            if(bufferedWriter!=null){
                bufferedWriter.close();
            }
            if(socket!=null){
                socket.close();
            }
        }catch (IOException e ){
            e.printStackTrace();
        }
    }

    //no main method cause this is called inside the server class
}
