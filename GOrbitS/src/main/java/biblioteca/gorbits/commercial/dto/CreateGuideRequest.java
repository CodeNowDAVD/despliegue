package biblioteca.gorbits.commercial.dto;

import biblioteca.gorbits.commercial.GuideStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record CreateGuideRequest(
        @NotNull Long campaignId,
        @NotNull Long clientId,
        @NotBlank @Pattern(regexp = "\\d{6}") String contractNumber,
        @NotNull LocalDate orderDate,
        GuideStatus status,
        @Size(max = 500) String note,
        List<Long> tagIds,
        @NotEmpty @Valid List<GuideLineRequest> lines
) {
}
