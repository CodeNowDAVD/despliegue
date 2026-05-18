package biblioteca.gorbits.commercial;

/** Reglas de negocio para contratos devueltos (solo lectura en cobranza y estado). */
public final class GuideLifecycleRules {

    private GuideLifecycleRules() {}

    public static boolean isReturned(SalesGuide guide) {
        return guide.getStatus() == GuideStatus.DEVUELTA || guide.getClientReturnAt() != null;
    }

    public static void assertMutable(SalesGuide guide) {
        if (isReturned(guide)) {
            throw new IllegalStateException(
                    "El contrato está devuelto: no se pueden registrar pagos ni modificar cuotas");
        }
    }

    public static void assertStatusPatchAllowed(SalesGuide guide, GuideStatus requested) {
        if (isReturned(guide)) {
            throw new IllegalStateException("El contrato devuelto no permite cambiar el estado");
        }
        if (requested == GuideStatus.DEVUELTA) {
            throw new IllegalStateException(
                    "Para marcar como devuelto use «Registrar devolución del cliente»");
        }
    }

    public static void assertClientReturnAllowed(SalesGuide guide) {
        if (isReturned(guide)) {
            throw new IllegalStateException("La devolución del cliente ya fue registrada");
        }
    }
}
