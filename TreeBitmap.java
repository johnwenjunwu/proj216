
/**
* TreeBitmap Simulator for CS-216
*
* @author Zhehan Li
* @version 1.0
* @since 2018.5.27
*
* ref:
* http://cseweb.ucsd.edu/~varghese/PAPERS/ccr2004.pdf
* https://keepingitclassless.net/2011/09/bgp-weight-and-local-preference/
* https://www.cisco.com/c/en/us/support/docs/ip/border-gateway-protocol-bgp/19345-bgp-noad.html#topic1
* https://learningnetwork.cisco.com/thread/123678
* https://en.wikipedia.org/wiki/Classful_network
* http://bgp.potaroo.net/as2.0/
*/

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

public class TreeBitmap {

    final int expIPNum = 1000000;
    final int twoPointerSize = 26 * 2; // 26-bits each => 64MB
    final int resultNodeSize = 64; // 8-bytes
    final boolean verbose = true; // output every lookup result

    int expEntryNum;
    int memTotalAccess;
    long memTotalStorage;
    int[] maskLength;
    int[] stride;
    boolean modified;
    String BGPTablePath;
    String IPTablePath;
    TreeNode rootNode;
    ArrayList<Long> memGrowth;

    public TreeBitmap(String BGPTablePath, String IPTablePath, boolean modified) {
        this.memTotalStorage = 0;
        this.memTotalAccess = 0;
        this.maskLength = new int[33];
        this.stride = new int[] {8,8,8,8,-1}; //-1 deal with the end node
        this.modified = modified;
        this.BGPTablePath = BGPTablePath;
        this.IPTablePath = IPTablePath;
        this.rootNode = new TreeNode(stride[0], 0);
        this.memGrowth = new ArrayList<>();
    }

    public class TreeNode {
        HashMap<String, String> data;
        HashMap<String, TreeNode> children;
        int stride;
        int level;
        int internalBitmapSize; // 2^stride - 1 [prefix bitmap]
        int externalBitmapSize; // 2^stride [child pointer bitmap]
        int ptrSize; // Children Array Pointer + Result Array Pointer [2 x pointerSize]
        int dataNodeSize = resultNodeSize;

        public TreeNode(int stride, int level){
            data = new HashMap<>();
            children = new HashMap<>();
            this.stride = stride;
            this.level = level;
            this.internalBitmapSize = (int)Math.pow(2.0,(double)stride) - 1;
            this.externalBitmapSize = (int)Math.pow(2.0,(double)stride);
            this.ptrSize = twoPointerSize;
            if (stride > 0) increaseMemory(internalBitmapSize + externalBitmapSize + ptrSize);
        }

        public boolean addData(String prefix, String nextHopData) {
            if (data.containsKey(prefix)) return false;
            data.put(prefix, nextHopData);
            increaseMemory(dataNodeSize);
            return true;
        }
    }

    public String internalBestMatch(String ipComponent, int stride, TreeNode node) {
        int ipValue;
        if (ipComponent.indexOf('/') != -1) {
            stride = Integer.valueOf(ipComponent.substring(ipComponent.indexOf('/') + 1)) + 1; // loop begin with stride - 1
            ipValue = Integer.valueOf(ipComponent.substring(0, ipComponent.indexOf('/'))) * 2; // loop begin with ipValue/2
        } else ipValue = Integer.valueOf(ipComponent);

        for (int i = stride - 1; i >= 0; --i) { // in case of stride = 8, we begin the match from xxx/7
            ipValue >>= 1;
            String tryMatch = ipValue + "/" + i;
            if (node.data.containsKey(tryMatch))
                return node.data.get(tryMatch);
        }
        return null;
    }

