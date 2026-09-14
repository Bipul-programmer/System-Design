import java.util.*;
class PaymentFactory {
    public static Payment createPayment(String type) {
        return switch (type) {
            case "UPI" -> new UPIPayment();
            case "CARD" -> new CardPayment();
            case "CASH" -> new CashPayment();
            default -> throw new IllegalArgumentException(
                    "Unsupported payment type"
            );
        };
    }
}