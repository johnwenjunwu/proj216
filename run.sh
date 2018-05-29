#!/bin/bash

TMP_DIR=tmp/

JAVA_FILE=*.java
JAVA_ENTRY=Trie

BGP_TABLE=bgptable.txt
BGP_ADDRESS=http://bgp.potaroo.net/as2.0/bgptable.txt

IP_TABLE=MillionIPAddrOutput.txt
TEST_TABLE=test.txt
RES_FILE=result.txt

MODIFIED=false

echo "Usage:"
echo "The program use [${BGP_TABLE}] to build Trie Tree"
echo "./run.sh [ClassName]    will look up every IP address in [${IP_TABLE}]"
echo "./run.sh [ClassName] -t will look up every IP address in [${TEST_TABLE}]"
echo "ClassName: MultiBit/Bitmap/TreeBitmap"
echo "e.g. [./run.sh TreeBitmap -t] will be a valid command"
echo "The results are saved in [${RES_FILE}]"
echo

if [ $# != 1 -a $# != 2 ] ; then
  echo "Wrong Parameter"
  exit 1
fi

# download bgptable if necessary
if [ ! -f "${BGP_TABLE}" ]; then
  echo "Downloading ${BGP_TABLE}..."
  wget -q -O ${BGP_TABLE} ${BGP_ADDRESS}
fi

# choose the loopup table
if [ "$2" == "-t" -o "$2" == "-test" ]; then
  IP_TABLE=${TEST_TABLE}
fi

# clean any existing files
rm -rf ${TMP_DIR}
mkdir ${TMP_DIR}

# copy source file
cp ${JAVA_FILE} ${TMP_DIR}

# compile java source file
echo "Compiling $1.Java..."
javac ${TMP_DIR}${JAVA_FILE}
if [ "$?" -ne "0" ]; then
    echo "ERROR: Compilation Failed" 1>&2
    rm -rf ${TMP_DIR}
    exit 1
fi

# run the java executable file
echo "Testing $1.Java..."
java -classpath ${TMP_DIR} ${JAVA_ENTRY} $1 ${BGP_TABLE} ${IP_TABLE} ${MODIFIED} > ${RES_FILE}

# clean up
rm -rf ${TMP_DIR}
