import java.util.*;
public class User {
    private String name;
    private String email;
    private String phone;
    private int age;
    private String city;
    private String country;

    private User(Builder builder) {
        this.name = builder.name;
        this.email = builder.email;
        this.phone = builder.phone;
        this.age = builder.age;
        this.city = builder.city;
        this.country = builder.country;
    }

    static class Builder {
        private String name;
        private String email;
        private String phone;
        private int age;
        private String city;
        private String country;

        Builder name(String name) {
            this.name = name;
            return this;
        }

        Builder email(String email) {
            this.email = email;
            return this;
        }

        Builder phone(String phone) {
            this.phone = phone;
            return this;
        }

        Builder age(int age) {
            this.age = age;
            return this;
        }

        Builder city(String city) {
            this.city = city;
            return this;
        }

        Builder country(String country) {
            this.country = country;
            return this;
        }

        User build() {
            return new User(this);
        }
    }
}
