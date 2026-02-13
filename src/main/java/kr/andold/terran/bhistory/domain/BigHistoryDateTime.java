package kr.andold.terran.bhistory.domain;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import kr.andold.terran.bhistory.antlr.BigHistoryDateTimeLexer;
import kr.andold.terran.bhistory.antlr.BigHistoryDateTimeParser;
import kr.andold.utils.Utility;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BigHistoryDateTime {
	private Double bigbang;

	private Integer year;
	private Integer month;
	private Integer day;

	public Double getValue() {
		if (bigbang != null) {
			return bigbang;
		}
		
		// Anno Domini, 서기, 기원후
		Double value = 0d;
		value += Utility.UNIVERSE_AGE;
		value += year * Utility.ONE_SOLAR_YEAR;
		
		return value;
	}

	static public BigHistoryDateTime of(String string) {
		try {
	        BigHistoryDateTimeLexer lexer = new BigHistoryDateTimeLexer(CharStreams.fromString(string));
	        CommonTokenStream tokens = new CommonTokenStream(lexer);
	        BigHistoryDateTimeParser parser = new BigHistoryDateTimeParser(tokens);
	        return parser.bigHistoryDateTime().value;
		} catch (Exception e) {
			log.warn("{}", e);
		}
		
		return new BigHistoryDateTime(0d, null, null, null);
	}
	static public BigHistoryDateTime of(Double bigbang) {
		return new BigHistoryDateTime(bigbang, null, null, null);
	}
	static public BigHistoryDateTime of(Integer year, Integer month, Integer day) {
		return new BigHistoryDateTime(null, year, month, day);
	}

}
