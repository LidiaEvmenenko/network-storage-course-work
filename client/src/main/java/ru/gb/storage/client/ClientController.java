package ru.gb.storage.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import ru.gb.storage.commons.handler.JsonDecoder;
import ru.gb.storage.commons.handler.JsonEncoder;
import ru.gb.storage.commons.message.*;
import java.io.*;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ClientController extends Window implements Initializable {
    @FXML
    private TextField textField;
    @FXML
    private Button buttClientList;
    @FXML
    private Button buttClientSend;
    @FXML
    private Button buttClientMd;
    @FXML
    private Button buttServerList;
    @FXML
    private Button buttServerReceive;
    @FXML
    private Button buttServerDelete;
    @FXML
    private Label labelClient;
    @FXML
    private Label labelServer;
    @FXML
    private TreeView<String> treeViewClient;
    @FXML
    private Button buttClientAdd;
    @FXML
    private Button buttClientDelete;
    @FXML
    private ListView listViewServer;
    private RandomAccessFile accessFile;
    private String fileChoiceServer;
    private StringBuilder pathBuilder = null;
    private ObservableList<String> langs;
    private boolean buttOkDelete = false;
    private NioEventLoopGroup group;
    private Channel channel = null;
    private AuthController authControl = null;
    private Path absPath;
    private Path pathChoiceClient;
    private String fileChoiceClient;
    private Path pathReceiveFileServer;

    public void nettyConnect() throws IOException {
        group = new NioEventLoopGroup(1);
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        protected void initChannel(NioSocketChannel ch) {
                            ch.pipeline().addLast(
                                    new LengthFieldBasedFrameDecoder(1024 * 1024, 0, 3, 0, 3),
                                    new LengthFieldPrepender(3),
                                    new JsonDecoder(),
                                    new JsonEncoder(),
                                    new SimpleChannelInboundHandler<Message>() {
                                        @Override
                                        protected void channelRead0(ChannelHandlerContext ctx, Message msg) {
                                            if (msg instanceof AuthMessage) {
                                                taskAuthMessage(msg);
                                            }
                                            if (msg instanceof ListMessage) {
                                                taskListMessage(msg);
                                            }
                                            if (msg instanceof FileContentMessage) {
                                                taskFileContentMessage(msg);
                                            }
                                        }
                                    }
                            );
                        }
                    });

            ChannelFuture future = bootstrap.connect("localhost", 9000).sync();
            updateUI(() -> textField.setText("Связь с сервером установлена."));
            channel = future.channel();
            updateUI(() -> {
                try {
                    authRun();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }
    }
    public void closeWindow() {
        updateUI(() -> {
        group.shutdownGracefully();
        System.exit(0);
        });
    }

    public void receiveFileServer() {
        if (fileChoiceServer != null) {
            FileRequestMessage msg = new FileRequestMessage();
            msg.setFileName(fileChoiceServer);
            updateUI(() -> textField.setText("Скачивается файл " + fileChoiceServer + "..."));
            channel.writeAndFlush(msg);
            if (pathChoiceClient == null){
                pathChoiceClient = Paths.get(".", "client_repository");
            }
            if (Files.isRegularFile(pathChoiceClient)){
                pathChoiceClient = pathChoiceClient.getParent();
            }
            pathReceiveFileServer = pathChoiceClient;
        }
    }

    private void taskFileContentMessage(Message msg){
        FileContentMessage fcm =(FileContentMessage) msg;
        Path path = Paths.get(String.valueOf(pathReceiveFileServer), fcm.getFileName());
        try (RandomAccessFile accessFile = new RandomAccessFile(String.valueOf(path),"rw"))
        {
            accessFile.seek(fcm.getStartPosition());
            accessFile.write(fcm.getContent());
            if (fcm.isLast()){
                accessFile.close();
                updateUI(() -> textField.setText("Файл " + fcm.getFileName() + " получен."));
                newListClient();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public void authRun() throws IOException {
        Stage stage = new Stage();
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/auth.fxml"));
        Scene s = new Scene(fxmlLoader.load(), 300, 120);
        stage.setScene(s);
        authControl = fxmlLoader.getController();
        stage.setTitle("Авторизация");
        stage.setOnCloseRequest(event -> authControl.closeClient());
        stage.show();
        authControl.setChannelAuth(channel);
        authControl.setMainController(this);
    }

    public void newListServer() {
        ListMessage message = new ListMessage();
        channel.writeAndFlush(message);
    }

    public void newDirectoryClient() throws IOException {
        if (pathChoiceClient == null){
            alertError("Не выбрана директория для создания поддиректории.");
        }else {
            Stage stage = new Stage();
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/newdir.fxml"));
            Scene s = new Scene(fxmlLoader.load(), 270, 80);
            TreeItem<String> tr = treeViewClient.getSelectionModel().getSelectedItem();
            stage.setScene(s);
            NewdirController newdirController = fxmlLoader.getController();
            stage.setTitle("Новая папка");
            stage.setOnCloseRequest(windowEvent -> newListClient());
            stage.show();
            newdirController.setTreeItemNewDir(tr);
            newdirController.setPath(pathChoiceClient);
            newdirController.setMainController(this);
        }
    }

    private void taskListMessage(Message message) {
        ListMessage msg = (ListMessage) message;
        List<String> pathList = msg.getPathList();
        updateUI(() -> {
            langs.clear();
            langs.addAll(pathList);
            listViewServer.setItems(langs);
        });
    }

    public void newListClient() {
        Path path = Paths.get(".", "client_repository");
        updateUI(() -> treeViewClient.setRoot(getNodesForDirectoryClient(new File(String.valueOf(path)))));
        newPathChoise();
    }

    public TreeItem<String> getNodesForDirectoryClient(File directory) {
        TreeItem<String> root = new TreeItem<>(directory.getName());
        root.setExpanded(true);
        for (File f : directory.listFiles()) {
            if (f.isDirectory())
                root.getChildren().add(getNodesForDirectoryClient(f));
            else
                root.getChildren().add(new TreeItem<>(f.getName()));
        }
        return root;
    }

    private void taskAuthMessage(Message msg) {
        AuthMessage message = (AuthMessage) msg;
        if (message.isRegistration()) {
            authControl.receiveAuthMessage(message);
        } else {
            try {
                authControl.receiveAuthMessage(message);
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (authControl != null && !message.isError()) {
                buttClientList.setDisable(false);
                buttClientMd.setDisable(false);
                buttClientSend.setDisable(false);
                buttServerDelete.setDisable(false);
                buttServerList.setDisable(false);
                buttServerReceive.setDisable(false);
                buttClientAdd.setDisable(false);
                buttClientDelete.setDisable(false);
                newListClient();
                newListServer();
            }
        }
    }

    private void createClientRepository(){
        Path path = Paths.get(".", "client_repository");
        if (!Files.exists(path)) {
            try {
                Files.createDirectory(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        absPath = Paths.get(String.valueOf(path.toAbsolutePath()));
        absPath = absPath.getParent();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
       createClientRepository();
        treeViewListenerClient();
        listViewListenerServer();
        newPathChoise();
        updateUI(() -> textField.setText("Устанавливается связь с сервером. Ждите..."));
        new Thread(() -> {
            try {
                nettyConnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void listViewListenerServer(){
        langs = FXCollections.observableArrayList();
        listViewServer.setItems(langs);
        listViewServer.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                fileChoiceServer = (String) listViewServer.getSelectionModel().getSelectedItem();
                labelServer.setText(fileChoiceServer);
            }
        });
    }

    private void treeViewListenerClient(){
        treeViewClient.getSelectionModel().getSelectedItems().addListener((ListChangeListener<TreeItem<String>>) change -> {
            pathBuilder = new StringBuilder();
            for (TreeItem<String> item = treeViewClient.getSelectionModel().getSelectedItem();
                 item != null; item = item.getParent()) {
                pathBuilder.insert(0, item.getValue());
                pathBuilder.insert(0, "\\");
            }
            String path1 = pathBuilder.toString();
            labelClient.setText(path1);
            pathChoiceClient = Paths.get(String.valueOf(absPath), path1);
            fileChoiceClient = null;
            if (Files.isRegularFile(pathChoiceClient)) {
                fileChoiceClient = String.valueOf(pathChoiceClient.getName(pathChoiceClient.getNameCount()-1));
                pathChoiceClient = pathChoiceClient.getParent();
            }
        });
    }

    private void newPathChoise(){
        Path path = Paths.get(".", "client_repository");
        updateUI(() -> labelClient.setText(String.valueOf(path)));
        pathChoiceClient = Paths.get(String.valueOf(absPath), String.valueOf(path));
        fileChoiceClient = null;
    }

    public void choiceListViewServer() {
        fileChoiceServer = (String) listViewServer.getSelectionModel().getSelectedItem();
        labelServer.setText(fileChoiceServer);
    }

    public void addFileClient() throws IOException {
        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showOpenDialog(this);
        if (pathChoiceClient == null) {
            alertError("Не выбрана директория для вставки файла.");
        } else {
            Files.copy(file.toPath(), Paths.get(String.valueOf(pathChoiceClient), file.getName()));
            textField.setText("Файл " + file.getName() + " добавлен в директорию " + pathChoiceClient + ".");
            newListClient();
        }
    }

    private void alertError(String text) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("ВНИМАНИЕ");
        alert.setHeaderText(text);
        ButtonType buttOK = new ButtonType("ОК");
        alert.getButtonTypes().clear();
        alert.getButtonTypes().addAll(buttOK);
        alert.showAndWait();
    }

    private void alertDelete(String text, String fileDelete) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Удаление файла");
        alert.setHeaderText(text + fileDelete + "?");
        ButtonType buttOK = new ButtonType("Да");
        ButtonType buttNO = new ButtonType("Отмена");
        alert.getButtonTypes().clear();
        alert.getButtonTypes().addAll(buttOK, buttNO);
        Optional<ButtonType> option = alert.showAndWait();
        buttOkDelete = option.get().getText().equals("Да");
    }

    private void updateUI(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }

    public void deleteFileServer() {
        if (fileChoiceServer != null){
            alertDelete("Удалить файл: ", fileChoiceServer);
            if (buttOkDelete) {
                DeleteMessage msg = new DeleteMessage();
                msg.setFileName(fileChoiceServer);
                channel.writeAndFlush(msg);
                newListServer();
            }
        }
    }

    public void sendFileClient() throws FileNotFoundException {
        Path path = Paths.get(String.valueOf(pathChoiceClient), fileChoiceClient);
        File file = new File(String.valueOf(path));
        accessFile = new RandomAccessFile(file, "r");
        updateUI(() -> textField.setText("Передается файл " + fileChoiceClient + "..."));
        sendFile();

    }

    private void sendFile() {
        try {
            byte[] fileContent;
            long avaible = accessFile.length() - accessFile.getFilePointer();
            if (avaible > 64 * 1024) {
                fileContent = new byte[64 * 1024];
            } else {
                fileContent = new byte[(int) avaible];
            }
            FileContentMessage msgContent = new FileContentMessage();
            msgContent.setStartPosition(accessFile.getFilePointer());
            accessFile.read(fileContent);
            msgContent.setFileName(fileChoiceClient);
            msgContent.setContent(fileContent);
            final boolean last = accessFile.getFilePointer() == accessFile.length();
            msgContent.setLast(accessFile.getFilePointer() == accessFile.length());
            channel.writeAndFlush(msgContent).addListener((ChannelFutureListener) channelFuture -> {
                if (!last) {
                    sendFile();
                }
            });
            if (last) {
                accessFile.close();
                updateUI(() -> {
                    textField.setText("Файл " + fileChoiceClient + " передан.");
                    newListServer();
                    newPathChoise();
                });
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteFileClient() throws IOException {
        if (fileChoiceClient != null) {
            Path path = Paths.get(String.valueOf(pathChoiceClient), fileChoiceClient);
            alertDelete("Удалить файл: ", fileChoiceClient);
            if (buttOkDelete) {
                Files.delete(path);
                updateUI(() -> textField.setText("Файл " + fileChoiceClient + " удален."));
            }
        }else {
            alertDelete("Удалить директорию: ", String.valueOf(pathChoiceClient));
            if (buttOkDelete) {
                try(DirectoryStream<Path> files = Files.newDirectoryStream(pathChoiceClient)) {
                        for (Path p : files) {
                            Files.delete(p);
                        }
                    }
                    Files.delete(pathChoiceClient);
                    updateUI(() -> textField.setText("Директория " + pathChoiceClient + " удалена."));
                }
        }
        treeViewClient.getSelectionModel().clearSelection();
        updateUI(() -> newListClient());
    }
}



