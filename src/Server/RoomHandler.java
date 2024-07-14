package Server;

import Database.DbHelper;
import Room.Room;

import java.io.*;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RoomHandler{
    private BufferedWriter out;
    private BufferedReader in;
    private Socket socket;
    public List<Room> rooms;
    private DbHelper db;
    private ClientHandler clientHandler;
    private Room currentRoom;


    public RoomHandler(Socket socket, DbHelper db, ClientHandler clientHandler, List<Room> rooms) throws IOException, SQLException {
        this.socket = socket;
        this.db = db;
        this.clientHandler = clientHandler;
        this.rooms =rooms;

        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Load rooms from the database if they are not already loaded
        if (this.rooms.isEmpty()) {
            loadRoomsFromDatabase();
        }

    }
    //i need this for the issue with the chatUsername
    public String splitInput(String input){
        String[] result = input.split(":");
        return result.length > 1 ? result[1].trim() : "";
    }

    public void promptForRoomChoice() throws IOException, SQLException {
        out.write("Enter '/rooms' for Server's Rooms or '/create' for creating one.");
        out.newLine();
        out.flush();
        String serverResponseRaw = in.readLine();
        String serverResponse = splitInput(serverResponseRaw);

        if (serverResponse.equalsIgnoreCase("/rooms")) {
            getServerRooms();
            joinRoom();
        } else if (serverResponse.equalsIgnoreCase("/create")) {
            createRoom();
        }
    }


    public void joinRoom() throws IOException {
        out.write("Choose a room: ");
        out.newLine();
        out.flush();
        String choice = splitInput(in.readLine());
        boolean roomFound = false;

        for (Room room : rooms) {
            if (room.getName().equalsIgnoreCase(choice)) {
                roomFound = true;
                currentRoom = room;
                room.addClient(clientHandler);
                break;
            }
        }

        if (!roomFound) {
            out.write("Room not found. Try again.");
            out.newLine();
            out.flush();
            joinRoom();
        } else {
            out.write("Joined room: " + choice);
            out.newLine();
            out.flush();
        }
    }

    private void loadRoomsFromDatabase() {
        try {
            this.rooms.addAll(db.getRooms());
        } catch (SQLException e) {
            e.printStackTrace();
            // Handle the exception appropriately, maybe log it or send an error message to the client
        }
    }

    public void getServerRooms() throws IOException, SQLException {
        rooms = db.getRooms();
        out.write("Server's Rooms:");
        out.newLine();
        out.flush();
        for (Room room : rooms){
            out.write(room.getName());
            out.newLine();
            out.flush();
        }
    }

    public String setRoomChoice() throws IOException {
        out.write("Choose a room: ");
        out.newLine();
        out.flush();
        String choice = splitInput(in.readLine());
        boolean done = false;
        while(!done){
            for(Room room : rooms){
                if (room.getName().equalsIgnoreCase(choice)) {
                    done = true;
                    break;
                }
            }
        }

        return choice;
    }
    public Room getCurrentRoom(){
        return currentRoom;
    }

    public void createRoom(){
        try {
            out.write("Choose a name for the room: ");
            out.newLine();
            out.flush();
            String roomName = splitInput(in.readLine());

            out.write("Choose a password for the room and share it only with users you want to access this room");
            out.newLine();
            out.flush();
            String password = in.readLine();

            db.addRoom(roomName, password);
            currentRoom = new Room(roomName, password);
            rooms.add(currentRoom);
            currentRoom.addClient(clientHandler);

            out.write("Room created and joined: " + roomName);
            out.newLine();
            out.flush();

        }catch (IOException e){
            e.printStackTrace();
        }
    }


}
