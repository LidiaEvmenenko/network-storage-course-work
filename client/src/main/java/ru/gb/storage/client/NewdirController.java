package ru.gb.storage.client;
import io.netty.channel.Channel;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.stage.Stage;
import ru.gb.storage.commons.message.NewDirMessage;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;

public class NewdirController{
    @FXML
    private TextField newDirField;
    @FXML
    private Button buttOK;
    @FXML
    private Button buttCancel;
    @FXML
    private Label label;
    private Channel channel;
    private TreeItem<String> treeItem;
    private Path path;

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public void newDirName() throws IOException {
        path = Paths.get(String.valueOf(path), newDirField.getText());
        System.out.println(path);
        if (newDirField.getText().isEmpty()){
            label.setText("Введите имя директории.");
        }else {
            if (channel == null){
                Files.createDirectory(path);
            }else {
                NewDirMessage msg = new NewDirMessage();
                msg.setPath(path);
                channel.writeAndFlush(msg);
                channel.flush();
            }
            closeWindow();
        }
    }

    public void setChannelNewDir(Channel channel) {
        this.channel = channel;
    }

    public void setTreeItemNewDir(TreeItem<String> treeItem) {
        this.treeItem = treeItem;
    }

    public void closeWindow() {
        Stage stage = (Stage) buttCancel.getScene().getWindow();
        updateUI(() -> {
            stage.close();
        });
    }
    private void updateUI(Runnable r){// для того, чтобы можно было обновлять интерфейс из любого потока
        if (Platform.isFxApplicationThread()){
            r.run();
        }else {
            Platform.runLater(r);

        }
    }

}

