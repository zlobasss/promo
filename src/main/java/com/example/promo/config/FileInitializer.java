package com.example.promo.config;

import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Component
public class FileInitializer {

    private static final String NOT_INIT_PATH = "src/main/resources/static/not-init";
    private static final String INIT_PATH = "src/main/resources/static/init";


    public List<Path> getFilesNotInit() {
        // Получаем пути к директориям
        Path notInitDir = Paths.get(NOT_INIT_PATH);

        // Перемещаем файлы из not-init в init
        try {
            return Files.list(notInitDir).toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
