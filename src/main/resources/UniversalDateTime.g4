grammar UniversalDateTime;

@header {
import java.util.Calendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kr.andold.utils.Utility;
import kr.andold.terran.bhistory.domain.UniversalDateTime;
}

@members	{
	private final Logger log = LoggerFactory.getLogger(getClass());

}

// Parser Rules - global. include all document.
universalDateTime returns [UniversalDateTime value]
:(	//	BIGBANG
	// 1E-43초 빅뱅후
	base=INTEGER (EXPONENT_MARK sign? exponent=INTEGER)? SECOND BIGBANG	{
	$value = new UniversalDateTime(UniversalDateTime.UniversalDateTimeOrigin.BIGBANG, 0.0, $base.text, $EXPONENT_MARK.text, $sign.text, $exponent.text);
})|(
	// 23년 빅뱅후
	head=INTEGER (DOT tail=INTEGER)? YEAR BIGBANG	{
	$value = new UniversalDateTime(UniversalDateTime.UniversalDateTimeOrigin.BIGBANG, $head.text, $DOT.text, $tail.text);
})|(
	// 10만년 빅뱅후
	head=INTEGER (DOT tail=INTEGER)? NUMBER1E4 YEAR BIGBANG	{
	$value = new UniversalDateTime(UniversalDateTime.UniversalDateTimeOrigin.BIGBANG, $head.text, $DOT.text, $tail.text, 10000L);
})|(	// 137.8억년 빅뱅후
	head=INTEGER (DOT tail=INTEGER)? NUMBER1E8 YEAR BIGBANG	{
	$value = new UniversalDateTime(UniversalDateTime.UniversalDateTimeOrigin.BIGBANG, $head.text, $DOT.text, $tail.text, 100000000L);
})|(//	KOREAN_LUNAR
	// 1592년(선조) 5월 1일(음력 4월 13일)
	year=INTEGER YEAR BRACKET_LEFT KING BRACKET_RIGHT INTEGER MONTH INTEGER DAY BRACKET_LEFT LUNAR month=INTEGER MONTH day=INTEGER DAY BRACKET_RIGHT	{
	$value = new UniversalDateTime(UniversalDateTime.UniversalDateTimeOrigin.KOREAN_LUNAR, $year.text, $month.text, $day.text);
});


sign:	PLUS | MINUS;

fragment DIGIT:	[0-9];
fragment NEWLINE:	'\r'? '\n';

BLANK:		[ \u000B\f\r\u00A0]	-> skip;
BLANK_LINE:	{getCharPositionInLine() == 0}? [ \u000B\f\r\u00A0]* NEWLINE	-> skip;

BIGBANG:	'빅뱅' ('직후' | '후')?;
BRACKET_LEFT:	'(';
BRACKET_RIGHT:	')';
COLON:	':';
DAY:	'일';
DOT:	'.';
EXPONENT_MARK:	[eE];
INTEGER:	DIGIT+;
KING:	'선조';
LUNAR:	'음력';
MINUS:		'-';
MONTH:	'월';
NUMBER1E1:	'십';
NUMBER1E2:	'백';
NUMBER1E3:	'천';
NUMBER1E4:	'만';
NUMBER1E8:	'억';
PLUS:		'+';
SECOND:	'초';
YEAR:	'년';



