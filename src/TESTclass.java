public class TESTclass {
    public static void main(String[] args) {
        boolean l = false;

        for (int i = 0; i < 10; i++){
            System.out.println(i + ": " + l);
            l = !l;
        }
    }
}
