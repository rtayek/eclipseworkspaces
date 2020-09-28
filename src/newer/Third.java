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
        static void printIf(String name,Collection<?> collection,int n) {
            printIf(name,collection,n,true);
        }
        static void printIf(String name,Collection<?> collection,int n,boolean complain) {
            int size=collection.size();
            if(size>0) if(size<n) System.out.println("\t"+collection.size()+" "+name+"(s): "+collection);
            else System.out.println("\t"+collection.size()+" "+name+"(s) ... ");
            else if(complain) System.out.println("\t"+name+" is empty.");
        }
        boolean isNormal() {
            if(all.size()!=both.size()) return false;
            if(all.size()!=metaProjects.size()) return false;
            if(missingAndOnlyInAWorkspace.size()>0) return false;
            if(missingAndNotInOnlyInAFolder.size()>0) return false;
            if(onlyInAWorkspace.size()>0) return false;
            if(onlyInAFolder.size()>0) return false;
            if(locationFiles.size()>0) return false;
            return true;
        }
        private void init() {
            partition();
            resolveImports();
            analyzeMissing();
            if(isNormal()) System.out.println("\tnormal");
            else {
                printIf("all",all,10);
                printIf("missingAndOnlyInAWorkspace",missingAndOnlyInAWorkspace,10);
                if(!missingAndNotInOnlyInAFolder.equals(missingAndOnlyInAWorkspace)) printIf("missingAndNotInOnlyInAFolder",missingAndNotInOnlyInAFolder,10);
                printIf("locationFiles",locationFiles,10,false);
                System.out.println("\tpartition");
                if(!both.equals(metaProjects.keySet())) printIf("both",both,10);
                printIf("onlyInAWorkspace",onlyInAWorkspace,10);
                printIf("onlyInAFolder",onlyInAFolder,10);
                printIf("metaProjects",metaProjects.entrySet(),25);
                printIf("just folders",justAFolders,5);
                printIf("nonProjectFolders",nonProjectFolders,10);
                if(all.size()!=both.size()) System.out.println("strange perhaps.");
            }
            System.out.println("\tend of: "+path);
        }
        Workspace(Path path,Third third) throws UnsupportedEncodingException,IOException {
            this.path=path;
            this.parent=third;
            File file_=new File(path.toFile(),dotProjectsFolderString);
            dotProjectsFolder=file_.exists()?file_:null;
            if(dotProjectsFolder==null) System.out.println("not really a workspace!");
            getMetadataProjects();
            getProjectFolders();
        }
        void print() { // refactor this and all other printouts?
            System.out.println("partition: "+path);
            System.out.println("\t"+"(in process) both: "+both.size()+" "+both);
            if(onlyInAWorkspace.size()>0) System.out.println("\t"+"(in process) only in a workspace (maybeimported2): "+onlyInAWorkspace.size()+" "+onlyInAWorkspace);
            if(onlyInAFolder.size()>0) System.out.println("\t"+"(in process) only in a folder: "+onlyInAFolder);
            // from middle here
            System.out.println("analyzeMissing: "+path);
        }
        static public int indexOf(byte target,byte[] bytes,int fromIdx) {
            for(int i=0;i<bytes.length;i++)
                if(bytes[i]==target) return i;
            return -1;
        }
        static Byte[] toObjects(byte[] bytes) {
            Byte[] b=new Byte[bytes.length];
            Arrays.setAll(b,n->bytes[n]);
            return b;
        }
        private List<Byte> getPrefix(byte[] bytes) {
            Byte[] byteObjects=toObjects(bytes);
            int n=16; // was 19, but the 3 bytes in front of :URI vary:"
            // 0, 0, 0
            // 0, 45, 85
            // 0, 96, 85
            Byte[] startingBytes=new Byte[n];
            System.arraycopy(byteObjects,0,startingBytes,0,n);
            List<Byte> startingBytesList=Arrays.asList(startingBytes);
            return startingBytesList;
        }
        private String getImportedProject(Path dotLocationFile) throws IOException {
            int n=dotLocationFile.getNameCount();
            Path project=dotLocationFile.getName(n-2);
            System.out.println("project name: "+project+" reads: "+dotLocationFile);
            byte[] bytes=Files.readAllBytes(dotLocationFile);
            List<Byte> startingBytesList=getPrefix(bytes);
            if(uriPrefix.add(startingBytesList)) System.out.println("new location pefix added: "+startingBytesList);
            String string=new String(bytes,"UTF-8");
            String locationString="";
            for(int i=0;i<string.length();i++) { // get string just in case we can't do better.
                char c=string.charAt(i);
                if(32<=c&&c<=126) locationString=locationString+string.charAt(i);
            }
            int start=string.indexOf("URI");
            int end=string.indexOf(0,start);
            if(start!=-1&&end!=-1) {
                String mine=string.substring(start,end);
                if(start!=16) System.out.println("strange start: "+start);
                String target3="URI//file:/";
                if(mine.startsWith(target3)) {
                    mine=mine.substring(target3.length());
                    File file=new File(mine);
                    if(file.exists()) System.out.println("imported project: "+file+" exists."+" in workspace: "+path);
                    else System.out.println("imported project: "+file+" loes not exist! "+dotLocationFile+" in workspace: "+path);
                    return file.toString();
                } else System.out.println(mine+" bad uri!"+" "+dotLocationFile+" in workspace:"+path);
            } else {
                System.out.println("one of the indices is -1!"+" "+dotLocationFile+" in workspace: "+path);
            }
            return locationString;
        }
        void getMetadataProjects() throws IOException,UnsupportedEncodingException {
            if(dotProjectsFolder!=null) for(File dotProjectFolder:dotProjectsFolder.listFiles()) {
                File locationFile=new File(dotProjectFolder,".location");
                if(locationFile.exists()) {
                    System.out.println("location file: "+locationFile);
                    Path locationPath=Paths.get(""+locationFile);
                    String locationString=getImportedProject(locationPath);
                    //System.out.println(locationString);
                    metaProjects.put(dotProjectFolder.getName(),locationString);
                    //ystem.out.println("added: "+file.getName()+" "+locationString);
                    locationFiles.add(locationPath.getParent().getFileName()+"/"+locationPath.getFileName());
                } else {
                    metaProjects.put(dotProjectFolder.getName(),null); // normal case?
                    //System.out.println("added: "+file.getName()+" null");
                }
            }
        }
        private void getProjectFolders() {
            for(File folder:path.toFile().listFiles())
                if(folder.isDirectory()) {
                    String projectName=folder.getName();
                    if(false&&projectName.equals(".metadata")) System.out.println("skipping .metadata folder in project folder");
                    if(!projectName.startsWith(".")&&!projectName.equals(rst)) {
                        if(new File(folder,dotProjectFilename).exists()) {
                            projectFolders.add(folder.toPath());
                            String folderName=folder.getName();
                            if(!parent.allProjectNames.add(folderName)) {
                                //System.out.println("\t"+file+" is a duplicate project name.");
                                parent.duplicateProjectNames.add(folderName);
                            }
                        } else nonProjectFolders.add(folder);
                    } else; // starts with a dot
                }
        }
        private void partition() {
            for(String file:metaProjects.keySet())
                metadataSet.add(file.toString());
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
            //System.out.println("metadata: "+metaProjects);
            //System.out.println("folders: "+projectFolders);
        }
        private void checkImports(String folder,Set<Path> importedBy,Path path2) {
            Workspace otherWorkspace=parent.workspaces.get(path2);
            if(otherWorkspace.onlyInAWorkspace.contains(folder)) {
                System.out.println(path+" "+folder+" is importedby1: "+path2);
                File target=new File(path2.toFile(),dotProjectsFolderString);
                File target2=new File(target,folder);
                //System.out.println("potential target: "+target2);
                if(target2.exists()) {
                    if(onlyInAFolder.contains(folder)) System.out.println("\t "+folder+" is only in a folder!");
                    else System.out.println("\tmaybe delete: "+target2+".");
                } else System.out.println("\ttarget2: "+target2+" does not exist!.");
                importedBy.add(path2); // or maybe imports?
            } else;//System.out.println(workspace.onlyInAWorkspace+" does not contain: "+folder);
        }
        private void resolveImports() {
            System.out.println("\tresolve im+ports");
            SortedSet<String> more=new TreeSet<>(both);
            more.addAll(onlyInAFolder);
            for(String folder:more) {
                Set<Path> importedBy=new TreeSet<>();
                for(Entry<Path,Workspace> entry:parent.workspaces.entrySet())
                    if(!entry.getKey().equals(path)) {
                        Path path2=entry.getKey();
                        Workspace workspace=entry.getValue();
                        if(workspace.onlyInAWorkspace.size()>0) {
                            checkImports(folder,importedBy,path2);
                            // maybe check other sets?
                        }
                    }
                printIf("importedBy",importedBy,10,false);
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
        final File dotProjectsFolder;
        SortedSet<Path> justAFolders=new TreeSet<>();
        SortedSet<String> metadataSet=new TreeSet<>();
        SortedSet<String> folderSet=new TreeSet<>();
        SortedMap<String,String> metaProjects=new TreeMap<>();
        SortedSet<Path> projectFolders=new TreeSet<>();
        SortedSet<File> nonProjectFolders=new TreeSet<>();
        SortedSet<String> both=new TreeSet<>();
        SortedSet<String> onlyInAWorkspace=new TreeSet<>(); // in this workspace
        SortedSet<String> onlyInAFolder=new TreeSet<>();
        SortedSet<String> all=new TreeSet<>();
        SortedSet<Path> missingAndOnlyInAWorkspace=new TreeSet<>();
        SortedSet<Path> missingAndNotInOnlyInAFolder=new TreeSet<>();
        SortedSet<String> locationFiles=new TreeSet<>();
        static Set<List<Byte>> uriPrefix=new LinkedHashSet<>(); // not thread save!
    }
    private void inc() {
        ++level;
    }
    private void dec() {
        --level;
    }
    static Set<String> shortNames(String name,Set<Path> paths,int n) {
        Set<String> set=new LinkedHashSet<>();
        for(Path path:paths)
            set.add(path.getName(path.getNameCount()-n).toString());
        if(set.size()<paths.size()) System.out.println("duplicate filename(s)!");
        return set;
    }
    static Set<String> shortNames(String name,Set<Path> paths) {
        return shortNames(name,paths,1);
    }
    private void print() {
        System.out.println("<<<<<<<< summary");
        SortedSet<Path> workspaceNames=new TreeSet<>();
        for(Path path:workspaces.keySet())
            if(!workspaceNames.add(path.getFileName())) System.out.println("duplicate workspace name: "+path);
        if(workspaceNames.size()<workspaces.size()) System.out.println("duplicate workspace names!");
        Workspace.printIf("workspaces",workspaces.keySet(),10);
        Workspace.printIf("workspaceNames",workspaceNames,10);
        Workspace.printIf("emptyWorkspaces",emptyWorkspaces,10);
        Workspace.printIf("workspaceFoldersInProjects",workspaceFoldersInProjects,10);
        Workspace.printIf("someProjects",someProjects,10);
        //workspacesInAProjects
        Workspace.printIf("orphanProjects",orphanProjects,10);
        Workspace.printIf("gradleProjects",gradleProjects,10);
        Workspace.printIf("mavenProjects",mavenProjects,10);
        Workspace.printIf("pythonProjects",pythonProjects,10);
        Workspace.printIf("vsCode projects",vsCodeProjects,10);
        Workspace.printIf("duplicateProjectNames",duplicateProjectNames,10);
        Workspace.printIf("allProjectNames",allProjectNames,10);
        Workspace.printIf("all just folders: ",allJustAFolders,50);
        //System.out.println(allProjectNames.size()+" project names.");
        System.out.println(">>>>>>>>");
    }
    @Override public FileVisitResult preVisitDirectory(Path dir,BasicFileAttributes attrs) throws UnsupportedEncodingException,IOException {
        if(dir.toString().contains("RECYCLE.BIN")) return SKIP_SUBTREE;
        if(dir.toString().contains("justa")) {
            int x;
            x=2;
        }
        FileVisitResult rc=CONTINUE;
        if(verbose) System.out.println(" pre: "+level+"/"+maxLevels+" "+dir);
        if(level>maxLevels) {
            System.out.println("stopping at: "+level+" "+dir);
            //dec(); // why? - because post never gets called
            return SKIP_SUBTREE;
        }
        final File folder=dir.toFile();
        if(folder.getName().startsWith(".")) return SKIP_SUBTREE;
        inc();
        stack.push(dir);
        final File metadataFolder=new File(folder,dotMetadataFolder);
        File dotProjectsFolder=new File(folder,dotProjectsFolderString);
        boolean isAWorkspace=dotProjectsFolder.exists();
        final File mayBeAProject=new File(folder,dotProjectFilename);
        final boolean isAProject=mayBeAProject.exists();
        if(isAProject) someProjects.add(dir);
        final boolean isAGradleProject=new File(folder,"build.gradle").exists();
        final boolean isAMavenProject=new File(folder,"pom.xml").exists();
        final boolean isAPythonProject=new File(folder,"__init__.py").exists()||new File(folder,"__pycache__").exists();
        final boolean isAVSCodeProject=new File(folder,".vscode").exists();
        if(isAWorkspace&&isAProject) {
            System.out.println(metadataFolder);
            System.out.println(mayBeAProject);
            System.out.println(dir+"is both!"); // probably ok
            // as sometimes there is some .metadata in a project
            //throw new RuntimeException("both!");
        }
        if(isAWorkspace) { // it's a workspace, but may be empty,  might have folders that are NOT projects! these folders MIGHT have workspaces!
            Workspace workspace=new Workspace(dir,this);
            stack2.push(workspace);
            workspaces.put(dir,workspace);
            boolean descend=true;
            if(descend) {
                return CONTINUE; // we will get a lot of added just a folders!
            } else {
                dec();
                Path p=stack.pop();
                if(!p.equals(dir)) {
                    System.out.println(p+"!="+dir);
                    throw new RuntimeException("in pre: "+p+"!="+dir);
                }
                return SKIP_SUBTREE; // will this miss workspace and projects in this subtree?
            }
        } else {
            if(isAProject) {
                if(metadataFolder.exists()) // it's also a workspace
                    workspaceFoldersInProjects.add(dir); // never happen?
                else {
                    Path parent=dir.getParent();
                    Workspace workspace=workspaces.get(parent);
                    if(workspace!=null) //
                        ;//System.out.println("parent is a workspace: "+parent);
                    else orphanProjects.add(dir);
                }
            } else if(isAGradleProject) { // and probably not an eclipse project
                gradleProjects.add(dir);
                orphanProjects.add(dir);
            } else if(isAPythonProject) {
                pythonProjects.add(dir);
                orphanProjects.add(dir);
            } else if(isAVSCodeProject) {
                vsCodeProjects.add(dir);
                orphanProjects.add(dir);
            } else if(isAMavenProject) {
                mavenProjects.add(dir);
                orphanProjects.add(dir);
            } else {
                if(!stack2.isEmpty()) {
                    Workspace workspace=stack2.peek();
                    //System.out.println("adding: "+dir.getFileName()+" "+workspace.path.getFileName());
                    workspace.justAFolders.add(dir);
                } else;//System.out.println("omitting: "+dir);
            }
            return CONTINUE;
        }
    }
    @Override public FileVisitResult postVisitDirectory(Path dir,IOException exc) {
        dec();
        Path p=stack.pop();
        if(!stack2.isEmpty()) {
            Workspace workspace=stack2.peek();
            if(dir.equals(workspace.path)) stack2.pop();
        }
        if(verbose) System.out.println("post: "+level+"/"+maxLevels+" "+dir);
        if(!p.equals(dir)) { throw new RuntimeException(p+"!="+dir); }
        if(level>maxLevels) {
            System.out.println("stopping at: "+level+" "+dir);
            //return SKIP_SUBTREE;
        }
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
    void run(List<Path> paths) throws IOException {
        for(Path path:paths) {
            System.out.println("walk: "+path);
            Files.walkFileTree(path,this);
        }
        // add search for all .metadat, .location, .projects etc. ?
        System.out.println("after all walks.");
        System.out.println(workspaces.size()+" workspaces.");
        for(Workspace workspace:workspaces.values()) {
            System.out.println(workspace.path);
            workspace.init();
            System.out.println("----------------------------------");
        }
        print();
        System.out.println(Workspace.uriPrefix.size()+" unique prefixes in .location files.");
        System.out.println(Workspace.uriPrefix);
    }
    public static void main(String[] arguments) throws IOException {
        String[] strings=null;
        if(false) {
            strings=new String[] {"D:/ray/dev/chandler","D:/ray/dev/john","D:/ray/dev/androidapps"};
        } else if(arguments==null||arguments.length==0) {
            strings=new String[] {"D:/ray/main","D:/ray/dev"};
            strings=new String[] {"D:/ray/main"};
            strings=new String[] {"D:/ray/main","D:/ray/dev","d:/dev"};
            strings=new String[] {"D:/ray/dev"};
        } else strings=arguments;
        List<Path> paths=new ArrayList<>();
        for(String string:strings)
            paths.add(Path.of(string));
        System.out.println("using: "+paths);
        new Third().run(paths);
    }
    int totalProjects;
    int level;
    boolean verbose,inAWorkspace;
    Workspace lastWorkspace;
    Stack<Path> stack=new Stack<>(); // just what we need for in-a-workspace!
    Stack<Workspace> stack2=new Stack<>(); // just what we need for in-a-workspace!
    SortedSet<Path> allJustAFolders=new TreeSet<>();
    SortedMap<Path,Workspace> workspaces=new TreeMap<>();
    SortedSet<Path> nonWorkspaceFolders=new TreeSet<>();
    SortedSet<Path> workspaceFoldersInProjects=new TreeSet<>();
    SortedSet<Path> someProjects=new TreeSet<>();
    SortedSet<Path> orphanProjects=new TreeSet<>();
    SortedSet<Path> emptyWorkspaces=new TreeSet<>();
    SortedSet<Path> gradleProjects=new TreeSet<>();
    SortedSet<Path> pythonProjects=new TreeSet<>();
    SortedSet<Path> vsCodeProjects=new TreeSet<>();
    SortedSet<Path> mavenProjects=new TreeSet<>();
    SortedSet<String> allProjectNames=new TreeSet<>();
    SortedSet<String> duplicateProjectNames=new TreeSet<>();
    //    if(helper.onlyInAFolder.contains(filename)) System.out.println(filename+" is missing and only in a folder."); // rare, not seen yet
    //    else System.out.println("project folder: "+filename+" is missing and not in only in a folder."); 
    //private Map<Path,Set<File>> map=new TreeMap<>(); // projects folder to project folder
    static final int maxLevels=100;
    private static final String rst="RemoteSystemsTempFiles";
    private static final String dotMetadataFolder=".metadata";
    private static final String dotProjectFilename=".project";
    private static final String dotProjectsFolderString=".metadata\\.plugins\\org.eclipse.core.resources\\.projects";
}
