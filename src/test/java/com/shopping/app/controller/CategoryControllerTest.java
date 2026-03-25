package com.shopping.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopping.app.dto.request.CategoryRequest;
import com.shopping.app.dto.response.CategoryResponse;
import com.shopping.app.security.CustomUserDetailsService;
import com.shopping.app.service.CategoryService;
import com.shopping.app.support.SecuredControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static com.shopping.app.support.SecurityTestHelper.roleJwt;
import static com.shopping.app.support.SecurityTestHelper.userJwt;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
@SecuredControllerTest
@DisplayName("CategoryController Tests")
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private CategoryResponse testCategoryResponse;
    private CategoryRequest testCategoryRequest;

    @BeforeEach
    void setUp() {
        testCategoryResponse = CategoryResponse.builder()
                .id(1L)
                .name("Electronics")
                .slug("electronics")
                .description("Electronic devices and accessories")
                .imageUrl("https://example.com/electronics.jpg")
                .parentId(null)
                .parentName(null)
                .sortOrder(1)
                .active(true)
                .children(List.of())
                .createdAt(LocalDateTime.now())
                .build();

        testCategoryRequest = CategoryRequest.builder()
                .name("Electronics")
                .description("Electronic devices and accessories")
                .parentId(null)
                .imageUrl("https://example.com/electronics.jpg")
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/categories")
    class GetAllCategories {

        @Test
        @DisplayName("Should return 200 with all categories")
        void getAllCategories_Returns200() throws Exception {
            when(categoryService.getAllCategories()).thenReturn(List.of(testCategoryResponse));

            mockMvc.perform(get("/api/v1/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].name", is("Electronics")))
                    .andExpect(jsonPath("$.data[0].slug", is("electronics")));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/categories/{id}")
    class GetCategoryById {

        @Test
        @DisplayName("Should return 200 with category when found")
        void getCategoryById_Returns200() throws Exception {
            when(categoryService.getCategoryById(1L)).thenReturn(testCategoryResponse);

            mockMvc.perform(get("/api/v1/categories/{id}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id", is(1)))
                    .andExpect(jsonPath("$.data.name", is("Electronics")))
                    .andExpect(jsonPath("$.data.active", is(true)));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/categories/top-level")
    class GetTopLevelCategories {

        @Test
        @DisplayName("Should return 200 with top-level categories")
        void getTopLevelCategories_Returns200() throws Exception {
            when(categoryService.getTopLevelCategories()).thenReturn(List.of(testCategoryResponse));

            mockMvc.perform(get("/api/v1/categories/top-level"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].name", is("Electronics")));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/categories")
    class CreateCategory {

        @Test
        @DisplayName("Should return 201 when admin creates category")
        void createCategory_AsAdmin_Returns201() throws Exception {
            when(categoryService.createCategory(anyString(), anyString(), isNull(), anyString()))
                    .thenReturn(testCategoryResponse);

            mockMvc.perform(post("/api/v1/categories")
                            .with(roleJwt("admin@test.com", "ROLE_ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testCategoryRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.name", is("Electronics")))
                    .andExpect(jsonPath("$.data.slug", is("electronics")));
        }

        @Test
        @DisplayName("Should return 400 when name is blank")
        void createCategory_BlankName_Returns400() throws Exception {
            CategoryRequest invalidRequest = CategoryRequest.builder()
                    .name("")
                    .description("Some description")
                    .build();

            mockMvc.perform(post("/api/v1/categories")
                            .with(roleJwt("admin@test.com", "ROLE_ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 403 when non-admin creates category")
        void createCategory_AsUser_Returns403() throws Exception {
            mockMvc.perform(post("/api/v1/categories")
                            .with(userJwt("user@test.com"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testCategoryRequest)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/categories/{id}")
    class UpdateCategory {

        @Test
        @DisplayName("Should return 200 when admin updates category")
        void updateCategory_AsAdmin_Returns200() throws Exception {
            CategoryResponse updatedResponse = CategoryResponse.builder()
                    .id(1L)
                    .name("Updated Electronics")
                    .slug("updated-electronics")
                    .description("Updated description")
                    .active(true)
                    .children(List.of())
                    .build();

            when(categoryService.updateCategory(eq(1L), anyString(), anyString(), isNull(), anyString()))
                    .thenReturn(updatedResponse);

            mockMvc.perform(put("/api/v1/categories/{id}", 1L)
                            .with(roleJwt("admin@test.com", "ROLE_ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testCategoryRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name", is("Updated Electronics")));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/categories/{id}")
    class DeleteCategory {

        @Test
        @DisplayName("Should return 200 when admin deletes category")
        void deleteCategory_AsAdmin_Returns200() throws Exception {
            mockMvc.perform(delete("/api/v1/categories/{id}", 1L)
                            .with(roleJwt("admin@test.com", "ROLE_ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)));
        }
    }
}
