# proj216

## Tree Bitmap Simulator

* Load real BGP table to build Trie Tree and Look up 1 million IP addresses in *MillionIPAddrOutput.txt*:
```
./run.sh TreeBitmap
```

* Load real BGP table to build Trie Tree and Look up customized IP addresses in *test.txt*:
```
./run.sh TreeBitmap -t
```
 
* Internal Nodes + End Nodes + Pseudo Null Nodes + Real Null Nodes: 433905
* Internal Nodes + End Nodes + Pseduo Null Nodes: 31998
* Internal Nodes + End Nodes: 25349
* Best Size: 4497KB -> 31998 nodes [Shrink all real null nodes and keep psuduo null nodes]

## UniPrefix Simulator

```
./run.sh Bitmap [-t]
```
* Internal Nodes + End Nodes + Pseudo Null Nodes + Real Null Nodes: 4049552
* Internal Nodes + End Nodes + Pseduo Null Nodes: 107743
* Internal Nodes + End Nodes: 25349

* Best Size: 3592 KB -> 25349 nodes [Shrink all null nodes in all level (mode 0)]
* Best Size: 6538 KB -> 107743 nodes [Shrink all real null nodes and keep psudeo null nodes (mode 1)]

## MultiBit Simulator 

#### // TODO

```
./run.sh MultiBit [-t]
```
