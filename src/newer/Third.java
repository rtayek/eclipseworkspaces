package newer;
import java.nio.file.*;
// no more "maybe delete" messages from eclipse workspaces project
// added paths to rapps projects. no maybe! 
// that's because paths is not being looked at, use something in ray/dev/
// problems with string in location
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.Map.Entry;
import static java.nio.file.FileVisitResult.*;
import java.io.*;
class Third extends SimpleFileVisitor<Path> {
    static class Workspace {
        Workspace(Path path,Third third) {
            this.path=path;
            this.parent=third;
        }
        void getMetadataProjects(File projectsFolder_) throws IOException,UnsupportedEncodingException {
            if(projectsFolder_.exists()) for(File file:projectsFolder_.listFiles()) {
                File locationFile=new File(file,".location");
                if(locationFile.exists()) {
                    Path location=Paths.get(""+locationFile);
                    byte[] bytes=Files.readAllBytes(location);
                    String string=new String(bytes,"UTF-8");
                    String locationString="";
                    for(int i=0;i<string.length();i++) {
                        char c=string.charAt(i);
                        if(32<=c&&c<=126) locationString=locationString+string.charAt(i);
                    }
                    metaProjects.put(file,locationFile);
                } else metaProjects.put(file,null); // normal case?
            }
        }
        private void getProjectFolders() {
            for(File file:path.toFile().listFiles())
                if(file.isDirectory()) {
                    String projectName=file.getName();
                    if(!projectName.startsWith(".")&&!projectName.equals(rst)) if(new File(file,".project").exists()) projectFolders.add(file);
                    else nonProjectFolders.add(file);
                }
        }
        private void partition() {
            System.out.println("partition: "+path);
            for(File file:metaProjects.keySet())
                metadataSet.add(file.getName());
            for(File file:projectFolders)
                folderSet.add(file.getName());
            //System.out.println(set1);
            //System.out.println(set2);
            both=new TreeSet<>(metadataSet);
            both.retainAll(folderSet);
            System.out.println("\t"+"(in process) both: "+both);
            onlyInAWorkspace=new TreeSet<>(metadataSet);
            onlyInAWorkspace.removeAll(folderSet);
            if(onlyInAWorkspace.size()>0) System.out.println("\t"+"(in process) only in a workspace (imported?): "+onlyInAWorkspace);
            onlyInAFolder=new TreeSet<>(folderSet);
            onlyInAFolder.removeAll(metadataSet);
            if(onlyInAFolder.size()>0) System.out.println("\t"+"(in process) only in a folder: "+onlyInAFolder);
            //System.out.println("either: "+either);
            if(onlyInAFolder.size()>0) System.out.println("\t"+"(in process) only in a folder is non empty! ******");
        }
        private void resolveImports(SortedMap<Path,Workspace> projectsInWorkspaces) {
            System.out.println("workspace 2: "+path);
            for(String folder:both) { // project lives in both
                // maybe look for more in one of the only guys?
                // how can clojurec2 live in both????
                Set<Path> importedBy=new TreeSet<>();
                for(Entry<Path,Workspace> entry:projectsInWorkspaces.entrySet())
                    if(!entry.getKey().equals(path)) {
                        Workspace workspace=entry.getValue();
                        Path path2=entry.getKey();
                        if(workspace.onlyInAWorkspace.contains(folder)) {
                            System.out.println(path+" "+folder+" is imported by: "+path2);
                            File target=new File(path2.toFile(),dotProjectsFolder);
                            File target2=new File(target,folder);
                            if(target2.exists()) System.out.println("maybe delete: "+target2+" or maybe not.");
                            else System.out.println("target2: "+target2+" does not exist!.");
                            importedBy.add(path2);
                        }
                    }
                if(importedBy.size()==0);//System.out.println(path+" "+folder+" is not imported by any workspace.");
                else System.out.println("is imported by: "+importedBy);
            }
        }
        private void analyzeMissing() { // needs parent for all and missing
            System.out.println("analyzeMissing: "+path);
            Set<String> allInThisFolder=new TreeSet<>(onlyInAFolder);
            allInThisFolder.addAll(both);
            allInThisFolder.addAll(onlyInAWorkspace);
            for(String filename:allInThisFolder)
                if(!parent.allProjectNames.contains(filename)) { // will fail badly if duplicates.
                    //System.out.println("project folder: "+filename+" is missing from: "+path);
                    File missingFile=new File(path.toFile(),filename);
                    if(onlyInAWorkspace.contains(filename)) {
                        //System.out.println(filename+" is missing and only in a workspace "+path);
                        missingAndOnlyInAWorkspace.add(missingFile);
                    } else System.out.println("project folder: "+filename+" is missing and not in only in a workspace."); // rare, not seen yet
                    if(onlyInAFolder.contains(filename)) System.out.println(filename+" is missing and only in a folder."); // rare, not seen yet
                    else {
                        //System.out.println("project folder: "+filename+" is missing and not in only in a folder.");
                        missingAndNotInOnlyInAFolder.add(missingFile);
                    }
                }
            //if(allInThisFolder.size()>0) System.out.println(allInThisFolder.iterator().next());
            System.out.println("\t"+allInThisFolder.size()+" projects in: "+path);
            if(missingAndOnlyInAWorkspace.size()>0) System.out.println(missingAndOnlyInAWorkspace.size()+" missingAndOnlyInAWorkspace: "+missingAndOnlyInAWorkspace);
            if(missingAndNotInOnlyInAFolder.size()>0) System.out.println(missingAndNotInOnlyInAFolder.size()+" missingAndNotInOnlyInAFolder: "+missingAndNotInOnlyInAFolder);
            for(Entry<File,File> entry2:metaProjects.entrySet())
                if(entry2.getValue()!=null) {
                    String dirname=entry2.getKey().getName();
                    String location=entry2.getValue().getName();
                    System.out.println("\t"+dirname+" location.");
                }
        }
        final Third parent;
        // maybe change this to map of filename to location?
        final Path path;
        SortedSet<String> metadataSet=new TreeSet<>();
        SortedSet<String> folderSet=new TreeSet<>();
        SortedMap<File,File> metaProjects=new TreeMap<>();
        SortedSet<File> projectFolders=new TreeSet<>();
        SortedSet<File> nonProjectFolders=new TreeSet<>();
        SortedSet<String> both=new TreeSet<>();
        SortedSet<String> onlyInAWorkspace=new TreeSet<>(); // in this workspace
        SortedSet<String> onlyInAFolder=new TreeSet<>();
        SortedSet<File> missingAndOnlyInAWorkspace=new TreeSet<>();
        SortedSet<File> missingAndNotInOnlyInAFolder=new TreeSet<>();
    }
    @Override public FileVisitResult preVisitDirectory(Path dir,BasicFileAttributes attrs) throws UnsupportedEncodingException,IOException {
        final File folder=dir.toFile();
        if(folder.getName().startsWith(".")) return SKIP_SUBTREE;
        final File metadataFolder=new File(folder,dotMetadataFolder);
        final boolean isAWorkspace=metadataFolder.exists();
        final File mayBeAProject=new File(folder,dotProjectFilename);
        final boolean isAProject=mayBeAProject.exists();
        final boolean isAGradleProject=new File(folder,"build.gradle").exists();
        final boolean isAMavenProject=new File(folder,"pom.xml").exists();
        if(folder.toString().endsWith("ies\\util")) {
            int x;
            x=2;
        }
        if(isAWorkspace&&isAProject) {
            System.out.println(metadataFolder);
            System.out.println(mayBeAProject);
            System.out.println(dir+"both! ******************************");
            throw new RuntimeException("both!");
        }
        if(isAWorkspace) { // it's a workspace, but may be empty,  might have folders that are NOT projects! these folders MIGHT have workspaces!
            Workspace workspace=new Workspace(dir,this);
            Workspaces.put(dir,workspace);
            if(new File(folder,dotProjectFilename).exists()) workspacesInProjects.add(dir);
            File projectsFolder_=new File(folder,dotProjectsFolder);
            workspace.getMetadataProjects(projectsFolder_);
            workspace.getProjectFolders();
            for(File file:workspace.projectFolders) {
                String filename=file.getName();
                if(!allProjectNames.add(filename)) {
                    System.out.println("\t"+file+" is a duplicate project name.");
                    duplicateNames.add(filename);
                }
            }
            return SKIP_SUBTREE;
        } else {
            if(isAProject) { // this is an isolated project! - treat as such!
                if(metadataFolder.exists()) // it's also a workspace
                    workspaceFoldersInProjects.add(dir); // never happen?
                orphanProjects.add(dir);
            } else if(isAGradleProject) { // and probably not an eclipse project
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
        if(attrs.isSymbolicLink()) {
            System.out.format("Symbolic link: %s ",file);
        } else if(attrs.isRegularFile()) {
            ;//System.out.format("Regular file: %s ",file);
        } else {
            System.out.format("Other: %s ",file);
        }
        return CONTINUE;
    }
    @Override public FileVisitResult visitFileFailed(Path file,IOException exc) {
        System.out.println(file+" "+exc);
        return CONTINUE;
    }
    private void print() { // gets called more than once
        System.out.println("<<<<<<<< summary");
        SortedSet<Path> workspaceNames=new TreeSet<>();
        for(Path path:Workspaces.keySet())
            if(!workspaceNames.add(path.getFileName())) System.out.println("duplicate workspace name: "+path);
        if(workspaceNames.size()<Workspaces.size()) System.out.println("duplicate workspace names!");
        System.out.println(Workspaces.size()+" workspaces: "+Workspaces.keySet());
        System.out.println(workspaceNames.size()+" unique workspace names: "+workspaceNames);
        if(emptyWorkspaces.size()>0) System.out.println(emptyWorkspaces.size()+" empty Workspaces: "+emptyWorkspaces);
        if(workspaceFoldersInProjects.size()>0) System.out.println(workspaceFoldersInProjects.size()+" workspace folders in projects: "+workspaceFoldersInProjects);
        if(workspacesInProjects.size()>0) System.out.println(workspacesInProjects.size()+" workspacesInAProjects: "+workspacesInProjects);
        //workspacesInAProjects
        if(orphanProjects.size()>0) System.out.println(orphanProjects.size()+" orphans: "+orphanProjects);
        if(gradleProjects.size()>0) System.out.println(gradleProjects.size()+" gradle projects: "+gradleProjects);
        if(mavenProjects.size()>0) System.out.println(mavenProjects.size()+" maven projects: "+mavenProjects);
        if(duplicateNames.size()>0) System.out.println(duplicateNames.size()+" duplicate project names: "+duplicateNames);
        System.out.println(allProjectNames.size()+" project names.");
        System.out.println(">>>>>>>>");
    }
    void run(Path startingDir) throws IOException {
        Files.walkFileTree(startingDir,this);
        System.out.println("after walk.");
        for(Path dir:Workspaces.keySet()) { // summary
            Workspace workspace=Workspaces.get(dir);
            System.out.println(dir+" "+workspace.metaProjects.size()+" (.projects) "+workspace.projectFolders.size()+" (folders)");
        }
        for(Workspace workspace:Workspaces.values()) {
            workspace.partition();
            // check for no projects
        }
        print();
        System.out.println("------------------------------------");
        for(Entry<Path,Workspace> entry:Workspaces.entrySet())
            entry.getValue().resolveImports(Workspaces);
        print();
        for(Entry<Path,Workspace> entry:Workspaces.entrySet()) {
            Workspace workspace=entry.getValue();
            workspace.analyzeMissing();
            if(entry.getValue().metaProjects.size()==0) System.out.println("\t"+entry.getKey()+" has no projects from metadata! *****");
        }
        print();
    }
    public static void main(String[] arguments) throws IOException {
        if(arguments==null||arguments.length==0) new Third().run(Paths.get("D:/ray/dev/"));
        else for(String arg:arguments)
            new Third().run(Paths.get("D:/ray/dev/")); // this won'r combine all!
    }
    // these are similar, maybe combine?
    int totalProjects;
    SortedMap<Path,Workspace> Workspaces=new TreeMap<>();
    SortedSet<Path> nonWorkspaceFolders=new TreeSet<>();
    SortedSet<Path> workspaceFoldersInProjects=new TreeSet<>();
    SortedSet<Path> workspacesInProjects=new TreeSet<>();
    SortedSet<Path> orphanProjects=new TreeSet<>();
    SortedSet<Path> emptyWorkspaces=new TreeSet<>();
    SortedSet<Path> gradleProjects=new TreeSet<>();
    SortedSet<Path> mavenProjects=new TreeSet<>();
    SortedSet<String> allProjectNames=new TreeSet<>();
    SortedSet<String> duplicateNames=new TreeSet<>();
    //    if(helper.onlyInAFolder.contains(filename)) System.out.println(filename+" is missing and only in a folder."); // rare, not seen yet
    //    else System.out.println("project folder: "+filename+" is missing and not in only in a folder."); 
    //private Map<Path,Set<File>> map=new TreeMap<>(); // projects folder to project folder
    private static final String common=".metadata\\.plugins\\org.eclipse.core.resources\\.projects";
    private static final String rst="RemoteSystemsTempFiles";
    private static final String dotMetadataFolder=".metadata";
    private static final String dotProjectFilename=".project";
    private static final String dotProjectsFolder=".metadata\\.plugins\\org.eclipse.core.resources\\.projects";
}
