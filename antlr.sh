#!/bin/bash
#
#
SOURCE_DIR=$(cd "$(dirname "$0")" && pwd)
GRAMMAR_DIR=$SOURCE_DIR/src/main/resources
TARGET_DIR=$SOURCE_DIR/src/main/java/kr/andold/terran/bhistory/antlr
FILE_NAME_ANTLR_JAR=$SOURCE_DIR/src/main/resources/antlr4-4.13.0-complete.jar
PACKAGE_ANTLR=kr.andold.terran.bhistory.antlr
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
java -jar $FILE_NAME_ANTLR_JAR -encoding UTF8 -package $PACKAGE_ANTLR -visitor -o $TARGET_DIR $GRAMMAR_DIR/BigHistoryDateTime.g4
java -jar $FILE_NAME_ANTLR_JAR -encoding UTF8 -package $PACKAGE_ANTLR -visitor -o $TARGET_DIR $GRAMMAR_DIR/DateTime.g4
java -jar $FILE_NAME_ANTLR_JAR -encoding UTF8 -package $PACKAGE_ANTLR -visitor -o $TARGET_DIR $GRAMMAR_DIR/UniversalDateTime.g4
