import java.util.*;

public class Main {
    public static void main(String args[]) {
        Payment payment = PaymentFactory.createPayment("CARD");
        payment.payment();
    }
}