    public boolean lookupEntry(String ip) {
        if (ip == null) return false;
        int level = 0;
        int memoryAccess = 0;
        TreeNode curNode = rootNode;
        String nextHopData = null;
        String[] ipComponents = calculateIPComponent(ip, 32);

        if (ipComponents == null) return false;

        while (true) {
            memoryAccess++; // access child array
            // find the best match for current trie node
            String longestMatch = internalBestMatch(ipComponents[level], stride[level], curNode);
            if (longestMatch != null) nextHopData = longestMatch;
            // proceed to next level
            if (!curNode.children.containsKey(ipComponents[level])) break;
            curNode = curNode.children.get(ipComponents[level]);
            level++;
        }
        if (nextHopData != null) memoryAccess++; // access data

        accessMemory(memoryAccess);
        if (verbose) {
            System.out.println("Look up IP address : " + ip + " => Memory Access: " + memoryAccess + " times");
            System.out.println("Next Hop Data: " + (nextHopData == null ? "Not Found" : nextHopData));
        }

        return true;
    }

    public void loadIPTable() {
        try {
            FileInputStream inputStream = new FileInputStream(IPTablePath);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            int ipCount = 0;
            String line = null;
            memTotalAccess = 0;

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

    public boolean insertEntry(String entry) {
        if (entry == null) return false;
        String[] fields = entry.substring(3).split(" +"); // skip "*" or "*>"
        int level = 0;
        int maskBits = calculateMask(fields[0]); // figure out the default mask
        TreeNode curNode = rootNode;
        String[] ipComponents = calculateIPComponent(fields[0], maskBits); // turn prefix into array of n*[child index]+[data index]

        if (maskBits == -1 || ipComponents == null) return false;
        maskLength[maskBits]++; // record mask length distribution
        // Follow the child pointers to the last level
        while (level < ipComponents.length - 1) {
            accessMemory(1); // read TreeNode
            if (!curNode.children.containsKey(ipComponents[level]))
                curNode.children.put(ipComponents[level], new TreeNode(stride[level+1], level+1));
            curNode = curNode.children.get(ipComponents[level++]);
        }
        // Add the next hop data
        String prefix = ipComponents[level];
        accessMemory(2); // read TreeNode + write Data
        if (!curNode.addData(prefix, String.join(" ", fields))) return false;
        recordMemory();
        return true;
    }

    public int calculateMask(String prefix) {
        String[] str = prefix.split("/");
        if (str.length == 2) return Integer.valueOf(str[1]);
        int topAddress = Integer.valueOf(prefix.substring(0, prefix.indexOf('.')));
        if (prefix.endsWith(".0.0.0") && topAddress < 128) return 8; // class A;
        if (prefix.endsWith(".0.0") && topAddress >= 128 && topAddress < 192) return 16; // class B;
        if (prefix.endsWith(".0") && topAddress >= 192 && topAddress < 224) return 24; // class C;
        return -1;
    }

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
            ipComponents.add(((IPAddress & leadingZero) >>> shiftAmount) + (maskBits >= stride[level]? "": ("/" + maskBits)));
            leadingZero >>>= stride[level];
            if (maskBits == stride[level]) ipComponents.add("0/0");
            maskBits -= Math.min(stride[level++], maskBits);
        }
        return ipComponents.toArray(new String[ipComponents.size()]);
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
                    entry = line; // begin a new entry
                } else {
                    entry += line.substring(3); // skip "*>" or "*"
                }
//                if (entryCount == 10) break;
            }
            if (insertEntry(entry)) entryCount++;
            System.out.println(entryCount + " prefixes inserted. " + (entryCount == expEntryNum? "Match":"ERROR: Mismatch"));
            System.out.println("Total Memory Storage: " + memTotalStorage / 8 / 1024 + " KB");
            System.out.println("Total Memory Access: " + memTotalAccess + " times");
            if (verbose) {
                System.out.println("\nMask Size Distribution:");
                for (int i = 1, step = 4, sum = 0; i < maskLength.length; i += step, sum = 0) {
                    for (int j = i; j < i + step; ++j) sum += maskLength[j];
                    System.out.println("Length:" + i + "~" + (i + step - 1) + " => " + sum);
                }
                System.out.println("\nMemory Growth Record:");
                for (int step = 100000, i = step; i < entryCount; i += step)
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

    public static void main(String[] args){
       TreeBitmap treeBitmap = new TreeBitmap(args[0], args[1], false);
        treeBitmap.start();
    }

}
