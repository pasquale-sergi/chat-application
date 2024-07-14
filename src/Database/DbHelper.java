package Database;

import User.User;

import java.net.ConnectException;
import java.sql.*;

public class DbHelper {
    private Connection connection;

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
                System.out.println("User found: " + retrievedUsername);
                return new User(retrievedUsername, retrievedPassword);
            } else {
                System.out.println("User not found for username: " + username);
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
        return null;
    }
}
