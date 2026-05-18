package biblioteca.gorbits.commercial;

import biblioteca.gorbits.commercial.dto.CreateSalesContractTagRequest;
import biblioteca.gorbits.commercial.dto.SalesContractTagResponse;
import biblioteca.gorbits.inventory.InventoryService;
import biblioteca.gorbits.inventory.ProviderStockService;
import biblioteca.gorbits.catalog.Book;
import biblioteca.gorbits.catalog.BookRepository;
import biblioteca.gorbits.catalog.BookType;
import biblioteca.gorbits.catalog.ResourceNotFoundException;
import biblioteca.gorbits.commercial.dto.CampaignResponse;
import biblioteca.gorbits.commercial.dto.ClientRequest;
import biblioteca.gorbits.commercial.dto.ClientResponse;
import biblioteca.gorbits.commercial.dto.ClientReturnInfoResponse;
import biblioteca.gorbits.commercial.dto.CreateGuideRequest;
import biblioteca.gorbits.commercial.dto.GuideDetailResponse;
import biblioteca.gorbits.commercial.dto.GuideLineRequest;
import biblioteca.gorbits.commercial.dto.GuideLineResponse;
import biblioteca.gorbits.commercial.dto.GuideListItemResponse;
import biblioteca.gorbits.commercial.dto.GuideReturnListItemResponse;
import biblioteca.gorbits.commercial.dto.PatchGuideStatusRequest;
import biblioteca.gorbits.commercial.dto.ProviderProfileResponse;
import biblioteca.gorbits.commercial.dto.RegisterClientReturnRequest;
import biblioteca.gorbits.commercial.dto.UpdateProviderProfileRequest;
import biblioteca.gorbits.commercial.dto.ZoneResponse;
import biblioteca.gorbits.user.UserAccount;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommercialService {

    private final SalesZoneRepository zones;
    private final CampaignRepository campaigns;
    private final ProviderProfileRepository profiles;
    private final ClientRepository clients;
    private final SalesGuideRepository guides;
    private final BookRepository books;
    private final SalesContractTagRepository tags;
    private final ProviderStockService providerStock;
    private final InventoryService inventory;

    public CommercialService(
            SalesZoneRepository zones,
            CampaignRepository campaigns,
            ProviderProfileRepository profiles,
            ClientRepository clients,
            SalesGuideRepository guides,
            BookRepository books,
            SalesContractTagRepository tags,
            ProviderStockService providerStock,
            InventoryService inventory) {
        this.zones = zones;
        this.campaigns = campaigns;
        this.profiles = profiles;
        this.clients = clients;
        this.guides = guides;
        this.books = books;
        this.tags = tags;
        this.providerStock = providerStock;
        this.inventory = inventory;
    }

    @Transactional(readOnly = true)
    public List<ZoneResponse> listZones() {
        return zones.findAllByOrderByNameAsc().stream()
                .map(z -> new ZoneResponse(z.getId(), z.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CampaignResponse> listCampaigns() {
        return campaigns.findAllByOrderByStartsOnDesc().stream()
                .map(c -> new CampaignResponse(c.getId(), c.getName(), c.getStartsOn(), c.getEndsOn()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProviderProfileResponse getProviderProfile(UserAccount proveedor) {
        ProviderProfile profile = profiles
                .findWithZoneByUser_Id(proveedor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de embajador no encontrado"));
        return EmbassadorProfileMapper.toResponse(profile, proveedor);
    }

    @Transactional
    public ProviderProfileResponse updateProviderProfile(UserAccount proveedor, UpdateProviderProfileRequest request) {
        SalesZone zone = zones.findById(request.zoneId())
                .orElseThrow(() -> new ResourceNotFoundException("Zona no encontrada"));
        ProviderProfile profile = profiles
                .findWithZoneByUser_Id(proveedor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de embajador no encontrado"));
        profile.setZone(zone);
        profiles.save(profile);
        return EmbassadorProfileMapper.toResponse(profile, proveedor);
    }

    @Transactional(readOnly = true)
    public List<ClientResponse> listClients(UserAccount owner, String query) {
        String q = trimSearchQuery(query);
        List<Client> rows = q == null
                ? clients.findByOwner_IdOrderByFullNameAsc(owner.getId())
                : clients.findByOwner_IdAndFullNameContainingIgnoreCaseOrderByFullNameAsc(owner.getId(), q);
        return rows.stream().map(this::toClientResponse).toList();
    }

    @Transactional(readOnly = true)
    public ClientResponse getClient(UserAccount owner, Long clientId) {
        Client c = clients
                .findByIdAndOwner_Id(clientId, owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
        return toClientResponse(c);
    }

    @Transactional
    public ClientResponse createClient(UserAccount owner, ClientRequest request) {
        Client c = new Client(owner, request.fullName().trim(), emptyToNull(request.phone()), null, emptyToNull(request.addressNote()));
        c = clients.save(c);
        return toClientResponse(c);
    }

    @Transactional
    public ClientResponse updateClient(UserAccount owner, Long clientId, ClientRequest request) {
        Client c = clients
                .findByIdAndOwner_Id(clientId, owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
        c.setFullName(request.fullName().trim());
        c.setPhone(emptyToNull(request.phone()));
        c.setEmail(null);
        c.setAddressNote(emptyToNull(request.addressNote()));
        return toClientResponse(c);
    }

    @Transactional
    public void deleteClient(UserAccount owner, Long clientId) {
        Client c = clients
                .findByIdAndOwner_Id(clientId, owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
        if (guides.existsByClient_Id(clientId)) {
            throw new IllegalStateException("No se puede eliminar: el cliente tiene guías registradas");
        }
        clients.delete(c);
    }

    @Transactional(readOnly = true)
    public List<SalesContractTagResponse> listSalesContractTags(UserAccount owner) {
        return tags.findByOwner_IdOrderByNameAsc(owner.getId()).stream()
                .map(t -> new SalesContractTagResponse(t.getId(), t.getName()))
                .toList();
    }

    @Transactional
    public SalesContractTagResponse createSalesContractTag(UserAccount owner, CreateSalesContractTagRequest request) {
        String name = request.name().trim();
        if (tags.existsByOwner_IdAndNameIgnoreCase(owner.getId(), name)) {
            throw new IllegalArgumentException("Ya existe una etiqueta con ese nombre");
        }
        SalesContractTag tag = tags.save(new SalesContractTag(owner, name));
        return new SalesContractTagResponse(tag.getId(), tag.getName());
    }

    @Transactional(readOnly = true)
    public List<GuideListItemResponse> listGuides(UserAccount owner, Long tagId, String query) {
        String q = trimSearchQuery(query);
        List<SalesGuide> rows;
        if (tagId == null) {
            rows = q == null
                    ? guides.findByOwner_IdOrderByCreatedAtDesc(owner.getId())
                    : guides.findByOwner_IdAndContractNumberContainingIgnoreCaseOrderByCreatedAtDesc(
                            owner.getId(), q);
        } else {
            rows = q == null
                    ? guides.findByOwner_IdAndTag_IdOrderByCreatedAtDesc(owner.getId(), tagId)
                    : guides.findByOwner_IdAndTag_IdAndContractNumberContainingIgnoreCaseOrderByCreatedAtDesc(
                            owner.getId(), tagId, q);
        }
        return rows.stream().map(this::toListItem).toList();
    }

    @Transactional(readOnly = true)
    public GuideDetailResponse getGuide(UserAccount owner, Long guideId) {
        SalesGuide g = guides
                .findDetailedByIdAndOwner_Id(guideId, owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Guía no encontrada"));
        return toDetail(g);
    }

    @Transactional
    public GuideDetailResponse createGuide(UserAccount owner, CreateGuideRequest request) {
        Campaign campaign = campaigns
                .findById(request.campaignId())
                .orElseThrow(() -> new ResourceNotFoundException("Campaña no encontrada"));
        Client client = clients
                .findByIdAndOwner_Id(request.clientId(), owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no pertenece al proveedor"));
        GuideStatus status = request.status() != null ? request.status() : GuideStatus.ACTIVA;
        String contractNumber = request.contractNumber().trim();
        if (guides.existsByOwner_IdAndContractNumber(owner.getId(), contractNumber)) {
            throw new IllegalArgumentException("Ya existe una guía con el contrato " + contractNumber);
        }
        SalesGuide guide = new SalesGuide(
                owner,
                campaign,
                client,
                status,
                contractNumber,
                request.orderDate(),
                Instant.now(),
                emptyToNull(request.note()));

        for (GuideLineRequest line : request.lines()) {
            Book book = books.findById(line.bookId()).orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado"));
            guide.addLine(book, line.quantity(), line.unitPrice());
            addPackageCompanionLine(guide, book, line.quantity());
        }
        guide.setTags(resolveTags(owner, request.tagIds()));

        Map<Long, Integer> qtyByBook = new LinkedHashMap<>();
        for (SalesGuideLine line : guide.getLines()) {
            qtyByBook.merge(line.getBook().getId(), line.getQuantity(), Integer::sum);
        }
        providerStock.ensureAvailable(owner, qtyByBook);

        guide = guides.save(guide);
        inventory.logContractSale(owner, guide.getId(), qtyByBook, guide.getCreatedAt());
        return guides
                .findDetailedByIdAndOwner_Id(guide.getId(), owner.getId())
                .map(this::toDetail)
                .orElseThrow();
    }

    @Transactional(readOnly = true)
    public List<GuideReturnListItemResponse> listGuideReturns(UserAccount owner, boolean includeHidden) {
        return guides.findByOwner_IdAndStatusForReturns(owner.getId(), GuideStatus.DEVUELTA).stream()
                .filter(g -> includeHidden || !g.isClientReturnHidden())
                .map(
                        g -> new GuideReturnListItemResponse(
                                g.getId(),
                                g.getCampaign().getId(),
                                g.getCampaign().getName(),
                                g.getClient().getId(),
                                g.getClient().getFullName(),
                                g.getClientReturnAt(),
                                g.getClientReturnReason(),
                                g.isClientReturnHidden()))
                .toList();
    }

    @Transactional
    public GuideDetailResponse registerClientReturn(
            UserAccount owner, Long guideId, RegisterClientReturnRequest request) {
        SalesGuide g = guides
                .findDetailedByIdAndOwner_Id(guideId, owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Guía no encontrada"));
        GuideLifecycleRules.assertClientReturnAllowed(g);
        Instant at = request.returnedAt() != null ? request.returnedAt() : Instant.now();
        String reason = request.reason().trim();
        if (reason.isEmpty()) {
            throw new IllegalArgumentException("El motivo no puede estar vacío");
        }
        g.setStatus(GuideStatus.DEVUELTA);
        g.setClientReturnMeta(at, reason, request.hideFromReturnList());
        guides.save(g);
        if (request.restoreStockToField()) {
            Map<Long, Integer> qtyByBook = new LinkedHashMap<>();
            for (SalesGuideLine line : g.getLines()) {
                qtyByBook.merge(line.getBook().getId(), line.getQuantity(), Integer::sum);
            }
            inventory.logContractClientReturn(owner, guideId, qtyByBook, at);
        }
        return guides
                .findDetailedByIdAndOwner_Id(guideId, owner.getId())
                .map(this::toDetail)
                .orElseThrow();
    }

    @Transactional
    public GuideDetailResponse patchGuideStatus(UserAccount owner, Long guideId, PatchGuideStatusRequest request) {
        SalesGuide g =
                guides.findByIdAndOwner_Id(guideId, owner.getId()).orElseThrow(() -> new ResourceNotFoundException("Guía no encontrada"));
        GuideLifecycleRules.assertStatusPatchAllowed(g, request.status());
        g.setStatus(request.status());
        guides.save(g);
        return guides
                .findDetailedByIdAndOwner_Id(guideId, owner.getId())
                .map(this::toDetail)
                .orElseThrow();
    }

    private GuideDetailResponse toDetail(SalesGuide g) {
        List<GuideLineResponse> lines = g.getLines().stream().map(this::toLineResponse).toList();
        BigDecimal total = lines.stream()
                .map(GuideLineResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        ClientReturnInfoResponse clientReturn = null;
        if (g.getStatus() == GuideStatus.DEVUELTA && g.getClientReturnAt() != null) {
            clientReturn = new ClientReturnInfoResponse(
                    g.getClientReturnAt(), g.getClientReturnReason(), g.isClientReturnHidden());
        }
        return new GuideDetailResponse(
                g.getId(),
                g.getContractNumber(),
                g.getOrderDate(),
                g.getCampaign().getId(),
                g.getCampaign().getName(),
                g.getClient().getId(),
                g.getClient().getFullName(),
                g.getStatus(),
                g.getCreatedAt(),
                g.getNote(),
                lines,
                total,
                g.getTags().stream().map(SalesContractTag::getName).sorted().toList(),
                clientReturn);
    }

    private GuideListItemResponse toListItem(SalesGuide g) {
        return new GuideListItemResponse(
                g.getId(),
                g.getContractNumber(),
                g.getOrderDate(),
                g.getCampaign().getId(),
                g.getCampaign().getName(),
                g.getClient().getId(),
                g.getClient().getFullName(),
                g.getStatus(),
                g.getCreatedAt(),
                g.getTags().stream().map(SalesContractTag::getName).sorted().toList());
    }

    private Set<SalesContractTag> resolveTags(UserAccount owner, List<Long> tagIds) {
        if (tagIds == null) {
            return Set.of();
        }
        if (tagIds.isEmpty()) {
            return Set.of();
        }
        Set<SalesContractTag> resolved = new HashSet<>();
        for (Long tagId : tagIds) {
            SalesContractTag tag = tags.findByIdAndOwner_Id(tagId, owner.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Etiqueta no encontrada"));
            resolved.add(tag);
        }
        return resolved;
    }

    private void addPackageCompanionLine(SalesGuide guide, Book book, int quantity) {
        if (book.getBookType() != BookType.PAQUETE) {
            return;
        }
        if (book.getCompanionBook() == null) {
            return;
        }
        boolean alreadyOnGuide = guide.getLines().stream()
                .anyMatch(l -> l.getBook().getId().equals(book.getCompanionBook().getId()));
        if (alreadyOnGuide) {
            return;
        }
        Book companion = book.getCompanionBook();
        guide.addLine(companion, quantity, companion.getPrice());
    }

    private GuideLineResponse toLineResponse(SalesGuideLine line) {
        BigDecimal total = line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQuantity()));
        return new GuideLineResponse(
                line.getId(),
                line.getBook().getId(),
                line.getBook().getTitle(),
                line.getBook().getBookType(),
                line.getQuantity(),
                line.getUnitPrice(),
                total);
    }

    private ClientResponse toClientResponse(Client c) {
        return new ClientResponse(c.getId(), c.getFullName(), c.getPhone(), c.getAddressNote());
    }

    private String trimSearchQuery(String query) {
        if (query == null) {
            return null;
        }
        String trimmed = query.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String emptyToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
