public class MilkDecorator extends CoffeeDecorator {
    public MilkDecorator(Coffee coffee) {
        super(coffee);
    }

    public double Cost() {
        return coffee.Cost() + 20;
    }

    public String Description() {
        return coffee.Description() + ", Milk";
    }
}
