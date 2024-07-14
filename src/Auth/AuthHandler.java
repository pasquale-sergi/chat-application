package Auth;

import Database.DbHelper;
import User.User;

import java.sql.SQLException;

public class AuthHandler {
    private DbHelper dbHelper;


    public AuthHandler(DbHelper dbHelper){
        this.dbHelper = dbHelper;
    }

    public void registerUser(String username, String password){
        try {
            dbHelper.createUser(username, password);
        }catch (SQLException e){
            e.printStackTrace();
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
