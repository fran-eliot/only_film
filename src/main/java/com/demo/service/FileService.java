package com.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.UUID;

@Service
public class FileService {
    public static final String UPLOAD_DIR = "uploads";
    private static final int MAX_BYTES = 10 * 1024 * 1024;
    private static final long MAX_PIXELS = 20_000_000L;
    private static final int MAX_DIMENSION = 6000;

    @Value("${onlyfilm.upload-dir:uploads}")
    private String uploadDir = UPLOAD_DIR;

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("La imagen supera el límite de 10 MB");
        }

        try {
            // Never trust the client-supplied name, extension or MIME type.
            byte[] bytes;
            try (var in = file.getInputStream()) {
                bytes = in.readNBytes(MAX_BYTES + 1);
            }
            if (bytes.length > MAX_BYTES) {
                throw new IllegalArgumentException("La imagen supera el límite de 10 MB");
            }

            ImageData verified = verifyAndNormalize(bytes);
            Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            Path destination = dir.resolve(UUID.randomUUID() + "." + verified.extension());
            Files.write(destination, verified.bytes(), StandardOpenOption.CREATE_NEW);
            return "/uploads/" + destination.getFileName();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar la imagen", e);
        }
    }

    private static ImageData verifyAndNormalize(byte[] bytes) throws IOException {
        try (ImageInputStream imageStream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (imageStream == null) {
                throw new IllegalArgumentException("El archivo no es una imagen válida");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageStream);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("Solo se admiten imágenes JPEG o PNG");
            }
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName().toLowerCase(java.util.Locale.ROOT);
                String extension;
                if ("jpeg".equals(format) || "jpg".equals(format)) {
                    extension = "jpg";
                } else if ("png".equals(format)) {
                    extension = "png";
                } else {
                    throw new IllegalArgumentException("Solo se admiten imágenes JPEG o PNG");
                }
                reader.setInput(imageStream, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > MAX_DIMENSION
                        || height > MAX_DIMENSION || (long) width * height > MAX_PIXELS) {
                    throw new IllegalArgumentException("La imagen excede las dimensiones permitidas");
                }
                BufferedImage decoded = reader.read(0);
                if (decoded == null) {
                    throw new IllegalArgumentException("El archivo no es una imagen válida");
                }

                // Re-encode to strip arbitrary metadata and trailing active content.
                BufferedImage normalized = decoded;
                if ("jpg".equals(extension)) {
                    normalized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                    Graphics2D graphics = normalized.createGraphics();
                    try {
                        graphics.drawImage(decoded, 0, 0, null);
                    } finally {
                        graphics.dispose();
                    }
                }
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                if (!ImageIO.write(normalized, extension.equals("jpg") ? "jpeg" : "png", output)) {
                    throw new IOException("Image writer unavailable");
                }
                return new ImageData(output.toByteArray(), extension);
            } finally {
                reader.dispose();
            }
        } catch (javax.imageio.IIOException e) {
            throw new IllegalArgumentException("El archivo no es una imagen válida", e);
        }
    }

    private record ImageData(byte[] bytes, String extension) { }
}
