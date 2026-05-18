package biblioteca.gorbits.inventory;

import biblioteca.gorbits.catalog.Book;
import biblioteca.gorbits.catalog.BookRepository;
import biblioteca.gorbits.catalog.ResourceNotFoundException;
import biblioteca.gorbits.commercial.SalesGuideLineRepository;
import biblioteca.gorbits.user.UserAccount;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProviderStockService {

    private final LibrarySupplyInvoiceLineRepository invoiceLines;
    private final LibraryStockReturnLineRepository returnLines;
    private final SalesGuideLineRepository guideLines;
    private final BookRepository books;

    public ProviderStockService(
            LibrarySupplyInvoiceLineRepository invoiceLines,
            LibraryStockReturnLineRepository returnLines,
            SalesGuideLineRepository guideLines,
            BookRepository books) {
        this.invoiceLines = invoiceLines;
        this.returnLines = returnLines;
        this.guideLines = guideLines;
        this.books = books;
    }

    @Transactional(readOnly = true)
    public int availableUnits(UserAccount owner, Long bookId) {
        long ownerId = owner.getId();
        int purchased = invoiceLines.sumPurchasedQtyByOwnerAndBook(ownerId, bookId);
        int returned = returnLines.sumReturnedQtyByOwnerAndBook(ownerId, bookId);
        int sold = guideLines.sumSoldQtyByOwnerAndBook(ownerId, bookId);
        return Math.max(0, purchased - returned - sold);
    }

    @Transactional(readOnly = true)
    public void ensureAvailable(UserAccount owner, Map<Long, Integer> quantityByBookId) {
        if (quantityByBookId.isEmpty()) {
            return;
        }
        List<Long> bookIds = new ArrayList<>(quantityByBookId.keySet());
        Collections.sort(bookIds);
        for (Long bookId : bookIds) {
            int need = quantityByBookId.get(bookId);
            if (need <= 0) {
                throw new IllegalArgumentException("Cantidad inválida en líneas de contrato de venta");
            }
            int available = availableUnits(owner, bookId);
            if (available < need) {
                Book book = books.findById(bookId).orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado"));
                throw new IllegalArgumentException(
                        "Stock insuficiente para \"" + book.getTitle() + "\" (disponible: " + available + ", requerido: "
                                + need + "). Registre antes una factura de la librería con ese libro.");
            }
        }
    }

    /** Cantidad aún devolvable a la librería para un libro (sin elegir factura). */
    @Transactional(readOnly = true)
    public int returnableToLibrary(UserAccount owner, Long bookId) {
        return availableUnits(owner, bookId);
    }

    /**
     * Reparte una devolución a la librería en líneas de factura (FIFO por fecha de factura).
     */
    @Transactional(readOnly = true)
    public List<InvoiceLineAllocation> allocateLibraryReturn(UserAccount owner, Long bookId, int totalQty) {
        if (totalQty <= 0) {
            throw new IllegalArgumentException("Cantidad inválida en devolución a librería");
        }
        int max = returnableToLibrary(owner, bookId);
        if (totalQty > max) {
            Book book = books.findById(bookId).orElseThrow();
            throw new IllegalArgumentException(
                    "No puede devolver más de lo disponible para \"" + book.getTitle() + "\" (máximo: " + max + ")");
        }
        List<LibrarySupplyInvoiceLine> candidates =
                invoiceLines.findReturnableLinesForOwnerAndBook(owner.getId(), bookId);
        List<InvoiceLineAllocation> out = new ArrayList<>();
        int remaining = totalQty;
        for (LibrarySupplyInvoiceLine line : candidates) {
            if (remaining <= 0) {
                break;
            }
            int already = returnLines.sumReturnedQuantityForInvoiceLine(line.getId());
            int lineRemaining = line.getQuantity() - already;
            if (lineRemaining <= 0) {
                continue;
            }
            int take = Math.min(lineRemaining, remaining);
            out.add(new InvoiceLineAllocation(line, take));
            remaining -= take;
        }
        if (remaining > 0) {
            throw new IllegalStateException("No se pudo asignar la devolución a líneas de factura");
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<ProviderBookStockRow> stockByBook(UserAccount owner) {
        long ownerId = owner.getId();
        Map<Long, ProviderBookStockRow> rows = new LinkedHashMap<>();
        for (Object[] r : invoiceLines.sumPurchasedByBookForOwner(ownerId)) {
            long bookId = (Long) r[0];
            String title = (String) r[1];
            long categoryId = (Long) r[2];
            String categoryName = (String) r[3];
            int purchased = ((Number) r[4]).intValue();
            rows.put(
                    bookId,
                    new ProviderBookStockRow(bookId, title, categoryId, categoryName, purchased, 0, 0, purchased));
        }
        for (Object[] r : returnLines.sumReturnedByBookForOwner(ownerId)) {
            long bookId = (Long) r[0];
            int returned = ((Number) r[1]).intValue();
            ProviderBookStockRow row = rows.get(bookId);
            if (row != null) {
                rows.put(bookId, row.withReturned(returned));
            }
        }
        for (Object[] r : guideLines.sumSoldByBookForOwner(ownerId)) {
            long bookId = (Long) r[0];
            int sold = ((Number) r[1]).intValue();
            ProviderBookStockRow row = rows.get(bookId);
            if (row != null) {
                rows.put(bookId, row.withSold(sold));
            }
        }
        return rows.values().stream()
                .sorted(Comparator.comparing(ProviderBookStockRow::categoryName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(ProviderBookStockRow::bookTitle, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public record InvoiceLineAllocation(LibrarySupplyInvoiceLine invoiceLine, int quantity) {}

    public record ProviderBookStockRow(
            long bookId,
            String bookTitle,
            long categoryId,
            String categoryName,
            int purchased,
            int returnedToLibrary,
            int soldOnContracts,
            int available) {

        ProviderBookStockRow withReturned(int returned) {
            int avail = Math.max(0, purchased - returned - soldOnContracts);
            return new ProviderBookStockRow(
                    bookId, bookTitle, categoryId, categoryName, purchased, returned, soldOnContracts, avail);
        }

        ProviderBookStockRow withSold(int sold) {
            int avail = Math.max(0, purchased - returnedToLibrary - sold);
            return new ProviderBookStockRow(
                    bookId, bookTitle, categoryId, categoryName, purchased, returnedToLibrary, sold, avail);
        }
    }
}
