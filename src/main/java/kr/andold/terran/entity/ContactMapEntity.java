package kr.andold.terran.entity;

import java.time.Instant;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Entity
@Table(name = "vcard_map")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ContactMapEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Integer id;

	@Column(name = "vcard_id")
	private Integer vcardId;

	@Column(name = "field_key")
	private String key;

	@Column(name = "field_value")
	private Integer value; // priority

	@Column(name = "created")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone="Asia/Seoul")
	@Temporal(TemporalType.TIMESTAMP)
	private Date created;

	@Column(name = "updated")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone="Asia/Seoul")
	@Temporal(TemporalType.TIMESTAMP)
	private Date updated;

	@Override
	public String toString() {
		return Utility.toStringJson(this);
	}

	public static ContactMapEntity sample() {
		return ContactMapEntity.builder()
			.id(7098)
			.vcardId(23)
			.key("CJ헬로모바일.1688-0022")
			.value(2345)
			.created(Date.from(Instant.parse("2023-03-15 22:39")))
			.updated(Date.from(Instant.parse("2023-03-15 22:39")))
			.build();
	}


}
