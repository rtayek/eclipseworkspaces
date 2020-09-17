package p;
//Find all possible positive integers a, b, c, and d such that 
//ab = c+d
//and
//cd = a+b.
public class Puzzle {
    public static void main(String[] args) {
        for(int a=1;a<1000;a++)
            for(int b=1;b<1000;b++)
                for(int c=1;c<1000;c++)
                    for(int d=1;d<1000;d++) //
                        if(a*b==c+d) if(c*d==a+b) System.out.println(a+" "+b+" "+c+" "+d);
    }
}
