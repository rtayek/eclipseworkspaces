package old;
import static java.nio.file.FileVisitResult.*;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
class U {
    public static int getLongestCommonSubsequence(String a,String b) {
        int m=a.length();
        int n=b.length();
        int[][] dp=new int[m+1][n+1];
        for(int i=0;i<=m;i++) {
            for(int j=0;j<=n;j++) {
                if(i==0||j==0) {
                    dp[i][j]=0;
                } else if(a.charAt(i-1)==b.charAt(j-1)) {
                    dp[i][j]=1+dp[i-1][j-1];
                } else {
                    dp[i][j]=Math.max(dp[i-1][j],dp[i][j-1]);
                }
            }
        }
        return dp[m][n];
    }
}
class PrintFiles extends SimpleFileVisitor<Path> {
    // Print information about
    // each type of file.
    public FileVisitResult preVisitDirectory(Path dir,BasicFileAttributes attrs) {
        File file=new File(""+dir,".location");
        if(file.exists()) {
            int n=U.getLongestCommonSubsequence(""+dir,""+previous);
            System.out.println(n+" "+dir.getNameCount()+" "+file);
            previous=dir;
            int l=dir.getNameCount();
            Path projectFoleder=dir.getName(l-1);
            Map.Entry<Path,Path> pairs;
            System.out.println("project folder: "+projectFoleder+" "+dir);
            boolean result=projectFolders.add(projectFoleder);
            if(!result) System.out.println("project folder: "+projectFoleder+" is a duplictae!");
            boolean result2=projectFolders.add(dir);
            if(!result) System.out.println("folder: "+dir+" is a duplictae!");
            String dirString=dir.toString();
            if(dir.toString().contains(common)) {
                int start=dirString.indexOf(common);
                if(start!=-1) {
                    String x=dirString.substring(0,start);
                    String y=dirString.substring(start+common.length());
                    System.out.println(x+" "+y);
                } else {
                    System.out.println("no start of common!");
                    throw new RuntimeException("oops");
                }
            } else {
                System.out.println("no common!");
                throw new RuntimeException("oops");
            }
            return SKIP_SUBTREE;
        }
        return CONTINUE;
    }
    @Override public FileVisitResult visitFile(Path file,BasicFileAttributes attr) {
        if(attr.isSymbolicLink()) {
            System.out.format("Symbolic link: %s ",file);
        } else if(attr.isRegularFile()) {
            ;//System.out.format("Regular file: %s ",file);
        } else {
            System.out.format("Other: %s ",file);
        }
        //System.out.println("("+attr.size()+"bytes)");
        return CONTINUE;
    }
    // Print each directory visited.
    @Override public FileVisitResult postVisitDirectory(Path dir,IOException exc) {
        //System.out.format("Directory: %s%n",dir);
        return CONTINUE;
    }
    // If there is some error accessing
    // the file, let the user know.
    // If you don't override this method
    // and an error occurs, an IOException 
    // is thrown.
    @Override public FileVisitResult visitFileFailed(Path file,IOException exc) {
        System.err.println(exc);
        return CONTINUE;
    }
    Path previous=null;
    Set<Path> projectFolders=new TreeSet<>();
    Set<Path> folders=new TreeSet<>();
    static final String common=".metadata\\.plugins\\org.eclipse.core.resources\\.projects";
}
public class OldMain0 {
    public static void main(String[] args) throws IOException {
        Path startingDir=Paths.get("d:/ray/dev");
        PrintFiles pf=new PrintFiles();
        Files.walkFileTree(startingDir,pf);
    }
}
