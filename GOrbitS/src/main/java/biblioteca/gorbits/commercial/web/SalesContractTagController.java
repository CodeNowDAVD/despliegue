package biblioteca.gorbits.commercial.web;

import biblioteca.gorbits.commercial.CommercialService;
import biblioteca.gorbits.commercial.CurrentUserService;
import biblioteca.gorbits.commercial.dto.CreateSalesContractTagRequest;
import biblioteca.gorbits.commercial.dto.SalesContractTagResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sales-contract-tags")
@PreAuthorize("hasRole('PROVEEDOR')")
public class SalesContractTagController {

    private final CommercialService commercialService;
    private final CurrentUserService currentUser;

    public SalesContractTagController(CommercialService commercialService, CurrentUserService currentUser) {
        this.commercialService = commercialService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<SalesContractTagResponse> list() {
        return commercialService.listSalesContractTags(currentUser.requireProveedor());
    }

    @PostMapping
    public ResponseEntity<SalesContractTagResponse> create(@RequestBody @Valid CreateSalesContractTagRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commercialService.createSalesContractTag(currentUser.requireProveedor(), request));
    }
}
