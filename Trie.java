/**
 * Trie Simulator for CS-216
 *
 * @author Zhehan Li
 * @version 2.0
 * @since 2018.5.28
 *
 */

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

public abstract class Trie {

    final int expIPNum = 1000000;
    final int ptrSize = 26; // 26-bits each => 64MB
    final boolean verbose = true; // output every lookup result

    int trieNodeNum;
    int expEntryNum;
    int memTotalAccess;
    long memTotalStorage;
    int[] maskLength;
    int[] stride;
    boolean modified;
    String BGPTablePath;
    String IPTablePath;
    ArrayList<Long> memGrowth;

    public Trie(String BGPTablePath, String IPTablePath, boolean modified) {
        this.trieNodeNum = 0;
        this.memTotalStorage = 0;
        this.memTotalAccess = 0;
        this.maskLength = new int[33];
        this.stride = new int[] {8,8,8,8};
        this.modified = modified;
        this.BGPTablePath = BGPTablePath;
        this.IPTablePath = IPTablePath;
        this.memGrowth = new ArrayList<>();
    }

    public abstract boolean lookupEntry(String ip);

    public void loadIPTable() {
        try {
            FileInputStream inputStream = new FileInputStream(IPTablePath);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            int ipCount = 0;
            String line = null;

            while((line = bufferedReader.readLine()) != null)
                if (lookupEntry(line)) ipCount++;
            System.out.println("\n" + ipCount + " prefixes looked up. " + (ipCount == expIPNum? "Match":"ERROR: Mismatch"));
            System.out.println("Total Memory Access: " + memTotalAccess + " times");

            inputStream.close();
            bufferedReader.close();

        } catch (FileNotFoundException e) {
            System.out.println("IP Table File Not Found");
            System.exit(0);
        } catch (IOException e) {
            System.out.println("IO Exception");
            e.printStackTrace();
            System.exit(0);
        }
    }

    // Responsible for recording [memGrowth] [maskLength] and updating [memTotalStorage] [memTotalAccess].
    public abstract boolean insertEntry(String entry);

    public int calculateMask(String prefix) {
        String[] str = prefix.split("/");
        if (str.length == 2) return Integer.valueOf(str[1]);
        int topAddress = Integer.valueOf(prefix.substring(0, prefix.indexOf('.')));
        if (prefix.endsWith(".0.0.0") && topAddress < 128) return 8; // class A;
        if (prefix.endsWith(".0.0") && topAddress >= 128 && topAddress < 192) return 16; // class B;
        if (prefix.endsWith(".0") && topAddress >= 192 && topAddress < 224) return 24; // class C;
        return -1;
    }

    // "1.2.255.4/16" -> "1","2/8"
    // "1.2.255.4/17" -> "1","2","1/1"
    // "1.2.255.4/18" -> "1","2","3/2"
    // "1.2.255.4/19" -> "1","2","7/3"
    // "1.2.255.4/23" -> "1","2","127/7"
    // "1.0.255.4/24" -> "1","0","255/8"
    public String[] calculateIPComponent(String prefix, int maskBits) {
        String[] str = prefix.split("/");
        if (str.length == 2) prefix = str[0];
        str = prefix.split("\\.");
        if (str.length != 4) return null;

        int level = 0;
        long IPAddress = 0;
        int leadingZero = -1;
        int shiftAmount = 32;
        ArrayList<String> ipComponents = new ArrayList<>();
        for (int i = 0; i < 4; ++i) IPAddress = IPAddress * 256 + Long.valueOf(str[i]);

        while (maskBits != 0) {
            shiftAmount -= Math.min(stride[level], maskBits);
            ipComponents.add(((IPAddress & leadingZero) >>> shiftAmount) + (maskBits > stride[level]? "": ("/" + maskBits)));
            leadingZero >>>= stride[level];
            maskBits -= Math.min(stride[level++], maskBits);
        }
        return ipComponents.toArray(new String[ipComponents.size()]);
    }

    public void increaseNode() {
        trieNodeNum++;
    }

    public void accessMemory(int times) {
        memTotalAccess += times;
    }

    public void increaseMemory(int amount) {
        memTotalStorage += amount;
    }

    public void recordMemory() {
        memGrowth.add(memTotalStorage/8);
    }

    public void loadBGPTable() {
        try {
            FileInputStream inputStream = new FileInputStream(BGPTablePath);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            int entryCount = 0;
            boolean validData = false;
            String line, entry = null;

            while((line = bufferedReader.readLine()) != null) {
                if (line.length() == 0) validData = !validData; // skip comments ("BGP table version...")
                if (validData && line.split(" +").length <= 2) line += bufferedReader.readLine(); //concatenate "truncated line" (*> 23.128.125.240/28 \n 202.12.28.1 0 4777...)
                if (!validData && line.startsWith("Displayed")) expEntryNum = Integer.valueOf(line.split(" +")[1]); // Get expected entry number
                if (!validData || line.charAt(0) != '*') continue; // skip headers ("Network Next Hop Metric...")

                if (line.charAt(3) != ' ') { // Only lines containing "network" have line[3] as non-space
                    if (insertEntry(entry)) entryCount++; // insert previous entry
                    entry = line.substring(3); // begin a new entry and skip "*>" or "*"
                } else {
                    entry += line.substring(3); // skip "*>" or "*"
                }
            }
            if (insertEntry(entry)) entryCount++;

            System.out.println(entryCount + " prefixes inserted. " + (entryCount == expEntryNum? "Match":"ERROR: Mismatch"));
            System.out.println("Total Trie Node: " + trieNodeNum + " nodes");
            System.out.println("Total Memory Storage: " + memTotalStorage / 8 / 1024 + " KB");
            if (verbose) {
                System.out.println("\nMask Size Distribution:");
                for (int i = 1, step = 4, sum = 0; i < maskLength.length; i += step, sum = 0) {
                    for (int j = i; j < i + step; ++j) sum += maskLength[j];
                    System.out.println("Length:" + i + "~" + (i + step - 1) + " => " + sum);
                }
                System.out.println("\nMemory Growth Record:");
                for (int step = 100000, i = step; i < memGrowth.size(); i += step)
                    System.out.println("Inserting " + i + " entries => " + memGrowth.get(i) / 1024 + " KB");
            }

            inputStream.close();
            bufferedReader.close();

        } catch (FileNotFoundException e) {
            System.out.println("BGP Table File Not Found");
            System.exit(0);
        } catch (IOException e) {
            System.out.println("IO Exception");
            e.printStackTrace();
            System.exit(0);
        }
    }

    public void start() {
        System.out.println("Inserting IP prefix...\n");
        loadBGPTable();
        System.out.println("\n-----------------------\n");
        System.out.println("Looking up IP prefix...\n");
        loadIPTable();
    }

    public static void main(String[] args) throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        Constructor constructor = Class.forName(args[0]).getDeclaredConstructor(String.class, String.class, boolean.class);
        Trie trie = (Trie) constructor.newInstance(args[1], args[2], args[3].equals("true"));
        trie.start();
    }

}
