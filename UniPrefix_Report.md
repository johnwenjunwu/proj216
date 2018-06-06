## Uni-Prefix Bitmap
(Team members.. @UCLA CS216)

### 0 See what we've done in just few lines!
IP prefix lookup is of great importance in routing. Currently, several fancy prefix lookup algorithms (e.g. Tree Bitmap, Multibit Trie) are widely used in many real-world scenarios. We came up with a modified algorithm on the basis of Multibit Trie and Lulea called **Uni-Prefix Bitmap** to acheive better performance (fewer nodes as well as shorter lookup time). 

We've implemented four different IP prefix lookup engines in JAVA: (pure) Multibit Trie, Uni-Prefix Multibit Trie, Tree Bitmap and Uni-Prefix Bitmap.

// insert "four schemes.png" here to show different schemes, then create a table here to show their performance

// e.g. (# of nodes, instead of using absolute numbers, try ratio?), (average node size), (total memory usage)

## 1 Backgrounds
- Related Works (In brief)
    - A unibit trie is a tree in which each node is an array containing a 0-pointer and a 1-pointer, so it may make 32 accesses for a 32-bit prefix lookup. But multibit trie allows the number of indexing bits to change, by expanding the prefix length. Multibit trie searches faster at the cost of larger database size.
    - Lulea is a multibit-trie scheme that uses fixed-stride trie nodes but uses bitmap compression to replace consecutive identical elements with a single value. A node bitmap (0 refers to removed positions) allows fast indexing on the compressed nodes.
    - Tree Bitmap // (ZL)
        - End Node Optimization: push back end nodes, what are the NullNodes

## 2 Uni-Prefix Design
The core of Uni-Prefix design is basically pushing the data (aka prefix) to nodes in the next level to shrink the size of each node.
- How did we come up with this idea?
- What are the benefits that this modification can bring us?

### 2.1 Uni-Prefix Multibit Trie // (WW) 
- w/o pushback hack
    - The figure below shows the orginal optimization that we move the prefix to the next layer. In this case, we can reduce the size of each node to half, but will increase the memory access for each ip lookup by 1. ![alt text](https://github.com/johnwenjunwu/proj216/blob/master/figures/Uniprefix.png "Original Uni-Prefix")
    - It turns out that we waste a lot storage at leaf nodes, since there is only one prefix without any pointer to other child nodes, which leads the our next optimization to reduce leaf nodes by pushing the prefix one layer back.
        
- W/ pushback hack
    - As shown in the figure below, we can push all the prefix at leaf node one layer up to replace the pointer. But we need one extra bit to note whether it's a pointer to the child node or a prefix, which can be put together with the 32-bit pointer or prefix. It allows 0 extra memory access when it's fetched with each item in the node. ![alt text](https://github.com/johnwenjunwu/proj216/blob/master/figures/UniprefixWithPushBack.png "Original Uni-Prefix") 
    - After this modification, we can see that all the leaf nodes is a mixed storage with pointers and prefixes with 1 extra bit. The number of nodes are exactly same as multibit trie, but node's size is only the half of before. And also, the memory access for any ip lookup is same as before.

### 2.2 Uni-Prefix Bitmap // (ZL)
In order to avoid the obvious waste, we compress the pointers using bitmap + pointer array, which is pretty similar to the method used in Tree Bitmap. In Tree Bitmap, we can only shrink all **real null nodes**. The pseudo null nodes, however, have to be kept, because data and pointers are stored separately. There is no such worry in Uni-Prefix Bitmap as it only compresses pointers. 

// 1 figure (actually 2 in 1) to show why tree bitmap cannot reduce pseudo null node but uniprefix bitmap can.

## 3 Experiments, Results & Analysis (TODO)
- Upsides & downsides of our design
## 4 Conclusion
