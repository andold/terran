package kr.andold.terran.ics.entity;

import java.util.Date;
import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonFormat;
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

@SuperBuilder
@Entity
@Table(name = "vcalendar_component")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class VCalendarComponentEntity {
	public static final Date DEFAULT_START = Utility.parseDateTime("1900-01-01");
	public static final Date DEFAULT_END = Utility.parseDateTime("2200-01-01");

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Integer id;

	@Column(name = "content")
	private String content;

	@Column(name = "vcalendar_id")
	private Integer vcalendarId;

	@Column(name = "field_start")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "Asia/Seoul")
	@Temporal(TemporalType.TIMESTAMP)
	private Date start;

	@Column(name = "field_end")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "Asia/Seoul")
	@Temporal(TemporalType.TIMESTAMP)
	private Date end;


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
		if (getContent() == null) {
			setContent("");
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

}
