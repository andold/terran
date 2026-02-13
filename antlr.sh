#!/bin/bash
#
#
HOME_DIR=/home/andold
SOURCE_DIR=$HOME_DIR/src/eclipse-workspace/bhistory
TARGET_DIR=$HOME_DIR/src/main/java/kr/andold/bhistory/antlr
FILE_NAME_ANTLR_JAR=$HOME_DIR/src/main/resources/antlr-4.10.1-complete.jar
PACKAGE_ANTLR=kr.andold.bhistory.antlr
#
#
#
date
#
#
#
rm -f $TARGET_DIR/*
#
#
#
java -jar $FILE_NAME_ANTLR_JAR -encoding UTF8 -package $PACKAGE_ANTLR -visitor -o $TARGET_DIR $SOURCE_DIR/src/main/resources/antlr4v2/BigHistoryDateTime.g4
java -jar $FILE_NAME_ANTLR_JAR -encoding UTF8 -package $PACKAGE_ANTLR -visitor -o $TARGET_DIR $SOURCE_DIR/src/main/resources/antlr4v2/DateTime.g4
java -jar $FILE_NAME_ANTLR_JAR -encoding UTF8 -package $PACKAGE_ANTLR -visitor -o $TARGET_DIR $SOURCE_DIR/src/main/resources/antlr4v2/UniversalDateTime.g4
