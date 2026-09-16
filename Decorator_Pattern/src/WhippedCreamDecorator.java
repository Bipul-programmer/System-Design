public class WhippedCreamDecorator extends CoffeeDecorator{
    public WhippedCreamDecorator(Coffee coffee) {
        super(coffee);
    }

    public double Cost() {
        return coffee.Cost() + 40;
    }

    public String Description() {
        return "This is a WhippedCream Coffee";
    }
}
