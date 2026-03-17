package com.dojangkok.chat.common.config;

import com.dojangkok.chat.converter.MessageContentReadConverter;
import com.dojangkok.chat.converter.MessageContentWriteConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.util.List;

@Configuration
public class MongoConfig {

    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(List.of(
                new MessageContentReadConverter(),
                new MessageContentWriteConverter()
        ));
    }
}
