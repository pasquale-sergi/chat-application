package Server;

import Auth.AuthHandler;
import Database.DbHelper;
import Room.Room;

import java.io.*;
import java.net.Socket;
import java.sql.SQLException;
import java.util.*;

public class ClientHandler implements Runnable{

    private BufferedReader bufferedReader; //read the message from clients
    private BufferedWriter bufferedWriter; //send messages to clients
    private String clientUsername;
    private static ArrayList<ClientHandler> connections = new ArrayList<>();
    private Socket socket; //socket passed by the server in order to stabilish the connection
    private DbHelper db;
    private AuthHandler auth;
    private RoomHandler roomHandler;
    private Room currentRoom;
    private List<Room> rooms;

    public ClientHandler(Socket socket, AuthHandler auth, DbHelper db, List<Room> rooms){
        this.auth = auth;
        this.db = db;
        this.rooms = rooms;
        try{
            this.socket = socket;

            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            //reading the line to take the username to assign to the client
            this.clientUsername = bufferedReader.readLine();
            Scanner scanner =new Scanner(System.in);
            while(true) {
                bufferedWriter.write("Are you registered? y/n: ");
                bufferedWriter.newLine();
                bufferedWriter.flush();
                String isRegisteredRaw = bufferedReader.readLine();
                String[] isRegistered = isRegisteredRaw.split(":");

                //authenticate client
                if (isRegistered[1].trim().equalsIgnoreCase("y")) {
                    authenticateClient();
                    break;
                } else if (isRegistered[1].trim().equalsIgnoreCase("n")) {
                    registerUser();
                    break;
                }
            }

            //this rapresent the client handler user
            connections.add(this);

            //WHERE ROOMS COME IN
            // Initialize RoomHandler
            roomHandler = new RoomHandler(socket, db, this, rooms);
            //choose the room
            setRoomChoice();
            //now we send the entered chat message
            broadcastMessage(clientUsername+" joined the chat!");
        }catch (IOException e){
            e.printStackTrace();        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }
    public void setRoomChoice() throws IOException, SQLException {
        roomHandler.promptForRoomChoice();
        currentRoom = roomHandler.getCurrentRoom();
    }

    public boolean authenticateClient() throws IOException {
        try {
            boolean done = false;
            while (!done) {
                bufferedWriter.write("Enter username: ");
                bufferedWriter.newLine();
                bufferedWriter.flush();
                String usernameLine = bufferedReader.readLine();
                String[] array = usernameLine.split(":");
                String username = array[1];

                bufferedWriter.write("Enter password: ");
                bufferedWriter.newLine();
                bufferedWriter.flush();
                String passwordLine = bufferedReader.readLine();
                String[] arr = passwordLine.split(":");
                String password = arr[1];


                if (auth.authenticate(username.trim(), password.trim())) {
                    clientUsername = username;
                    bufferedWriter.write("Authentication successful!\nYou joined the chat as " + clientUsername);
                    bufferedWriter.newLine();
                    bufferedWriter.flush();
                    done = true;
                } else {
                    bufferedWriter.write("Authentication failed.Try again");
                    bufferedWriter.newLine();
                    bufferedWriter.flush();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;

    }

    public String splitInput(String input){
        String[] result = input.split(":");
        return result[1].trim();
    }

    // Ensure to set the current room in the ClientHandler
    public void setCurrentRoom(Room room) {
        this.currentRoom = room;
    }

    public boolean registerUser(){
        try {
            bufferedWriter.write("Enter username: ");
            bufferedWriter.newLine();
            bufferedWriter.flush();
            String usernameLine = bufferedReader.readLine();
            if(usernameLine == null) return false;
            String[] array = usernameLine.split(":");
            String username = array[1];

            bufferedWriter.write("Enter password: ");
            bufferedWriter.newLine();
            bufferedWriter.flush();
            String passwordLine = bufferedReader.readLine();
            if(passwordLine==null) return false;
            String[] arr = passwordLine.split(":");
            String password = arr[1];


            auth.registerUser(username.trim(), password.trim());
            bufferedWriter.write("Registration complete!\nYou joined the chat as "+clientUsername);
            bufferedWriter.newLine();
            bufferedWriter.flush();
            return true;
    } catch (IOException e) {
            throw new RuntimeException(e);
        }}


        @Override
    public void run() {
        //we need different threads to handle sending and receiving messages without blocking the whole flow
        String messageFromClient;

        while(socket.isConnected()){
            try {
                //we read the message from client
                messageFromClient = bufferedReader.readLine();
                if(messageFromClient.equalsIgnoreCase("/quit")){
                    closeEverything();
                    break;
                }

                broadcastMessage(messageFromClient);
            }catch (IOException e ){
                e.printStackTrace();
                break;
            }
        }

    }

    public void broadcastMessage(String message) {
        synchronized (currentRoom.getClients()) {
            for(Room room : roomHandler.rooms){
                System.out.println("ROOM: "+room.getName()+"CLIENTS: "+room.getClients());
            }
                for (ClientHandler ch : currentRoom.getClients()) {
                    try {

                        if (!ch.clientUsername.equals(clientUsername)) {
                            ch.bufferedWriter.write(message);
                            //this means "im done with sending data"
                            ch.bufferedWriter.newLine();
                            //we flush
                            ch.bufferedWriter.flush();
                        }

                    } catch (IOException e) {
                        System.out.println("error broadcastin smthing");
                        e.printStackTrace();
                    }
                }

        }
    }

    public void closeEverything(){

        try {
            currentRoom.removeClient(this);
            connections.remove(this);
            broadcastMessage(clientUsername + " has left the chat");
            bufferedReader.close();
            bufferedWriter.close();
            socket.close();
        }catch (IOException e ){
            e.printStackTrace();
        }
    }

    //no main method cause this is called inside the server class
}
