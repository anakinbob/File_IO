
import java.io.*;
import java.util.*;

/**
 * Created by cherryzard on 2/1/2016.
 */

public class FileIO {
    private static final String ROOT = "/Users/ankitbahl/Workspace/test";
    private static int patternCount = 0;
    private static int overallProgress = 1;
    public static void main(String[] args) {
        writeCompressed(new File(ROOT, "test2.mkv"), new File(ROOT, "test2.compr"));
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
            output.write(getCompressed(buffer,2));
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
     *      a base 256 converted number - 128 for each digit (the number of digits will be equal to numPatByte)
     *
     *      some sequence of bytes
     *
     */
    private static byte[] getCompressed(byte[] array, int minPatternSize) {

        int[] usedBytes = new int[256];
        for(byte b: array) {
            usedBytes[(int) b + 128] = 1;
        }
        List<Pattern> patternList = new ArrayList<>();
        Byte patByte = null;
        Byte refByte = null;
        HashMap<Byte,Set<Integer>> byteIndexes = new HashMap<>();
        List<ReferenceMap> referenceMappings = new ArrayList<>(); //maps index of the start of a reference to the reference it uses
        for(int i = 0; i < usedBytes.length; i++) {
            if(usedBytes[i] == 0) {
                if(patByte == null) {
                    patByte = (byte)(i - 128);
                } else {
                    refByte = (byte)(i - 128);
                    break;
                }
            }
        }
        ArrayList<Byte> compressed = new ArrayList<>();
        compressed.add(patByte);
        compressed.add(refByte);
        int counter = 0;
        for(int i = 0; i < array.length; i++) {
            if(counter % 100


                    == 0) {
                System.out.println("progress("+ overallProgress +"/9): " + (double)i / array.length);
            }
            counter++;
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
                    if(patternList.contains(bigPatternData)) {
                        // reuse of old pattern
                        referenceMappings.add(new ReferenceMap(i,patternList.indexOf(bigPatternData)));
                    } else if(isPartOfPattern(bigPatternData,patternList)) {
                        // dont use pattern
                    } else {
                        // use new pattern
                        patternList.add(bigPatternData);
                        referenceMappings.add(new ReferenceMap(i,patternList.size() - 1));
                        i += bigPatternData.pattern.size() - 1;
                    }

                }
            }
        }

        byte numPatByte = (byte)(Math.ceil(Math.log(patternList.size())/Math.log(255d)));
        compressed.add(numPatByte);
        int refCounter = 0;
        int patCounter = 0;
        for(int i = 0; i < array.length; ) {
            int refIndex = refCounter < referenceMappings.size() ? referenceMappings.get(refCounter).refIndex : -1;
            int patIndex = patCounter < patternList.size() ? patternList.get(patCounter).arrayIndex : -1;
            if(i == refIndex) {
                //start of a reference
                compressed.add(refByte);
                int patternIndex = referenceMappings.get(refCounter).ref;
                addIndexToCompressed(patternIndex,numPatByte,compressed);
                refCounter++;
                i += patternList.get(patternIndex).pattern.size();
            } else if(i == patIndex) {
                //start of a pattern
                compressed.add(patByte);
                compressed.addAll(patternList.get(patCounter).pattern);
                compressed.add(patByte);
                i += patternList.get(patCounter).pattern.size();
                patCounter++;
            } else {
                //regular bytes
                compressed.add(array[i]);
                i++;
            }

        }
        byte[] compressedArray = new byte[compressed.size()];
        for(int i = 0; i < compressed.size(); i++) {
            compressedArray[i] = compressed.get(i);
        }
        overallProgress++;
        if(minPatternSize == 10) {
            return compressedArray;
        }
        byte[] biggerArray = getCompressed(array,minPatternSize + 1);
        if(biggerArray.length < compressedArray.length) {
            return biggerArray;
        }
        return compressedArray;
    }

    private static void addIndexToCompressed(int index, int numIndexes, ArrayList<Byte> compressed) {
        ArrayList<Integer> base256 = new ArrayList<>();
        while(index > 0) {
            base256.add(index % 256);
            index /= 256;
        }
        while (base256.size() < numIndexes) {
            base256.add(0);
        }

        for(int i = base256.size() - 1; i >= 0; i--) {
            compressed.add((byte)(base256.get(i) - 128));
        }

    }

    private static Pattern getBiggestPattern(Set<Integer> indexList, byte[] array, int check, List<Pattern> patterns) {
        Pattern biggestPattern = new Pattern(Collections.emptyList(),-1,-1);
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
        for(int i = 0 ; check + i < array.length && array[pattern + i] == array[check + i] ; i++) {
            match.add(array[check +i]);
        }
            patternCount++;
            return new Pattern(match,patternCount - 1,pattern);
    }

    private static boolean matchPattern(byte[] array, int check, Pattern pattern) {
        for(int i = 0 ; check + i < array.length && i < pattern.pattern.size() &&
                pattern.pattern.get(i) == array[check + i] ; i++) {
            if(i == pattern.pattern.size() - 1) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPartOfPattern(Pattern pattern, List<Pattern> patternList) {
        for(Pattern patternInList : patternList) {
            if(doPatternsOverlap(patternInList,pattern)) {
                return true;
            }
        }
        return false;
    }

    private static boolean doPatternsOverlap(Pattern p1, Pattern p2) {
        return !((p1.arrayIndex < p2.arrayIndex && p1.arrayIndex + p1.pattern.size() <= p2.arrayIndex) ||
                (p2.arrayIndex < p1.arrayIndex && p2.arrayIndex + p2.pattern.size() <= p1.arrayIndex));
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
