
import java.io.*;
import java.util.*;

/**
 * Created by cherryzard on 2/1/2016.
 */

public class FileIO {
    private static final String ROOT = "D:\\Workspace\\test";
    private static int patternCount = 0;
    public static void main(String[] args) {
        writeCompressed(new File(ROOT, "test.txt"), new File(ROOT, "test2.compr"));
//        HashMap<Integer,Set<Integer>> testMap = new HashMap<>();
//        Set<Integer> set = new HashSet<>();
//        set.add(1);
//        set.add(2);
//        set.add(3);
//        testMap.put(0,set);
//        printSet(testMap.get(0));
//        Set<Integer> get = testMap.get(0);
//        get.add(4);
//        printSet(testMap.get(0));
    }

    private static void printSet(Set<Integer> set) {
        for(int i : set) {
            System.out.print(i + ",");
        } System.out.print("\n");
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

    private static void writeCompressed(File inputPath, File outputPath) {
        BufferedInputStream input = null;
        BufferedOutputStream output = null;
        try {
            input = new BufferedInputStream(new FileInputStream(inputPath));
            output = new BufferedOutputStream(new FileOutputStream(outputPath));
            byte[] buffer = new byte[input.available()];
            input.read(buffer);
            output.write(getCompressed(buffer));
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


    /**
     * spec for compression:
     *
     * first byte of file is used to signify the start and end of a pattern(patByte)
     * second byte of file is used to signify the start of a reference(refByte)
     * third byte is the number of bytes used to signify a pattern (numPatByte)
     * next is any number of either of the following:
     *
     *      patByte
     *      some sequence of bytes
     *      patByte
     *
     *      refByte
     *      an index for the pattern reference (the number of bytes will be equal to numPatByte)
     *
     *      some sequence of bytes
     *
     */
    private static byte[] getCompressed(byte[] array) {

        int[] usedBytes = new int[256];
        for(byte b: array) {
            usedBytes[(int)b + 128] = 1;
        }
        int minPatternSize = 5;
        List<Pattern> patternList = new ArrayList<>();
        Byte patByte = null;
        Byte refByte = null;
        Byte numPatByte = null;
        HashMap<Byte,Set<Integer>> byteIndexes = new HashMap<>();
        for(int i = 0; i < usedBytes.length; i++) {
            if(usedBytes[i] == 0) {
                if(patByte == null) {
                    patByte = (byte)(i - 128);
                } else {
                    refByte = (byte)(i - 128);
                }
            }
        }
        ArrayList<Byte> compressed = new ArrayList<>();
        compressed.add(patByte);
        compressed.add(refByte);
        compressed.add((byte)0); //placeholder to be replaced later with the actual byte count data
        for(int i = 0; i < array.length; i++) {
            byte b = array[i];
            Set<Integer> indexList = byteIndexes.get(b);
            if(indexList == null) {
                indexList = new LinkedHashSet<>();
                indexList.add(i);
                byteIndexes.put(b,indexList);
            }
            else {
                Set<Integer> copySet = new LinkedHashSet<>(indexList);
                indexList.add(i);
                Pattern bigPatternData = getBiggestPattern(copySet,array,i,patternList);
                if(bigPatternData.pattern.size() >= minPatternSize) {
                    patternList.add(bigPatternData);

                }
            }
        }
        byte[] compressedArray = new byte[compressed.size()];
        for(int i = 0; i < compressed.size(); i++) {
            compressedArray[i] = compressed.get(i);
        }
        return compressedArray;
    }

    private static Pattern getBiggestPattern(Set<Integer> indexList, byte[] array, int check, List<Pattern> patterns) {
        Pattern biggestPattern = new Pattern(Collections.emptyList(),-1);
        for(int index: indexList) {
            Pattern pattern = matchPattern(array,check,index);
            if(pattern.pattern.size() > biggestPattern.pattern.size()) {
                biggestPattern = pattern;
            }
        }
        for(Pattern pattern : patterns) {
            if(matchPattern(array,check,pattern)) {
                return pattern;
            }
        }
        return biggestPattern;
    }

    private static Pattern matchPattern(byte[] array, int check, int pattern) {
        List<Byte> match = new ArrayList<>();
        for(int i = 0 ; array[pattern + i] == array[check + i] ; i++) { //TODO index out of bounds
            match.add(array[check +i]);
        }
            patternCount++;
            return new Pattern(match,patternCount - 1);
    }

    private static boolean matchPattern(byte[] array, int check, Pattern pattern) {
        for(int i = 0 ; pattern.pattern.get(i) == array[check + i] ; i++) { //TODO index out of bounds
            if(i == pattern.pattern.size() - 1) {
                return true;
            }
        }
        return false;
    }

    /**
     * spec for compression:
     *
     */
    private static byte[] getCompressed2(byte[] array) {

        int[] usedBytes = new int[127+128+1];
        for(byte b: array) {
            usedBytes[(int)b + 128] = 1;
        }
        Byte pattern = null;
        Byte patternUsed = null;
        Byte reference = null;
        for(int i = 0; i < usedBytes.length; i++) {
            if(usedBytes[i] == 0) {
                if(pattern == null) {
                    pattern = (byte)(i - 128);
                } else if(patternUsed  == null) {
                    patternUsed = (byte)(i - 128);
                } else {
                    reference= (byte)(i - 128);
                    break;
                }
            }
        }
        ArrayList<Byte> compressed = new ArrayList<>();
        compressed.add(pattern);
        compressed.add(patternUsed);
        compressed.add(reference);
        byte prev = array[0];
        int counter = 1;
        for(int i = 1; i < array.length; i++) {
            if(array[i] == prev) {
                counter++;
            } else {
                compressed.add(prev);
                compressed.add((byte)counter);
                prev = array[i];
                counter = 1;
            }
        }
        compressed.add(prev);
        compressed.add((byte)counter);
        byte[] compressedArray = new byte[compressed.size()];
        for(int i = 0; i < compressed.size(); i++) {
            compressedArray[i] = compressed.get(i);
        }
        return compressedArray;
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
