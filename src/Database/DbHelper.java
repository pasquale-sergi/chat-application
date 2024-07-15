package Database;

import Room.Room;
import User.User;
import com.mysql.cj.x.protobuf.MysqlxPrepare;

import java.net.ConnectException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DbHelper {
    private Connection connection;
    private Room rooms;

    public DbHelper(String dbUrl, String dbUser, String dbPassword) throws SQLException{
        try {
            connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
            System.out.println("connected");
        }catch (SQLException e){
            System.out.println("error with db connection");
        }
    }

    public void createUser(String username, String passwordHash) throws SQLException {
        String query = "insert into users(username, password) values(?,?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, username);
            statement.setString(2, passwordHash);
            statement.executeUpdate();
        }
    }

    public User getUserByUsername(String username) throws SQLException {
        String query = "select username,password from users where username = ?";
        try(PreparedStatement statement = connection.prepareStatement(query)){
            statement.setString(1,username);
            ResultSet res = statement.executeQuery();
            if(res.next()){
                String retrievedUsername = res.getString("username");
                String retrievedPassword = res.getString("password");
                return new User(retrievedUsername, retrievedPassword);
            } else {
                System.out.println("User not found for username: " + username);
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
        return null;
    }

    public boolean addRoom(String name, String password){
        String query = "insert into rooms(name, password) values(?,?)";
        try(PreparedStatement st = connection.prepareStatement(query)){
            st.setString(1,name);
            st.setString(2,password);
            st.executeUpdate();
            return true;
        }catch (SQLException e){
            e.printStackTrace();
            return false;
        }
    }

    public List<Room> getRooms() throws SQLException {
        String query = "SELECT * FROM rooms";
        List<Room> rooms = new ArrayList<>();
        try (Statement statement = connection.createStatement()) {
            ResultSet res = statement.executeQuery(query);
            while (res.next()) {
                rooms.add(new Room(res.getString("name"), res.getString("password")));
            }
        }
        return rooms;
    }

    public Room getRoomByName(String name){
        String query = "select name, password from rooms where name = ?";
        try(PreparedStatement st= connection.prepareStatement(query)){
            st.setString(1, name);
            ResultSet res = st.executeQuery();
            if(res.next()){
                String retrievedName = res.getString("name");
                String retrievedPassword = res.getString("password");
                return new Room(retrievedName, retrievedPassword);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

}
