public class Multiply{
    public static int mult(int[] arr){
        int result = 1;
        for(int i : arr){
            result *= i;
        }
        return result;
    }
}