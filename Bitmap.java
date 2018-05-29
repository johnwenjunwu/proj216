/**
 * Bitmap (Lulea Scheme) Simulator for CS-216
 *
 * @author Zhehan Li
 * @version 1.0
 * @since 2018.5.28
 *
 */

import java.util.HashMap;

public class Bitmap extends Trie {

    TreeNode rootNode;

    public Bitmap(String BGPTablePath, String IPTablePath, boolean modified) {
        super(BGPTablePath, IPTablePath, modified);
        this.rootNode = new TreeNode(stride[0], 0);
    }

    public class TreeNode {
        HashMap<String, Integer> prefixCount; // how many times a prefix appears in prefix array
        HashMap<String, String> data;
        HashMap<String, TreeNode> children;
        int stride;
        int level;
        int prefixBitmapSize; // 2^stride [prefix bitmap]
        int childBitmapSize; // 2^stride [child pointer bitmap]
        int[] prefixValue;
        int[] prefixMask;

        public TreeNode(int stride, int level){
            prefixCount = new HashMap<>();
            data = new HashMap<>();
            children = new HashMap<>();
            this.stride = stride;
            this.level = level;
            this.prefixBitmapSize = (int)Math.pow(2, stride);
            this.childBitmapSize = (int)Math.pow(2, stride);
            this.prefixValue = new int [this.prefixBitmapSize];
            this.prefixMask = new int [this.prefixBitmapSize];
            increaseMemory(prefixBitmapSize + childBitmapSize);
        }

        public void addChild(String index, TreeNode child) {
            this.children.put(index, child);
            increaseMemory(PointerSize);
        }

        public boolean addData(String prefix, String nextHopData) {
            if (data.containsKey(prefix)) return false;
            data.put(prefix, nextHopData);
            int value = Integer.valueOf(prefix.substring(0, prefix.indexOf('/')));
            int mask = Integer.valueOf(prefix.substring(prefix.indexOf('/') + 1));
            int appearCount = 0;
            int beginIndex = value << (stride - mask);
            int endIndex = beginIndex + (1 << (stride - mask));

            for (int i = beginIndex; i < endIndex; ++i) {
                if (prefixMask[i] == mask) return false;
                if (prefixMask[i] > mask) continue;

                String oldPrefix = prefixValue[i] + "/" + prefixMask[i];
                int oldAppearCount = prefixCount.getOrDefault(oldPrefix, -1) - 1;
                if (prefixMask[i] > 0) prefixCount.put(oldPrefix, oldAppearCount);
                if (prefixMask[i] > 0 && oldAppearCount < 0) return false;
                if (prefixMask[i] > 0 && oldAppearCount == 0) increaseMemory(-dataNodeSize); // previous prefix is completely covered by others

                appearCount++;
                prefixMask[i] = mask;
                prefixValue[i] = value;
            }

            prefixCount.put(prefix, appearCount);
            increaseMemory(dataNodeSize);
            return true;
        }
    }



    public boolean lookupEntry(String ip) {
        if (ip == null) return false;
        int level = 0;
        int memoryAccess = 0;
        TreeNode curNode = rootNode;
        String nextHopData = null;
        String[] ipComponents = calculateIPComponent(ip, 32);
        if (ipComponents == null) return false;
        // change the last "1234/8" to "1234"
        ipComponents[stride.length - 1] = ipComponents[stride.length - 1].substring(0, ipComponents[stride.length - 1].indexOf('/'));


        while (true) {
            memoryAccess++; // access child array

            // find the best match for current trie node
            String longestMatch = curNode.prefixValue[Integer.valueOf(ipComponents[level])] + "/";
            longestMatch += curNode.prefixMask[Integer.valueOf(ipComponents[level])];
            if (curNode.data.get(longestMatch) != null) nextHopData = curNode.data.get(longestMatch);

            // proceed to next level
            if (!curNode.children.containsKey(ipComponents[level])) break;
            curNode = curNode.children.get(ipComponents[level]);
            level++;
        }

        accessMemory(memoryAccess);
        if (verbose) {
            System.out.println("Look up IP address : " + ip + " => Memory Access: " + memoryAccess + " times");
            System.out.println("Next Hop Data: " + (nextHopData == null ? "Not Found" : nextHopData));
        }

        return true;
    }

    public boolean insertEntry(String entry) {
        if (entry == null) return false;
        String[] fields = entry.split(" +");
        int level = 0;
        int maskBits = calculateMask(fields[0]); // figure out the default mask
        TreeNode curNode = rootNode;
        String[] ipComponents = calculateIPComponent(fields[0], maskBits); // turn prefix into array of child index

        if (maskBits == -1 || ipComponents == null) return false;
        maskLength[maskBits]++; // record mask length distribution

        // Follow the child pointers to the next level
        while (level < ipComponents.length - 1) {
            accessMemory(1); // read TreeNode
            if (!curNode.children.containsKey(ipComponents[level]))
                curNode.addChild(ipComponents[level], new TreeNode(stride[level+1], level+1));
            curNode = curNode.children.get(ipComponents[level++]);
        }

        // Add the next hop data
        String prefix = ipComponents[level];
        accessMemory(1); // read TreeNode + write Data -> All within the node
        if (!curNode.addData(prefix, String.join(" ", fields))) return false;
        recordMemory();
        return true;
    }
}
