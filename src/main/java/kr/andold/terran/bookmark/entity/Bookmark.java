package kr.andold.terran.bookmark.entity;

import java.net.URLDecoder;
import java.util.Date;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import kr.andold.utils.Utility;

import org.springframework.format.annotation.DateTimeFormat;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Builder
@Entity
@Table(name = "bookmark")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString()
public class Bookmark {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	@NotNull
	private Integer id;

	@Column(name = "title")
	private String title;

	@Column(name = "url")
	private String url;

	@Column(name = "description")
	private String description;

	@Column(name = "parent_id")
	private Integer pid;

	@Column(name = "count")
	private Integer count;

	@Column(name = "created")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	@Temporal(TemporalType.TIMESTAMP)
	private Date created;

	@Column(name = "updated")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	@Temporal(TemporalType.TIMESTAMP)
	private Date updated;

	/**
	 * null, 0:	normal
	 * 1:		deleted
	 */
	@Column(name = "status")
	private Integer status;

	@JsonIgnore
	public String getJson() {
		return Utility.toStringJson(this);
	}

	public static Bookmark of(String string) {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setSerializationInclusion(Include.NON_NULL);
		objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		try {
			return objectMapper.readValue(string, Bookmark.class);
		} catch (Exception e) {
			try {
				return objectMapper.readValue(URLDecoder.decode(string, "UTF-8"), Bookmark.class);
			} catch (Exception f) {
				e.printStackTrace();
				f.printStackTrace();
			}
		}

		return null;
	}

	public static Bookmark sample() {
		return new Bookmark(23, "국가통계포털", "http://kosis.kr/", "국가통계포털 - http://kosis.kr/", 123, 0, new Date(), new Date(), null);
	}

}
