package com.nuutti.chatserver;

import java.io.File;
import java.util.ArrayList;
import java.util.Base64;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.security.SecureRandom;
import org.apache.commons.codec.digest.Crypt;

public class ChatDatabase {

    private static ChatDatabase singleton = null;
    private SecureRandom securerandom;

    public static synchronized ChatDatabase getInstance() {
        if (null == singleton) {
            singleton = new ChatDatabase();
        }
        return singleton;
    } 

    private ChatDatabase() {
    }

    private boolean exists;
    private Connection connection;
    private String sq = "jdbc:sqlite:";


    public void open(String dbName) throws SQLException {
        File file = new File(dbName);
        exists = file.exists();
        String sqpath = file.getAbsolutePath();
        sq = sq + sqpath;
        connection = DriverManager.getConnection(sq);
        if (exists == false) {
            exists = initializeDatabase();
        }
    }

    public boolean initializeDatabase() throws SQLException {
        if (null != connection) { 
            String createUsersString = "CREATE TABLE users (user TEXT PRIMARY KEY, password TEXT NOT NULL, email TEXT NOT NULL);";
            String createMessagesString = "CREATE TABLE messages (user TEXT NOT NULL, message TEXT NOT NULL, sent LONG NOT NULL);";
            Statement createStatement = connection.createStatement();
            createStatement.executeUpdate(createUsersString);
            createStatement.executeUpdate(createMessagesString);
            createStatement.close();
            return true;
        }
        return false;
    }

    public void close() throws SQLException {
        connection.close();
    }

    public boolean addUser(String username, String password, String email) throws SQLException {
        String checkString = "SELECT user FROM users;";
        Statement createStatement = connection.createStatement();
        ResultSet set = createStatement.executeQuery(checkString);
        while (set.next()) {
            if (username.equals(set.getString(1))) {
                createStatement.close();
                return false;
            }
        }

        securerandom = new SecureRandom();
        byte bytes[] = new byte[13];
        securerandom.nextBytes(bytes);
        String saltBytes = new String(Base64.getEncoder().encode(bytes));
        String salt = "$6$" + saltBytes;
        String hashedPassword = Crypt.crypt(password, salt);

        String insertUser = "insert into users " + "VALUES('" + username + "','" + hashedPassword + "','" + email + "')";
        createStatement.executeUpdate(insertUser);
        createStatement.close();
        return true;
    }
    
    public boolean checkCredentials(String username, String password) throws SQLException {
        String checkPass = "SELECT password FROM users WHERE user='" + username + "';";
        Statement createStatement = connection.createStatement();
        ResultSet set = createStatement.executeQuery(checkPass);
        String hashedPassword = set.getString(1);
        createStatement.close();

        if (hashedPassword.equals(Crypt.crypt(password, hashedPassword))) return true;
        return false;
    }

    public void addMessage(ChatMessage message) throws SQLException {
        String insertMessage = "insert into messages VALUES('" + message.user + "','" + message.message + "','" + message.dateAsInt() + "')";
        Statement createStatement = connection.createStatement();
        createStatement.executeUpdate(insertMessage);
        createStatement.close();
    }

    public ArrayList<ChatMessage> getMessages(long messagesSince) throws SQLException {
        String getmessages = "select user, message, sent from messages where sent > '" + messagesSince + "' order by sent asc;";
        Statement createStatement = connection.createStatement();
        ResultSet set = createStatement.executeQuery(getmessages);

        ArrayList<ChatMessage> messages = new ArrayList<ChatMessage>();
        while (set.next()) {
            messages.add(new ChatMessage(LocalDateTime.ofInstant(Instant.ofEpochMilli(set.getLong("sent")), ZoneOffset.UTC), set.getString("user"), set.getString("message")));
        }
        createStatement.close();
        return messages;
    }
}
