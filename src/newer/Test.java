package newer;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
public class Test extends SimpleFileVisitor<Path> {
    private void run(Path path) throws IOException {
        Files.walkFileTree(path,this);
    }
    public static void main(String[] args) throws IOException {
        Test test=new Test();
        test.run(Path.of("d:/ray/dev"));
    }
    @Override public FileVisitResult preVisitDirectory(Path dir,BasicFileAttributes attrs) throws IOException {
        Objects.requireNonNull(dir);
        Objects.requireNonNull(attrs);
        ++level;
        System.out.println(level+" "+dir.getFileName());
        File file=new File(dir.toFile(),".metadata");
        if(file.exists()) {
            --level;
            System.out.println("project: "+dir);
            return FileVisitResult.SKIP_SUBTREE;
        }
        return FileVisitResult.CONTINUE;
    }
    @Override public FileVisitResult visitFile(Path file,BasicFileAttributes attrs) throws IOException {
        Objects.requireNonNull(file);
        Objects.requireNonNull(attrs);
        return FileVisitResult.CONTINUE;
    }
    @Override public FileVisitResult visitFileFailed(Path file,IOException exc) throws IOException {
        Objects.requireNonNull(file);
        throw exc;
    }
    @Override public FileVisitResult postVisitDirectory(Path dir,IOException exc) throws IOException {
        Objects.requireNonNull(dir);
        if(exc!=null) throw exc;
        System.out.println(level+" "+dir.getFileName());
        --level;
        return FileVisitResult.CONTINUE;
    }
    int level;
    static int max=10;
}
