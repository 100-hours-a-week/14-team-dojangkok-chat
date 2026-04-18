package com.dojangkok.chat.converter;

import com.dojangkok.chat.common.enums.Code;
import com.dojangkok.chat.common.exception.GeneralException;
import com.dojangkok.chat.dto.MessageContent;
import com.dojangkok.chat.dto.MessageContent.ImageContent;
import com.dojangkok.chat.dto.MessageContent.TextContent;
import com.dojangkok.chat.dto.MessageContent.VideoContent;
import org.bson.Document;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class MessageContentReadConverter implements Converter<Document, MessageContent> {

    @Override
    public MessageContent convert(Document source) {
        if (source.containsKey("text")) {
            return new TextContent(source.getString("text"));
        }

        if (source.containsKey("duration")) {
            return new VideoContent(
                    source.getString("url"),
                    source.getInteger("duration", 0),
                    source.getInteger("width", 0),
                    source.getInteger("height", 0),
                    source.getLong("size")
            );
        }

        if (source.containsKey("url")) {
            return new ImageContent(
                    source.getString("url"),
                    source.getInteger("width", 0),
                    source.getInteger("height", 0),
                    source.getLong("size")
            );
        }

        throw new GeneralException(Code.CHAT_INVALID_CONTENT_TYPE);
    }
}
