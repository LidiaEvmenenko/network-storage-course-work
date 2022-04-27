package ru.gb.storage.client;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.geometry.Pos;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class Client extends Application {
    private Scene primary;
    private Scene auth;
    private Stage primaryStage;
    private Parent root1;
    private Scene scene;
    private Stage stage1;

    public static void main(String[] args) {
        Application.launch(args);
    }
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader();
        Parent root = fxmlLoader.load(getClass().getResource("/client.fxml"));
        ClientController controller = (ClientController) fxmlLoader.getController();
        stage.setTitle("Клиент");
        stage.setOnCloseRequest(event -> controller.closeWindow());
        Scene scene = new Scene(root, 900, 400);
        stage.setScene(scene);
        stage.show();

    }

}
