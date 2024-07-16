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
    private boolean inRoomMode = false;
    private volatile boolean userHandled = false;


    private boolean isInPrivate = false;
    private volatile boolean inPrivateChatRequest = false;



    public ClientHandler(Socket socket, AuthHandler auth, DbHelper db, List<Room> rooms) {
        this.auth = auth;
        this.db = db;
        this.rooms = rooms;
        try {
            this.socket = socket;

            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            //reading the line to take the username to assign to the client
            this.clientUsername = bufferedReader.readLine();



            //now we send the entered chat message
            //broadcastMessage(clientUsername + " joined the chat!");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void run() {

        try {
            authentication();
            handleUserChoice();
            listenForMessage();

        }catch (Exception e){
            System.out.println("having issues with run");
            e.printStackTrace();
        }
    }


    //PRIVATE CHAT AND GROUP CHAT HANDLING

    private void handleUserChoice() {
        while(true) {
            try {
                while(!isInPrivate && !inPrivateChatRequest &&!inRoomMode) {

                    String choice = prompt("'j' for joining a room or create one\n'p' to start a private chat with someone: ");


                    if (choice.equalsIgnoreCase("j")) {
                        inRoomMode = true;
                        joinRoom();
                        break;
                         // User has been handled

                    } else if (choice.equalsIgnoreCase("p")) {
                        // Start a private chat
                        inPrivateChatRequest = true;
                        startPrivateChat();
                        break;

                    } else {
                        write("Invalid choice. Please choose one of the option.");

                    }
                }
            } catch (IOException | SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void joinRoom() throws IOException, SQLException {
        roomHandler = new RoomHandler(socket, db, this, rooms);
        roomHandler.promptForRoomChoice();
        listenForMessage();
        //Thread roomHandlerThread = new Thread(roomHandler);
        //roomHandlerThread.start();
    }
    public void startPrivateChat() throws IOException {

        String recipientUsername =prompt("Enter the username of the person you want to chat with: ");

        for(ClientHandler ch : connections){

            if(ch.getClientUsername().equalsIgnoreCase(recipientUsername)){
                ch.promptPrivateChatRequest(this);
                return;
            }
        }
        write("User " + recipientUsername + " is not available.");

    }
    public void promptPrivateChatRequest(ClientHandler requester) throws IOException {
        String response = prompt(clientUsername+" wants to start a private chat with you. Accept? y/n: ");
        if (response != null && response.trim().equalsIgnoreCase("y")) {
            isInPrivate = true;
            requester.startPrivateChatWith(this);

        } else {
            requester.notifyPrivateChatDeclined();
        }
    }

    public void startPrivateChatWith(ClientHandler ch) throws IOException {
        write("Private chat started with " + ch.getClientUsername());

        ch.write("Private chat started with " + this.clientUsername);

        PrivateChatHandler privateChat = new PrivateChatHandler(this, ch);
        Thread chatThread = new Thread(privateChat);
        chatThread.start();
    }

    public void notifyPrivateChatDeclined() throws IOException {
        write("User declined your private chat request.");
    }

    //UTILITIES
    public void setIsInPrivateChat(boolean isInPrivate) {
        this.isInPrivate = isInPrivate;
    }
    //write and reading buffer
    private String prompt(String message) throws IOException {
        bufferedWriter.write(message);
        bufferedWriter.newLine();
        bufferedWriter.flush();
        return splitInput(bufferedReader.readLine());
    }
    //writing buffer
    private void write(String message) throws IOException{
        bufferedWriter.write(message);
        bufferedWriter.newLine();
        bufferedWriter.flush();
    }

    public String splitInput(String input){
        String[] result = input.split(":");
        return result[1].trim();
    }

    //SERVER COMMUNICATION METHODS
    public void listenForMessage(){
        String messageFromClient;

        while(socket.isConnected()){
            try {
                //we read the message from client
                messageFromClient = bufferedReader.readLine();
                if(messageFromClient.equalsIgnoreCase("/quit")) {
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

    public void broadcastMessage(String message) throws IOException {
        System.out.println("here in the broadcast");

        if (isInPrivate) {
            synchronized (connections) {
                for (ClientHandler ch : connections) {
                    if (!ch.clientUsername.equalsIgnoreCase(clientUsername) && ch.isInPrivate) {
                        ch.bufferedWriter.write(message);
                        //this means "im done with sending data"
                        ch.bufferedWriter.newLine();
                        //we flush
                        ch.bufferedWriter.flush();
                    }
                }
            }
        } else {
            synchronized (currentRoom.getClients()) {
                for (Room room : roomHandler.rooms) {
                    System.out.println("ROOM: " + room.getName() + "CLIENTS: " + room.getClients());
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
    }
        public void closeEverything () {

            try {
                if (currentRoom != null) {
                    currentRoom.removeClient(this);
                }
                connections.remove(this);
                broadcastMessage(clientUsername + " has left the chat");
                bufferedReader.close();
                bufferedWriter.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    //AUTHENTICATION METHODS
    private void authentication() {
        try {
            String isRegistered = prompt("Are you registered? y/n: ");

            if (isRegistered != null && isRegistered.trim().equalsIgnoreCase("y")) {
                authenticateClient();
            } else if (isRegistered != null && isRegistered.trim().equalsIgnoreCase("n")) {
                registerUser();
            } else {
                throw new IOException("Invalid registration input");
            }

        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public void authenticateClient() throws IOException {
        while (true) {
            String username = prompt("Enter username: ");

            String password = prompt("Enter password: ");

            if (auth.authenticate(username, password)) {
                write("Authentication successful!\nYou joined the chat as " + username);
                connections.add(this);
                return;
            } else {
                write("Authentication failed. Try again.");

            }
        }

    }




    public void registerUser() throws IOException {
        while (true) {
            String username = prompt("Enter username: ");

            if(username!=null){
                String password = prompt("Enter password: ");
                if(password!=null){
                    auth.registerUser(username, password);
                    write("Registration complete! You joined the chat as " + username);

                    return;
                }else {
                    write("Invalid password.Try again.\n");

                }

            }else{
               write("Invalid username.Try again.\n");

            }


        }
    }
    //GETTERS AND SETTERS
    public BufferedReader getBufferedReader() {
        return bufferedReader;
    }

    public BufferedWriter getBufferedWriter() {
        return bufferedWriter;
    }

    public String getClientUsername() {
        return clientUsername;
    }

    public void setCurrentRoom(Room room) {
        this.currentRoom = room;
    }

    public Room getCurrentRoom() {
        return currentRoom;
    }

    //no main method cause this is called inside the server class
}
