package Database;

import java.sql.*;

public class DbTestConnection {
    public static void main(String[] args) throws ClassNotFoundException, SQLException {
        String url
                = "jdbc:postgresql://localhost:5432/chat_app"; // table details
        String username = "pasquale"; // MySQL credentials
        String password = "pasqui.123";
        String query
                = "select *from users"; // query to be run
        Class.forName(
                "com.mysql.cj.jdbc.Driver"); // Driver name
        Connection con = DriverManager.getConnection(
                url, username, password);
        System.out.println(
                "Connection Established successfully");
        Statement st = con.createStatement();
        ResultSet rs
                = st.executeQuery(query); // Execute query
        rs.next();
        String name
                = rs.getString("username"); // Retrieve name from db

        System.out.println(name); // Print result on console
        st.close(); // close statement
        con.close(); // close connection
        System.out.println("Connection Closed....");
    }
}
