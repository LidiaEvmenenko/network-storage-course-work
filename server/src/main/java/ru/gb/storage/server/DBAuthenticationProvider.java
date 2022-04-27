package ru.gb.storage.server;


import java.io.IOException;
import java.sql.*;

public class DBAuthenticationProvider {
    private Connection connection;
    private PreparedStatement ps;
    private Statement statement;

    private String message;


    public DBAuthenticationProvider() {
        connect();
        createTable();
    }

    public Connection getConnection() {
        return connection;
    }

    public void connect() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:dbjava.db");
            connection.setAutoCommit(true); //автоматически закрывается транзакция
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
    }

    public void createTable() {
        try {
            if (connection != null) {
                ps = connection.prepareStatement("create table if not exists users (" +
                        "id integer primary key autoincrement," +
                        "login text not null," +
                        "password text not null)");
                ps.execute();
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private synchronized void insert( String login, String password) {

        try {
            ps = connection.prepareStatement("insert into users(login,password) values (?,?)");
            ps.setString(1, login);
            ps.setString(2, password);
            ps.executeUpdate();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            try {
                connection.rollback(); // в случае ошибки произойдет откат всей транзакции
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void select() {
        try {
            System.out.println("select");
            ps = connection.prepareStatement("select * from users");
            ResultSet rs = ps.executeQuery();
            StringBuilder builder = new StringBuilder(50);
            while (rs.next()) {
                System.out.printf("%d %s %s \n", rs.getInt(1),
                        rs.getString(2), rs.getString(3));
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public void dropTable() {
        try {
            ps = connection.prepareStatement("drop table users");
            ps.execute();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public boolean getLoginByLoginAndPassword(String login, String password) {
        try {
            ps = connection.prepareStatement("select login from users where login = ? AND password = ?");
            ps.setString(1, login);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return true;
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return false;
    }

    public synchronized boolean insertUsers(String login, String password) throws SQLException { // вставка по одной записи
        try {
            ps= connection.prepareStatement("select login from users where login = ?");
            ps.setString(1, login);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                ps = connection.prepareStatement("insert into users (login,password) values(?,?);");
                ps.setString(1, login);
                ps.setString(2, password);
                ps.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}

