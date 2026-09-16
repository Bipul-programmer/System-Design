public class SugarDecorator extends CoffeeDecorator{

    public SugarDecorator(Coffee coffee) {
        super(coffee);
    }

    public double Cost() {
        return coffee.Cost() + 30;
    }

    public String Description() {
        return "This is a Sugar Coffee";
    }
}
