package biblioteca.gorbits.billing.web;

import biblioteca.gorbits.billing.BillingService;
import biblioteca.gorbits.commercial.CurrentUserService;
import biblioteca.gorbits.billing.dto.CalendarDayResponse;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/billing/calendar")
@PreAuthorize("hasRole('PROVEEDOR')")
public class BillingCalendarController {

    private final BillingService billingService;
    private final CurrentUserService currentUser;

    public BillingCalendarController(BillingService billingService, CurrentUserService currentUser) {
        this.billingService = billingService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<CalendarDayResponse> calendar(
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return billingService.calendar(currentUser.requireProveedor(), from, to);
    }
}
