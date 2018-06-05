## Uni-Prefix Bitmap
(Team members.. @UCLA CS216)

### See what we've done in few lines!
IP prefix lookup is of great importance in routing. Currently, several fancy prefix lookup algorithms (e.g. Tree Bitmap, Multibit Trie) are widely used in many real-world scenarios. We came up with a modified algorithm on the basis of Multibit Trie and Lulea called **Uni-Prefix Bitmap** to acheive better performance (fewer nodes as well as shorter lookup time). 

We've implemented four different IP prefix lookup engines in JAVA: (pure) Multibit Trie, Uni-Prefix Multibit Trie, Tree Bitmap and Uni-Prefix Bitmap.
// insert "four schemes.png" here to show different schemes
// create a table here to show their performance
// e.g. (# of nodes, instead of using absolute number, try ratio?)
//  (# of levels ~ lookup time, note to mark different mode of tree bitmap and uniprefix bitmap, w/o pushback, w/ pushback, tackling w/ pseudoNullNodes)
// (average node size)
//  (total memory usage)

## 1 Backgrounds
- Related Works (In brief)
    - Unibit Trie & Multibit Trie
    - Lulea (necessary?)
    - Tree Bitmap
        - End Node Optimization: push back end nodes, what are the NullNodes

## 2 Uni-Prefix Design
The core of Uni-Prefix design is basically pushing the data (aka prefix) to nodes in the next level to shrink the size of each node.
- How did we come up with this idea?
- What are the benefits that this modification can bring us?

### 2.1 Uni-Prefix Multibit Trie 
- w/o pushback hack
    - //1 figure here for illustration
        - How did we come up with the pushback hack?
- W/ pushback hack
    - // 1 figure here as well..

### 2.2 Uni-Prefix Bitmap
In order to avoid the obvious waste, we compress the pointers using bitmap + pointer array, which is pretty similar to the method used in Tree Bitmap. In Tree Bitmap, we can only shrink all **real null nodes**. The pseudo null nodes, however, have to be kept, because data and pointers are stored separately. There is no such worry in Uni-Prefix Bitmap as it only compresses pointers. 
// 1 figure (actually 2 in 1) to show why tree bitmap cannot reduce pseudo null node but uniprefix bitmap can.

## 3 Implementation
## 4 Experiments, Results & Analysis
## 5 Conclusion