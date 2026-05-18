package biblioteca.gorbits.commercial.dto;

import biblioteca.gorbits.commercial.GuideStatus;
import jakarta.validation.constraints.NotNull;

public record PatchGuideStatusRequest(@NotNull GuideStatus status) {
}
