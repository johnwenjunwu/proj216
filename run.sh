#!/bin/bash

TMP_DIR=tmp/

JAVA_FILE=*.java
JAVA_ENTRY=Trie

BGP_TABLE=bgptable.txt
BGP_ADDRESS=http://bgp.potaroo.net/as2.0/bgptable.txt

IP_TABLE=MillionIPAddrOutput.txt
RES_FILE=result.txt

SAMPLE_BGP=sample_bgptable.txt
SAMPLE_IP=sample_iptable.txt
SAMPLE_RES=sample_result.txt

echo
echo "Usage:"
echo
echo "./run.sh [ClassName].java will run the program on real data set"
echo "=> [${BGP_TABLE}] to build Trie Tree, [${IP_TABLE} to look up]"
echo
echo "./run.sh [ClassName].java -t will run the program on real data set"
echo "=> [${SAMPLE_BGP}] to build Trie Tree, [${SAMPLE_IP} to look up]"
echo
echo "ClassName: MultiBit | UniPrefixMultibit | TreeBitmap | UniPrefixBitmap"
echo
echo "e.g. [./run.sh TreeBitmap.java] will be a valid command"
echo "The results are saved in [${RES_FILE}]"
echo "=============================================================="
echo

if [ ${1:(-4)} == "java" ]; then
  JAVA_CLASS=${1:0:${#1}-5}
else
  echo "Should Append .java As Suffix!"
  exit 1
fi

if [ $# != 1 -a $# != 2 ]; then
  echo "Wrong Parameter"
  exit 1
fi

# choose the loopup table
if [ "$2" == "-t" -o "$2" == "-test" ]; then
  BGP_TABLE=${SAMPLE_BGP}
  IP_TABLE=${SAMPLE_IP}
# download bgptable if necessary
elif [ ! -f "${BGP_TABLE}" ]; then
  echo "Downloading ${BGP_TABLE}..."
  wget -q -O ${BGP_TABLE} ${BGP_ADDRESS}
fi

# clean any existing files
rm -rf ${TMP_DIR}
mkdir ${TMP_DIR}

# copy source file
cp ${JAVA_FILE} ${TMP_DIR}

# compile java source file
echo "Compiling $1..."
javac ${TMP_DIR}${JAVA_FILE}
if [ "$?" -ne "0" ]; then
    echo "ERROR: Compilation Failed" 1>&2
    rm -rf ${TMP_DIR}
    exit 1
fi

# run the java executable file
echo "Testing $1..."
java -classpath ${TMP_DIR} ${JAVA_ENTRY} ${JAVA_CLASS} ${BGP_TABLE} ${IP_TABLE} > ${RES_FILE}

# compare with the sample result if necessary
if [ "$2" == "-t" -o "$2" == "-test" ]; then
  diff ${RES_FILE} ${SAMPLE_RES} | grep -q "Next Hop Data"
  if [ "$?" -ne "0" ]; then
    echo "Look Up Result Correct!"
  else
    echo "Look Up Result Wrong!"
    echo "Run [diff ${RES_FILE} ${SAMPLE_RES}] to See the Difference"
  fi
fi

# clean up
rm -rf ${TMP_DIR}
