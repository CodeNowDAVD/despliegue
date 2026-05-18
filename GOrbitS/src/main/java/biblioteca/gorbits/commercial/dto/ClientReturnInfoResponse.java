package biblioteca.gorbits.commercial.dto;

import java.time.Instant;

/** Datos de la devolución registrada por el proveedor (solo aplica si la guía está DEVUELTA). */
public record ClientReturnInfoResponse(Instant returnedAt, String reason, boolean hiddenFromReturnList) {}
