package ru.gb.storage.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import ru.gb.storage.commons.handler.JsonDecoder;
import ru.gb.storage.commons.handler.JsonEncoder;
import ru.gb.storage.commons.handler.ClientHandler;
import ru.gb.storage.commons.message.*;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;

public class Server implements Runnable {
    private int port;
    private static DBAuthenticationProvider db;
    private Connection connection = null;

    public Server(int port) {
        this.port = port;
        run();

    }

    public static void main(String[] args) {
        db = new DBAuthenticationProvider();
        if (db.getConnection() == null) {
            System.out.println("Нет связи с БД.");
        } else {
            new Server(9000);
        }
    }

    @Override
    public void run() {
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
        NioEventLoopGroup workGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap server = new ServerBootstrap();
            server.group(bossGroup, workGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<NioSocketChannel>() {
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
                                                AuthMessage message = (AuthMessage) msg;
                                                taskAuthMessage(ctx, message);
                                            }
                                            if (msg instanceof ListMessage) {
                                                ctx.fireChannelRead(msg);
                                            }
                                            if (msg instanceof DeleteMessage) {
                                                ctx.fireChannelRead(msg);
                                            }
                                            if (msg instanceof FileContentMessage) {
                                                ctx.fireChannelRead(msg);
                                            }
                                            if (msg instanceof FileRequestMessage) {
                                                System.out.println("Пришло сообщение");
                                                ctx.fireChannelRead(msg);
                                            }
                                        }

                                    },
                                    new ClientHandler());
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture future = server.bind(port).sync();
            System.out.println("Server started.");
            future.channel().closeFuture().sync();
        } catch (Exception e) {
             e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }
    }

    private void taskAuthMessage(ChannelHandlerContext ctx, Message msg) throws SQLException {
        AuthMessage message = (AuthMessage) msg;
        AuthMessage authMessage = new AuthMessage();
        if (message.isRegistr()) {
            authMessage.setRegistr(true);
            if (db.insertUsers(message.getLogin(), message.getPassword())) {
                authMessage.setMessage("Регистрация прошла успешно.");
                authMessage.setError(false);
            } else {
                authMessage.setError(true);
                authMessage.setMessage("При регистрации произошла ошибка.");
            }
        } else {
            authMessage.setRegistr(false);
            if (db.getLoginByLoginAndPassword(message.getLogin(), message.getPassword())) {
                authMessage.setError(false);
                Path parent = Paths.get(".");
                Path path = Paths.get( "server_repository", message.getLogin());
                for (Path p : path){
                    Path cur = parent.resolve(p);
                    if(!Files.exists(parent.resolve(p))){
                        try {
                            Files.createDirectory(cur);
                        } catch (IOException e) {
                            System.out.println("Директория уже существует");
                        }
                    }
                    parent = cur;
                }
                ctx.fireChannelRead(msg);
            } else {
                authMessage.setError(true);
                authMessage.setMessage("Неверный логин/пароль.");
            }
        }
        ctx.writeAndFlush(authMessage);
    }
}



