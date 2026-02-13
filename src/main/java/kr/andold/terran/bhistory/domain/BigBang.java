package kr.andold.terran.bhistory.domain;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import kr.andold.terran.bhistory.antlr.DateTimeLexer;
import kr.andold.terran.bhistory.antlr.DateTimeParser;
import kr.andold.utils.Utility;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BigBang {
	private Double year;
	private Double second;
	
	@Override
	public String toString() {
		return Utility.toStringJson(this);
	}

	public static BigBang parse(String string) {
        DateTimeLexer lexer = new DateTimeLexer(CharStreams.fromString(string));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        DateTimeParser parser = new DateTimeParser(tokens);
        return parser.bigbang().value;
	}

}
