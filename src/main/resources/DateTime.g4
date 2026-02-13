/*
 * @(#)DateTime.g4 2022-04-24
 *
 * Copyright 2024 andold@naver.com All rights Reserved. 
 * andold@naver.com PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

grammar DateTime;

@header {
import java.util.Calendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kr.andold.utils.Utility;
import kr.andold.terran.bhistory.domain.BigBang;
import kr.andold.terran.bhistory.domain.AnnoDomini;
}

@members	{
	public static Object OBJECT = null;

	private final Logger log = LoggerFactory.getLogger(getClass());

}

// Parser Rules - global. include all document.
dateString
	:	bigbang
	|	ad
;


ad returns [AnnoDomini value]
:(
	//	2022-03-19 13:24:32
	year=INTEGER MINUS month=INTEGER MINUS day=INTEGER hour=INTEGER COLON minute=INTEGER COLON second=INTEGER {
	$value = new AnnoDomini(Utility.parseLong($year.text), Utility.parseLong($month.text), Utility.parseLong($day.text)
		, Utility.parseLong($hour.text), Utility.parseLong($minute.text), Utility.parseLong($second.text), null
	);	
});


bigbang returns [BigBang value]
:(
	// 1E-43초 빅뱅후
	base=INTEGER (EXPONENT_MARK sign? exponent=INTEGER)? SECOND BIGBANG	{
	$value = new BigBang(0.0, Utility.parseDouble($base.text, $EXPONENT_MARK.text, $sign.text, $exponent.text));
})
|(
	// 23년 빅뱅후
	head=INTEGER (DOT tail=INTEGER)? YEAR BIGBANG	{
	$value = new BigBang(Utility.parseDouble($head.text, $DOT.text, $tail.text), null);
})
|(
	// 10만년 빅뱅후
	head=INTEGER (DOT tail=INTEGER)? NUMBER1E4 YEAR BIGBANG	{
	$value = new BigBang(Utility.parseDouble($head.text, $DOT.text, $tail.text) * 10000.0, null);
})
|(	// 137.8억년 빅뱅후
	head=INTEGER (DOT tail=INTEGER)? NUMBER1E8 YEAR BIGBANG	{
	$value = new BigBang(Utility.parseDouble($head.text, $DOT.text, $tail.text) * 100000000.0, null);
});

sign:	PLUS | MINUS;

fragment DIGIT:	[0-9];
fragment NEWLINE:	'\r'? '\n';

BLANK:		[ \u000B\f\r\u00A0]	-> skip;
BLANK_LINE:	{getCharPositionInLine() == 0}? [ \u000B\f\r\u00A0]* NEWLINE	-> skip;

BIGBANG:	'빅뱅' ('직후' | '후')?;
COLON:	':';
DOT:	'.';
EXPONENT_MARK:	[eE];
INTEGER:	DIGIT+;
MINUS:		'-';
NUMBER1E1:	'십';
NUMBER1E2:	'백';
NUMBER1E3:	'천';
NUMBER1E4:	'만';
NUMBER1E8:	'억';
PLUS:		'+';
SECOND:	'초';
YEAR:	'년';



