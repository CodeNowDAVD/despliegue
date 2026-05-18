package biblioteca.gorbits.commercial.web;

import biblioteca.gorbits.commercial.CommercialService;
import biblioteca.gorbits.commercial.CurrentUserService;
import biblioteca.gorbits.commercial.dto.CreateGuideRequest;
import biblioteca.gorbits.commercial.dto.GuideDetailResponse;
import biblioteca.gorbits.commercial.dto.GuideListItemResponse;
import biblioteca.gorbits.commercial.dto.GuideReturnListItemResponse;
import biblioteca.gorbits.commercial.dto.PatchGuideStatusRequest;
import biblioteca.gorbits.commercial.dto.RegisterClientReturnRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/guides")
@PreAuthorize("hasRole('PROVEEDOR')")
public class SalesGuideController {

    private final CommercialService commercialService;
    private final CurrentUserService currentUser;

    public SalesGuideController(CommercialService commercialService, CurrentUserService currentUser) {
        this.commercialService = commercialService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<GuideListItemResponse> list(
            @RequestParam(required = false) Long tagId, @RequestParam(required = false) String q) {
        return commercialService.listGuides(currentUser.requireProveedor(), tagId, q);
    }

    @GetMapping("/returns")
    public List<GuideReturnListItemResponse> listReturns(
            @RequestParam(name = "includeHidden", defaultValue = "false") boolean includeHidden) {
        return commercialService.listGuideReturns(currentUser.requireProveedor(), includeHidden);
    }

    @GetMapping("/{id}")
    public GuideDetailResponse get(@PathVariable Long id) {
        return commercialService.getGuide(currentUser.requireProveedor(), id);
    }

    @PostMapping
    public ResponseEntity<GuideDetailResponse> create(@RequestBody @Valid CreateGuideRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commercialService.createGuide(currentUser.requireProveedor(), request));
    }

    @PostMapping("/{id}/client-return")
    public GuideDetailResponse registerClientReturn(
            @PathVariable Long id, @RequestBody @Valid RegisterClientReturnRequest request) {
        return commercialService.registerClientReturn(currentUser.requireProveedor(), id, request);
    }

    @PatchMapping("/{id}/status")
    public GuideDetailResponse patchStatus(@PathVariable Long id, @RequestBody @Valid PatchGuideStatusRequest request) {
        return commercialService.patchGuideStatus(currentUser.requireProveedor(), id, request);
    }
}
