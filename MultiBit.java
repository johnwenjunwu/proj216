import java.util.*;


import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;


class MultiBit extends Trie {

    int [] stride ;
    private Node root;

    public MultiBit(String BGPTablePath, String IPTablePath) {
        super(BGPTablePath, IPTablePath);
        this.stride = new int[super.stride.length]; 
        System.arraycopy(super.stride, 0, this.stride, 0, super.stride.length);
        this.root = new Node(0);
    }

    private class Node {
        String[] prefix ;
        Node [] pointer ;
        HashMap<String, String> data;
        int level;

        Node(int level){
            this.level= level;
            this.prefix = new String [(int)Math.pow(2, stride[level])];
            this.pointer = new Node [(int)Math.pow(2, stride[level])];
            this.data = new HashMap<>();
            increaseNode();
            increaseMemory(ptrSize*(int)Math.pow(2, stride[level])*2);
        }

        public boolean addData(String prefix, String nextHopData) {
            if (data.containsKey(prefix)) return false;
            data.put(prefix, nextHopData);
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
        maskLength[maskBits]++; // record mask length distribution

        Node cur = root;
        int level =0;
        while(level<ipComponents.length-1){
            int index = Integer.valueOf(ipComponents[level]);
            if (cur.pointer[index]==null){
                cur.pointer[index]= new Node(level+1);

            }
            cur = cur.pointer[index];
            level++;
        }

        String prefix = ipComponents[level];
        String strs[] = prefix.split("/");
        int diff = stride[level]-Integer.valueOf(strs[1]);
        int basic = Integer.valueOf(strs[0])<< diff;

        int extend = basic;

        for(int i= 0; i<((int)Math.pow(2, diff));i++){
            extend = basic+i;
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

    @Override
    public int countPrefix() {
        return 0;
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
            System.out.println("Level"+tmp.level);
            int level = tmp.level;

            for(int i =0;i<(int)Math.pow(2, stride[level]);i++){
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

}
