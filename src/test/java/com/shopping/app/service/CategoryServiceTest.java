package com.shopping.app.service;

import com.shopping.app.dto.response.CategoryResponse;
import com.shopping.app.entity.Category;
import com.shopping.app.exception.ResourceNotFoundException;
import com.shopping.app.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService Tests")
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category testCategory;
    private Category testChildCategory;

    @BeforeEach
    void setUp() {
        testCategory = Category.builder()
                .id(1L)
                .name("Electronics")
                .slug("electronics")
                .description("Electronic products")
                .imageUrl("https://example.com/electronics.jpg")
                .parent(null)
                .children(new ArrayList<>())
                .sortOrder(0)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        testChildCategory = Category.builder()
                .id(2L)
                .name("Laptops")
                .slug("laptops")
                .description("Laptop computers")
                .imageUrl("https://example.com/laptops.jpg")
                .parent(testCategory)
                .children(new ArrayList<>())
                .sortOrder(0)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Get All Categories")
    class GetAllCategories {

        @Test
        @DisplayName("Should return list of active categories")
        void getAllCategories_ReturnsList() {
            // Arrange
            when(categoryRepository.findByActiveTrue())
                    .thenReturn(List.of(testCategory));

            // Act
            List<CategoryResponse> responses = categoryService.getAllCategories();

            // Assert
            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).getName()).isEqualTo("Electronics");
            assertThat(responses.get(0).isActive()).isTrue();
            verify(categoryRepository).findByActiveTrue();
        }
    }

    @Nested
    @DisplayName("Get Category By Id")
    class GetCategoryById {

        @Test
        @DisplayName("Should return category when found")
        void getCategoryById_ReturnsResponse() {
            // Arrange
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));

            // Act
            CategoryResponse response = categoryService.getCategoryById(1L);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getName()).isEqualTo("Electronics");
            assertThat(response.getSlug()).isEqualTo("electronics");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when category not found")
        void getCategoryById_NotFound_ThrowsException() {
            // Arrange
            when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> categoryService.getCategoryById(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Category");
        }
    }

    @Nested
    @DisplayName("Get Subcategories")
    class GetSubcategories {

        @Test
        @DisplayName("Should return children for existing parent")
        void getSubcategories_ReturnsChildren() {
            // Arrange
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
            when(categoryRepository.findByParentIdAndActiveTrue(1L))
                    .thenReturn(List.of(testChildCategory));

            // Act
            List<CategoryResponse> responses = categoryService.getSubcategories(1L);

            // Assert
            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).getName()).isEqualTo("Laptops");
            assertThat(responses.get(0).getParentId()).isEqualTo(1L);
            verify(categoryRepository).findByParentIdAndActiveTrue(1L);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when parent not found")
        void getSubcategories_ParentNotFound_ThrowsException() {
            // Arrange
            when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> categoryService.getSubcategories(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Category");
        }
    }

    @Nested
    @DisplayName("Create Category")
    class CreateCategory {

        @Test
        @DisplayName("Should create category with generated slug")
        void createCategory_SavesWithSlug() {
            // Arrange
            when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);

            // Act
            CategoryResponse response = categoryService.createCategory(
                    "Electronics", "Electronic products", null, "https://example.com/electronics.jpg");

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getName()).isEqualTo("Electronics");
            assertThat(response.getParentId()).isNull();
            verify(categoryRepository).save(any(Category.class));
        }

        @Test
        @DisplayName("Should create subcategory with parent")
        void createCategory_WithParent_SavesWithParent() {
            // Arrange
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
            when(categoryRepository.save(any(Category.class))).thenReturn(testChildCategory);

            // Act
            CategoryResponse response = categoryService.createCategory(
                    "Laptops", "Laptop computers", 1L, "https://example.com/laptops.jpg");

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getName()).isEqualTo("Laptops");
            assertThat(response.getParentId()).isEqualTo(1L);
            verify(categoryRepository).save(any(Category.class));
        }
    }

    @Nested
    @DisplayName("Delete Category")
    class DeleteCategory {

        @Test
        @DisplayName("Should soft delete category by setting active to false")
        void deleteCategory_SoftDeletes() {
            // Arrange
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
            when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);

            // Act
            categoryService.deleteCategory(1L);

            // Assert
            assertThat(testCategory.isActive()).isFalse();
            verify(categoryRepository).save(testCategory);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when category not found")
        void deleteCategory_NotFound_ThrowsException() {
            // Arrange
            when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> categoryService.deleteCategory(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Category");
        }
    }
}
