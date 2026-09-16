import java.net.CookieHandler;

public class CoffeeDecorator implements Coffee {
    protected Coffee coffee;

    public CoffeeDecorator(Coffee coffee) {
        this.coffee = coffee;
    }

    @Override
    public double Cost() {
        return 0;
    }

    @Override
    public String Description() {
        return "";
    }
}
