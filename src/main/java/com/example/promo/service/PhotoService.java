package com.example.promo.service;

import com.example.promo.entity.PhotoEntity;
import com.example.promo.repository.PhotoRepository;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class PhotoService {

    private static final String NOT_INIT_PATH = "static/not-init";
    private static final String INIT_PATH = "static/init";
    private final PhotoRepository photoRepository;

    public PhotoService(PhotoRepository photoRepository) {
        this.photoRepository = photoRepository;
    }

    public PhotoEntity save(PhotoEntity photoEntity) {
        return photoRepository.save(photoEntity);
    }

    public List<PhotoEntity> findAll() {
        return photoRepository.findAll();
    }

    public PhotoEntity findByPhotoId(String vkId) {
        return photoRepository.findByPhotoId(vkId);
    }

    public List<File> getFilesFromNotInit() {
        List<File> files = new ArrayList<>();

        try {
            // Получаем ресурсы через ClassLoader
            Path notInitDir = Paths.get(Objects.requireNonNull(getClass().getClassLoader().getResource(NOT_INIT_PATH)).toURI());

            // Получаем все файлы из директории not-init
            Files.list(notInitDir).forEach(path -> files.add(path.toFile()));

        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }

        return files;
    }

    public void sendFilesFromNotInitToInit() {
        try {
            // Получаем пути к директориям
            Path notInitDir = Paths.get(NOT_INIT_PATH);
            Path initDir = Paths.get(INIT_PATH);

            // Проверяем, существует ли папка назначения, если нет - создаем
            if (!Files.exists(initDir)) {
                Files.createDirectories(initDir);
            }

            // Перемещаем файлы из not-init в init
            Files.list(notInitDir).forEach(file -> {
                try {
                    // Получаем путь нового файла в папке init
                    Path targetFile = initDir.resolve(file.getFileName());

                    // Перемещаем файл
                    Files.move(file, targetFile);
                    System.out.println("Файл " + file.getFileName() + " перемещен в init");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
