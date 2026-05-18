package biblioteca.gorbits.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.billing.client-payment-message")
public record ClientPaymentMessageProperties(String currencySymbol, String template) {

    private static final String DEFAULT_TEMPLATE =
            "Estimado/a {cliente}: confirmamos abono de {moneda}{abono} con fecha {fechaAbono}. "
                    + "Guía #{guiaId}, cuota {cuotaSeq}. Pendiente en esta cuota: {moneda}{saldoCuota}. "
                    + "Pendiente total de la guía: {moneda}{saldoGuia}. Campaña: {campana}.";

    public ClientPaymentMessageProperties {
        if (currencySymbol == null || currencySymbol.isBlank()) {
            currencySymbol = "S/";
        }
        if (template == null || template.isBlank()) {
            template = DEFAULT_TEMPLATE;
        }
    }
}
