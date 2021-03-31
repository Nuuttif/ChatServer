package com.nuutti.chatserver;
import java.sql.SQLException;
import com.sun.net.httpserver.*;

public class ChatAuthenticator extends BasicAuthenticator{

    public ChatAuthenticator() {
        super("chat");
    }
  
    @Override
    public boolean checkCredentials(String arg0, String arg1) {
        ChatDatabase database = ChatDatabase.getInstance();
        try {
            if (database.checkCredentials(arg0, arg1)) return true;
         return false;
        } catch (SQLException e) {
            ChatServer.log("Error in checkcredentials: " + e.getMessage());
            return false;
        }
    }
}
