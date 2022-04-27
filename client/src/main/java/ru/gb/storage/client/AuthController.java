package ru.gb.storage.client;
import io.netty.channel.Channel;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ru.gb.storage.client.message.AuthMessage;
import ru.gb.storage.client.message.Message;

public class AuthController {
    @FXML
    private Button buttCancel;
    @FXML
    private TextField loginField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label labelAuth;
    private Channel channel;

    public void receiveAuthMessage(Message msg) {
        AuthMessage message = (AuthMessage) msg;
        if (message.isRegistr()){
            updateUI(() -> {
                labelAuth.setText(message.getMessage());
            });

        }else {
            if (message.isError()){
                updateUI(() -> {
                    labelAuth.setText(message.getMessage());
                });
            }else {
                closeWindow();
            }
        }
    }

    public void closeWindow() {
        Stage stage = (Stage) buttCancel.getScene().getWindow();
        updateUI(() -> {
            stage.close();
        });
    }
    public void setChannelAuth(Channel channel){
        this.channel = channel;
    }

    public void registerClient() {
        labelAuth.setText("");
        if (loginField.getText().isEmpty() || passwordField.getText().isEmpty()){
            labelAuth.setText("Введите логин/пароль.");
        }else {
            AuthMessage authMessage = new AuthMessage();
            authMessage.setLogin(loginField.getText());
            authMessage.setPassword(passwordField.getText());
            authMessage.setRegistr(true);
            channel.writeAndFlush(authMessage);
            channel.flush();
        }
    }

    public void authClient() {
        labelAuth.setText("");
        if (loginField.getText().isEmpty() || passwordField.getText().isEmpty()){
            labelAuth.setText("Введите логин/пароль.");
        }else {
            AuthMessage authMessage = new AuthMessage();
            authMessage.setLogin(loginField.getText());
            authMessage.setPassword(passwordField.getText());
            authMessage.setRegistr(false);
            channel.writeAndFlush(authMessage);
            channel.flush();
        }
    }
    private void updateUI(Runnable r){// для того, чтобы можно было обновлять интерфейс из любого потока
        if (Platform.isFxApplicationThread()){
            r.run();
        }else {
            Platform.runLater(r);
        }
    }
}
