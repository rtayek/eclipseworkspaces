package newer;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.Map.Entry;
import static java.nio.file.FileVisitResult.*;
import java.io.*;
class Third extends SimpleFileVisitor<Path> {
    static class Helper {
        Helper(Set<File> metaProjects,Set<File> folderProjects) {
            this.metaProjects=metaProjects;
            this.folderProjects=folderProjects;
        }
        @Override public String toString() {
            return "Helper [metaProjects="+metaProjects+", folderProjects="+folderProjects+" ... ]";
        }
        final Set<File> metaProjects; // from workspace/.metadata ... /.projects folder
        final Set<File> folderProjects; // from folders in workspace;
        Set<String> both,onlyInAWorkspace,onlyInAFolder;
    }
    @Override public FileVisitResult preVisitDirectory(Path dir,BasicFileAttributes attrs) throws UnsupportedEncodingException,IOException {
        File folder=dir.toFile();
        if(folder.getName().startsWith(".")) return SKIP_SUBTREE;
        final File metadataFolder=new File(folder,dotMetadataFolder);
        final boolean isAWorkspace=metadataFolder.exists();
        final File maybe=new File(folder,dotProjectFilename);
        final boolean isAProject=maybe.exists();
        final boolean isAGradleProject=new File(folder,"build.gradle").exists();
        final boolean isAMavenProject=new File(folder,"pom.xml").exists();
        // .vscode
        if(folder.toString().endsWith("ies\\util")) {
            int x;
            x=2;
        }
        if(isAWorkspace&&isAProject) {
            System.out.println(metadataFolder);
            System.out.println(maybe);
            System.out.println(dir+"both! ******************************");
            throw new RuntimeException("both!");
        }
        if(isAWorkspace) { // it's a workspace, but may be empty,  might have folders that are NOT projects! these folders MIGHT have workspaces!
            if(new File(folder,dotProjectFilename).exists()) workspacesAreProjects.add(dir);
            File projectsFolder_=new File(folder,dotProjectsFolder);
            SortedSet<File> metaProjects=new TreeSet<>();
            SortedSet<File> projectFolders=new TreeSet<>();
            if(projectsFolder_.exists()) metaProjects.addAll(Arrays.asList(projectsFolder_.listFiles()));
            if(metaProjects.size()==0) System.out.println(dir+" has no projects from metadata! *****");
            File[] files=dir.toFile().listFiles();
            for(File file:files)
                if(file.isDirectory()) {
                    String projectName=file.getName();
                    if(!projectName.startsWith(".")&&!projectName.equals(rst)) {
                        //System.out.println(projectName+" in "+dir.getFileName()+" may be a project");
                        if(new File(file,".project").exists()) {
                            projectFolders.add(file);
                        } else;//System.out.println(file+" is not a project!");
                    }
                }
            projectsInWorkspaces.put(dir,new Helper(metaProjects,projectFolders));
            for(File file:projectFolders) {
                String filename=file.getName();
                if(!allProjectNames.add(filename)) {
                    System.out.println(file+" is a duplicate project name.");
                    duplicateNames.add(filename);
                }
            }
            return SKIP_SUBTREE;
        } else {
            if(isAProject) { // this is an isolated project! - treat as such!
                if(metadataFolder.exists()) // it's also a workspace
                    workspaceFoldersInProjects.add(dir); // never happen?
                orphanProjects.add(dir);
            } else if(isAGradleProject) {
                gradleProjects.add(dir);
                orphanProjects.add(dir);
            } else if(isAMavenProject) {
                mavenProjects.add(dir);
                orphanProjects.add(dir);
            }
        }
        return CONTINUE;
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
    private void print() {
        System.out.println(projectsInWorkspaces.size()+" workspaces: "+projectsInWorkspaces.keySet());
        if(emptyWorkspaces.size()>0) System.out.println(emptyWorkspaces.size()+" empty Workspaces: "+emptyWorkspaces);
        if(workspaceFoldersInProjects.size()>0) System.out.println(workspaceFoldersInProjects.size()+" workspace folders in projects: "+workspaceFoldersInProjects);
        if(orphanProjects.size()>0) System.out.println(orphanProjects.size()+" orphans: "+orphanProjects);
        if(gradleProjects.size()>0) System.out.println(gradleProjects.size()+" gradle projects: "+gradleProjects);
        if(mavenProjects.size()>0) System.out.println(mavenProjects.size()+" maven projects: "+mavenProjects);
        System.out.println(duplicateNames.size()+" duplicate project names: "+duplicateNames);
        System.out.println(allProjectNames.size()+" project names.");
    }
    private void processEntry(Entry<Path,Helper> entry) {
        Path path=entry.getKey();
        Helper pair=entry.getValue();
        System.out.println(path);
        //System.out.println(pair.projects1);
        //System.out.println(pair.projects2);
        metadataSet.clear();
        for(File file:pair.metaProjects)
            metadataSet.add(file.getName());
        folderSet.clear();
        for(File file:pair.folderProjects)
            folderSet.add(file.getName());
        //System.out.println(set1);
        //System.out.println(set2);
        Set<String> both=new TreeSet<>(metadataSet);
        both.retainAll(folderSet);
        System.out.println("both: "+both);
        Set<String> onlyInAWorkspace=new TreeSet<>(metadataSet);
        onlyInAWorkspace.removeAll(folderSet);
        if(onlyInAWorkspace.size()>0) System.out.println("only in a workspace (imported?): "+onlyInAWorkspace);
        Set<String> onlyInAFolder=new TreeSet<>(folderSet);
        onlyInAFolder.removeAll(metadataSet);
        if(onlyInAFolder.size()>0) System.out.println("only in a folder: "+onlyInAFolder);
        Set<String> either=new TreeSet<>(metadataSet);
        either.addAll(folderSet);
        //System.out.println("either: "+either);
        pair.both=both;
        pair.onlyInAFolder=onlyInAFolder;
        pair.onlyInAWorkspace=onlyInAWorkspace;
        if(either.size()==0) {
            System.out.println("no projects!");
            emptyWorkspaces.add(path);
        }
        if(onlyInAFolder.size()>0) System.out.println("only in a folder is non empty! ******");
    }
    void run(Path startingDir) throws IOException {
        Files.walkFileTree(startingDir,this);
        System.out.println("------------------------------------");
        for(Path dir:projectsInWorkspaces.keySet()) {
            Helper pair=projectsInWorkspaces.get(dir);
            System.out.println(dir+" "+pair.metaProjects.size()+"(.projects) "+pair.folderProjects.size()+"(folders)");
        }
        System.out.println("------------------------------------");
        boolean doOne=false;
        if(doOne) {
            Iterator<Entry<Path,Helper>> i=projectsInWorkspaces.entrySet().iterator();
            Entry<Path,Helper> entry=i.next();
            processEntry(entry);
            return;
        }
        for(Entry<Path,Helper> entry:projectsInWorkspaces.entrySet()) {
            processEntry(entry);
            System.out.println("-------------------");
        }
        print();
        System.out.println("------------------------------------");
        for(Entry<Path,Helper> entry:projectsInWorkspaces.entrySet()) {
            Path path=entry.getKey();
            Helper helper=entry.getValue();
            for(String folder:helper.both) { // project lives in folder?
                Set<Path> importedBy=new TreeSet<>();
                for(Entry<Path,Helper> entry2:projectsInWorkspaces.entrySet())
                    if(entry2!=entry) {
                        Helper helper2=entry2.getValue();
                        Path path2=entry2.getKey();
                        if(helper2.onlyInAWorkspace.contains(folder)) {
                            System.out.println(path+" "+folder+" is imported by: "+path2);
                            File target=new File(path2.toFile(),dotProjectsFolder);
                            File target2=new File(target,folder);
                            if(target2.exists()) System.out.println("maybe delete: "+target2);
                            else System.out.println("target2: "+target2+" does not exist!.");
                            importedBy.add(path2);
                        }
                    }
                if(importedBy.size()==0);//System.out.println(path+" "+folder+" is not imported by any workspace.");
                else System.out.println("are imported by: "+importedBy);
            }
        }
        print();
    }
    public static void main(String[] args) throws IOException {
        if(true) {
            new Third().run(Paths.get("D:/ray/dev/"));
        } else {
            File file=new File("D:/ray/dev/");
            String[] filenames=file.list();
            for(String filename:filenames) {
                File dir=new File(file,filename);
                System.out.println(dir);
                if(dir.exists()&&dir.isDirectory()) {
                    Path path=Paths.get(dir.toString());
                    new Third().run(path);
                    System.out.println("---------------------------------------");
                }
            }
        }
    }
    transient SortedSet<String> metadataSet=new TreeSet<>();
    transient SortedSet<String> folderSet=new TreeSet<>();
    int totalProjects;
    SortedMap<Path,Helper> projectsInWorkspaces=new TreeMap<>();
    SortedSet<Path> nonWorkspaceFolders=new TreeSet<>();
    SortedSet<Path> workspaceFoldersInProjects=new TreeSet<>();
    SortedSet<Path> workspacesAreProjects=new TreeSet<>();
    SortedSet<Path> orphanProjects=new TreeSet<>();
    SortedSet<Path> emptyWorkspaces=new TreeSet<>();
    SortedSet<Path> gradleProjects=new TreeSet<>();
    SortedSet<Path> mavenProjects=new TreeSet<>();
    SortedSet<String> allProjectNames=new TreeSet<>();
    SortedSet<String> duplicateNames=new TreeSet<>();
    //private Map<Path,Set<File>> map=new TreeMap<>(); // projects folder to project folder
    private static final String common=".metadata\\.plugins\\org.eclipse.core.resources\\.projects";
    private static final String rst="RemoteSystemsTempFiles";
    private static final String dotMetadataFolder=".metadata";
    private static final String dotProjectFilename=".project";
    private static final String dotProjectsFolder=".metadata\\.plugins\\org.eclipse.core.resources\\.projects";
}
