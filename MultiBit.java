import java.util.*;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;


class MultiBit extends Trie {
    private static final int STRIDE = 8;
    int [] stride ;
    private Node root;


    public MultiBit(String BGPTablePath, String IPTablePath, boolean modified) {
        super(BGPTablePath, IPTablePath, modified);
        this.root = new Node();
        this.stride = new int[] {8,8,8,8};
    }

    


    private class Node {
        String[] prefix = new String [(int)Math.pow(2, STRIDE)];
        Node [] pointer = new Node [(int)Math.pow(2, STRIDE)];
        HashMap<String, String> data=new HashMap<>();

        public boolean addData(String prefix, String nextHopData) {
            if (data.containsKey(prefix)) return false;
            data.put(prefix, nextHopData);
            // increaseMemory(dataNodeSize);
            // if (prefix != rootMatch || data.size() > 1) notAEndNode();
            return true;
        }
    }

    public boolean lookupEntry(String ip){
        if(ip==null)return false;
        int level = 0;
        int memoryAccess = 0;
        Node curNode = root;
        String nextHopData = null;
        String[] ipComponents = calculateIPComponent(ip, 32);
        if (ipComponents == null) return false;

        while(true){
            int ipValue;
            if(ipComponents[level].indexOf('/')!=-1){
                ipValue = Integer.valueOf(ipComponents[level].split("/")[0]);
            }else ipValue = Integer.valueOf(ipComponents[level]);
            memoryAccess++;//access to this level node
            String longestMatch = internalBestMacth(ipValue,curNode);
            if(longestMatch!=null) {
                memoryAccess++;
                //fetch the data 
                nextHopData = longestMatch;
            }
            if(curNode.pointer[ipValue]==null)break;
            memoryAccess++;//access pointer array
            curNode = curNode.pointer[ipValue];
            level++;
            
        }
        System.out.println("Look up IP address : " + ip + " => Memory Access: " + memoryAccess + " times");
        System.out.println("Next Hop Data: " + (nextHopData == null ? "Not Found" : nextHopData));
    

        return true;

    }
    public String internalBestMacth(int ipValue, Node curNode){
        if(curNode.prefix[ipValue]!=null) {
            if (curNode.data.containsKey(curNode.prefix[ipValue]))
            return curNode.data.get(curNode.prefix[ipValue]);
        } 
         return null;
    }

    public boolean insertEntry (String entry){
        if (entry==null)return false;
        String[] fields = entry.split(" +");
        int maskBits = calculateMask(fields[0]);
        String[] ipComponents = calculateIPComponent(fields[0], maskBits); // turn prefix into array of n*[child index]+[data index]
        if (maskBits == -1 || ipComponents == null) return false;
        Node cur = root;
        int level =0;
        while(level<ipComponents.length-1){
            int index = Integer.valueOf(ipComponents[level]);
            if (cur.pointer[index]==null){
                cur.pointer[index]= new Node();
                increaseNode();
                increaseMemory((int)Math.pow(2, STRIDE)*2);
            }
            cur = cur.pointer[index];
            level++;
        }

        String prefix = ipComponents[level];
        for(int extend :extension(prefix)){
            if(cur.prefix[extend]!=null){
                if(Integer.valueOf(cur.prefix[extend].split("/")[1])<Integer.valueOf(prefix.split("/")[1])){
                    cur.prefix[extend]=prefix;
                }
            }
            else cur.prefix[extend]=prefix;
        }
        if (!cur.addData(prefix, String.join(" ", fields))) return false;
        recordMemory();
        return true;
    }

    public int[] extension (String prefix){
        String strs[] = prefix.split("/");
        int diff = STRIDE-Integer.valueOf(strs[1]);
        int basic = Integer.valueOf(strs[0])<< diff;
        int []res = new int [(int)Math.pow(2, diff)];
        for(int i=0;i<(int)Math.pow(2, diff);i++){
            res[i]=basic+i;
        }
        return res;


    }


    public void display(){
        
        Queue<Node> queue = new LinkedList<>();
        Queue<Integer> id = new LinkedList<>();
        queue.offer(root);
        id.offer(0);
        int num=0;
        while(!queue.isEmpty()){
            Node tmp = queue.poll();
            int n = id.poll();
            System.out.println();
            System.out.println("Node:"+n);
            
           
            
            for(int i =0;i<(int)Math.pow(2, STRIDE);i++){
                System.out.print(i);
                System.out.print(":");
                if(tmp.prefix[i]==null) System.out.print("NULL");
                else System.out.print(tmp.prefix[i]+"*");

                
                if(tmp.pointer[i]!=null) {
                    queue.offer(tmp.pointer[i]);
                    num++;
                    id.offer(num);
                    System.out.print("|Point to:Node" +num);
                    
                }else{
                    System.out.print("|Point to:Null");
                }

                System.out.println();

            }

        }
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
}

