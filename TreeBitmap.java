/**
 * TreeBitmap Simulator for CS-216
 *
 * @author Zhehan Li
 * @version 4.0
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

    final String rootMatch = "0/0";
    int[] stride;
    TreeNode rootNode;


    public TreeBitmap(String BGPTablePath, String IPTablePath) {
        super(BGPTablePath, IPTablePath);
        this.stride = new int[super.stride.length + 1]; //{8,8,8,8} => {8,8,8,8,0}; 0 deal with the end node
        System.arraycopy(super.stride, 0, this.stride, 0, super.stride.length);
        this.rootNode = new TreeNode(0, null);
    }

    public class TreeNode {
        HashMap<String, String> data;
        HashMap<String, TreeNode> children;
        int level;
        int internalBitmapSize; // 2^stride - 1 [prefix bitmap]
        int externalBitmapSize; // 2^stride [child pointer bitmap]
        boolean nullNode; // nullNode -> endNode optimization:
        boolean endNode; // shrink the "null node" into the previous level ("end node")
        TreeNode parent;

        public TreeNode(int level, TreeNode parent){
            data = new HashMap<>();
            children = new HashMap<>();
            this.level = level;
            this.nullNode = true; // any node is by default null node [only contains "*" and its parent is not an end node]
            this.endNode = true; // any node is by default end node [all children are null node]
            this.parent = parent;
            this.internalBitmapSize = (int)Math.pow(2, stride[level]) - 1;
            this.externalBitmapSize = (int)Math.pow(2, stride[level]);
        }

        public void addChild(String index, TreeNode child) {
            children.put(index, child);
            notANullNode();
        }

        public boolean addData(String prefix, String nextHopData) {
            if (data.containsKey(prefix)) return false;
            data.put(prefix, nextHopData);
            increaseMemory(ptrSize); // data/result pointer
            if (prefix != rootMatch || !endNode) notANullNode();
            return true;
        }

        public void notANullNode() {
            if (nullNode) {
                nullNode = false;
                if (parent != null) parent.notAnEndNode();
                increaseNode();
                increaseMemory(internalBitmapSize + externalBitmapSize + 2 * ptrSize);
                // Child Array Pointer + Result Array Pointer [2 x pointerSize]
            }
        }

        public void notAnEndNode() {
            if (endNode) {
                endNode = false;
                for (TreeNode child : children.values()) child.notANullNode();
            }
        }

    }

    public boolean lookupEntry(String ip) {
        if (ip == null) return false;
        int level = 0;
        int memoryAccess = 0;
        TreeNode curNode = rootNode;
        String nextHopData = null;
        String longestMatch = null;
        String[] ipComponents = calculateIPComponent(ip, 32);

        if (ipComponents == null) return false;

        while (true) {
            memoryAccess++; // access [two bitmaps]
            // find the best match for current trie node
            longestMatch = internalBestMatch(ipComponents[level], stride[level], curNode);
            if (longestMatch != null) {
                memoryAccess++; // access [data array] and fetch the data pointer
                nextHopData = longestMatch;
            }
            // proceed to next level
            if (!curNode.children.containsKey(ipComponents[level])) break;
            memoryAccess++; // access [child array]
            curNode = curNode.children.get(ipComponents[level]);
            level++;
        }
        // No need to access the real "null node" in last level only containing "*"
        if (curNode.nullNode && curNode.parent.endNode) memoryAccess -= 2; // access [two bitmaps] + [data array]
        // No need to access the child array in the real "end node"
        if (curNode.nullNode && curNode.parent.endNode) memoryAccess--; // access [child array]
        // Need to access the data array in the real "end node" if we didn't do it in the "end node"
        if (curNode.nullNode && curNode.parent.endNode && longestMatch == null) memoryAccess++; // access [data array]
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
            if (!curNode.children.containsKey(ipComponents[level]))
                curNode.addChild(ipComponents[level], new TreeNode(level+1, curNode));
            curNode = curNode.children.get(ipComponents[level++]);
        }

        // Add the next hop data
        String prefix = ipComponents[level];
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
            IPComponents[length] = rootMatch;
            return IPComponents;
        }
        return standardIPComponents;
    }

    @Override
    public int countPrefix() {
        return 0;
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
