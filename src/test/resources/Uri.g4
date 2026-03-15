/**
 * RFC3986
 */
grammar Uri;
import	AsciiLexer, AsciiGrammer;

@members	{
	private final org.slf4j.Logger logUri = org.slf4j.LoggerFactory.getLogger(getClass());
}

/**
 *  uri = <As defined in Section 3 of [RFC3986]>
 */
uri:	scheme COLON hierpart (QUESTION query)? (NUMBER xxxfragment)?;
hierpart:	(SLASH SLASH authority pathabempty) | pathabsolute | pathrootless | pathempty;

//	2.1.  Percent-Encoding
pctencoded:	 PERCENT tHEXDIG tHEXDIG;

//	2.2.  Reserved Characters
reserved:	gendelims | subdelims;
gendelims:	COLON | SLASH | EXCLAMATION | NUMBER | BRACES_OPEN | BRACKETS_CLOSE | AT;
subdelims:	EXCLAMATION | DOLLAR | AMPERSAND | SQUOTE  | PARENTHESESOPEN | PARENTHESESCLOSE | ASTERISK | PLUS | COMMA | SEMICOLON | EQUALS;
                  
//	2.3.  Unreserved Characters
unreserved:	tALPHA | tDIGIT | MINUS | DOT | UNDERSCORE | TILDE;

//	3.1.  Scheme
scheme:	tALPHA (tALPHA | tDIGIT | PLUS | MINUS | DOT)*;

//	3.2.  Authority
authority:	(userinfo AT)? host (COLON port)?;

//	3.2.1.  User Information
userinfo:	(unreserved | pctencoded | subdelims | COLON)*;

//	3.2.2.  Host
host:	tIPliteral | tIPv4address | regname;

tIPliteral:	BRACKETS_OPEN  (tIPv6address | tIPvFuture) BRACKETS_CLOSE;
tIPvFuture:	LOWERCASEV tHEXDIG+ DOT (unreserved | subdelims | COLON)+;
tIPv6address:	(h16 COLON) (h16 COLON) (h16 COLON) (h16 COLON) (h16 COLON) (h16 COLON) ls32
			|	COLON COLON (h16 COLON) (h16 COLON) (h16 COLON) (h16 COLON) (h16 COLON) ls32
			|	(h16) COLON COLON (h16 COLON) (h16 COLON) (h16 COLON) (h16 COLON) ls32
			|	((h16 COLON)+ h16) COLON COLON (h16 COLON) (h16 COLON) (h16 COLON) ls32
			|	((h16 COLON) (h16 COLON)+ h16) COLON COLON (h16 COLON) (h16 COLON) ls32
			|	((h16 COLON) (h16 COLON) (h16 COLON)+ h16) COLON COLON h16 COLON ls32
			|	((h16 COLON) (h16 COLON) (h16 COLON) (h16 COLON)+ h16) COLON COLON  ls32
			|	((h16 COLON) (h16 COLON) (h16 COLON) (h16 COLON) (h16 COLON)+ h16) COLON COLON h16
			|	((h16 COLON) (h16 COLON) (h16 COLON) (h16 COLON) (h16 COLON) (h16 COLON)+ h16) COLON COLON;
//                  ; least-significant 32 bits of address
ls32:	(h16 COLON h16) | tIPv4address;
//                  ; 16 bits of address represented in hexadecimal
h16:	tHEXDIG tHEXDIG? tHEXDIG? tHEXDIG?;
tIPv4address:	decoctet DOT decoctet DOT decoctet DOT decoctet;
decoctet:	tDIGIT	//                 ; 0-9
		| tDIGIT_EXCEPT_0 tDIGIT	//         ; 10-99
		| NUMBER1 tDIGIT tDIGIT	//            ; 100-199
		| NUMBER2 (NUMBER0 | NUMBER1 | NUMBER2 | NUMBER3 | NUMBER4) tDIGIT	//     ; 200-249
		| NUMBER2 NUMBER5 (NUMBER0 | NUMBER1 | NUMBER2 | NUMBER3 | NUMBER4 | NUMBER5);	//          ; 250-255
regname:	(unreserved | pctencoded | subdelims)*;
  
//	3.2.3.  Port
port:	tDIGIT*;

//	3.3.  Path
path:	pathabempty	//    ; begins with "/" or is empty
	| pathabsolute	//   ; begins with "/" but not "//"
	| pathnoscheme	//   ; begins with a non-colon segment
	| pathrootless	//   ; begins with a segment
	| pathempty;	//      ; zero characters
pathabempty:	(SLASH segment)*;
pathabsolute:	SLASH (segmentnz (SLASH segment)*)?;
pathnoscheme:	segmentnznc (SLASH segment)*;
pathrootless:	segmentnz (SLASH segment)*;
pathempty:			;	//	path-empty    = 0<pchar>
segment:	pchar*;
segmentnz:	pchar+;
segmentnznc:	(unreserved | pctencoded | subdelims | AT)*;	//	; non-zero-length segment without any colon ":"
pchar:	unreserved | pctencoded | subdelims | COLON | AT;

//	3.4.  Query
query:	(pchar | SLASH | EXCLAMATION)*;

//	3.5.  Fragment
xxxfragment:	(pchar | SLASH | EXCLAMATION)*;
