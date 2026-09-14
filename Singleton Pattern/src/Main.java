public class Main {
    public static void main(String[] args) {
        SingleTon s1 = SingleTon.getInstance();
        SingleTon s2 = SingleTon.getInstance();

        System.out.println(s1 == s2);
    }
}