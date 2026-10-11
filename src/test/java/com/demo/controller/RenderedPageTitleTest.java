package com.demo.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Thymeleaf shared head must yield a real HTML title, not just a template fragment. */
@SpringBootTest
@AutoConfigureMockMvc
class RenderedPageTitleTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void movieCatalogueRendersDocumentTitleFromSharedHead() throws Exception {
        mockMvc.perform(get("/movies"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<title>Cartelera — Only Film</title>")));
    }
}
