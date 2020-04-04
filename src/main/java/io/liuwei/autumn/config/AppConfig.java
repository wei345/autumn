package io.liuwei.autumn.config;

import com.vip.vjtools.vjkit.mapper.JsonMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
}
