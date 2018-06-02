/**
 * UniPrefix UniPrefixBitmap (Lulea Scheme + push data to next level + End node optimization) Simulator for CS-216
 *
 * @author Zhehan Li
 * @version 3.0
 * @since 2018.5.28
 *
 */

public class UniPrefixBitmap extends Trie {

    int mode = 0; // mode = 0: all compress, mode = 1: endnode compress
    int[] stride;
    TreeNode rootNode;

    public UniPrefixBitmap(String BGPTablePath, String IPTablePath) {
        super(BGPTablePath, IPTablePath);
        this.stride = new int[super.stride.length + 1];  //{8,8,8,8} => {8,8,8,8,0}; 0 deal with the end node
        System.arraycopy(super.stride, 0, this.stride, 0, super.stride.length);
        this.rootNode = new TreeNode(0, null);
    }

    public class TreeNode {
        int level;
        int pointerArraySize;
        int prefixValue;
        int prefixMask;
        int childPtrCounter; // used when a node changed from end node to non-end node
        int ptrBitmapSize; // 2^stride [pointer bitmap] -> compress the [child pointer] + [data pointer] array
        int dataBitmapSize; // 2^stride [data pointer bitmap] -> [child pointer] -> 0, [data pointer] -> 1
        String nextHopData;
        boolean nullNode; // nullNode -> endNode optimization:
        boolean endNode; // shrink the "null node" into the previous level ("end node")
        TreeNode parent;
        TreeNode[] children;

        public TreeNode(int level, TreeNode parent){
            this.level = level;
            this.pointerArraySize = 0;
            this.prefixValue = -1;
            this.prefixMask = -1; // data unavailable
            this.childPtrCounter = 0;
            this.ptrBitmapSize = (int)Math.pow(2, stride[level]);
            this.dataBitmapSize = (int)Math.pow(2, stride[level]);
            this.nullNode = true; // any node is by default null node [only contains data OR children == null]
            this.endNode = true; // any node is by default end node [all children are null node]
            this.parent = parent;
            this.children = null; // lazy initialization
        }

        public void addChild(String index, TreeNode child) {
            if (children == null) children = new TreeNode[ptrBitmapSize];
            children[Integer.valueOf(index)] = child;
            notANullNode();
            childPtrCounter++; // add a distinct pointer to pointer array
            increaseMemory(childPtrCounter * ptrSize - pointerArraySize);
            pointerArraySize = childPtrCounter * ptrSize;

        }

        public boolean addData(String prefixValueStr, String prefixMaskStr, String nextHopData) {
            int value = Integer.valueOf(prefixValueStr);
            int mask = Integer.valueOf(prefixMaskStr);
            int beginIndex = value << (stride[level] - mask);
            int endIndex = beginIndex + (1 << (stride[level] - mask));

            notANullNode();
            if (children == null) children = new TreeNode[ptrBitmapSize];

            for (int i = beginIndex; i < endIndex; ++i) {
                if (children[i] == null) {
                    children[i] = new TreeNode(level + 1, this);
                    childPtrCounter++;
                    if (mode == 1 && !endNode) { // internal nodes [not an end node, not a null node] in mode 1
                        // all child node are not a null node
                        children[i].notANullNode();
                        // no need to compress pointer array, every pointer is child pointer and will be different
                        increaseMemory(childPtrCounter * ptrSize - pointerArraySize);
                        pointerArraySize = childPtrCounter * ptrSize;
                    }
                }
                if (children[i].prefixMask == mask) return false;
                if (children[i].prefixMask > mask) continue;
                children[i].prefixMask = mask;
                children[i].prefixValue = value;
                children[i].nextHopData = nextHopData;
            }

            // internal nodes [not an end node, not a null node] in mode 1
            // no need to compress pointer array, every pointer is child pointer and will be different
            // pointer size added in the for-loop above
            if (mode == 1 && !endNode) return true;

            // recalculate the pointer array size
            int newPtrArraySize = 0;
            int previousValue = -1;
            int previousMask = -1;
            for (int i = 0; i < children.length; ++i) {
                if (children[i] == null) {
                    if (previousMask == -2) continue;
                    previousMask = -2;
                    newPtrArraySize += ptrSize; // a distinct null pointer
                } else if (!children[i].nullNode || children[i].prefixMask == -1)
                    newPtrArraySize += ptrSize; // must be a distinct child pointer
                else if (children[i].prefixMask == previousMask && children[i].prefixValue == previousValue)
                    continue;
                else { // a different data pointer
                    previousMask = children[i].prefixMask;
                    previousValue = children[i].prefixValue;
                    newPtrArraySize += ptrSize; // a distinct data pointer
                }
            }
            increaseMemory(newPtrArraySize - pointerArraySize);
            pointerArraySize = newPtrArraySize;
            return true;
        }

