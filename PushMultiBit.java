import java.util.*;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;


class PushMultiBit extends Trie {
    private static final int STRIDE = 8;
    int [] stride ;
    private Node root;

    public PushMultiBit(String BGPTablePath, String IPTablePath, boolean modified) {
        super(BGPTablePath, IPTablePath, modified);
        this.root = new Node();
        this.stride = new int[] {8,8,8,8};
    }
    private class Node {
        String prefix = null;
        Node [] pointer = new Node [(int)Math.pow(2, STRIDE)];
        String nexthoop = null;
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
        if(curNode.pointer[ipValue]!=null) {
            // if (curNode.data.containsKey(curNode.prefix[ipValue]))
            // return curNode.data.get(curNode.prefix[ipValue]);
            if(curNode.pointer[ipValue].nexthoop!=null)
                return curNode.pointer[ipValue].nexthoop;
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
                increaseMemory((int)Math.pow(2, STRIDE)*ptrSize+ptrSize);
            }
            cur = cur.pointer[index];
            level++;
        }

        String prefix = ipComponents[level];
        for(int extend :extension(prefix)){
            if(cur.pointer[extend]!=null && cur.pointer[extend].prefix!=null){
                if(Integer.valueOf(cur.pointer[extend].prefix.split("/")[1])<Integer.valueOf(prefix.split("/")[1])){
                    cur.pointer[extend].prefix = prefix;
                    cur.pointer[extend].nexthoop = entry;
                    
                }
            }
            else {
                cur.pointer[extend] = new Node();
                increaseNode();
                increaseMemory((int)Math.pow(2, STRIDE)*ptrSize+ptrSize);
                cur.pointer[extend].prefix = prefix;
                cur.pointer[extend].nexthoop = entry;
              
            }
          
            
        }
        
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
            if(tmp.prefix==null) System.out.println("NULL");
                else System.out.println(tmp.prefix);

            if(tmp.nexthoop==null) System.out.println("NULL");
            else System.out.println(tmp.nexthoop);
           
            
            for(int i =0;i<(int)Math.pow(2, STRIDE);i++){
                
       
                if(tmp.pointer[i]!=null) {
                    queue.offer(tmp.pointer[i]);
                    num++;
                    id.offer(num);
                    System.out.print(i);
                    System.out.println("|Point to:Node" +num);
                    
                }

            }

        }
    }


}