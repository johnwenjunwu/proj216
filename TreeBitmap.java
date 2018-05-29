/**
 * TreeBitmap Simulator for CS-216
 *
 * @author Zhehan Li
 * @version 2.0
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

import java.util.HashMap;

public class TreeBitmap extends Trie {

    int[] stride;
    TreeNode rootNode;

    public TreeBitmap(String BGPTablePath, String IPTablePath, boolean modified) {
        super(BGPTablePath, IPTablePath, modified);
        this.stride = new int[super.stride.length + 1];
        System.arraycopy(super.stride, 0, this.stride, 0, super.stride.length);
        this.stride[super.stride.length] = -1; //{8,8,8,8} => {8,8,8,8,-1}; -1 deal with the end node
        this.rootNode = new TreeNode(stride[0], 0);
    }

    public class TreeNode {
        HashMap<String, String> data;
        HashMap<String, TreeNode> children;
        int stride;
        int level;
        int internalBitmapSize; // 2^stride - 1 [prefix bitmap]
        int externalBitmapSize; // 2^stride [child pointer bitmap]
        int ptrSize; // Children Array Pointer + Result Array Pointer [2 x pointerSize]
        boolean endNode; // endNode optimization: shrink the "null node" into the previous level

        public TreeNode(int stride, int level){
            data = new HashMap<>();
            children = new HashMap<>();
            this.stride = stride;
            this.level = level;
            this.endNode = false;
            this.internalBitmapSize = (int)Math.pow(2, stride) - 1;
            this.externalBitmapSize = (int)Math.pow(2, stride);
            this.ptrSize = 2 * PointerSize;
            if (stride > 0) increaseMemory(internalBitmapSize + externalBitmapSize + ptrSize);
            if (stride < 0) endNode = true;
        }

        public boolean addData(String prefix, String nextHopData) {
            if (data.containsKey(prefix)) return false;
            data.put(prefix, nextHopData);
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
        if (curNode.endNode) memoryAccess--; // No need to access the "null node" in last level only containing "*"

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
        String[] ipComponents = calculateIPComponent(fields[0], maskBits); // turn prefix into array of n*[child index]+[data index]

        if (maskBits == -1 || ipComponents == null) return false;
        maskLength[maskBits]++; // record mask length distribution

        // Follow the child pointers to the next level
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

    public String[] calculateIPComponent(String prefix, int maskBits) {
        String[] standardIPComponents = super.calculateIPComponent(prefix, maskBits);
        if (standardIPComponents == null) return null;
        int length = standardIPComponents.length;
        // change "1.0.255.4/24" -> "1","0","255/8" to "1","0","255","0/0"
        if (standardIPComponents[length - 1].endsWith("/" + stride[length - 1])) {
            String[] IPComponents = new String[length + 1];
            System.arraycopy(standardIPComponents, 0, IPComponents, 0, length);
            IPComponents[length - 1] = IPComponents[length - 1].substring(0, IPComponents[length - 1].indexOf('/'));
            IPComponents[length] = "0/0";
            return IPComponents;
        }
        return standardIPComponents;
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

}


