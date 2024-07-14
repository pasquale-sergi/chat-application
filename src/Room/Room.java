package Room;

import Server.ClientHandler;
import User.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Room {
    private String name;
    private String password;
    private List<ClientHandler> clients;

    public Room(String name, String password) {
        this.name = name;
        this.password = password;
        this.clients = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public List<ClientHandler> getClients() {
        return clients;
    }

    public void addClient(ClientHandler client) {
        clients.add(client);
    }

    public void removeClient(ClientHandler client) {
        clients.remove(client);
    }
}


