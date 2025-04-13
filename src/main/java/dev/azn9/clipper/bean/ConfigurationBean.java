package dev.azn9.clipper.bean;

import com.google.gson.Gson;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class ConfigurationBean {

    private static final Gson GSON = new Gson();

    @Bean
    public dev.azn9.clipper.data.Configuration getConfiguration() throws IOException {
        String content = String.join("", Files.readAllLines(Path.of("config.json")));
        return ConfigurationBean.GSON.fromJson(content, dev.azn9.clipper.data.Configuration.class);
    }

}
