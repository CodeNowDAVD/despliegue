package biblioteca.gorbits.catalog;

import biblioteca.gorbits.catalog.dto.BookRequest;
import biblioteca.gorbits.catalog.dto.BookResponse;
import biblioteca.gorbits.catalog.dto.CategoryRequest;
import biblioteca.gorbits.catalog.dto.CategoryResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /** Lectura permitida a proveedor y administrador. */

    @GetMapping("/categories")
    @PreAuthorize("hasAnyRole('ADMIN','PROVEEDOR')")
    public List<CategoryResponse> listCategories() {
        return catalogService.listCategories();
    }

    @GetMapping("/categories/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PROVEEDOR')")
    public CategoryResponse getCategory(@PathVariable Long id) {
        return catalogService.getCategory(id);
    }

    @PostMapping("/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> createCategory(@RequestBody @Valid CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogService.createCategory(request));
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CategoryResponse updateCategory(@PathVariable Long id, @RequestBody @Valid CategoryRequest request) {
        return catalogService.updateCategory(id, request);
    }

    @DeleteMapping("/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        catalogService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/books")
    @PreAuthorize("hasAnyRole('ADMIN','PROVEEDOR')")
    public List<BookResponse> listBooks(@RequestParam(required = false) Long categoryId) {
        return catalogService.listBooks(categoryId);
    }

    @GetMapping("/books/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PROVEEDOR')")
    public BookResponse getBook(@PathVariable Long id) {
        return catalogService.getBook(id);
    }

    @PostMapping("/books")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookResponse> createBook(@RequestBody @Valid BookRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogService.createBook(request));
    }

    @PutMapping("/books/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public BookResponse updateBook(@PathVariable Long id, @RequestBody @Valid BookRequest request) {
        return catalogService.updateBook(id, request);
    }

    @DeleteMapping("/books/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        catalogService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }
}
