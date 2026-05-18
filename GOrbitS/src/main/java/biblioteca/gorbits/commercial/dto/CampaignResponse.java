package biblioteca.gorbits.commercial.dto;

import java.time.LocalDate;

public record CampaignResponse(Long id, String name, LocalDate startsOn, LocalDate endsOn) {
}
