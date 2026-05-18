package biblioteca.gorbits.catalog;

import biblioteca.gorbits.catalog.dto.BookRequest;
import biblioteca.gorbits.catalog.dto.BookResponse;
import biblioteca.gorbits.catalog.dto.CategoryRequest;
import biblioteca.gorbits.catalog.dto.CategoryResponse;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogService {

    private final BookCategoryRepository categories;
    private final BookRepository books;

    public CatalogService(BookCategoryRepository categories, BookRepository books) {
        this.categories = categories;
        this.books = books;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories() {
        return categories.findAll().stream()
                .map(c -> new CategoryResponse(c.getId(), c.getName()))
                .sorted((a, b) -> a.name().compareToIgnoreCase(b.name()))
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategory(Long id) {
        return categories.findById(id)
                .map(c -> new CategoryResponse(c.getId(), c.getName()))
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        if (categories.existsByNameIgnoreCase(request.name().trim())) {
            throw new IllegalArgumentException("Ya existe una categoría con ese nombre");
        }
        BookCategory c = new BookCategory(request.name().trim());
        c = categories.save(c);
        return new CategoryResponse(c.getId(), c.getName());
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        BookCategory c = categories.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));
        String name = request.name().trim();
        categories.findByNameIgnoreCase(name).ifPresent(other -> {
            if (!other.getId().equals(id)) {
                throw new IllegalArgumentException("Ya existe una categoría con ese nombre");
            }
        });
        c.setName(name);
        return new CategoryResponse(c.getId(), c.getName());
    }

    @Transactional
    public void deleteCategory(Long id) {
        if (!categories.existsById(id)) {
            throw new ResourceNotFoundException("Categoría no encontrada");
        }
        if (books.existsByCategoryId(id)) {
            throw new IllegalStateException("No se puede eliminar: hay libros en esta categoría");
        }
        categories.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<BookResponse> listBooks(Long categoryId) {
        List<Book> list =
                categoryId == null ? books.findAllByOrderByTitleAsc() : books.findByCategoryIdOrderByTitleAsc(categoryId);
        return list.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public BookResponse getBook(Long id) {
        return books.findById(id).map(this::toResponse).orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado"));
    }

    @Transactional
    public BookResponse createBook(BookRequest request) {
        BookCategory category = categories
                .findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));
        Book b = new Book(category, request.title().trim(), request.price(), request.bookType(), request.packageNote());
        applyCompanion(b, request);
        validatePackageNote(b);
        b = books.save(b);
        return toResponse(b);
    }

    @Transactional
    public BookResponse updateBook(Long id, BookRequest request) {
        Book b = books.findById(id).orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado"));
        BookCategory category = categories
                .findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));
        b.setCategory(category);
        b.setTitle(request.title().trim());
        b.setPrice(request.price());
        b.setBookType(request.bookType());
        b.setPackageNote(request.packageNote());
        applyCompanion(b, request);
        validatePackageNote(b);
        b = books.save(b);
        return toResponse(b);
    }

    @Transactional
    public void deleteBook(Long id) {
        if (!books.existsById(id)) {
            throw new ResourceNotFoundException("Libro no encontrado");
        }
        books.deleteById(id);
    }

    private void validatePackageNote(Book b) {
        if (b.getBookType() == BookType.UNITARIO) {
            b.setPackageNote(null);
            b.setCompanionBook(null);
            b.setCompanionLinePrice(null);
            return;
        }
        if (b.getPackageNote() != null && b.getPackageNote().isBlank()) {
            b.setPackageNote(null);
        }
        if (b.getCompanionBook() == null) {
            throw new IllegalArgumentException("Un libro PAQUETE debe tener un complemento configurado en catálogo");
        }
    }

    private void applyCompanion(Book b, BookRequest request) {
        if (request.bookType() == BookType.UNITARIO) {
            b.setCompanionBook(null);
            b.setCompanionLinePrice(null);
            return;
        }
        if (request.companionBookId() == null) {
            throw new IllegalArgumentException("Indique el libro complemento del paquete");
        }
        Book companion = books.findById(request.companionBookId())
                .orElseThrow(() -> new ResourceNotFoundException("Libro complemento no encontrado"));
        if (companion.getBookType() != BookType.UNITARIO) {
            throw new IllegalArgumentException("El complemento debe ser un libro unitario");
        }
        if (b.getId() != null && companion.getId().equals(b.getId())) {
            throw new IllegalArgumentException("El complemento no puede ser el mismo libro");
        }
        b.setCompanionBook(companion);
        b.setCompanionLinePrice(companion.getPrice());
    }

    private BookResponse toResponse(Book b) {
        Book companion = b.getCompanionBook();
        return new BookResponse(
                b.getId(),
                b.getCategory().getId(),
                b.getCategory().getName(),
                b.getTitle(),
                b.getPrice(),
                b.getBookType(),
                b.getPackageNote(),
                companion != null ? companion.getId() : null,
                companion != null ? companion.getTitle() : null,
                b.getCompanionLinePrice());
    }
}
