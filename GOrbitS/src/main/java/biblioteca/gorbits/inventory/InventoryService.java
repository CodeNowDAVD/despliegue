package biblioteca.gorbits.inventory;

import biblioteca.gorbits.catalog.Book;
import biblioteca.gorbits.catalog.BookRepository;
import biblioteca.gorbits.catalog.BookType;
import biblioteca.gorbits.catalog.ResourceNotFoundException;
import biblioteca.gorbits.inventory.ProviderStockService.InvoiceLineAllocation;
import biblioteca.gorbits.inventory.ProviderStockService.ProviderBookStockRow;
import biblioteca.gorbits.inventory.dto.ProviderBookStockRowResponse;
import biblioteca.gorbits.commercial.Campaign;
import biblioteca.gorbits.commercial.CampaignRepository;
import biblioteca.gorbits.commercial.GuideStatus;
import biblioteca.gorbits.commercial.SalesGuideRepository;
import biblioteca.gorbits.inventory.dto.CategoryStockSummaryResponse;
import biblioteca.gorbits.inventory.dto.CreateLibraryPaymentRequest;
import biblioteca.gorbits.inventory.dto.CreateLibraryStockReturnRequest;
import biblioteca.gorbits.inventory.dto.CreateLibrarySupplyInvoiceRequest;
import biblioteca.gorbits.inventory.dto.CreateWithdrawalRequest;
import biblioteca.gorbits.inventory.dto.FieldStockRowResponse;
import biblioteca.gorbits.inventory.dto.InventoryMovementResponse;
import biblioteca.gorbits.inventory.dto.LibraryReconciliationSummaryResponse;
import biblioteca.gorbits.inventory.dto.LibraryStockReturnDetailResponse;
import biblioteca.gorbits.inventory.dto.LibraryStockReturnLineItemRequest;
import biblioteca.gorbits.inventory.dto.LibraryStockReturnLineResponse;
import biblioteca.gorbits.inventory.dto.LibraryStockReturnListItemResponse;
import biblioteca.gorbits.inventory.dto.LibraryPaymentDetailResponse;
import biblioteca.gorbits.inventory.dto.LibraryPaymentListItemResponse;
import biblioteca.gorbits.inventory.dto.LibrarySupplyInvoiceDetailResponse;
import biblioteca.gorbits.inventory.dto.LibrarySupplyInvoiceListItemResponse;
import biblioteca.gorbits.inventory.dto.LibrarySupplyLineItemRequest;
import biblioteca.gorbits.inventory.dto.LibrarySupplyLineResponse;
import biblioteca.gorbits.inventory.dto.SetWarehouseStockRequest;
import biblioteca.gorbits.inventory.dto.WarehouseStockRowResponse;
import biblioteca.gorbits.inventory.dto.WithdrawalDetailResponse;
import biblioteca.gorbits.inventory.dto.WithdrawalLineRequest;
import biblioteca.gorbits.inventory.dto.WithdrawalLineResponse;
import biblioteca.gorbits.inventory.dto.WithdrawalListItemResponse;
import biblioteca.gorbits.user.Role;
import biblioteca.gorbits.user.UserAccount;
import biblioteca.gorbits.user.UserAccountRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private static final BigDecimal ZERO_MONEY = new BigDecimal("0.00");

    private final BookRepository books;
    private final WarehouseStockRepository warehouse;
    private final ProviderFieldStockRepository field;
    private final StockWithdrawalRepository withdrawals;
    private final LibrarySupplyInvoiceRepository libraryInvoices;
    private final LibraryStockReturnRepository libraryStockReturns;
    private final CampaignRepository campaigns;
    private final SalesGuideRepository salesGuides;
    private final LibraryPaymentRepository libraryPayments;
    private final UserAccountRepository userAccounts;
    private final LibrarySupplyInvoiceLineRepository invoiceLines;
    private final LibraryStockReturnLineRepository libraryReturnLines;
    private final InventoryMovementRepository movements;
    private final InventoryMovementLogger movementLogger;
    private final ProviderStockService providerStock;

    public InventoryService(
            BookRepository books,
            WarehouseStockRepository warehouse,
            ProviderFieldStockRepository field,
            StockWithdrawalRepository withdrawals,
            LibrarySupplyInvoiceRepository libraryInvoices,
            LibraryStockReturnRepository libraryStockReturns,
            CampaignRepository campaigns,
            SalesGuideRepository salesGuides,
            LibraryPaymentRepository libraryPayments,
            UserAccountRepository userAccounts,
            LibrarySupplyInvoiceLineRepository invoiceLines,
            LibraryStockReturnLineRepository libraryReturnLines,
            InventoryMovementRepository movements,
            InventoryMovementLogger movementLogger,
            ProviderStockService providerStock) {
        this.books = books;
        this.warehouse = warehouse;
        this.field = field;
        this.withdrawals = withdrawals;
        this.libraryInvoices = libraryInvoices;
        this.libraryStockReturns = libraryStockReturns;
        this.campaigns = campaigns;
        this.salesGuides = salesGuides;
        this.libraryPayments = libraryPayments;
        this.userAccounts = userAccounts;
        this.invoiceLines = invoiceLines;
        this.libraryReturnLines = libraryReturnLines;
        this.movements = movements;
        this.movementLogger = movementLogger;
        this.providerStock = providerStock;
    }

    @Transactional(readOnly = true)
    public List<ProviderBookStockRowResponse> listProviderBookStock(UserAccount owner) {
        return providerStock.stockByBook(owner).stream().map(this::toStockRowResponse).toList();
    }

    @Transactional
    public void logContractSale(UserAccount owner, Long guideId, Map<Long, Integer> quantityByBookId, Instant at) {
        for (var entry : quantityByBookId.entrySet()) {
            Book book = books.findById(entry.getKey()).orElseThrow();
            movementLogger.log(
                    owner,
                    book,
                    InventoryMovementType.GUIDE_SALE,
                    -entry.getValue(),
                    0,
                    "SALES_CONTRACT",
                    guideId,
                    null,
                    at);
        }
    }

    @Transactional
    public void logContractClientReturn(
            UserAccount owner, Long guideId, Map<Long, Integer> quantityByBookId, Instant at) {
        for (var entry : quantityByBookId.entrySet()) {
            Book book = books.findById(entry.getKey()).orElseThrow();
            movementLogger.log(
                    owner,
                    book,
                    InventoryMovementType.CLIENT_RETURN,
                    entry.getValue(),
                    0,
                    "SALES_CONTRACT",
                    guideId,
                    null,
                    at);
        }
    }

    @Transactional(readOnly = true)
    public List<WarehouseStockRowResponse> listWarehouseStock() {
        List<Book> all = books.findAll();
        all.sort(Comparator.comparing(Book::getTitle, String.CASE_INSENSITIVE_ORDER));
        List<WarehouseStockRowResponse> out = new ArrayList<>();
        for (Book b : all) {
            int q = warehouse.findByBook_Id(b.getId()).map(WarehouseStock::getQuantity).orElse(0);
            out.add(new WarehouseStockRowResponse(b.getId(), b.getTitle(), q));
        }
        return out;
    }

    @Transactional
    public void setWarehouseStock(Long bookId, SetWarehouseStockRequest request) {
        Book book = books.findById(bookId).orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado"));
        WarehouseStock row =
                warehouse.findByBook_Id(bookId).orElseGet(() -> new WarehouseStock(book, 0));
        row.setQuantity(request.quantity());
        warehouse.save(row);
    }

    @Transactional(readOnly = true)
    public List<InventoryMovementResponse> listMovements(UserAccount owner) {
        return movements.findByOwner_IdOrderByOccurredAtDescIdDesc(owner.getId()).stream()
                .map(
                        m -> new InventoryMovementResponse(
                                m.getId(),
                                m.getBook().getId(),
                                m.getBook().getTitle(),
                                m.getMovementType(),
                                m.getQuantityDelta(),
                                m.getWarehouseDelta(),
                                m.getFieldDelta(),
                                m.getReferenceType(),
                                m.getReferenceId(),
                                m.getNote(),
                                m.getOccurredAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryStockSummaryResponse> stockSummaryByCategory(UserAccount owner) {
        Map<Long, List<ProviderBookStockRowResponse>> byCategory = new LinkedHashMap<>();
        Map<Long, String> categoryNames = new LinkedHashMap<>();
        for (ProviderBookStockRow row : providerStock.stockByBook(owner)) {
            ProviderBookStockRowResponse dto = toStockRowResponse(row);
            byCategory.computeIfAbsent(row.categoryId(), k -> new ArrayList<>()).add(dto);
            categoryNames.put(row.categoryId(), row.categoryName());
        }
        return byCategory.entrySet().stream()
                .map(e -> {
                    int total = e.getValue().stream().mapToInt(ProviderBookStockRowResponse::available).sum();
                    List<ProviderBookStockRowResponse> books = e.getValue().stream()
                            .sorted(Comparator.comparing(ProviderBookStockRowResponse::bookTitle, String.CASE_INSENSITIVE_ORDER))
                            .toList();
                    return new CategoryStockSummaryResponse(e.getKey(), categoryNames.get(e.getKey()), total, books);
                })
                .sorted(Comparator.comparing(CategoryStockSummaryResponse::categoryName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FieldStockRowResponse> listFieldStock(UserAccount owner) {
        return field.findPositiveByOwner_Id(owner.getId()).stream()
                .map(r -> new FieldStockRowResponse(r.getBook().getId(), r.getBook().getTitle(), r.getQuantity()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WithdrawalListItemResponse> listWithdrawals(UserAccount owner) {
        return withdrawals.findByOwner_IdWithLines(owner.getId()).stream()
                .map(w -> new WithdrawalListItemResponse(
                        w.getId(), w.getCreatedAt(), w.getNote(), totalUnits(w.getLines())))
                .toList();
    }

    @Transactional(readOnly = true)
    public WithdrawalDetailResponse getWithdrawal(UserAccount owner, Long id) {
        StockWithdrawal w = withdrawals
                .findDetailedByIdAndOwner_Id(id, owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Retiro no encontrado"));
        return toWithdrawalDetail(w);
    }

    private WithdrawalDetailResponse toWithdrawalDetail(StockWithdrawal w) {
        List<WithdrawalLineResponse> lines = w.getLines().stream()
                .map(l -> new WithdrawalLineResponse(l.getBook().getId(), l.getBook().getTitle(), l.getQuantity()))
                .toList();
        return new WithdrawalDetailResponse(
                w.getId(), w.getCreatedAt(), w.getNote(), totalUnits(w.getLines()), lines);
    }

    @Transactional
    public WithdrawalDetailResponse registerWithdrawal(UserAccount owner, CreateWithdrawalRequest request) {
        Map<Long, Integer> merged = new LinkedHashMap<>();
        for (WithdrawalLineRequest line : request.lines()) {
            merged.merge(line.bookId(), line.quantity(), Integer::sum);
        }
        List<Long> bookIds = new ArrayList<>(merged.keySet());
        bookIds.sort(Long::compareTo);

        for (Long bookId : bookIds) {
            int qty = merged.get(bookId);
            if (qty <= 0) {
                throw new IllegalArgumentException("Cantidad inválida");
            }
        }

        for (Long bookId : bookIds) {
            int qty = merged.get(bookId);
            Book book = books.findById(bookId).orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado"));

            WarehouseStock ws = warehouse
                    .findForUpdateByBook_Id(bookId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Sin existencias registradas en almacén para el libro: " + book.getTitle()));

            if (ws.getQuantity() < qty) {
                throw new IllegalArgumentException(
                        "Stock insuficiente en almacén para \"" + book.getTitle() + "\" (disponible: "
                                + ws.getQuantity() + ", solicitado: " + qty + ")");
            }
            ws.setQuantity(ws.getQuantity() - qty);
        }

        for (Long bookId : bookIds) {
            int qty = merged.get(bookId);
            Book book = books.findById(bookId).orElseThrow();
            ProviderFieldStock fs = field
                    .findByOwner_IdAndBook_Id(owner.getId(), bookId)
                    .orElseGet(() -> new ProviderFieldStock(owner, book, 0));
            fs.addQuantity(qty);
            field.save(fs);
        }

        String note = request.note() == null ? null : request.note().trim();
        if (note != null && note.isEmpty()) {
            note = null;
        }
        StockWithdrawal w = new StockWithdrawal(owner, Instant.now(), note);
        for (Long bookId : bookIds) {
            int qty = merged.get(bookId);
            Book book = books.findById(bookId).orElseThrow();
            w.addLine(book, qty);
        }
        w = withdrawals.save(w);
        Instant at = w.getCreatedAt();
        for (Long bookId : bookIds) {
            Book book = books.findById(bookId).orElseThrow();
            movementLogger.log(
                    owner,
                    book,
                    InventoryMovementType.WITHDRAWAL_TO_FIELD,
                    -merged.get(bookId),
                    merged.get(bookId),
                    "WITHDRAWAL",
                    w.getId(),
                    note,
                    at);
        }
        StockWithdrawal saved = withdrawals
                .findDetailedByIdAndOwner_Id(w.getId(), owner.getId())
                .orElseThrow();
        return toWithdrawalDetail(saved);
    }

    @Transactional(readOnly = true)
    public List<LibrarySupplyInvoiceListItemResponse> listLibrarySupplyInvoices(
            UserAccount viewer, Long providerIdFilter) {
        List<LibrarySupplyInvoice> rows;
        if (viewer.getRole() == Role.PROVEEDOR) {
            rows = libraryInvoices.findAllWithLinesByOwner_Id(viewer.getId());
        } else if (viewer.getRole() == Role.ADMIN) {
            if (providerIdFilter != null) {
                rows = libraryInvoices.findAllWithLinesByOwner_Id(providerIdFilter);
            } else {
                rows = libraryInvoices.findAllWithLines();
            }
        } else {
            throw new IllegalArgumentException("Rol no soportado para este listado");
        }
        return rows.stream()
                .map(
                        inv -> new LibrarySupplyInvoiceListItemResponse(
                                inv.getId(),
                                inv.getOwner().getId(),
                                inv.getOwner().getUsername(),
                                inv.getInvoiceNumber(),
                                inv.getIssuedOn(),
                                invoiceTotalUnits(inv.getLines()),
                                invoiceLinesTotalAmount(inv.getLines()),
                                inv.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public LibrarySupplyInvoiceDetailResponse getLibrarySupplyInvoice(UserAccount viewer, Long id) {
        LibrarySupplyInvoice inv;
        if (viewer.getRole() == Role.PROVEEDOR) {
            inv = libraryInvoices
                    .findDetailedByIdAndOwner_Id(id, viewer.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Factura no encontrada"));
        } else if (viewer.getRole() == Role.ADMIN) {
            inv = libraryInvoices
                    .findDetailedById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Factura no encontrada"));
        } else {
            throw new IllegalArgumentException("Rol no soportado");
        }
        return toLibrarySupplyDetail(inv);
    }

    @Transactional
    public LibrarySupplyInvoiceDetailResponse registerLibrarySupplyInvoice(
            UserAccount viewer, CreateLibrarySupplyInvoiceRequest request) {

        UserAccount invoiceOwner = resolveInvoiceOwner(viewer, request.ownerUserId());

        String num = request.invoiceNumber().trim();
        if (num.isEmpty()) {
            throw new IllegalArgumentException("El número de factura no puede estar vacío");
        }
        if (libraryInvoices.existsByOwner_IdAndInvoiceNumberIgnoreCase(invoiceOwner.getId(), num)) {
            throw new IllegalArgumentException("Ya existe una factura de librería con ese número para este proveedor");
        }

        validateIncludedBookQuantitiesMatch(request.lines());

        List<ResolvedLibraryLine> expandedLines = resolveLineTotals(request.lines());
        Map<Long, Integer> mergedQty = new LinkedHashMap<>();
        Map<Long, BigDecimal> mergedAmount = new LinkedHashMap<>();
        for (ResolvedLibraryLine line : expandedLines) {
            mergedQty.merge(line.bookId(), line.quantity(), Integer::sum);
            BigDecimal piece = line.lineTotal().setScale(2, RoundingMode.HALF_UP);
            mergedAmount.merge(line.bookId(), piece, BigDecimal::add);
        }
        List<Long> bookIds = new ArrayList<>(mergedQty.keySet());
        Collections.sort(bookIds);
        for (Long bookId : bookIds) {
            if (mergedQty.get(bookId) <= 0) {
                throw new IllegalArgumentException("Cantidad inválida en líneas de factura");
            }
            if (mergedAmount.get(bookId).compareTo(ZERO_MONEY) < 0) {
                throw new IllegalArgumentException("Importe de línea no puede ser negativo");
            }
        }

        String note = request.note() == null ? null : request.note().trim();
        if (note != null && note.isEmpty()) {
            note = null;
        }
        LibrarySupplyInvoice inv =
                new LibrarySupplyInvoice(invoiceOwner, num, request.issuedOn(), note, Instant.now());
        for (Long bookId : bookIds) {
            int qty = mergedQty.get(bookId);
            BigDecimal lineAmt = mergedAmount.get(bookId).setScale(2, RoundingMode.HALF_UP);
            Book book = books.findById(bookId).orElseThrow();
            inv.addLine(book, qty, lineAmt);
        }
        inv = libraryInvoices.save(inv);
        Instant at = inv.getCreatedAt();
        for (Long bookId : bookIds) {
            Book book = books.findById(bookId).orElseThrow();
            movementLogger.log(
                    invoiceOwner,
                    book,
                    InventoryMovementType.LIBRARY_INVOICE_ENTRY,
                    mergedQty.get(bookId),
                    0,
                    "LIBRARY_SUPPLY_INVOICE",
                    inv.getId(),
                    note,
                    at);
        }
        return libraryInvoices
                .findDetailedById(inv.getId())
                .map(this::toLibrarySupplyDetail)
                .orElseThrow();
    }

    @Transactional(readOnly = true)
    public List<LibraryStockReturnListItemResponse> listLibraryStockReturns(UserAccount owner) {
        return libraryStockReturns.findByOwner_IdOrderByCreatedAtDesc(owner.getId()).stream()
                .map(r -> new LibraryStockReturnListItemResponse(
                        r.getId(),
                        r.getCreatedAt(),
                        r.getNote(),
                        returnTotalUnits(r.getLines()),
                        r.getCampaign() != null ? r.getCampaign().getId() : null,
                        r.getCampaign() != null ? r.getCampaign().getName() : null))
                .toList();
    }

    @Transactional(readOnly = true)
    public LibraryStockReturnDetailResponse getLibraryStockReturn(UserAccount owner, Long id) {
        LibraryStockReturn r = libraryStockReturns
                .findDetailedByIdAndOwner_Id(id, owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Devolución a librería no encontrada"));
        return toLibraryStockReturnDetail(r);
    }

    @Transactional
    public LibraryStockReturnDetailResponse registerLibraryStockReturn(
            UserAccount owner, CreateLibraryStockReturnRequest request) {
        Map<Long, Integer> qtyByBook = new LinkedHashMap<>();
        for (LibraryStockReturnLineItemRequest lineReq : request.lines()) {
            qtyByBook.merge(lineReq.bookId(), lineReq.quantity(), Integer::sum);
        }
        List<InvoiceLineAllocation> allocations = new ArrayList<>();
        for (var entry : qtyByBook.entrySet()) {
            allocations.addAll(providerStock.allocateLibraryReturn(owner, entry.getKey(), entry.getValue()));
        }

        Campaign campaign = null;
        if (request.campaignId() != null) {
            campaign = campaigns
                    .findById(request.campaignId())
                    .orElseThrow(() -> new ResourceNotFoundException("Campaña no encontrada"));
        }
        String note = request.note() == null ? null : request.note().trim();
        if (note != null && note.isEmpty()) {
            note = null;
        }
        LibraryStockReturn entity = new LibraryStockReturn(owner, campaign, Instant.now(), note);
        for (InvoiceLineAllocation allocation : allocations) {
            entity.addLine(
                    allocation.invoiceLine(),
                    allocation.invoiceLine().getBook(),
                    allocation.quantity());
        }
        entity = libraryStockReturns.save(entity);
        Instant at = entity.getCreatedAt();
        for (InvoiceLineAllocation allocation : allocations) {
            movementLogger.log(
                    owner,
                    allocation.invoiceLine().getBook(),
                    InventoryMovementType.LIBRARY_STOCK_RETURN,
                    -allocation.quantity(),
                    0,
                    "LIBRARY_STOCK_RETURN",
                    entity.getId(),
                    note,
                    at);
        }
        LibraryStockReturn saved =
                libraryStockReturns.findDetailedByIdAndOwner_Id(entity.getId(), owner.getId()).orElseThrow();
        return toLibraryStockReturnDetail(saved);
    }

    @Transactional(readOnly = true)
    public LibraryReconciliationSummaryResponse libraryReconciliationSummary(UserAccount owner, Long campaignId) {
        long ownerId = owner.getId();
        if (campaignId == null) {
            long purchased = libraryInvoices.sumPurchasedUnitsByOwner(ownerId);
            long returned = libraryStockReturns.sumLineQtyByOwner(ownerId);
            long sold = salesGuides.sumLineQtyForOwnerAndStatus(ownerId, GuideStatus.CERRADA);
            BigDecimal invoiced = libraryInvoices.sumAllLineAmountsByOwner(ownerId);
            BigDecimal deposits = libraryPayments.sumAmountByOwner(ownerId);
            return new LibraryReconciliationSummaryResponse(
                    null,
                    null,
                    null,
                    null,
                    purchased,
                    returned,
                    purchased - returned,
                    sold,
                    scaleMoney(invoiced),
                    scaleMoney(deposits),
                    scaleMoney(invoiced.subtract(deposits)));
        }
        Campaign c = campaigns
                .findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaña no encontrada"));
        long purchased = libraryInvoices.sumLineQtyIssuedBetweenForOwner(ownerId, c.getStartsOn(), c.getEndsOn());
        long returned = libraryStockReturns.sumLineQtyByOwnerAndCampaign(ownerId, campaignId);
        long sold = salesGuides.sumLineQtyForOwnerCampaignAndStatus(ownerId, campaignId, GuideStatus.CERRADA);
        BigDecimal invoiced =
                libraryInvoices.sumLineAmountIssuedBetweenForOwner(ownerId, c.getStartsOn(), c.getEndsOn());
        BigDecimal deposits = libraryPayments.sumAmountByOwnerAndCampaign(ownerId, campaignId);
        return new LibraryReconciliationSummaryResponse(
                c.getId(),
                c.getName(),
                c.getStartsOn(),
                c.getEndsOn(),
                purchased,
                returned,
                purchased - returned,
                sold,
                scaleMoney(invoiced),
                scaleMoney(deposits),
                scaleMoney(invoiced.subtract(deposits)));
    }

    @Transactional(readOnly = true)
    public List<LibraryPaymentListItemResponse> listLibraryPayments(UserAccount owner) {
        return libraryPayments.findForOwnerOrderByPaidOnDesc(owner.getId()).stream()
                .map(
                        p -> new LibraryPaymentListItemResponse(
                                p.getId(),
                                p.getAmount(),
                                p.getPaidOn(),
                                p.getNote(),
                                p.getCampaign() != null ? p.getCampaign().getId() : null,
                                p.getCampaign() != null ? p.getCampaign().getName() : null,
                                p.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public LibraryPaymentDetailResponse getLibraryPayment(UserAccount owner, Long id) {
        LibraryPayment p = libraryPayments
                .findByIdAndOwner_Id(id, owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado"));
        return toLibraryPaymentDetail(p);
    }

    @Transactional
    public LibraryPaymentDetailResponse registerLibraryPayment(UserAccount owner, CreateLibraryPaymentRequest request) {
        Campaign campaign = null;
        if (request.campaignId() != null) {
            campaign = campaigns
                    .findById(request.campaignId())
                    .orElseThrow(() -> new ResourceNotFoundException("Campaña no encontrada"));
        }
        String note = request.note() == null ? null : request.note().trim();
        if (note != null && note.isEmpty()) {
            note = null;
        }
        BigDecimal amt = request.amount().setScale(2, RoundingMode.HALF_UP);
        LibraryPayment p = new LibraryPayment(owner, campaign, amt, request.paidOn(), note, Instant.now());
        p = libraryPayments.save(p);
        return toLibraryPaymentDetail(
                libraryPayments.findByIdAndOwner_Id(p.getId(), owner.getId()).orElseThrow());
    }

    private LibraryPaymentDetailResponse toLibraryPaymentDetail(LibraryPayment p) {
        return new LibraryPaymentDetailResponse(
                p.getId(),
                p.getAmount(),
                p.getPaidOn(),
                p.getNote(),
                p.getCampaign() != null ? p.getCampaign().getId() : null,
                p.getCampaign() != null ? p.getCampaign().getName() : null,
                p.getCreatedAt());
    }

    private static BigDecimal scaleMoney(BigDecimal v) {
        if (v == null) {
            return ZERO_MONEY;
        }
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    private LibraryStockReturnDetailResponse toLibraryStockReturnDetail(LibraryStockReturn r) {
        List<LibraryStockReturnLineResponse> lines = r.getLines().stream()
                .map(
                        l -> new LibraryStockReturnLineResponse(
                                l.getInvoiceLine().getId(),
                                l.getBook().getId(),
                                l.getBook().getTitle(),
                                l.getQuantity()))
                .toList();
        return new LibraryStockReturnDetailResponse(
                r.getId(),
                r.getCreatedAt(),
                r.getNote(),
                returnTotalUnits(r.getLines()),
                r.getCampaign() != null ? r.getCampaign().getId() : null,
                r.getCampaign() != null ? r.getCampaign().getName() : null,
                lines);
    }

    private static int returnTotalUnits(List<LibraryStockReturnLine> lines) {
        return lines.stream().mapToInt(LibraryStockReturnLine::getQuantity).sum();
    }

    private UserAccount resolveInvoiceOwner(UserAccount viewer, Long ownerUserIdFromRequest) {
        if (viewer.getRole() == Role.PROVEEDOR) {
            if (ownerUserIdFromRequest != null && !ownerUserIdFromRequest.equals(viewer.getId())) {
                throw new IllegalArgumentException("No puede asignar una factura a otro proveedor");
            }
            return viewer;
        }
        if (viewer.getRole() == Role.ADMIN) {
            if (ownerUserIdFromRequest == null) {
                throw new IllegalArgumentException("Debe indicar el proveedor (ownerUserId)");
            }
            return userAccounts
                    .findByIdAndRole(ownerUserIdFromRequest, Role.PROVEEDOR)
                    .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado"));
        }
        throw new IllegalArgumentException("No autorizado a registrar facturas de librería");
    }

    private record ResolvedLibraryLine(long bookId, int quantity, BigDecimal lineTotal) {}

    /**
     * Título ** y su libro incluido en la misma factura deben llevar exactamente la misma cantidad
     * (como en la remisión de la librería: 10 + 10, nunca 10 + 6).
     */
    private void validateIncludedBookQuantitiesMatch(List<LibrarySupplyLineItemRequest> lines) {
        Map<Long, Integer> qtyByBook = new LinkedHashMap<>();
        for (LibrarySupplyLineItemRequest line : lines) {
            qtyByBook.merge(line.bookId(), line.quantity(), Integer::sum);
        }
        for (Map.Entry<Long, Integer> entry : qtyByBook.entrySet()) {
            Book book = books.findById(entry.getKey()).orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado"));
            if (book.getBookType() == BookType.PAQUETE && book.getCompanionBook() != null) {
                int mainQty = entry.getValue();
                Long compId = book.getCompanionBook().getId();
                int compQty = qtyByBook.getOrDefault(compId, 0);
                if (compQty == 0) {
                    throw new IllegalArgumentException(
                            "«"
                                    + book.getTitle()
                                    + "» (**) debe ir con «"
                                    + book.getCompanionBook().getTitle()
                                    + "» en la misma cantidad ("
                                    + mainQty
                                    + " unidades).");
                }
                if (compQty != mainQty) {
                    throw new IllegalArgumentException(
                            "«"
                                    + book.getCompanionBook().getTitle()
                                    + "» debe tener "
                                    + mainQty
                                    + " unidades (igual que «"
                                    + book.getTitle()
                                    + "»), no "
                                    + compQty
                                    + ".");
                }
            }
        }
        for (Map.Entry<Long, Integer> entry : qtyByBook.entrySet()) {
            Book lineBook = books.findById(entry.getKey()).orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado"));
            for (Book pkg : books.findByCompanionBook_Id(entry.getKey())) {
                int compQty = entry.getValue();
                int mainQty = qtyByBook.getOrDefault(pkg.getId(), 0);
                if (mainQty == 0) {
                    throw new IllegalArgumentException(
                            "«"
                                    + lineBook.getTitle()
                                    + "» solo se factura junto con «"
                                    + pkg.getTitle()
                                    + "» (**), con la misma cantidad.");
                }
                if (compQty != mainQty) {
                    throw new IllegalArgumentException(
                            "«"
                                    + lineBook.getTitle()
                                    + "» debe tener "
                                    + mainQty
                                    + " unidades (igual que «"
                                    + pkg.getTitle()
                                    + "»), no "
                                    + compQty
                                    + ".");
                }
            }
        }
    }

    /** Una línea de request = una línea en factura (el proveedor replica la remisión de la librería). */
    private List<ResolvedLibraryLine> resolveLineTotals(List<LibrarySupplyLineItemRequest> lines) {
        List<ResolvedLibraryLine> resolved = new ArrayList<>();
        for (LibrarySupplyLineItemRequest line : lines) {
            Book book = books.findById(line.bookId()).orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado"));
            resolved.add(lineTotalForBook(book, line.quantity(), line.discountPercent()));
        }
        return resolved;
    }

    /** Precio unitario del catálogo × cantidad (− descuento opcional por línea). */
    private ResolvedLibraryLine lineTotalForBook(Book book, int quantity, BigDecimal discountPercent) {
        BigDecimal discount = discountPercent != null ? discountPercent : ZERO_MONEY;
        if (discount.compareTo(ZERO_MONEY) < 0 || discount.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Descuento inválido (0–100 %)");
        }
        BigDecimal gross = book.getPrice()
                .multiply(BigDecimal.valueOf(quantity))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal factor = BigDecimal.ONE.subtract(
                discount.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP));
        BigDecimal lineTotal = gross.multiply(factor).setScale(2, RoundingMode.HALF_UP);
        return new ResolvedLibraryLine(book.getId(), quantity, lineTotal);
    }

    private ProviderBookStockRowResponse toStockRowResponse(ProviderBookStockRow row) {
        return new ProviderBookStockRowResponse(
                row.bookId(),
                row.bookTitle(),
                row.categoryId(),
                row.categoryName(),
                row.purchased(),
                row.returnedToLibrary(),
                row.soldOnContracts(),
                row.available());
    }

    private LibrarySupplyInvoiceDetailResponse toLibrarySupplyDetail(LibrarySupplyInvoice inv) {
        List<LibrarySupplyLineResponse> lines = inv.getLines().stream()
                .map(
                        l -> {
                            int returned = libraryReturnLines.sumReturnedQuantityForInvoiceLine(l.getId());
                            int net = l.getQuantity() - returned;
                            return new LibrarySupplyLineResponse(
                                    l.getId(),
                                    l.getBook().getId(),
                                    l.getBook().getTitle(),
                                    l.getQuantity(),
                                    returned,
                                    net,
                                    scaleMoney(l.getLineTotal()));
                        })
                .toList();
        return new LibrarySupplyInvoiceDetailResponse(
                inv.getId(),
                inv.getOwner().getId(),
                inv.getOwner().getUsername(),
                inv.getInvoiceNumber(),
                inv.getIssuedOn(),
                inv.getNote(),
                invoiceTotalUnits(inv.getLines()),
                invoiceLinesTotalAmount(inv.getLines()),
                inv.getCreatedAt(),
                lines);
    }

    private int invoiceTotalUnits(List<LibrarySupplyInvoiceLine> lines) {
        return lines.stream().mapToInt(LibrarySupplyInvoiceLine::getQuantity).sum();
    }

    private BigDecimal invoiceLinesTotalAmount(List<LibrarySupplyInvoiceLine> lines) {
        return lines.stream()
                .map(l -> l.getLineTotal() != null ? l.getLineTotal() : ZERO_MONEY)
                .reduce(ZERO_MONEY, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private int totalUnits(List<StockWithdrawalLine> lines) {
        return lines.stream().mapToInt(StockWithdrawalLine::getQuantity).sum();
    }
}
