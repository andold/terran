package kr.andold.terran.ics.entity;

import java.util.Date;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "vcalendar")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class VCalendarEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Integer id;

	@Column(name = "title")
	private String title;

	@Column(name = "description")
	private String description;

	// start/root/initial component
	@Column(name = "component_id")
	private Integer componentId;

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

	@ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinColumn(name = "component_id", insertable = false, updatable = false)
	@NotFound(action = NotFoundAction.IGNORE)
	private VCalendarComponentEntity component;

	public void defaultIfNull() {
		if (getTitle() == null) {
			setTitle("");
		}
		if (getDescription() == null) {
			setDescription("");
		}
		if (getComponentId() == null) {
			setComponentId(0);
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
