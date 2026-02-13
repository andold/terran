package kr.andold.terran.bhistory.domain;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;

import kr.andold.terran.bhistory.antlr.UniversalDateTimeLexer;
import kr.andold.terran.bhistory.antlr.UniversalDateTimeParser;
import kr.andold.utils.Utility;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UniversalDateTime {
	public enum UniversalDateTimeOrigin {
		BIGBANG
		, BEFORE_CHRIST
		, ANNO_DOMINI
		, YEAR_AGO
		, KOREAN_LUNAR

	}

	private UniversalDateTimeOrigin origin;

	//	BIGBANG
	private Double yearBigBang;
	private Double secondBigBang;

	//	KOREAN_LUNAR
	private Integer year;
	private Integer month;
	private Integer day;

	private Integer hour;
	private Integer minute;
	private Integer second;

	private Integer offset;

	//	new UniversalDateTime(UniversalDateTime.UniversalDateTimeOrigin.BIGBANG, 0.0, $base.text, $EXPONENT_MARK.text, $sign.text, $exponent.text);
	public UniversalDateTime(UniversalDateTimeOrigin origin, Double yearBigBang, String base, String EXPONENT_MARK, String sign, String exponent) {
		this.origin = origin;
		this.yearBigBang = yearBigBang;
		this.secondBigBang = Utility.parseDouble(base, EXPONENT_MARK, sign, exponent);
	}
	public UniversalDateTime(UniversalDateTimeOrigin origin, String one, String two, String three) {
		this.origin = origin;
		switch (origin) {
			case ANNO_DOMINI:
				break;
			case BEFORE_CHRIST:
				break;
			case BIGBANG: // new UniversalDateTime(UniversalDateTime.UniversalDateTimeOrigin.BIGBANG, $head.text, $DOT.text, $tail.text);
				this.yearBigBang = Utility.parseDouble(one, two, three);
				break;
			case KOREAN_LUNAR: // new UniversalDateTime(UniversalDateTime.UniversalDateTimeOrigin.KOREAN_LUNAR, $year.text, $month.text, $day.text);
				this.year = Utility.parseInteger(one);
				this.month = Utility.parseInteger(two);
				this.day = Utility.parseInteger(three);
				break;
			case YEAR_AGO:
				break;
			default:
				break;
			
		}
	}
	//	new UniversalDateTime(UniversalDateTime.UniversalDateTimeOrigin.BIGBANG, $head.text, $DOT.text, $tail.text, 10000);
	public UniversalDateTime(UniversalDateTimeOrigin origin, String head, String DOT, String tail, Long scale) {
		this.origin = origin;
		this.yearBigBang = Utility.parseDouble(head, DOT, tail) * scale;
	}

	public static UniversalDateTime parse(String string) {
        try {
			UniversalDateTimeLexer lexer = new UniversalDateTimeLexer(CharStreams.fromString(string));
			CommonTokenStream tokens = new CommonTokenStream(lexer);
			UniversalDateTimeParser parser = new UniversalDateTimeParser(tokens);
			return parser.universalDateTime().value;
		} catch (RecognitionException e) {
			log.warn("RecognitionException:: {}", e.getLocalizedMessage());
		}

        return null;
	}
	
	@Override
	public String toString() {
		return Utility.toStringJsonPretty(this);
	}

}
