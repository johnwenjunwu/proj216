# proj216

## Project Report
See [UniPrefix_Report.md](https://github.com/johnwenjunwu/proj216/blob/master/UniPrefix_Report.md)

## Author: (in alphabetical order)
- Wenjun Wu (105068032)
- Yutong Han (705025619)
- Zhehan Li (404888352)
- Zutian Luo (804880250)

## Tree Bitmap Simulator

* Load real BGP table to build Trie Tree and Look up 1 million IP addresses in *MillionIPAddrOutput.txt*:
```
./run.sh TreeBitmap
```

* Load real BGP table to build Trie Tree and Look up customized IP addresses in *sample_iptable.txt*:
```
./run.sh TreeBitmap -t
```
 
* Internal Nodes + End Nodes + Pseudo Null Nodes + Real Null Nodes: 433905
* Internal Nodes + End Nodes + Pseduo Null Nodes: 31998
* Internal Nodes + End Nodes: 25349
* Best Size: 4497KB -> 31998 nodes [Shrink all real null nodes and keep psuduo null nodes]


* Total Trie Node: 31815 nodes
* Total Memory Storage: 4486 KB

## UniPrefix Bitmap Simulator

```
./run.sh UniPrefixBitmap [-t]
```
* Internal Nodes + End Nodes + Pseudo Null Nodes + Real Null Nodes: 4049552
* Internal Nodes + End Nodes + Pseduo Null Nodes: 107743
* Internal Nodes + End Nodes: 25349

* Best Size: 3592 KB -> 25349 nodes [Shrink all null nodes in all level (mode 0)]
* Best Size: 6538 KB -> 107743 nodes [Shrink all real null nodes and keep psudeo null nodes (mode 1)]

* Total Memory Storage: 3592 KB -> Total Trie Node: 25298 nodes [mode 0]
* Total Memory Storage: 6464 KB -> Total Trie Node: 105693 nodes [mode 1] 


## MultiBit Simulator 
```
./run.sh MultiBit [-t]
```
* Total Trie Node: 25349 nodes
* Total Memory Storage: 41192 KB
## UniPrefixMultibit Simulator 

```
./run.sh UniPrefixMultibit [-t]
```
* Total Trie Node: 4049552 nodes
* Total Memory Storage: 3303113 KB