        public void notANullNode() {
            if (nullNode) {
                nullNode = false;
                if (parent != null) parent.notAnEndNode();
                increaseNode();
                increaseMemory(ptrBitmapSize + ptrSize);
            }
        }

        public void notAnEndNode() {
            if (endNode) {
                endNode = false;
                if (mode == 0) increaseMemory(dataBitmapSize);
                // mode = 1: no need to have a data bitmap in internal nodes
                // mode = 1: but need to declare all other children nodes are not null node
                if (mode == 1 && children != null) {
                    for (int i = 0; i < children.length; ++i)
                        if (children[i] != null) children[i].notANullNode();
                    increaseMemory(childPtrCounter * ptrSize - pointerArraySize);
                    pointerArraySize = childPtrCounter * ptrSize;
                }
            }
        }

    }

    public boolean lookupEntry(String ip) {
        if (ip == null) return false;
        int level = 0;
        int memoryAccess = 0;
        TreeNode curNode = rootNode;
        String nextHopData = null;
        TreeNode nodePointer = null;
        String[] ipComponents = calculateIPComponent(ip, 32);
        if (ipComponents == null) return false;

        memoryAccess++; // fetch two bitmaps from root node
        while (curNode.children != null) {
            // find the best match for current trie node
            if (mode == 0) memoryAccess++; // access pointer array and fetch the pointer (child/null)
            nodePointer = curNode.children[Integer.valueOf(ipComponents[level])];
            if (mode == 1 && nodePointer != null) memoryAccess++; // access pointer array only when nodePointer is not null (tell in bitmap)
            if (mode == 1 && nodePointer == null && curNode.endNode) memoryAccess++; // access data pointer array and fetch pointer (data/null)
            if (nodePointer == null) break;
            memoryAccess++; // access next level node to fetch data and bitmaps
            if (nodePointer.nextHopData != null) nextHopData = nodePointer.nextHopData;
            // proceed to next level
            curNode = curNode.children[Integer.valueOf(ipComponents[level])];
            level++;
        }
        // mode 0: No need to access the null node level [to fetch data and bitmaps]
        if (mode == 0 && curNode.nullNode) memoryAccess--;
        // mode 1: No need to access the null node level if the parent is an end node
        if (mode == 1 && curNode.nullNode && curNode.parent.endNode) memoryAccess--;

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
        while (level < ipComponents.length - 2) {
            if (curNode.children == null || curNode.children[Integer.valueOf(ipComponents[level])] == null)
                curNode.addChild(ipComponents[level], new TreeNode(level+1, curNode));
            curNode = curNode.children[Integer.valueOf(ipComponents[level++])];
        }

        // Add the next hop data
        if (!curNode.addData(ipComponents[level], ipComponents[level + 1], String.join(" ", fields))) return false;
        recordMemory();
        return true;
    }

    public String[] calculateIPComponent(String prefix, int maskBits) {
        String[] standardIPComponents = super.calculateIPComponent(prefix, maskBits);
        if (standardIPComponents == null) return null;
        int length = standardIPComponents.length;
        // change "1.0.255/8" -> "1","0","255","8"
        String[] ipComponents = new String[length + 1];
        System.arraycopy(standardIPComponents, 0, ipComponents, 0, length);
        String prefixMask = ipComponents[length - 1];
        ipComponents[length - 1] = prefixMask.substring(0, prefixMask.indexOf('/'));
        ipComponents[length] = prefixMask.substring(prefixMask.indexOf('/') + 1, prefixMask.length());
        return ipComponents;
    }
}
