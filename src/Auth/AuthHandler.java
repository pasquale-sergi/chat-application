package Auth;

import Database.DbHelper;
import User.User;

import java.sql.SQLException;

public class AuthHandler {
    private DbHelper dbHelper;


    public AuthHandler(DbHelper dbHelper){
        this.dbHelper = dbHelper;
    }

    public boolean registerUser(String username, String password){
        try {
            dbHelper.createUser(username, password);
            return true;
        }catch (SQLException e){
            e.printStackTrace();
            return false;
        }
    }



    public boolean authenticate(String username, String password){
            try {
                User user = dbHelper.getUserByUsername(username);
                if(user!=null && password.equals(user.getPassword())){
                    return true;
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return false;
    }
}
