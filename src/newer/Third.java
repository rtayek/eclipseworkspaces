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
        Workspace(Path path) {
            this.path=path;
        }
        void getMetadataProjects(File projectsFolder_) throws IOException,UnsupportedEncodingException {
            if(projectsFolder_.exists()) {
                for(File file:projectsFolder_.listFiles()) {
                    File locationFile=new File(file,".location");
                    if(locationFile.exists()) {
                        Path location=Paths.get(""+locationFile);
                        byte[] bytes=Files.readAllBytes(location);
                        String string=new String(bytes,"UTF-8");
                        String locationString="";
                        for(int i=0;i<string.length();i++) {
                            char c=string.charAt(i);
                            if(32<=c&&c<=126) locationString+=string.charAt(i);
                        }
                        if(false) // screws up printout in console and dos box :(
                            System.out.println("\t"+file.getName()+" has location: "+locationString);
                        metaProjects.put(file,locationFile);
                        // maybe just put the string into the map
                    } else { // normal case?
                        //System.out.println(file.getName()+" has no location");
                        metaProjects.put(file,null);
                    }
                }
            }
            if(metaProjects.size()==0) System.out.println("\t"+path+" has no projects from metadata! *****");
        }
        // maybe change this to map of filename to location?
        final Path path;
        SortedMap<File,File> metaProjects=new TreeMap<>();
        SortedSet<File> projectFolders=new TreeSet<>(); 
        Set<String> both,onlyInAWorkspace,onlyInAFolder; // onlyInAWorkspace in this workspace.
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
            Workspace workspace=new Workspace(dir);
            projectsInWorkspaces.put(dir,workspace);
            if(new File(folder,dotProjectFilename).exists()) workspacesInProjects.add(dir);
            File projectsFolder_=new File(folder,dotProjectsFolder);
            workspace.getMetadataProjects(projectsFolder_);
            getProjectFolders(dir,workspace);
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
    private void getProjectFolders(Path dir,Workspace workspace) {
        for(File file:dir.toFile().listFiles())
            if(file.isDirectory()) {
                String projectName=file.getName();
                if(!projectName.startsWith(".")&&!projectName.equals(rst)) {
                    //System.out.println(projectName+" in "+dir.getFileName()+" may be a project");
                    if(new File(file,".project").exists()) {
                        workspace.projectFolders.add(file);
                    } else;//System.out.println(file+" is not a project!");
                }
            }
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
    private void print() {
        System.out.println("<<<<<<<< summary");
        System.out.println(projectsInWorkspaces.size()+" projectss: "+projectsInWorkspaces.keySet());
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
    private void processEntry(Entry<Path,Workspace> entry) {
        Path path=entry.getKey();
        System.out.println("workspace 1: "+path);
        Workspace workspace=entry.getValue();
        System.out.println(path);
        //System.out.println(pair.projects1);
        //System.out.println(pair.projects2);
        metadataSet.clear();
        for(File file:workspace.metaProjects.keySet())
            metadataSet.add(file.getName());
        folderSet.clear();
        for(File file:workspace.projectFolders)
            folderSet.add(file.getName());
        //System.out.println(set1);
        //System.out.println(set2);
        Set<String> both=new TreeSet<>(metadataSet);
        both.retainAll(folderSet);
        System.out.println("\t"+"(in process) both: "+both);
        Set<String> onlyInAWorkspace=new TreeSet<>(metadataSet);
        onlyInAWorkspace.removeAll(folderSet);
        if(onlyInAWorkspace.size()>0) System.out.println("\t"+"(in process) only in a workspace (imported?): "+onlyInAWorkspace);
        Set<String> onlyInAFolder=new TreeSet<>(folderSet);
        onlyInAFolder.removeAll(metadataSet);
        if(onlyInAFolder.size()>0) System.out.println("\t"+"(in process) only in a folder: "+onlyInAFolder);
        Set<String> either=new TreeSet<>(metadataSet);
        either.addAll(folderSet);
        //System.out.println("either: "+either);
        workspace.both=both;
        workspace.onlyInAFolder=onlyInAFolder;
        workspace.onlyInAWorkspace=onlyInAWorkspace;
        if(onlyInAFolder.size()>0) System.out.println("\t"+"(in process) only in a folder is non empty! ******");
    }
    private void processEntry2(Entry<Path,Workspace> entry) {
        Path path=entry.getKey();
        System.out.println("workspace 2: "+path);
        Workspace helper=entry.getValue();
        for(String folder:helper.both) { // project lives in both
            // maybe look for more in one of the only guys?
            // how can clojurec2 live in both????
            Set<Path> importedBy=new TreeSet<>();
            for(Entry<Path,Workspace> entry2:projectsInWorkspaces.entrySet())
                if(entry2!=entry) {
                    Workspace helper2=entry2.getValue();
                    Path path2=entry2.getKey();
                    if(helper2.onlyInAWorkspace.contains(folder)) {
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
    private int processentry3(Entry<Path,Workspace> entry) {
        Path path=entry.getKey();
        System.out.println("workspace 3: "+path);
        Workspace helper=entry.getValue();
        total+=helper.onlyInAFolder.size()+helper.both.size()+helper.onlyInAWorkspace.size();
        Set<String> allInThisFolder=new TreeSet<>(helper.onlyInAFolder);
        allInThisFolder.addAll(helper.both);
        allInThisFolder.addAll(helper.onlyInAWorkspace);
        for(String filename:allInThisFolder)
            if(!allProjectNames.contains(filename)) { // will fail badly if duplicates.
                //System.out.println("project folder: "+filename+" is missing from: "+path);
                missing.add(new File(path.toFile(),filename));
                File missingFile=new File(path.toFile(),filename);
                if(helper.onlyInAWorkspace.contains(filename)) {
                    //System.out.println(filename+" is missing and only in a workspace "+path);
                    helper.missingAndOnlyInAWorkspace.add(missingFile);
                } else System.out.println("project folder: "+filename+" is missing and not in only in a workspace."); // rare, not seen yet
                if(helper.onlyInAFolder.contains(filename)) System.out.println(filename+" is missing and only in a folder."); // rare, not seen yet
                else {
                    //System.out.println("project folder: "+filename+" is missing and not in only in a folder.");
                    helper.missingAndNotInOnlyInAFolder.add(missingFile);
                }
            }
        //if(allInThisFolder.size()>0) System.out.println(allInThisFolder.iterator().next());
        System.out.println("\t"+allInThisFolder.size()+" projects in: "+path);
        if(helper.missingAndOnlyInAWorkspace.size()>0) System.out.println(helper.missingAndOnlyInAWorkspace.size()+" missingAndOnlyInAWorkspace: "+helper.missingAndOnlyInAWorkspace);
        if(helper.missingAndNotInOnlyInAFolder.size()>0) System.out.println(helper.missingAndNotInOnlyInAFolder.size()+" missingAndNotInOnlyInAFolder: "+helper.missingAndNotInOnlyInAFolder);
        for(Entry<File,File> entry2:helper.metaProjects.entrySet())
            if(entry2.getValue()!=null) {
                String dirname=entry2.getKey().getName();
                String location=entry2.getValue().getName();
                System.out.println("\t"+dirname+" location.");
            }
        return total;
    }
    void run(Path startingDir) throws IOException {
        Files.walkFileTree(startingDir,this);
        System.out.println("after walk.");
        System.out.println("------------------------------------");
        for(Path dir:projectsInWorkspaces.keySet()) {
            Workspace workspace=projectsInWorkspaces.get(dir);
            System.out.println(dir+" "+workspace.metaProjects.size()+" (.projects) "+workspace.projectFolders.size()+" (folders)");
        }
        System.out.println("------------------------------------");
        boolean doOne=false;
        if(doOne) {
            Iterator<Entry<Path,Workspace>> i=projectsInWorkspaces.entrySet().iterator();
            Entry<Path,Workspace> entry=i.next();
            processEntry(entry);
            // check for no projects
            return;
        }
        for(Entry<Path,Workspace> entry:projectsInWorkspaces.entrySet()) {
            processEntry(entry);
            // check for no projects
            System.out.println("-------------------");
        }
        print();
        System.out.println("------------------------------------");
        for(Entry<Path,Workspace> entry:projectsInWorkspaces.entrySet())
            processEntry2(entry);
        print();
        for(Entry<Path,Workspace> entry:projectsInWorkspaces.entrySet())
            total=processentry3(entry);
        System.out.println(missing.size()+" missing: "+missing);
        System.out.println("total: "+total);
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
    // these are similar, maybe combine?
    transient SortedSet<String> metadataSet=new TreeSet<>();
    transient SortedSet<String> folderSet=new TreeSet<>();
    int totalProjects;
    int total;
    SortedMap<Path,Workspace> projectsInWorkspaces=new TreeMap<>();
    SortedSet<Path> nonWorkspaceFolders=new TreeSet<>();
    SortedSet<Path> workspaceFoldersInProjects=new TreeSet<>();
    SortedSet<Path> workspacesInProjects=new TreeSet<>();
    SortedSet<Path> orphanProjects=new TreeSet<>();
    SortedSet<Path> emptyWorkspaces=new TreeSet<>();
    SortedSet<Path> gradleProjects=new TreeSet<>();
    SortedSet<Path> mavenProjects=new TreeSet<>();
    SortedSet<String> allProjectNames=new TreeSet<>();
    SortedSet<String> duplicateNames=new TreeSet<>();
    SortedSet<File> missing=new TreeSet<>();
    //    if(helper.onlyInAFolder.contains(filename)) System.out.println(filename+" is missing and only in a folder."); // rare, not seen yet
    //    else System.out.println("project folder: "+filename+" is missing and not in only in a folder."); 
    //private Map<Path,Set<File>> map=new TreeMap<>(); // projects folder to project folder
    private static final String common=".metadata\\.plugins\\org.eclipse.core.resources\\.projects";
    private static final String rst="RemoteSystemsTempFiles";
    private static final String dotMetadataFolder=".metadata";
    private static final String dotProjectFilename=".project";
    private static final String dotProjectsFolder=".metadata\\.plugins\\org.eclipse.core.resources\\.projects";
}
