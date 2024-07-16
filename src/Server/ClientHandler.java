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
    private Thread handleUserChoiceThread;

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
            connections.add(this);
            // Start a separate thread to listen for messages from the client
            // Handle user input in the main thread
            handleUserChoice();
            // Listen for messages from client
            listenForMessage();
        }catch (Exception e){
            System.out.println("having issues with run");
            e.printStackTrace();
        }
    }


    //PRIVATE CHAT AND GROUP CHAT HANDLING

    private void handleUserChoice() {
        handleUserChoiceThread = Thread.currentThread();
        while(true) {
            try {
                while(!isInPrivate && !inPrivateChatRequest) {
                    bufferedWriter.write("'j' for joining a room or create one\n'p' to start a private chat with someone: ");
                    bufferedWriter.newLine();
                    bufferedWriter.flush();
                    String choice = splitInput(bufferedReader.readLine());


                    if (choice.equalsIgnoreCase("j")) {
                        // Join a room
                        roomHandler = new RoomHandler(socket, db, this, rooms);
                        roomHandler.promptForRoomChoice();
                        break;
                    } else if (choice.equalsIgnoreCase("p")) {
                        // Start a private chat
                        inPrivateChatRequest = true;
                        startPrivateChat();
                        break;
                    } else {
                        bufferedWriter.write("Invalid choice. Please choose one of the option.");
                        bufferedWriter.newLine();
                        bufferedWriter.flush();
                    }
                }
            } catch (IOException | SQLException e) {
                e.printStackTrace();
            }
        }
    }
    public void startPrivateChat() throws IOException {
        bufferedWriter.write("Enter the username of the person you want to chat with: ");
        bufferedWriter.newLine();
        bufferedWriter.flush();
        String recipientUsername = splitInput(bufferedReader.readLine());
        for(ClientHandler ch : connections){

            if(ch.getClientUsername().equalsIgnoreCase(recipientUsername)){
                ch.promptPrivateChatRequest(this);
                return;
            }
        }
        bufferedWriter.write("User " + recipientUsername + " is not available.");
        bufferedWriter.newLine();
        bufferedWriter.flush();
    }
    public void promptPrivateChatRequest(ClientHandler requester) throws IOException {
        String response = prompt("User wants to start a private chat with you. Accept? y/n: ");
        if (response != null && response.trim().equalsIgnoreCase("y")) {
            isInPrivate = true;
            requester.startPrivateChatWith(this);
            handleUserChoiceThread.interrupt();

        } else {
            requester.notifyPrivateChatDeclined();
        }
    }

    public void startPrivateChatWith(ClientHandler ch) throws IOException {
        bufferedWriter.write("Private chat started with " + ch.getClientUsername());
        bufferedWriter.newLine();
        bufferedWriter.flush();

        ch.bufferedWriter.write("Private chat started with " + this.clientUsername);
        ch.bufferedWriter.newLine();
        ch.bufferedWriter.flush();

        PrivateChatHandler privateChat = new PrivateChatHandler(this, ch);
        Thread chatThread = new Thread(privateChat);
        chatThread.start();
    }

    public void notifyPrivateChatDeclined() throws IOException {
        bufferedWriter.write("User declined your private chat request.");
        bufferedWriter.newLine();
        bufferedWriter.flush();
    }

    //UTILITIES
    public void setIsInPrivateChat(boolean isInPrivate) {
        this.isInPrivate = isInPrivate;
    }
    private String prompt(String message) throws IOException {
        bufferedWriter.write(message);
        bufferedWriter.newLine();
        bufferedWriter.flush();
        return splitInput(bufferedReader.readLine());
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
        if (isInPrivate) {
            synchronized (connections){
                for(ClientHandler ch:connections){
                    if(!ch.clientUsername.equalsIgnoreCase(clientUsername) && ch.isInPrivate){
                        ch.bufferedWriter.write(message);
                        //this means "im done with sending data"
                        ch.bufferedWriter.newLine();
                        //we flush
                        ch.bufferedWriter.flush();
                    }
                }
            }
        }else if(currentRoom!=null){
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

    public void closeEverything(){

        try {
            if(currentRoom!=null){
                currentRoom.removeClient(this);
            }
            connections.remove(this);
            broadcastMessage(clientUsername + " has left the chat");
            bufferedReader.close();
            bufferedWriter.close();
            socket.close();
        }catch (IOException e ){
            e.printStackTrace();
        }
    }

    //AUTHENTICATION METHODS
    private void authentication() {
        try {
            bufferedWriter.write("Are you registered? y/n: ");
            bufferedWriter.newLine();
            bufferedWriter.flush();
            String isRegistered = splitInput(bufferedReader.readLine());

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
            bufferedWriter.write("Enter username: ");
            bufferedWriter.newLine();
            bufferedWriter.flush();
            String username = splitInput(bufferedReader.readLine());

            bufferedWriter.write("Enter password: ");
            bufferedWriter.newLine();
            bufferedWriter.flush();
            String password = splitInput(bufferedReader.readLine());

            if (auth.authenticate(username, password)) {
                bufferedWriter.write("Authentication successful!\nYou joined the chat as " + username);
                bufferedWriter.newLine();
                bufferedWriter.flush();
                return;
            } else {
                bufferedWriter.write("Authentication failed. Try again.");
                bufferedWriter.newLine();
                bufferedWriter.flush();
            }
        }

    }




    public void registerUser() throws IOException {
        while (true) {
            bufferedWriter.write("Enter username: ");
            bufferedWriter.newLine();
            bufferedWriter.flush();
            String username = splitInput(bufferedReader.readLine());
            if(username!=null){
                bufferedWriter.write("Enter password: ");
                bufferedWriter.newLine();
                bufferedWriter.flush();
                String password = splitInput(bufferedReader.readLine());
                if(password!=null){
                    auth.registerUser(username, password);
                    bufferedWriter.write("Registration complete! You joined the chat as " + username);
                    bufferedWriter.newLine();
                    bufferedWriter.flush();
                    return;
                }else {
                    bufferedWriter.write("Invalid password.Try again.\n");
                    bufferedWriter.newLine();
                    bufferedWriter.flush();
                }

            }else{
                bufferedWriter.write("Invalid username.Try again.\n");
                bufferedWriter.newLine();
                bufferedWriter.flush();
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
