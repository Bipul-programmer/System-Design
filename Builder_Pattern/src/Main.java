public class Main {
    public static void main(String[] args) {
        User user = new User.Builder().
                name("Bipul")
                .email("bipul@gmail.com")
                .phone("9876543210")
                .age(25)
                .city("Hyderabad")
                .country("India")
                .build();
    }
}