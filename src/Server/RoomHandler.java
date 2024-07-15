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


    public RoomHandler(Socket socket, DbHelper db, ClientHandler clientHandler, List<Room> rooms) throws IOException, SQLException {
        this.socket = socket;
        this.db = db;
        this.clientHandler = clientHandler;
        this.rooms =rooms;

        this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

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
        while(true){
            out.write("Enter '/rooms' for Server's Rooms or '/create' for creating one.");
            out.newLine();
            out.flush();
            String serverResponseRaw = in.readLine();
            String serverResponse = splitInput(serverResponseRaw);

            if (serverResponse.equalsIgnoreCase("/rooms")) {
                getServerRooms();
                joinRoom();
                break;
            } else if (serverResponse.equalsIgnoreCase("/create")) {
                createRoom();
                break;
            }else {
                out.write("Invalid input.Try again please.\n");
            }

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
                Room roomObj = db.getRoomByName(choice);
                //check auth for room
                while(true) {
                    out.write("Insert the password: ('password' by default for Room 1 and 2)");
                    out.newLine();
                    out.flush();
                    String password = splitInput(in.readLine());
                    System.out.println(password);
                    if (roomObj.getPassword().equalsIgnoreCase(password)) {
                        out.write("Password correct.");
                        out.newLine();
                        out.flush();
                        room.addClient(clientHandler);
                        clientHandler.setCurrentRoom(room);
                        break;
                    }else {
                        out.write("Password incorrect.Try again.");
                    }
                }
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
        out.write("Server's Rooms:");
        out.newLine();
        out.flush();
        for (Room room : rooms){
            out.write(room.getName());
            out.newLine();
            out.flush();
        }
    }

    public void createRoom() throws IOException {
        try {
            out.write("Choose a name for the room: ");
            out.newLine();
            out.flush();
            String roomName = splitInput(in.readLine());

            out.write("Choose a password for the room and share it only with users you want to access this room");
            out.newLine();
            out.flush();
            String password =splitInput(in.readLine());


            db.addRoom(roomName, password);
            Room newRoom = new Room(roomName, password);
            rooms.add(newRoom);
            newRoom.addClient(clientHandler);
            clientHandler.setCurrentRoom(newRoom);
            out.write("Room created and joined: " + roomName);
            out.newLine();
            out.flush();

        }catch (IOException e){
            e.printStackTrace();
            out.write("Failed to create the room. Try again.");
            out.newLine();
            out.flush();
        }
    }


}
