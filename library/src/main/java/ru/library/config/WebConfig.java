package ru.library.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import ru.library.service.FileStorageService;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final FileStorageService files;

    public WebConfig(FileStorageService files) {
        this.files = files;
    }

    /** Обложки лежат в папке uploads/covers и отдаются по адресу /covers/... */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/covers/**")
                .addResourceLocations(files.getDir().toUri().toString());
        registry.addResourceHandler("/avatars/**")
                .addResourceLocations(files.getAvatarDir().toUri().toString());
    }
}
