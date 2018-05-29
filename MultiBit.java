

class MultiBit extends Trie {

    public MultiBit(String BGPTablePath, String IPTablePath, boolean modified) {
        super(BGPTablePath, IPTablePath, modified);
    }

    public boolean lookupEntry(String ip) {return false;}

    public boolean insertEntry(String entry) {return false;}
}


/*

import java.util.*;
class MultiBit {
    private static final int STRIDE = 3;
    private class Node {
        String[] prefix = new String [(int)Math.pow(2, STRIDE)];
        Node [] pointer = new Node [(int)Math.pow(2, STRIDE)];
    }
    private Node root = new Node();

    public void put (String prefix){
        if (prefix.length()==0) {

            for(String extend:extension(prefix,STRIDE)) {
                root = put(root,prefix,extend,0);
            }
        }
        else if(prefix.length()%STRIDE==0) {
            root = put(root,prefix,prefix,0);
        }else{
            int diff = STRIDE - prefix.length()%STRIDE;
            for(String extend:extension(prefix,diff)) {
                root = put(root,prefix,extend,0);
            }
        }
    }


    private Node put (Node cur,String prefix, String extend, int level){
        if (cur == null) {
            cur = new Node();
            System.out.println("Node++");
        }

        if(level+STRIDE == extend.length()) {
            // expand prefix may collside with an existing prefix or existing extanded prefix
            // store the longest prefix
            if(cur.prefix[strToInt(extend.substring(level, level+STRIDE))]!=null) {
                if(cur.prefix[strToInt(extend.substring(level, level+STRIDE))].length()<prefix.length()) {
                    cur.prefix[strToInt(extend.substring(level, level+STRIDE))] = prefix;
                }
            }
            else cur.prefix[strToInt(extend.substring(level, level+STRIDE))] = prefix;
            return cur;
        }
        cur.pointer[strToInt(extend.substring(level, level+STRIDE))] =
                put(cur.pointer[strToInt(extend.substring(level, level+STRIDE))], prefix, extend, level+STRIDE);
        return cur;
    }

    // public String longestPrefixMatching (String str){

    // }

    public void display(){

        Queue<Node> queue = new LinkedList<>();
        Queue<Integer> id = new LinkedList<>();
        queue.offer(root);
        id.offer(0);
        int num=0;
        while(!queue.isEmpty()) {
            Node tmp = queue.poll();
            int n = id.poll();
            System.out.println();
            System.out.println("Node:"+n);



            for(int i =0; i<(int)Math.pow(2, STRIDE); i++) {
                System.out.print(paddZeros(Integer.toBinaryString(i),STRIDE));
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


    private int strToInt( String str ){
        int i = 0;
        int num = 0;

        //process each char of the string;
        while( i < str.length()) {
            num *= 2;
            num += str.charAt(i++) - '0'; //minus the ASCII code of '0' to get the value of the charAt(i++)
        }


        return num;
    }

    public String[] extension (String prefix, int diff){
        String[] extend = new String [(int)Math.pow(2, diff)];
        for(int i=0; i<(int)Math.pow(2, diff); i++) {
            extend[i]=prefix+paddZeros(Integer.toBinaryString(i),diff);
        }
        return extend;
    }

    private String paddZeros(String str, int expansion) {
        if(str.length()==expansion)
            return str;

        else {
            String zeros = "";
            for (int i=0; i<expansion-str.length(); i++)
                zeros += '0';
            return zeros.concat(str);
        }

    }
    public static void main(String[] args) {

        MultiBit test = new MultiBit();
        test.put("1");
        test.put("111");
        test.put("11001");
        test.put("0");
        test.put("100000");
        test.put("1000");
//
//        test.put("100");
//        test.put("110");
        test.display();

    }

}





*/
