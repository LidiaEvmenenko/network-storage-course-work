package ru.gb.storage.commons.handler;

import io.netty.channel.*;
import ru.gb.storage.commons.message.*;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;


public class ClientHandler extends SimpleChannelInboundHandler {
    private String userName;
    private RandomAccessFile accessFile;
    private static final Logger LOGGER = LogManager.getLogger(ClientHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof AuthMessage) {
            AuthMessage message = (AuthMessage) msg;
            userName = message.getLogin();
            LOGGER.info("Получено новое сообщение типа AuthMessage от клиента " + userName);
        }
        if (msg instanceof ListMessage) {
            ListMessage message = new ListMessage();
            LOGGER.info("Получено новое сообщение типа ListMessage от клиента " + userName);
            Path path = Paths.get( "server_repository",userName);
            List<String> pathList = new ArrayList<>();
            try(DirectoryStream<Path> files = Files.newDirectoryStream(path)){
                for (Path p: files) {
                    pathList.add(p.toFile().getName());
                }
            }
            message.setPathList(pathList);
            ctx.writeAndFlush(message);
        }
        if (msg instanceof DeleteMessage) {
            DeleteMessage message = (DeleteMessage) msg;
            Path path = Paths.get( "server_repository",userName,message.getFileName());
            LOGGER.info("Получено новое сообщение типа DeleteMessage на удаление файла " + path);
            Files.delete(path);
        }
        if (msg instanceof FileContentMessage) {
            taskFileContentMessage((Message) msg);
        }
        if (msg instanceof FileRequestMessage) {
            FileRequestMessage msgReguest = (FileRequestMessage) msg;
            Path path = Paths.get( "server_repository", userName, msgReguest.getFileName());
            LOGGER.info("Клиент " + userName + " отправил запрос на скачивание файла " + msgReguest.getFileName() + ".");
            if (accessFile == null) {
                File file = new File(String.valueOf(path));
                accessFile = new RandomAccessFile(file, "r");
                sendFile(ctx, msgReguest);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("Error: Критическая ошибка обработки клиентского подключения. ",cause);
        ctx.close();
    }

    private void sendFile(final ChannelHandlerContext ctx, final FileRequestMessage msgReguest) throws IOException {
        if (accessFile != null) {
            byte[] fileContent;
            long avaible = accessFile.length() - accessFile.getFilePointer();
            if (avaible > 64 * 1024) {
                fileContent = new byte[64 * 1024];
            } else {
                fileContent = new byte[(int) avaible];
            }
            FileContentMessage message1 = new FileContentMessage();
            message1.setStartPosition(accessFile.getFilePointer());
            accessFile.read(fileContent);
            message1.setContent(fileContent);
            message1.setFileName(msgReguest.getFileName());
            final boolean last = accessFile.getFilePointer() == accessFile.length();
            message1.setLast(last);
            ctx.channel().writeAndFlush(message1).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (!last) {
                        sendFile(ctx, msgReguest);
                    }
                }
            });
            if (last){
                LOGGER.info("Файл" + msgReguest.getFileName() + " передан клиенту "+ userName + ".");
                accessFile.close();
                accessFile = null;
            }
        }
    }
    private void taskFileContentMessage(Message msg){
        FileContentMessage fcm =(FileContentMessage) msg;
        Path path = Paths.get(".","server_repository", userName, fcm.getFileName());
        try (RandomAccessFile accessFile1 = new RandomAccessFile(String.valueOf(path),"rw")) {
            accessFile1.seek(fcm.getStartPosition());
            accessFile1.write(fcm.getContent());
            if (fcm.isLast()){
                accessFile1.close();
                LOGGER.info("Файл" + fcm.getFileName() + " получен от клиента "+ userName + ".");
            }

        } catch (IOException e) {
            LOGGER.error("Error: Ошибка записи файла. ",e);
        }
    }
}
