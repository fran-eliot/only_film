package com.demo.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pages use the shared Thymeleaf head, whose fragment supplies the HTML title. */
class ThymeleafHeadTitleContractTest {
    private static final Path TEMPLATES = Path.of("src/main/resources/templates");

    @Test
    void sharedHeadFragmentDefinesTitle() throws IOException {
        String head = Files.readString(TEMPLATES.resolve("layout/head.html"));
        assertTrue(head.contains("<title"), "Shared head must contain a title element");
        assertTrue(head.contains("th:text=\"\u0024{title}\""),
                "Shared title must consume the fragment parameter");
    }

    @Test
    void standalonePagesUseSharedHeadWithTitleArgument() throws IOException {
        List<Path> pages;
        try (Stream<Path> files = Files.walk(TEMPLATES)) {
            pages = files.filter(path -> path.toString().endsWith(".html"))
                    .filter(path -> !path.startsWith(TEMPLATES.resolve("layout")))
                    .toList();
        }
        assertFalse(pages.isEmpty(), "Expected Thymeleaf pages");
        for (Path page : pages) {
            String html = Files.readString(page);
            assertTrue(html.contains("layout/head :: head("),
                    () -> page + " must use the shared head with a title argument");
        }
    }
}
