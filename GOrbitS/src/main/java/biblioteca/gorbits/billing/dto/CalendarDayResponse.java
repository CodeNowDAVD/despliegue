package biblioteca.gorbits.billing.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.util.List;

public record CalendarDayResponse(
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate date,
        long pendingCount,
        List<InstallmentCalendarItemResponse> items
) {
}
