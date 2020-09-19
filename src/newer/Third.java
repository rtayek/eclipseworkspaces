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
        private void init() {
            partition();
            resolveImports();
            analyzeMissing();
            System.out.println("\t"+all.size()+" projects.");
            if(missingAndOnlyInAWorkspace.size()>0)
                System.out.println("\t"+missingAndOnlyInAWorkspace.size()+" missingAndOnlyInAWorkspace: "+shortNames("tmissingAndOnlyInAWorkspace",missingAndOnlyInAWorkspace));
            if(missingAndNotInOnlyInAFolder.size()>0)
                System.out.println("\t"+missingAndNotInOnlyInAFolder.size()+" missingAndNotInOnlyInAFolder: "+shortNames("missingAndNotInOnlyInAFolder",missingAndNotInOnlyInAFolder));
            if(locationFiles.size()>0) System.out.println("\t"+locationFiles.size()+" locationFiles: "+shortNames("\tlocation files: ",locationFiles,2));
            System.out.println("\tpartition");
            System.out.println("\t"+"(in process) both: "+both.size()+" "+both);
            if(onlyInAWorkspace.size()>0) System.out.println("\t"+"(in process) only in a workspace (imported?): "+onlyInAWorkspace.size()+" "+onlyInAWorkspace+" "+path);
            if(onlyInAFolder.size()>0) System.out.println("\t"+"(in process) only in a folder: "+onlyInAFolder.size()+" "+onlyInAFolder);
            if(metaProjects.size()>0)
                System.out.println("\t"+"(in process) metaProjects: "+
            metaProjects.size()+" "+shortNames("metaProjects",metaProjects.keySet()));
            else System.out.println("\t"+"has no projects from metadata!");
            // check for no projects
            System.out.println("\t"+metaProjects.size()+" (.projects) "+projectFolders.size()+" (folders)");
            if(all.size()!=both.size())
                System.out.println("maybe strange.");
        }
        Workspace(Path path,Third third) throws UnsupportedEncodingException,IOException {
            this.path=path;
            this.parent=third;
            File projectsFolder_=new File(path.toFile(),dotProjectsFolder);
            getMetadataProjects(projectsFolder_);
            // ------------------------------------------------------------
            getProjectFolders();
            for(Path file:projectFolders) {
                String filename=file.getFileName().toString();
                if(!parent.allProjectNames.add(filename)) {
                    System.out.println("\t"+file+" is a duplicate project name.");
                    parent.duplicateNames.add(filename);
                }
            }
        }
        void print() {
            System.out.println("partition: "+path);
            System.out.println("\t"+"(in process) both: "+both.size()+" "+both);
            if(onlyInAWorkspace.size()>0) System.out.println("\t"+"(in process) only in a workspace (imported?): "+onlyInAWorkspace.size()+" "+onlyInAWorkspace);
            if(onlyInAFolder.size()>0) System.out.println("\t"+"(in process) only in a folder: "+onlyInAFolder);
            // from middle here
            System.out.println("analyzeMissing: "+path);
        }
        void getMetadataProjects(File projectsFolder) throws IOException,UnsupportedEncodingException {
            if(projectsFolder.exists()) for(File file:projectsFolder.listFiles()) {
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
                    metaProjects.put(file.toPath(),locationFile);
                    locationFiles.add(location);
                } else metaProjects.put(file.toPath(),null); // normal case?
            }
        }
        private void getProjectFolders() {
            for(File file:path.toFile().listFiles())
                if(file.isDirectory()) {
                    String projectName=file.getName();
                    if(!projectName.startsWith(".")&&!projectName.equals(rst)) if(new File(file,".project").exists()) projectFolders.add(file.toPath());
                    else nonProjectFolders.add(file);
                }
        }
        private void partition() {
            for(Path file:metaProjects.keySet())
                metadataSet.add(file.getFileName().toString());
            for(Path file:projectFolders)
                folderSet.add(file.getFileName().toString());
            both=new TreeSet<>(metadataSet);
            both.retainAll(folderSet);
            onlyInAWorkspace=new TreeSet<>(metadataSet);
            onlyInAWorkspace.removeAll(folderSet);
            onlyInAFolder=new TreeSet<>(folderSet);
            onlyInAFolder.removeAll(metadataSet);
            all=new TreeSet<>(onlyInAFolder);
            all.addAll(both);
            all.addAll(onlyInAWorkspace);
        }
        private void checkImports(String folder,Set<Path> importedBy,Path path2,Workspace workspace) {
            if(workspace.onlyInAWorkspace.contains(folder)) {
                //System.out.println(path+" "+folder+" is imported by: "+path2);
                File target=new File(path2.toFile(),dotProjectsFolder);
                File target2=new File(target,folder);
                //System.out.println("potential target: "+target2);
                if(target2.exists()) {
                    if(onlyInAFolder.contains(folder))
                        System.out.println("\t "+folder+" is only in a folder!");
                    else System.out.println("\tmaybe delete: "+target2+".");
                }
                else System.out.println("\ttarget2: "+target2+" does not exist!.");
                importedBy.add(path2);
            } else;//System.out.println(workspace.onlyInAWorkspace+" does not contain: "+folder);
        }
        private void resolveImports() {
            System.out.println("\tresolve imports");
            SortedSet<String> more=new TreeSet<>(both);
            more.addAll(onlyInAFolder);
            for(String folder:more) { // project lives in both
                // maybe look for more in one of the only guys?
                // maybe not, the project lives here.
                // how can clojurec2 live in both????
                //
                // processBuilder lives in chandler3. it is imported by chandler2.
                // so why don't we find it?
                // because there is no processBuilder in the .projects file in chandler3/
                // so it is in the folder projects as opposed to the metadata projects
                // also, it's not in both which is where targets are searched.
                Set<Path> importedBy=new TreeSet<>();
                for(Entry<Path,Workspace> entry:parent.workspaces.entrySet())
                    if(!entry.getKey().equals(path)) {
                        Path path2=entry.getKey();
                        Workspace workspace=entry.getValue();
                        if(workspace.onlyInAWorkspace.size()>0) {
                            //System.out.println("--------");
                            //System.out.println(path+" "+path2);
                            //System.out.println(folder+" in? "+workspace.onlyInAWorkspace);
                            checkImports(folder,importedBy,path2,workspace);
                            // maybe check other sets?
                        }
                    }
                if(importedBy.size()==0);//System.out.println(path+" "+folder+" is not imported by any workspace.");
                else System.out.println("\tis imported by: "+importedBy);
            }
        }
        private void analyzeMissing() { // needs parent for all and missing
            for(String filename:all)
                if(!parent.allProjectNames.contains(filename)) { // will fail badly if duplicates.
                    //System.out.println("project folder: "+filename+" is missing from: "+path);
                    Path missingFile=new File(path.toFile(),filename).toPath();
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
        }
        final Third parent;
        // maybe change this to map of filename to location?
        final Path path;
        SortedSet<String> metadataSet=new TreeSet<>();
        SortedSet<String> folderSet=new TreeSet<>();
        SortedMap<Path,File> metaProjects=new TreeMap<>();
        SortedSet<Path> projectFolders=new TreeSet<>();
        SortedSet<File> nonProjectFolders=new TreeSet<>();
        SortedSet<String> both=new TreeSet<>();
        SortedSet<String> onlyInAWorkspace=new TreeSet<>(); // in this workspace
        SortedSet<String> onlyInAFolder=new TreeSet<>();
        SortedSet<String> all=new TreeSet<>();
        SortedSet<Path> missingAndOnlyInAWorkspace=new TreeSet<>();
        SortedSet<Path> missingAndNotInOnlyInAFolder=new TreeSet<>();
        SortedSet<Path> locationFiles=new TreeSet<>();
    }
    @Override public FileVisitResult preVisitDirectory(Path dir,BasicFileAttributes attrs) throws UnsupportedEncodingException,IOException {
        final File folder=dir.toFile();
        if(folder.getName().startsWith(".")) return SKIP_SUBTREE;
        final File metadataFolder=new File(folder,dotMetadataFolder);
        final boolean isAWorkspace=metadataFolder.exists();
        final File mayBeAProject=new File(folder,dotProjectFilename);
        if(mayBeAProject.exists()) workspacesInProjects.add(dir);
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
            if(true/*||dir.getFileName().toString().contains("chandler")*/) {
                Workspace workspace=new Workspace(dir,this);
                workspaces.put(dir,workspace);
            }
            return SKIP_SUBTREE; // will this miss workspaces and projects in this subtree?
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
    static Set<String> shortNames(String name,Set<Path> paths,int n) {
        Set<String> sortedSet=new LinkedHashSet<>();
        for(Path path:paths)
            sortedSet.add(path.getName(path.getNameCount()-n).toString());
        if(sortedSet.size()<paths.size()) System.out.println("duplicate filename(s)!");
        return sortedSet;
    }
    static Set<String> shortNames(String name,Set<Path> paths) {
        return shortNames(name,paths,1);
    }
    private void print() { // gets called more than once
        System.out.println("<<<<<<<< summary");
        SortedSet<Path> workspaceNames=new TreeSet<>();
        for(Path path:workspaces.keySet())
            if(!workspaceNames.add(path.getFileName())) System.out.println("duplicate workspace name: "+path);
        if(workspaceNames.size()<workspaces.size()) System.out.println("duplicate workspace names!");
        System.out.println(workspaces.size()+" workspaces: "+workspaces.keySet());
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
        System.out.println(workspaces.size()+" workspaces.");
        for(Workspace workspace:workspaces.values()) {
            System.out.println(workspace.path);
            workspace.init();
            System.out.println("----------------------------------");
        }
        System.out.println("&&&&&&&&&&&&&&&&&&&&&&&&&");
        System.out.println(workspaces.size()+" workspaces: "+shortNames("workspaces",workspaces.keySet()));

        //print();
    }
    public static void main(String[] arguments) throws IOException {
        if(arguments==null||arguments.length==0) new Third().run(Paths.get("D:/ray/dev/"));
        else for(String arg:arguments)
            new Third().run(Paths.get("D:/ray/dev/")); // this won'r combine all!
    }
    int totalProjects;
    SortedMap<Path,Workspace> workspaces=new TreeMap<>();
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
