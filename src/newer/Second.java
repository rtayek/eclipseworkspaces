package newer;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import static java.nio.file.FileVisitResult.*;
import java.io.*;
class Second extends SimpleFileVisitor<Path> {
    private void processProjectFolder(File file,String projectName) {
        processed.add(file);
        //System.out.println("process project: "+file);
        String projectName_=file.getName();
        boolean added=projectFolders.add(file);
        if(!added) System.out.println(projectName_+" is a duplicate project path!");
        if(!projectNames.add(projectName)) {
            System.out.println(projectName_+" is a duplicate project name!");
            ++n;
            duplicateProjectNames.add(projectName_);
        } else;//System.out.println("added: "+projectName);
    }
    Map<String,String> projectToLocation(Path path) throws IOException,UnsupportedEncodingException {
        Map<String,String> map=new TreeMap<>();
        File projectsFolder=new File(path.toFile(),dotProjectsFolder);
        int oks=0;
        int i=0;
        if(projectsFolder.exists()) {
            File[] projectFolders=projectsFolder.listFiles();
            for(File dir:projectFolders) {
                File file=new File(dir,".location");
                if(file.exists()) {
                    Path location=Paths.get(""+file);
                    byte[] bytes=Files.readAllBytes(location);
                     String string=new String(bytes,"UTF-8");
                    //map.put(project,string);
                    System.out.println(dir.getName()+" has location");
                    oks++;
                } else {
                    if(false) // silent for now. i think most will not have one.
                    System.out.println(dir.getName()+" has no location");
                    //if(map.put(project,null)!=null) System.out.println("duplicate: "+project);
                    //throw new RuntimeException("oops");
                }
            }
        } else System.out.println(projectsFolder+" does not exist!");
        System.out.println("oks: "+oks+"/"+projectFolders.size());
        return map;
    }
    @Override public FileVisitResult preVisitDirectory(Path dir,BasicFileAttributes attrs) throws UnsupportedEncodingException, IOException {
        //System.out.println(dir+" "+attrs);
        File metadataFolder=new File(dir.toFile(),dotMetadataFolder);
        if(new File(dir.toFile(),dotProjectFilename).exists()) {
            if(projectFolders.contains(dir.toFile())) {
                //System.out.println(dir+" already processed");
                return SKIP_SUBTREE;
            } else {
                String projectName=dir.toFile().getName();
                if(projectName.startsWith(".")) return SKIP_SUBTREE;
                if(projectName.equals(rst)) return SKIP_SUBTREE;
                else {
                    processProjectFolder(dir.toFile(),projectName);
                    //System.out.println("skip project: "+projectName+" in: "+dir);
                    //skipped.add(dir.toFile());
                    return CONTINUE;
                }
            }
        } else if(metadataFolder.exists()) { // it's a workspace, but may be empty
                                             // might have folders that are NOT projects!
                                             // these folders MIGHT have workspaces!
            Map<String,String> from=projectToLocation(dir);
            // maybe keep another set of maps or use a pair?
            if(true) return CONTINUE;
            projectFolders.clear();
            File[] files=dir.toFile().listFiles();
            for(File file:files)
                if(file.isDirectory()) { // dir is a workspace
                    String projectName=file.getName();
                    if(!projectName.startsWith(".")&&!projectName.equals(rst)) {
                        //System.out.println(projectName+" in "+dir.getFileName()+" may be a project");
                        if(new File(file,".project").exists()) processProjectFolder(file,projectName);
                        else System.out.println(file+" is not a project!");
                    }
                }
            Set<File> projects=new TreeSet<>();
            projects.addAll(projectFolders);
            map.put(dir,projects);
            totalProjects+=projects.size();
            return CONTINUE;
        } else return CONTINUE;
    }
    @Override public FileVisitResult postVisitDirectory(Path dir,IOException exc) {
        //System.out.println(dir+" "+exc);
        return CONTINUE;
    }
    @Override public FileVisitResult visitFile(Path file,BasicFileAttributes attrs) {
        //System.out.println(file+" "+attrs);
        return CONTINUE;
    }
    @Override public FileVisitResult visitFileFailed(Path file,IOException exc) {
        System.out.println(file+" "+exc);
        return CONTINUE;
    }
    void run(Path startingDir) throws IOException {
        Files.walkFileTree(startingDir,this);
        Set<File> both=new TreeSet<File>(skipped);
        both.retainAll(processed);
        Set<File> onlySkipped=new TreeSet<File>(skipped);
        onlySkipped.removeAll(processed);
        Set<File> onlyProcessed=new TreeSet<File>(processed);
        onlyProcessed.removeAll(skipped);
        for(File file:onlySkipped)
            System.out.println("only skipped: "+file);
        for(File file:onlyProcessed)
            System.out.println("only processed: "+file);
        for(File file:both)
            System.out.println("both: "+file);
        System.out.println(skipped.size()+" skipped.");
        System.out.println(processed.size()+" processed.");
        System.out.println(both.size()+" both: "+both);
        System.out.println(onlySkipped.size()+" only skipped: "+onlySkipped);
        System.out.println(onlyProcessed.size()+" only processed.");
        System.out.println(totalProjects+" total project folders (approximate).");
        System.out.println(projectNames.size()+" project names.");
        //System.out.println(projectNames);
        System.out.println(n+" duplicate project names: "+duplicateProjectNames);
        if(true) return;
        for(Path path:map.keySet()) {
            Set<File> files=map.get(path);
            System.out.println(path+" "+files);
            for(File file:files) {
                System.out.println("\t"+file);
            }
        }
    }
    public static void main(String[] args) throws IOException {
        new Second().run(Paths.get("D:/ray/dev"));
    }
    int n,totalProjects;
    Map<Path,Set<File>> map=new TreeMap<>(); // projects folder to project folder
    Set<String> projectNames=new TreeSet<>(); // all of them
    Set<String> duplicateProjectNames=new TreeSet<>();
    transient Set<File> projectFolders=new TreeSet<>();
    Set<File> skipped=new TreeSet<>();
    Set<File> processed=new TreeSet<>();
    static final String rst="RemoteSystemsTempFiles";
    static final String dotMetadataFolder=".metadata";
    static final String dotProjectFilename=".project";
    static final String dotProjectsFolder=".metadata\\.plugins\\org.eclipse.core.resources\\.projects";
}
