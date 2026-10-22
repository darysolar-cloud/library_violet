package ru.library.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/** Сохраняет обложки (uploads/covers) и аватарки (uploads/avatars). */
@Service
public class FileStorageService {

    private static final List<String> EXTENSIONS = List.of("png", "jpg", "jpeg", "webp", "gif");

    private final Path coversDir;
    private final Path avatarsDir;

    public FileStorageService(@Value("${library.upload-dir}") String uploadDir) {
        this.coversDir = Path.of(uploadDir, "covers").toAbsolutePath();
        this.avatarsDir = Path.of(uploadDir, "avatars").toAbsolutePath();
        try {
            Files.createDirectories(coversDir);
            Files.createDirectories(avatarsDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось создать папки для файлов: " + uploadDir, e);
        }
    }

    public Path getDir() {
        return coversDir;
    }

    public Path getAvatarDir() {
        return avatarsDir;
    }

    public String save(MultipartFile file) {
        return store(file, coversDir, "Обложка");
    }

    public String saveAvatar(MultipartFile file) {
        if (file.getSize() > 2L * 1024 * 1024) {
            throw new LibraryException("Аватарка должна быть не больше 2 МБ.");
        }
        return store(file, avatarsDir, "Аватарка");
    }

    public void delete(String stored) {
        remove(coversDir, stored);
    }

    public void deleteAvatar(String stored) {
        remove(avatarsDir, stored);
    }

    private String store(MultipartFile file, Path dir, String what) {
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.') + 1) : "";
        String type = file.getContentType();
        if (type == null || !type.startsWith("image/") || !EXTENSIONS.contains(ext)) {
            throw new LibraryException(what + " должна быть картинкой PNG, JPG, WebP или GIF.");
        }
        String stored = UUID.randomUUID() + "." + ext;
        try {
            file.transferTo(dir.resolve(stored));
        } catch (IOException e) {
            throw new LibraryException("Не удалось сохранить файл.");
        }
        return stored;
    }

    private void remove(Path dir, String stored) {
        try {
            Files.deleteIfExists(dir.resolve(stored));
        } catch (IOException ignored) {
            // файл мог быть уже удалён вручную
        }
    }
}