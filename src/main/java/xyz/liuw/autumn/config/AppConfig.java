package xyz.liuw.autumn.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vip.vjtools.vjkit.mapper.JsonMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/23.
 */
@Configuration
public class AppConfig {
    @Bean
    public JsonMapper jsonMapper() {
        return JsonMapper.nonNullMapper();
    }

    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return new GenericJackson2JsonRedisSerializer(mapper);
    }
}
