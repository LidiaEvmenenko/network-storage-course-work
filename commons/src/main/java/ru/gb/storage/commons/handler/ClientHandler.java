package ru.gb.storage.commons.handler;


import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import ru.gb.storage.commons.message.*;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static java.nio.file.Files.*;

public class ClientHandler extends ChannelInboundHandlerAdapter {
    private String userName;
    private RandomAccessFile accessFile;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof AuthMessage) {
            AuthMessage message = (AuthMessage) msg;
            userName = message.getLogin();
        }
        if (msg instanceof ListMessage) {
            ListMessage message = new ListMessage();
            Path path = Paths.get( "server_repository",userName);
            List<String> pathList = new ArrayList<>();
            try(DirectoryStream<Path> files = Files.newDirectoryStream(path)){
                for (Path p: files) {
                    pathList.add(p.toFile().getName());
                }
            }
            message.setPathList(pathList);
            ctx.writeAndFlush(message);
            ctx.flush();
        }
        if (msg instanceof DeleteMessage) {
            DeleteMessage message = (DeleteMessage) msg;
            Path path = Paths.get( "server_repository",userName,message.getFileName());
            Files.delete(path);
        }
        if (msg instanceof FileContentMessage) {
            taskFileContentMessage((Message) msg);
        }
        if (msg instanceof FileRequestMessage) {
            FileRequestMessage msgReguest = (FileRequestMessage) msg;
            Path path = Paths.get( "server_repository", userName, msgReguest.getFileName());
            if (accessFile == null) {
                File file = new File(String.valueOf(path));
                accessFile = new RandomAccessFile(file, "r");
                sendFile(ctx, msgReguest);
            }
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
    private void sendFile(final ChannelHandlerContext ctx, final FileRequestMessage msgReguest) throws IOException, InterruptedException {
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
                accessFile.close();
                accessFile = null;
            }
        }
    }
    private void taskFileContentMessage(Message msg){
        FileContentMessage fcm =(FileContentMessage) msg;
        try {
            Path path = Paths.get(".","server_repository", userName, fcm.getFileName());
            RandomAccessFile accessFile = new RandomAccessFile(String.valueOf(path),"rw");
            accessFile.seek(fcm.getStartPosition());
            accessFile.write(fcm.getContent());
            if (fcm.isLast()){
                accessFile.close();
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
