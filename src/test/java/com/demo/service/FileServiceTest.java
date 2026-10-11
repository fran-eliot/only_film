package com.demo.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileServiceTest {
    @TempDir Path directory;
    FileService fileService = new FileService();

    private void configureDirectory() throws Exception {
        var field = FileService.class.getDeclaredField("uploadDir");
        field.setAccessible(true);
        field.set(fileService, directory.toString());
    }

    private byte[] image(String format) throws Exception {
        BufferedImage img = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(img, format, stream));
        return stream.toByteArray();
    }

    @Test
    void nullAndEmptyFilesAreIgnored() throws Exception {
        configureDirectory();
        assertNull(fileService.store(null));
        assertNull(fileService.store(new MockMultipartFile("file", new byte[0])));
    }

    @Test
    void storesAndReencodesRealPngWithoutTrustingFilename() throws Exception {
        configureDirectory();
        byte[] png = image("png");
        String url = fileService.store(new MockMultipartFile("file",
                "../../evil.html", "text/html", png));
        assertTrue(url.matches("/uploads/[0-9a-f-]{36}\\.png"));
        Path stored = directory.resolve(url.substring("/uploads/".length()));
        assertTrue(Files.exists(stored));
        assertNotNull(ImageIO.read(stored.toFile()));
        assertEquals(8, ImageIO.read(stored.toFile()).getWidth());
    }

    @Test
    void jpegIsSavedAsJpgEvenWithWrongExtension() throws Exception {
        configureDirectory();
        String url = fileService.store(new MockMultipartFile("file", "image.exe",
                "application/octet-stream", image("jpeg")));
        assertTrue(url.endsWith(".jpg"));
        assertNotNull(ImageIO.read(directory.resolve(url.substring(9)).toFile()));
    }

    @Test
    void jpegWithTrailingContentIsNormalizedAndDecodable() throws Exception {
        configureDirectory();
        byte[] original = image("jpeg");
        byte[] tail = "<script>injected</script>".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] supplied = java.util.Arrays.copyOf(original, original.length + tail.length);
        System.arraycopy(tail, 0, supplied, original.length, tail.length);

        String url = fileService.store(new MockMultipartFile("file", "photo.png", "image/png", supplied));
        assertTrue(url.endsWith(".jpg"));
        Path saved = directory.resolve(url.substring("/uploads/".length()));
        byte[] stored = Files.readAllBytes(saved);
        assertNotNull(ImageIO.read(saved.toFile()));
        assertFalse(new String(stored, java.nio.charset.StandardCharsets.ISO_8859_1)
                .contains("<script>injected</script>"));
    }

    @Test
    void rejectsFakeImageAndNeverCreatesAFile() throws Exception {
        configureDirectory();
        MockMultipartFile invalid = new MockMultipartFile("file", "fake.png", "image/png",
                "<script>alert('x')</script>".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> fileService.store(invalid));
        assertFalse(Files.exists(directory.resolve("fake.png")));
    }

    @Test
    void rejectsUnsupportedBmp() throws Exception {
        configureDirectory();
        MockMultipartFile bmp = new MockMultipartFile("file", "foo.bmp", "image/bmp", image("bmp"));
        assertThrows(IllegalArgumentException.class, () -> fileService.store(bmp));
    }

    @Test
    void rejectsMoreThanTenMegabytesBeforeDecoding() throws Exception {
        configureDirectory();
        MockMultipartFile oversized = new MockMultipartFile(
                "file", "big.png", "image/png", new byte[10 * 1024 * 1024 + 1]);
        assertThrows(IllegalArgumentException.class, () -> fileService.store(oversized));
    }

    @Test
    void rejectsOversizedImageDimensions() throws Exception {
        configureDirectory();
        BufferedImage img = new BufferedImage(6001, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(img, "png", out));
        MockMultipartFile huge = new MockMultipartFile("file", "huge.png", "image/png", out.toByteArray());
        assertThrows(IllegalArgumentException.class, () -> fileService.store(huge));
    }

    @Test
    void stripsTrailingPayloadWhenReencoding() throws Exception {
        configureDirectory();
        byte[] original = image("png");
        byte[] payload = "<script>unsafe</script>".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] withPayload = java.util.Arrays.copyOf(original, original.length + payload.length);
        System.arraycopy(payload, 0, withPayload, original.length, payload.length);
        String url = fileService.store(new MockMultipartFile("file", "test.png", "image/png", withPayload));
        byte[] stored = Files.readAllBytes(directory.resolve(url.substring("/uploads/".length())));
        assertFalse(new String(stored, java.nio.charset.StandardCharsets.ISO_8859_1).contains("<script>"));
    }
}
