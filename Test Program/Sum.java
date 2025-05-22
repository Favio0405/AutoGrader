public class Sum{
    public static int sum(int[] arr){
        int result = 0;
        for(int i : arr){
            result += i;
        }
        return result;
    }
}