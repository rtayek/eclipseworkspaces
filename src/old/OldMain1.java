package old;
import static java.nio.file.FileVisitResult.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
// strategy:
// find all workspaces
// process one at a time and stash stuff in sets
// look for duplicates and inconsistencies.
// in a workspace:
// .metadata/.plugins/org.eclipse.core.resources/.projects/
// will have a list of projects.
// match these with directories in this workspace
// and look in .location files to see how this uri relates

class LocationFiles extends SimpleFileVisitor<Path> {
    void removeCommon(Path dir) {
        String dirString=dir.toString();
        if(dirString.toString().contains(common)) {
            int start=dirString.indexOf(common);
            if(start!=-1) {
                String x=dirString.substring(0,start);
                String y=dirString.substring(start+common.length());
                System.out.println(x+"..."+y);
            } else {
                System.out.println("no start of common!");
                throw new RuntimeException("oops");
            }
        } else {
            System.out.println("no common!");
            throw new RuntimeException("oops");
        }
    }
    String fix(String string) {
        String rc="";
        for(int i=0;i<string.length();i++) {
            Character c=string.charAt(i);
            if(Character.isAlphabetic(c)) rc+=c;
        }
        return rc;
    }
    public FileVisitResult preVisitDirectory(Path dir,BasicFileAttributes attrs) throws IOException {
        File file=new File(""+dir,".location");
        if(file.exists()) {
            System.out.println(dir);
            boolean result=locationFileDirectories.add(dir);
            removeCommon(dir);
            if(!result) System.out.println("folder: "+dir+" is a duplicate folder!");
            Path path=Paths.get(""+file);
            byte[] bytes=Files.readAllBytes(path);
            String string=new String(bytes,"UTF-8");
            fix(string);
            URI uri=null;
            if(false) try {
                uri=new URI(string);
            } catch(URISyntaxException e) {
                //e.printStackTrace();
                System.out.println("caught: "+e);
                //throw new RuntimeException("oops");
            }
            if(locations.values().contains(string)) {
                System.out.println(path+" has a duplicate!");
                System.out.println(fix(string)+" is a duplicate!");
            }
            locations.put(path,string);
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
    @Override public FileVisitResult visitFileFailed(Path file,IOException exc) {
        System.err.println(exc);
        return CONTINUE;
    }
    Set<Path> locationFileDirectories=new TreeSet<>(); // project dir in metadata
    Map<Path,String> locations=new TreeMap<>(); // uri from .location file
    static final String common=".metadata\\.plugins\\org.eclipse.core.resources\\.projects";
}
public class OldMain1 {
    public static void main(String[] args) throws IOException {
        Path startingDir=Paths.get("d:/ray/dev");
        LocationFiles lf=new LocationFiles();
        Files.walkFileTree(startingDir,lf);
        System.out.println(lf.locationFileDirectories.size()+" location file directories.");
        System.out.println(lf.locations.size()+" locations.");
    }
}
