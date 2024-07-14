package Server;

import Auth.AuthHandler;
import Database.DbHelper;
import Room.Room;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Server {

    private ServerSocket serverSocket; //responsible for incoming client
    private DbHelper dbHelper;
    private AuthHandler auth;

    private List<Room> rooms;


    //making the constructor
    public Server(ServerSocket serverSocket, DbHelper dbHelper, AuthHandler auth){
        this.serverSocket = serverSocket;
        this.dbHelper = dbHelper;
        this.auth = auth;
        this.rooms = new ArrayList<>();
    }

    public void startServer( ){
        try{
            while(!serverSocket.isClosed()){
                //waiting for someone to connect
                Socket socket = serverSocket.accept();
                ClientHandler client = new ClientHandler(socket, auth, dbHelper, rooms);
                System.out.println("A new client has connected!");


                //we need a new thread cause this gotta be a multithread operation otherwise can't connect multiple clients
                Thread thread = new Thread(client);
                thread.start();
            }
        }catch (IOException e){
            e.printStackTrace();
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
        try {
            DbHelper database = new DbHelper("jdbc:postgresql://localhost:5432/chat_app", "pasquale", "pasqui.123");
            AuthHandler auth = new AuthHandler(database);
            Server server = new Server(serverSocket, database, auth);
            System.out.println("Starting chat server...");
            server.startServer();
            System.out.println("Chat server started...\n");



        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to connect to the database: " + e.getMessage());
        }
    }

}
