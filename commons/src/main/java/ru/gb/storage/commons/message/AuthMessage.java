package ru.gb.storage.commons.message;

public class AuthMessage extends Message {
    private String login;
    private String password;
    private boolean registration; // признак вход или регистрация
    private String message = null;
    private boolean error; //признак ошибки

    public AuthMessage() {

    }

    public boolean isRegistration() {
        return registration;
    }

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    public void setRegistration(boolean registration) {
        this.registration = registration;
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
                ", registration=" + registration +
                ", message='" + message + '\'' +
                ", error=" + error +
                '}';
    }
}

