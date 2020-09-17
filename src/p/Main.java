package p;
import static java.nio.file.FileVisitResult.*;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
//strategy:
//find all workspaces
//process one at a time and stash stuff in sets
//look for duplicates and inconsistencies.
//in a workspace:
//.metadata/.plugins/org.eclipse.core.resources/.projects/
//will have a list of projects.
//match these with directories in this workspace
//and look in .location files to see how this uri relates
class FindWorkspaces extends SimpleFileVisitor<Path> {
    public FileVisitResult preVisitDirectory(Path dir,BasicFileAttributes attrs) {
        if(dir.toString().endsWith("\\ray\\dev")) return CONTINUE;
        File folder=new File(""+dir+projectsFolder);
        if(folder.exists()) {
            if(!workspaces.add(dir)) System.out.println(dir+" is a duplicate!");
            return SKIP_SUBTREE;
        } else {
            notWorkspaces.add(dir);
            //System.out.println(dir+" is not a workspace!");
        }
        return SKIP_SUBTREE;
    }
    Set<Path> workspaces=new TreeSet<>();
    Set<Path> notWorkspaces=new TreeSet<>();
    static final String projectsFolder="\\.metadata\\.plugins\\org.eclipse.core.resources\\.projects";
    static final String rstf="RemoteSystemsTempFiles";
    static final String external=".org.eclipse.jdt.core.external.folders";
}
public class Main {
    static List<String> split(String string) {
        String[] words=string.split(",");
        List<String> w=Arrays.asList(words);
        return w;
    }
    static Set<String> processWorkSpace(Path path) {
        Set<String> projects=new TreeSet<>();
        System.out.println("path: "+path);
        File file=new File(""+path+FindWorkspaces.projectsFolder);
        String[] filenames=file.list();
        System.out.println(filenames.length+" filenames.");
        for(String filename:filenames)
            if(filename.contains(FindWorkspaces.rstf)) System.out.println("omitting: "+filename);
            else if(filename.contains(FindWorkspaces.external)) System.out.println("omitting: "+filename);
            else if(filename.startsWith(".")) System.out.println("omitting: "+filename);
            else {
                //System.out.println(projects.size()+" adding: "+filename);
                projects.add(filename);
            }
        return projects;
    }
    Map<String,String> projectToLocation(Path path) throws IOException,UnsupportedEncodingException {
        Map<String,String> map=new TreeMap<>();
        Set<String> projects=processWorkSpace(path);
        System.out.println(projects.size()+" projects "+projects);
        File projectsFolder=new File(""+path+FindWorkspaces.projectsFolder);
        int oks=0;
        int i=0;
        if(projectsFolder.exists()) for(String project:projects) {
            File dir=new File(projectsFolder,project);
            String[] files=dir.list();
            System.out.println(project+" files: "+Arrays.asList(files));
            File file=new File(dir,".location");
            if(file.exists()) {
                Path location=Paths.get(""+file);
                byte[] bytes=Files.readAllBytes(location);
                String string=new String(bytes,"UTF-8");
                map.put(project,string);
                System.out.println("ok: "+project+" has location");
                oks++;
            } else {
                //System.out.println("location for "+project+": "+file+" does not exist!");
                System.out.println("location for "+project+" does not exist!");
                if(map.put(project,null)!=null) System.out.println("duplicate: "+project);
                //throw new RuntimeException("oops");
            }
        }
        else System.out.println(projectsFolder+" does not exist!");
        System.out.println("oks: "+oks+"/"+projects.size());
        return map;
    }
    Set<String> foldersInProjectsFolder(Path path) {
        Set<String> foldersInProjectsFolder=new TreeSet<>();
        File dir=new File(""+path+FindWorkspaces.projectsFolder);
        String[] filenames=dir.list();
        for(String filename:filenames) {
            if(filename.contains("poker")) System.out.println("has poker: "+dir);
            File file=new File(""+path+FindWorkspaces.projectsFolder,filename);
            if(file.exists()&&file.isDirectory()) foldersInProjectsFolder.add(filename);
        }
        return foldersInProjectsFolder;
    }
    Set<String> foldersInWorkspace(Path path) {
        Set<String> foldersInWorkspace=new TreeSet<>();
        File dir=new File(""+path);
        String[] filenames=dir.list();
        for(String filename:filenames) {
            File file=new File(""+path,filename);
            //if(filename.contains("poker")) System.out.println("workspace folder has poker: "+dir);
            if(file.exists()&&file.isDirectory()) foldersInWorkspace.add(filename);
            else System.out.println(filename+" is a file in the workspace folder.");
        }
        return foldersInWorkspace;
    }
    private void processWorkspace(List<String> list,Set<String> set,Path path) {
        System.out.println("workspace folder:"+path);
        Set<String> foldersInProjectsFolder=foldersInProjectsFolder(path);
        System.out.println("in .projects/: "+foldersInProjectsFolder);
        list.addAll(foldersInProjectsFolder);
        set.addAll(foldersInProjectsFolder);
        Set<String> foldersInWorkspace=foldersInWorkspace(path);
        System.out.println("from workspace folder/: "+foldersInWorkspace);
        Set<String> both=new TreeSet<>();
        for(String dir:foldersInProjectsFolder)
            if(foldersInWorkspace.contains(dir)) {
                //System.out.println("in both: "+dir);
                both.add(dir);
            } else;//System.out.println("not in both: "+dir);
        if(both.size()==0) System.out.println("both is empty.");
        else System.out.println(foldersInWorkspace.size()+" "+foldersInProjectsFolder.size()+" "+both.size());
        Set<String> onlyInProjectsFolder=new TreeSet<>(foldersInProjectsFolder);
        onlyInProjectsFolder.removeAll(foldersInWorkspace);
        Set<String> onlyInWorkspaceFolder=new TreeSet<>(foldersInWorkspace);
        onlyInWorkspaceFolder.removeAll(foldersInProjectsFolder);
        System.out.println("only in workspace: "+onlyInWorkspaceFolder);
        System.out.println("only in projects: "+onlyInProjectsFolder);
    }
    void run(Path startingDir) throws IOException {
        FindWorkspaces findWorkspaces=new FindWorkspaces();
        //System.out.println(Arrays.asList(FileVisitOption.values()));
        Files.walkFileTree(startingDir,EnumSet.noneOf(FileVisitOption.class),7,findWorkspaces);
        System.out.println("workspaces: "+findWorkspaces.workspaces);
        System.out.println(findWorkspaces.workspaces.size()+" workspaces");
        List<String> list=new ArrayList<>();
        Set<String> set=new TreeSet<>();
        for(Path path:findWorkspaces.workspaces) {
            System.out.println("<<<<<<<<<<<<<");
            processWorkspace(list,set,path);
            System.out.println(">>>>>>>>>>>>>");
            if(true) break;
        }
        System.out.println(list.size()+" projects.");
        list.removeAll(set);
        if(list.size()>0) System.out.println("diff: "+list);
        if(true) for(Path path:findWorkspaces.workspaces) {
            Map<String,String> map=projectToLocation(path);
            this.map.put(path,map);
        }
        System.out.println("not workspaces: "+findWorkspaces.notWorkspaces);
    }
    public static void main(String[] args) throws IOException {
        Path startingDir=Paths.get("d:/ray/dev");
        new Main().run(startingDir);
    }
    Map<Path,Map<String,String>> map=new TreeMap<>();
}
