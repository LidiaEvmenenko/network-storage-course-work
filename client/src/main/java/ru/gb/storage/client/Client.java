package ru.gb.storage.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Client extends Application {

    public static void main(String[] args) {
        Application.launch(args);
    }
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/client.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 900, 400);
        ClientController controller = fxmlLoader.getController();
        stage.setTitle("Клиент");
        stage.setOnCloseRequest(event -> controller.closeWindow());
        stage.setScene(scene);
        stage.show();

    }

}
