package com.UrlShortener.URLShortener;

import com.UrlShortener.URLShortener.dto.CreateUrlRequest;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UrlControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testCreateShortUrl_validRequest_returns201() throws Exception {
        CreateUrlRequest request = new CreateUrlRequest("https://spring.io", null, null);

        mockMvc.perform(post("/api/v1/urls")
                        .header("X-Forwarded-For", "192.168.1.1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode", notNullValue()))
                .andExpect(jsonPath("$.originalUrl", is("https://spring.io")))
                .andExpect(jsonPath("$.shortUrl", containsString("http://localhost:8080/")));
    }

    @Test
    void testCreateShortUrl_withCustomAlias_returns201() throws Exception {
        CreateUrlRequest request = new CreateUrlRequest("https://github.com", "custom-gh-test", null);

        mockMvc.perform(post("/api/v1/urls")
                        .header("X-Forwarded-For", "192.168.1.2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode", is("custom-gh-test")))
                .andExpect(jsonPath("$.shortUrl", is("http://localhost:8080/custom-gh-test")));
    }

    @Test
    void testCreateShortUrl_duplicateCustomAlias_returns409Conflict() throws Exception {
        CreateUrlRequest request = new CreateUrlRequest("https://google.com", "dup-alias", null);

        // First request succeeds
        mockMvc.perform(post("/api/v1/urls")
                        .header("X-Forwarded-For", "192.168.1.3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Second request with same alias returns 409
        mockMvc.perform(post("/api/v1/urls")
                        .header("X-Forwarded-For", "192.168.1.3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("Conflict")));
    }

    @Test
    void testCreateShortUrl_invalidUrl_returns400() throws Exception {
        CreateUrlRequest request = new CreateUrlRequest("not-a-valid-url", null, null);

        mockMvc.perform(post("/api/v1/urls")
                        .header("X-Forwarded-For", "192.168.1.4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRedirect_existingCode_returns302() throws Exception {
        CreateUrlRequest request = new CreateUrlRequest("https://docs.oracle.com", "oracle-docs", null);

        mockMvc.perform(post("/api/v1/urls")
                        .header("X-Forwarded-For", "192.168.1.5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Test root redirect
        mockMvc.perform(get("/oracle-docs"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://docs.oracle.com"));

        // Test API redirect
        mockMvc.perform(get("/api/v1/urls/oracle-docs/redirect"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://docs.oracle.com"));
    }

    @Test
    void testGetStats_returns200() throws Exception {
        CreateUrlRequest request = new CreateUrlRequest("https://en.wikipedia.org", "wiki-test", null);

        mockMvc.perform(post("/api/v1/urls")
                        .header("X-Forwarded-For", "192.168.1.6")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Perform 2 redirects to increase click count
        mockMvc.perform(get("/wiki-test")).andExpect(status().isFound());
        mockMvc.perform(get("/wiki-test")).andExpect(status().isFound());

        mockMvc.perform(get("/api/v1/urls/wiki-test/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode", is("wiki-test")))
                .andExpect(jsonPath("$.clickCount", is(2)));
    }

    @Test
    void testGetUrl_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/urls/nonexistent999/stats"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("Not Found")));
    }

    @Test
    void testDeleteUrl_returns204NoContent() throws Exception {
        CreateUrlRequest request = new CreateUrlRequest("https://news.ycombinator.com", "hn-delete", null);

        mockMvc.perform(post("/api/v1/urls")
                        .header("X-Forwarded-For", "192.168.1.7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/v1/urls/hn-delete"))
                .andExpect(status().isNoContent());

        // After deletion, getting it should 404
        mockMvc.perform(get("/api/v1/urls/hn-delete/stats"))
                .andExpect(status().isNotFound());
    }
}
