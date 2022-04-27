package ru.gb.storage.commons.message;

public class AuthMessage extends Message {
    private String login;
    private String password;
    private boolean registr; // признак вход или регистрация
    private String message = null;
    private boolean error; //признак ошибки

    public AuthMessage() {

    }

    public boolean isRegistr() {
        return registr;
    }

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    public void setRegistr(boolean registr) {
        this.registr = registr;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "AuthMessage{" +
                "login='" + login + '\'' +
                ", password='" + password + '\'' +
                ", registr=" + registr +
                ", message='" + message + '\'' +
                ", error=" + error +
                '}';
    }
}

