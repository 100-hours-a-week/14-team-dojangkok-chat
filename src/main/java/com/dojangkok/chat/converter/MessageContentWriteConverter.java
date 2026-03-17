package com.dojangkok.chat.converter;

import com.dojangkok.chat.dto.MessageContent;
import com.dojangkok.chat.dto.MessageContent.ImageContent;
import com.dojangkok.chat.dto.MessageContent.TextContent;
import com.dojangkok.chat.dto.MessageContent.VideoContent;
import org.bson.Document;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class MessageContentWriteConverter implements Converter<MessageContent, Document> {

    @Override
    public Document convert(MessageContent source) {
        Document doc = new Document();

        switch (source) {
            case TextContent text -> {
                doc.put("_contentType", "TEXT");
                doc.put("text", text.text());
            }
            case ImageContent image -> {
                doc.put("_contentType", "IMAGE");
                doc.put("url", image.url());
                doc.put("width", image.width());
                doc.put("height", image.height());
                doc.put("size", image.size());
            }
            case VideoContent video -> {
                doc.put("_contentType", "VIDEO");
                doc.put("url", video.url());
                doc.put("duration", video.duration());
                doc.put("width", video.width());
                doc.put("height", video.height());
                doc.put("size", video.size());
            }
        }

        return doc;
    }
}
