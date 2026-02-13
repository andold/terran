package kr.andold.terran.bhistory.entity;

import java.util.Date;
import org.springframework.format.annotation.DateTimeFormat;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import kr.andold.utils.Utility;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "big_history")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class BigHistoryEntity {
	private static final Double DEFAULT_START = 0.;
	private static final Double DEFAULT_END = 1.;
	private static final String DEFAULT_CATEGORY = "미정";

	@Id
	@Column(name = "id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(name = "field_start")
	private Double start;

	@Column(name = "field_end")
	private Double end;

	@Column(name = "title")
	private String title;

	@Column(name = "description")
	private String description;

	@Column(name = "category")
	private String category;

	@Column(name = "created")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "Asia/Seoul")
	@Temporal(TemporalType.TIMESTAMP)
	private Date created;

	@Column(name = "updated")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "Asia/Seoul")
	@Temporal(TemporalType.TIMESTAMP)
	private Date updated;

	public void defaultIfNull() {
		if (getTitle() == null) {
			setTitle("");
		}
		if (getDescription() == null) {
			setDescription("");
		}
		if (getCategory() == null) {
			setCategory(DEFAULT_CATEGORY);
		}
		if (getStart() == null) {
			setStart(DEFAULT_START);
		}
		if (getEnd() == null) {
			setEnd(DEFAULT_END);
		}
		Date date = new Date();
		if (getCreated() == null) {
			setCreated(date);
		}
		if (getUpdated() == null) {
			setUpdated(date);
		}
	}

	@JsonIgnore public String getStartHtml() {		return toHtmlTime(getStart());	}
	@JsonIgnore public String getStartTextSpan() {		return toTextSpanTime(getStart(), 8);	}
	@JsonIgnore public String getEndHtml() {		return toHtmlTime(getEnd());	}
	@JsonIgnore public String getEndTextSpan() {		return toTextSpanTime(getEnd(), 8);	}
	@JsonIgnore public Double getStartSignificant() {		return getStart() == null ? null : (getStart() == 0d ? 0d : getStart() / Math.pow(10, (int)Math.log10(getStart())));	}
	@JsonIgnore public Double getEndSignificant() {		return getEnd() == null ? null : (getEnd() == 0d ? 0d : getEnd() / Math.pow(10, (int)Math.log10(getEnd())));	}
	@JsonIgnore public Integer getStartExponent() {	return getStart() == null ? null : (getStart() == 0d ? 0 : (int)Math.log10(getStart()));	}
	@JsonIgnore public Integer getEndExponent() {	return getEnd() == null ? null : (getEnd() == 0d ? 0 : (int)Math.log10(getEnd()));	}
	@JsonIgnore public String getDurationHtml() {	return toHtmlDuration(getEnd() - getStart());	}
	@JsonIgnore public String getDurationTextSpan() {	return toTextSpanDuration(getEnd() - getStart(), 8);	}

	public static String toTextSpanDuration(double duration, int move) {
		if (duration == 0d) {
			return "0";
		}
		
		if (duration < 1d) {	//	빅뱅후 1초
			int exponent = (int)Math.log10(duration);
			double significant = duration / Math.pow(10, exponent);
			if  (significant == 1d) {
				return String.format("10</tspan><tspan dy=\"-%d\">%d</tspan><tspan dy=\"%d\"> 초", move, exponent, move);
			}

			return String.format("%s x 10</tspan><tspan dy=\"-%d\">%d</tspan><tspan dy=\"%d\"> 초", toHtmlSignificant(significant), move, exponent, move);
		}

		if (duration < Utility.ONE_SOLAR_YEAR) {	//	1년
			return String.format("%s 초", toHtmlSignificant(duration));
		}

		if (duration < Utility.ONE_SOLAR_YEAR * Math.pow(10d, 4d)) {	//	1만년
			return String.format("%s 년", toHtmlSignificant(duration / Utility.ONE_SOLAR_YEAR));
		}

		if (duration < Utility.ONE_SOLAR_YEAR * Math.pow(10d, 8d)) {	//	빅뱅후 1억년
			return String.format("%s 만년", toHtmlSignificant(duration / Utility.ONE_SOLAR_YEAR / Math.pow(10d, 4d)));
		}

		if (duration < Utility.ONE_SOLAR_YEAR * Math.pow(10d, 12d)) {	//	빅뱅후 1조년
			return String.format("%s 억년", toHtmlSignificant(duration / Utility.ONE_SOLAR_YEAR / Math.pow(10d, 8d)));
		}

		int exponent = (int)Math.log10(duration / Utility.ONE_SOLAR_YEAR);
		double significant = duration / Utility.ONE_SOLAR_YEAR / Math.pow(10d, exponent);
		if  (significant == 1d) {
			return String.format("10</tspan><tspan dy=\"-%d\">%d</tspan><tspan dy=\"%d\"> 년", move, exponent, move);
		}

		return String.format("%s x 10</tspan><tspan dy=\"-%d\">%d</tspan><tspan dy=\"%d\"> 년", toHtmlSignificant(significant), move, exponent, move);
	}
	public static String toTextSpanTime(Double time, int move) {
		if (time == null) {
			return "";
		}

		if (time < Utility.ONE_SOLAR_YEAR * 7000000000.0d) {	//	빅뱅후 70억년
			return toTextSpanDuration(time, move);
		}

		if (time < Utility.UNIVERSE_AGE) {	//	기원전
			double bc = Utility.UNIVERSE_AGE - time;
			if (bc < Utility.ONE_SOLAR_YEAR * Math.pow(10d, 4d)) {	//	기원전 1만년
				return String.format("BC %s", toTextSpanDuration(bc, move));
			}

			return String.format("%s전", toTextSpanDuration(bc, move));
		}

		//	기원후
		double ad = time - Utility.UNIVERSE_AGE;
		if (ad < Utility.ONE_SOLAR_YEAR * Math.pow(10d, 8d)) {	//	기원후 1억년
			return String.format("AD %s", toTextSpanDuration(ad, move));
		}

		return String.format("%s후", toTextSpanDuration(ad, move));
	}
	private static Long possibleLong(double significant, int figure) {
		if (Math.round(significant) * Math.pow(10d, figure) == Math.round(significant * Math.pow(10d, figure))) {
			return Math.round(significant);
		}

		return null;
	}
	public static String toHtmlSignificant(double significant) {
		Long possibleLong = possibleLong(significant, 4);
		if (possibleLong == null) {
			return String.format("%.3f", significant);
		}
		
		return String.format("%d", (long) possibleLong);
	}
	public static String toHtmlDuration(Double duration) {
		if (duration == 0d) {
			return "0";
		}
		
		if (duration < 1d) {	//	빅뱅후 1초
			int exponent = (int)Math.log10(duration);
			double significant = duration / Math.pow(10, exponent);
			if  (significant == 1d) {
				return String.format("10<sup>%d</sup> 초", exponent);
			}

			return String.format("%s x 10<sup>%d</sup> 초", toHtmlSignificant(significant), exponent);
		}

		if (duration < Utility.ONE_SOLAR_YEAR) {	//	1년
			return String.format("%s 초", toHtmlSignificant(duration));
		}

		if (duration < Utility.ONE_SOLAR_YEAR * Math.pow(10d, 4d)) {	//	1만년
			return String.format("%s 년", toHtmlSignificant(duration / Utility.ONE_SOLAR_YEAR));
		}

		if (duration < Utility.ONE_SOLAR_YEAR * Math.pow(10d, 8d)) {	//	빅뱅후 1억년
			return String.format("%s 만년", toHtmlSignificant(duration / Utility.ONE_SOLAR_YEAR / Math.pow(10d, 4d)));
		}

		if (duration < Utility.ONE_SOLAR_YEAR * Math.pow(10d, 12d)) {	//	빅뱅후 1조년
			return String.format("%s 억년", toHtmlSignificant(duration / Utility.ONE_SOLAR_YEAR / Math.pow(10d, 8d)));
		}

		int exponent = (int)Math.log10(duration / Utility.ONE_SOLAR_YEAR);
		double significant = duration / Utility.ONE_SOLAR_YEAR / Math.pow(10d, exponent);
		if  (significant == 1d) {
			return String.format("10<sup>%d</sup> 년", exponent);
		}

		return String.format("%s x 10<sup>%d</sup> 년", toHtmlSignificant(significant), exponent);
	}
	public static String toHtmlTime(Double time) {
		if (time == null) {
			return "";
		}

		if (time < Utility.ONE_SOLAR_YEAR * 7000000000.0d) {	//	빅뱅후 70억년
			return toHtmlDuration(time);
		}

		if (time < Utility.UNIVERSE_AGE) {	//	기원전
			double bc = Utility.UNIVERSE_AGE - time;
			if (bc < Utility.ONE_SOLAR_YEAR * Math.pow(10d, 4d)) {	//	기원전 1만년
				return String.format("BC %s", toHtmlDuration(bc));
			}

			return String.format("%s전", toHtmlDuration(bc));
		}

		//	기원후
		double ad = time - Utility.UNIVERSE_AGE;
		if (ad < Utility.ONE_SOLAR_YEAR * Math.pow(10d, 8d)) {	//	기원후 1억년
			return String.format("AD %s", toHtmlDuration(ad));
		}

		return String.format("%s후", toHtmlDuration(ad));
	}
	public static Double significant(Double time, int figure) {
		if (time == null) {
			return null;
		}
		if (time == 0d) {
			return 0d;
		}
		int bulk = (int)Math.pow(10, figure);
		double exponent = Math.pow(10, (int)Math.log10(time));
		return Math.floor(time / exponent * bulk) / bulk * exponent;
	}
}
