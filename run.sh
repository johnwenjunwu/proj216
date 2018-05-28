#!/bin/bash

TMP_DIR=tmp/

JAVA_EXE=TreeBitmap
JAVA_FILE=${JAVA_EXE}.java

BGP_TABLE=bgptable.txt
BGP_ADDRESS=http://bgp.potaroo.net/as2.0/bgptable.txt

IP_TABLE=MillionIPAddrOutput.txt
TEST_TABLE=test.txt

RES_FILE=result.txt

echo "Usage:"
echo "The program use [${BGP_TABLE}] to build Trie Tree"
echo "[./run.sh] will look up every IP address in [${IP_TABLE}]"
echo "[./run.sh -t] will look up every IP address in [${TEST_TABLE}]"
echo "The results are saved in [${RES_FILE}]"
echo

# download bgptable if necessary
if [ ! -f "${BGP_TABLE}" ]; then
  echo "Downloading ${BGP_TABLE}..."
  wget -q -O ${BGP_TABLE} ${BGP_ADDRESS}
fi

# choose the loopup table
if [ "$1" == "-t" -o "$1" == "-test" ]
then
  IP_TABLE=${TEST_TABLE}
fi

# clean any existing files
rm -rf ${TMP_DIR}
mkdir ${TMP_DIR}

# copy source file
cp ${JAVA_FILE} ${TMP_DIR}

# compile java source file
echo "Compiling ${JAVA_FILE}..."
javac ${TMP_DIR}${JAVA_FILE}
if [ "$?" -ne "0" ]
then
    echo "ERROR: Compilation of ${JAVA_FILE} failed" 1>&2
    rm -rf ${TMP_DIR}
    exit 1
fi

# run the java executable file
echo "Testing ${JAVA_FILE}..."
java -classpath ${TMP_DIR} ${JAVA_EXE} ${BGP_TABLE} ${IP_TABLE} > ${RES_FILE}

# clean up
rm -rf ${TMP_DIR}
