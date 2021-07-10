package io.liuwei.autumn.config;

import com.vip.vjtools.vjkit.mapper.JsonMapper;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.jruby.internal.JRubyAsciidoctor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author liuwei
 * Created by liuwei on 2018/11/23.
 */
@Configuration
@EnableConfigurationProperties(AutumnProperties.class)
public class AppConfig {
    @Bean
    public JsonMapper jsonMapper() {
        return JsonMapper.nonNullMapper();
    }

    @Bean
    public Asciidoctor asciidoctor() {
        return new JRubyAsciidoctor();
    }
}
