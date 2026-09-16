public class Main {

    public static void main(String[] args) {

        Coffee coffee = new SimpleCoffee();

        coffee = new MilkDecorator(coffee);

        coffee = new SugarDecorator(coffee);

        coffee = new WhippedCreamDecorator(coffee);

        System.out.println(coffee.Description());
        System.out.println(coffee.Cost());
    }
}