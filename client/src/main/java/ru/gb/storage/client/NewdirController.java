package ru.gb.storage.client;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class NewdirController{
    @FXML
    private TextField newDirField;
    @FXML
    private Button buttOK;
    @FXML
    private Button buttCancel;
    @FXML
    private Label label;
    private TreeItem<String> treeItem;
    private Path path;
    protected ClientController mainController;

    public void setMainController(ClientController mainController) {
        this.mainController = mainController;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public void newDirName() throws IOException {
        path = Paths.get(String.valueOf(path), newDirField.getText());
        if (newDirField.getText().isEmpty()){
            label.setText("Введите имя директории.");
        }else {
                Files.createDirectory(path);
            closeWindow();
        }
    }

    public void setTreeItemNewDir(TreeItem<String> treeItem) {
        this.treeItem = treeItem;
    }

    public void closeWindow() {
        Stage stage = (Stage) buttCancel.getScene().getWindow();
        updateUI(() -> {
            mainController.newListClient();
            stage.close();
        });
    }
    private void updateUI(Runnable r){
        if (Platform.isFxApplicationThread()){
            r.run();
        }else {
            Platform.runLater(r);

        }
    }

}

