
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by cherryzard on 2/1/2016.
 */

public class FileIO {
    private static final String ROOT = "D:\\Users\\cherryzard\\Desktop\\All\\test";
    public static void main(String[] args) {
        /*
        File root = new File(ROOT);

        File master = new File(root,"master");
        File slave = new File(root,"slave");
        fileCopyPaste(master,slave);
        folderCopyPaste(master,slave);
        */
        test();
    }

    public static void test() {
        boolean test = (Integer.class.equals(int.class));
        System.out.printf(""+test);
    }



    private static void copyPaste(File inputPath, File outputPath) {
        BufferedInputStream input = null;
        BufferedOutputStream output = null;
        try {
            input = new BufferedInputStream(new FileInputStream(inputPath));
            output = new BufferedOutputStream(new FileOutputStream(outputPath));
            byte[] buffer = new byte[1024];
            int len;
            while((len = input.read(buffer)) > 0) {
                output.write(buffer,0,len);
            }
        } catch( IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (input != null)
                    input.close();
                if (output != null)
                    output.close();
            }catch (IOException ignored){}
        }
    }

    private static List<File> folderList(File dir) {
        File[] allFiles = dir.listFiles();
        List<File> folders = new ArrayList<>();
        for(File file : allFiles) {
            if(!file.isFile()) {
                folders.add(file);
            }
        }
        return folders;
    }

    private static List<File> fileList(File dir) {
        File[] allFiles = dir.listFiles();
        List<File> folders = new ArrayList<>();
         for(File file : allFiles) {
            if(file.isFile()) {
                folders.add(file);
            }
        }
        return folders;
    }
    private static void folderCopyPasteDeprecated(File master, File slave) {
        for(File folder : folderList(master)) {
            File newFolder = new File(slave,folder.getName());
            newFolder.mkdir();
            for(File file : fileList(folder)) {
                copyPaste(file,new File(newFolder,file.getName()));
            }
        }
    }

    private static void fileCopyPasteDeprecated(File master,File slave) {
        for(File file : fileList(master)) {
            File newFile = new File(slave,file.getName());
            copyPaste(file,newFile);
        }
    }

    private static void fileCopyPaste(File masterDir, File slaveDir) {
        List<File> masterFiles = fileList(masterDir);
        for(File slaveFile : fileList(slaveDir)) {
            if(!containsFile(slaveFile,masterFiles)) {
                slaveFile.delete();
                System.out.printf("Deleted the following file: " + slaveFile.getName() + "\n");
            }
        }
        List<File> slaveFiles = fileList(slaveDir);
        for(File masterFile : masterFiles) {
            if(!containsFile(masterFile,slaveFiles)) {
                copyPaste(masterFile,new File(slaveDir,masterFile.getName()));
                System.out.printf("Copy pasted following file: " + masterFile.getName() + "\n");
            }
        }
    }

    private static void folderCopyPaste(File masterDir, File slaveDir) {
        List<File> masterFolders = folderList(masterDir);
        for(File slaveFolder : folderList(slaveDir)) {
            if(!containsFile(slaveFolder,masterFolders)) {
                deleteFolder(slaveFolder);
                System.out.printf("Deleted the following folder: " + slaveFolder.getName() + "\n");
            }
        }

        for(File slaveFolder : fileList(slaveDir)) {
            fileCopyPaste(new File(masterDir,slaveFolder.getName()),slaveFolder);
        }
        List<File> slaveFolders = fileList(slaveDir);
        for(File masterFolder : masterFolders) {
            if(!containsFile(masterFolder,slaveFolders)) {
                File needToCopyFolder = new File(slaveDir,masterFolder.getName());
                needToCopyFolder.mkdir();
                for(File file : fileList(masterFolder)) {
                    copyPaste(file,new File(needToCopyFolder,file.getName()));
                }
            }
        }
    }

    private static boolean containsFile(File toCheck, List<File> fileList) {
        for(File file : fileList) {
            if(file.getName().equals(toCheck.getName())) {
                return true;
            }
        }
        return false;
    }

    private static void deleteFolder(File folder) {
        for(File file : fileList(folder)) {
            file.delete();
        }
        folder.delete();
    }

}
