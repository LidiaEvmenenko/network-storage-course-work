package ru.gb.storage.client.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;
import ru.gb.storage.client.message.Message;

public class JsonEncoder extends MessageToMessageEncoder<Message> {

    private static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    protected void encode(ChannelHandlerContext ctx, Message message, List<Object> list) throws Exception {

        byte[] value = OBJECT_MAPPER.writeValueAsBytes(message);
        list.add(ctx.alloc().buffer().writeBytes(value));
    }
}