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
import java.net.Socket;
import java.net.URL;
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
    private ListView listViewServer;
    private static Socket socket = null;
    private File file;
    private String fileChoiceServer;
    private StringBuilder pathBuilder = null;
    private ObservableList<String> langs;
    private boolean buttOkDelete = false;
    private NioEventLoopGroup group;
    private Channel channel = null;
    private AuthController authControl = null;
    private NewdirController newdirController = null;
    private Path absPath;
    private Path pathChoiceClient;
    private String fileChoiceClient;

    public void nettyConnect() throws IOException {
        group = new NioEventLoopGroup(1);
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        protected void initChannel(NioSocketChannel ch) throws Exception {
                            ch.pipeline().addLast(
                                    new LengthFieldBasedFrameDecoder(1024 * 1024, 0, 3, 0, 3),
                                    new LengthFieldPrepender(3),
                                    new JsonDecoder(),
                                    new JsonEncoder(),
                                    new SimpleChannelInboundHandler<Message>() {
                                        @Override
                                        protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {
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
            System.out.println("Client started.");
            updateUI(() -> {
                textField.setText("Связь с сервером установлена.");
            });
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
        group.shutdownGracefully();
        System.exit(0);
    }
    public void receiveFileServer() {
        if (fileChoiceServer != null) {
            FileRequestMessage msg = new FileRequestMessage();
            msg.setFileName(fileChoiceServer);
            fileChoiceServer = null;
            channel.writeAndFlush(msg);
        }
    }

    private void taskFileContentMessage(Message msg){
        FileContentMessage fcm =(FileContentMessage) msg;
        try {
            Path path = Paths.get(String.valueOf(pathChoiceClient), fcm.getFileName());
            RandomAccessFile accessFile = new RandomAccessFile(String.valueOf(path),"rw");
            accessFile.seek(fcm.getStartPosition());
            accessFile.write(fcm.getContent());
            if (fcm.isLast()){
                accessFile.close();
                updateUI(() -> {
                    textField.setText("Файл " + fcm.getFileName() + " получен.");
                });
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
        stage.setOnCloseRequest(event -> authControl.closeWindow());
        stage.show();
        authControl.setChannelAuth(channel);
    }

    public void newListServer() {
        ListMessage message = new ListMessage();
        channel.writeAndFlush(message);
    }

    public void newDirectoryClient() throws IOException {
        Stage stage = new Stage();
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/newdir.fxml"));
        Scene s = new Scene(fxmlLoader.load(), 270, 80);
        TreeItem<String> tr = treeViewClient.getSelectionModel().getSelectedItem();
        stage.setScene(s);
        newdirController = fxmlLoader.getController();
        stage.setTitle("Новая папка");
        stage.setOnCloseRequest(event -> newdirController.closeWindow());
        stage.show();
        newdirController.setChannelNewDir(null);
        newdirController.setTreeItemNewDir(tr);
        newdirController.setPath(pathChoiceClient);
        newListClient();
    }

    private void taskListMessage(Message message) {
        ListMessage msg = (ListMessage) message;
        List<String> pathList = msg.getPathList();
        updateUI(() -> {
            langs.clear();
            for (int i = 0; i < pathList.size(); i++) {
                langs.add(pathList.get(i));
            }
            listViewServer.setItems(langs);
        });
    }

    public void newListClient() {
        treeViewClient.setRoot(getNodesForDirectoryClient(new File("..\\client_repository\\")));
        TreeItem<String> root1 =treeViewClient.getRoot();
        int k = root1.getChildren().size();
        for (int i=0;i<k; i++){
            System.out.println("  root.getChildren() = " + root1.getChildren().get(i).getValue());
        }
    }

    public TreeItem<String> getNodesForDirectoryClient(File directory) {
        TreeItem<String> root = new TreeItem<>(directory.getName());
        root.setExpanded(true);

        for (File f : directory.listFiles()) {
            if (f.isDirectory()) //если каталог идем на рекурсию
                root.getChildren().add(getNodesForDirectoryClient(f));
            else //если просто файл заполняем только имя
                root.getChildren().add(new TreeItem<>(f.getName()));
        }
        return root;
    }

    private void taskAuthMessage(Message msg) {
        AuthMessage message = (AuthMessage) msg;
        if (message.isRegistr()) {
            authControl.receiveAuthMessage(message);
        } else {
            try {
                authControl.receiveAuthMessage(message);
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (authControl != null) {
                buttClientList.setDisable(false);
                buttClientMd.setDisable(false);
                buttClientSend.setDisable(false);
                buttServerDelete.setDisable(false);
                buttServerList.setDisable(false);
                buttServerReceive.setDisable(false);
                buttClientAdd.setDisable(false);
            }
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Path p = Path.of("client_repository");
        absPath = Paths.get(String.valueOf(p.toAbsolutePath()));
        absPath = absPath.getParent();
        absPath = absPath.getParent();

        updateUI(() -> {
            textField.setText("Устанавливается связь с сервером. Ждите...");
        });
        new Thread(() -> {
            try {
                nettyConnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        treeViewClient.getSelectionModel().getSelectedItems().addListener(new ListChangeListener<TreeItem<String>>() {
            @Override
            public void onChanged(Change<? extends TreeItem<String>> change) {
                File f = new File("\\client_repository");
                pathBuilder = new StringBuilder();
                for (TreeItem<String> item = treeViewClient.getSelectionModel().getSelectedItem();
                     item != null; item = item.getParent()) {

                    pathBuilder.insert(0, item.getValue());
                    pathBuilder.insert(0, "\\");
                }
                String path = pathBuilder.toString();
                labelClient.setText(path);
                pathChoiceClient = Paths.get(String.valueOf(absPath), path);
                fileChoiceClient = String.valueOf(pathChoiceClient.getName(pathChoiceClient.getNameCount()-1));
                if (Files.isRegularFile(pathChoiceClient)) {
                    pathChoiceClient = pathChoiceClient.getParent();
                }
            }
        });
        langs = FXCollections.observableArrayList();
        listViewServer.setItems(langs);
        listViewServer.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                fileChoiceServer = (String) listViewServer.getSelectionModel().getSelectedItem();
            }
        });
    }
    public void choiceListView() {
        fileChoiceServer = (String) listViewServer.getSelectionModel().getSelectedItem();
        System.out.println("fileChoiceServer"+fileChoiceServer);
    }
    public void addFileClient() throws IOException {
        FileChooser fileChooser = new FileChooser();
        file = fileChooser.showOpenDialog(this);
        Files.copy(file.toPath(), Paths.get(String.valueOf(pathChoiceClient), file.getName()));
    }

    private void alertDelete(String fileDelete) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Удаление файла.");
        alert.setHeaderText("Удалить файл: " + fileDelete + "?");
        ButtonType buttOK = new ButtonType("Да");
        ButtonType buttNO = new ButtonType("Отмена");
        alert.getButtonTypes().clear();
        alert.getButtonTypes().addAll(buttOK, buttNO);
        Optional<ButtonType> option = alert.showAndWait();
        if (option.get().getText().equals("Да")) {
            buttOkDelete = true;
        }
    }

    private void updateUI(Runnable r) {// для того, чтобы можно было обновлять интерфейс из любого потока
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }

    public void deleteFileServer() {
        if (fileChoiceServer != null){
            alertDelete(fileChoiceServer);
            if (buttOkDelete) {
                DeleteMessage msg = new DeleteMessage();
                msg.setFileName(fileChoiceServer);
                channel.writeAndFlush(msg);
            }
            fileChoiceServer = null;
        }
    }

    public void sendFileClient() {
        Path path = Paths.get(String.valueOf(pathChoiceClient), fileChoiceClient);
        File file = new File(String.valueOf(path));
        System.out.println(fileChoiceClient);
        try {
            RandomAccessFile accessFile = new RandomAccessFile(file, "r");
            while (accessFile.getFilePointer() != accessFile.length()) {
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
                msgContent.setLast(accessFile.getFilePointer() == accessFile.length());
                channel.writeAndFlush(msgContent);
            }
            updateUI(() -> {
                textField.setText("Файл " + fileChoiceClient + " передан.");
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}



