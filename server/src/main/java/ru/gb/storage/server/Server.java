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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Server implements Runnable {
    private final int port;
    private static DBAuthenticationProvider db;
    private static final Logger LOGGER = LogManager.getLogger(Server.class);

    public Server(int port) {
        this.port = port;
        run();
    }

    public static void main(String[] args) {
        db = new DBAuthenticationProvider();
        if (db.getConnection() == null) {
            System.out.println("Нет связи с БД.");
            LOGGER.error("Error: Нет связи с БД.");
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
                        protected void initChannel(NioSocketChannel ch) {
                            ch.pipeline().addLast(
                                    new LengthFieldBasedFrameDecoder(1024 * 1024, 0, 3, 0, 3),
                                    new LengthFieldPrepender(3),
                                    new JsonDecoder(),
                                    new JsonEncoder(),
                                    new SimpleChannelInboundHandler<Message>() {
                                        @Override
                                        public void channelActive(ChannelHandlerContext ctx) {
                                            LOGGER.info("Подключился новый клиент.");
                                        }

                                        @Override
                                        public void channelInactive(ChannelHandlerContext ctx)  {
                                            LOGGER.info("Клиент отключился от сервера.");
                                        }

                                        @Override
                                        protected void channelRead0(ChannelHandlerContext ctx, Message msg) {
                                            if (msg instanceof AuthMessage) {
                                                AuthMessage message = (AuthMessage) msg;
                                                LOGGER.info("Получено новое сообщение типа AuthMessage.");
                                                taskAuthMessage(ctx, message);
                                            }
                                            if (msg instanceof ListMessage || msg instanceof DeleteMessage || msg instanceof FileContentMessage || msg instanceof FileRequestMessage) {
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
            LOGGER.info("Сервер запущен.");
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            LOGGER.error("Error: Ошибка запуска сервера. ",e);
        } finally {
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
            db.disconnect();
            LOGGER.info("Сервер завершил свою работу.");
        }
    }

    private void taskAuthMessage(ChannelHandlerContext ctx, Message msg) {
        AuthMessage message = (AuthMessage) msg;
        AuthMessage authMessage = new AuthMessage();
        if (message.isRegistration()) {
            authMessage.setRegistration(true);
            if (db.insertUsers(message.getLogin(), message.getPassword())) {
                authMessage.setMessage("Регистрация прошла успешно.");
                authMessage.setError(false);
                LOGGER.info("Зарегистрирован новый клиент: "+ message.getLogin() + ".");
            } else {
                authMessage.setError(true);
                authMessage.setMessage("При регистрации произошла ошибка.");
                LOGGER.warn("Неверно сформирован запрос на регистрацию.");
            }
        } else {
            authMessage.setRegistration(false);
            if (db.getLoginByLoginAndPassword(message.getLogin(), message.getPassword())) {
                authMessage.setError(false);
                LOGGER.info("Успешно авторизован клиент: " + message.getLogin() + ".");
                Path parent = Paths.get(".");
                Path path = Paths.get("server_repository", message.getLogin());
                for (Path p : path){
                    Path cur = parent.resolve(p);
                    if(!Files.exists(parent.resolve(p))){
                        try {
                            Files.createDirectory(cur);
                            LOGGER.info("Создана директория для нового клиента "+ message.getLogin() + ".");
                        } catch (IOException e) {
                            LOGGER.error("Error: Не удалось создать директорию нового клиента: " + message.getLogin() + ". ",e);
                        }
                    }
                    parent = cur;
                }
                ctx.fireChannelRead(msg);
            } else {
                authMessage.setError(true);
                authMessage.setMessage("Неверный логин/пароль.");
                LOGGER.warn("Неверно указан логин/пароль при авторизации.");
            }
        }
        ctx.writeAndFlush(authMessage);
    }
}



