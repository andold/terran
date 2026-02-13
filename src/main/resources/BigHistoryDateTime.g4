grammar BigHistoryDateTime;

@header {
import java.util.Calendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kr.andold.utils.Utility;
import kr.andold.terran.bhistory.domain.BigHistoryDateTime;
}

@members	{
	private final Logger log = LoggerFactory.getLogger(getClass());

}

// Parser Rules - global. include all document.
bigHistoryDateTime returns [BigHistoryDateTime value]
:	bigbang	{
	$value = BigHistoryDateTime.of($bigbang.value);
}
|	annoDomini	{
	$value = BigHistoryDateTime.of($annoDomini.valueYear, $annoDomini.valueMonth, $annoDomini.valueDay);
}
;


annoDomini returns [Integer valueYear, Integer valueMonth, Integer valueDay]
:	'AD'? year=INTEGER yearMark? (MINUS? month=INTEGER monthMark? MINUS? day=INTEGER dayMark?)?	{
	$valueYear = Utility.parseInteger($year.text);
	$valueMonth = Utility.parseInteger($month.text);
	$valueDay = Utility.parseInteger($day.text);
}
;


bigbang returns [Double value]
:
	numberFloat timeUnitMark	{	$value = Utility.parseDouble($numberFloat.text) * $timeUnitMark.value; }
;
timeUnitMark returns [Double value]
:	secondMark?	{	$value = 1d;	}
|	minuteMark	{	$value = 60d;	}
|	yearMark	{	$value = 3600d * 24d * 31556926.08d;	}
;

yearMark: '년' | 'year';
monthMark: '월';
dayMark: '일';
minuteMark: '분' | 'minute' 's'?;
secondMark: '초' | 'sec' ('ond'? 's')?;

numberFloat
:
	INTEGER (DOT INTEGER)? EXPONENT_MARK sign? INTEGER
;


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



