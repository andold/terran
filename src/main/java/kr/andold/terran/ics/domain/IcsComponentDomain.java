package kr.andold.terran.ics.domain;

import java.io.IOException;
import java.io.StringReader;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.BeanUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.usingsky.calendar.KoreanLunarCalendar;

import kr.andold.terran.ics.entity.VCalendarComponentEntity;
import kr.andold.utils.Utility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.PeriodList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.model.property.RRule;

@Slf4j
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IcsComponentDomain extends VCalendarComponentEntity {
	private static final String VCACLANDAR_HEAD = "BEGIN:VCALENDAR";
	private static final String VCACLANDAR_FOOT = "END:VCALENDAR";
	private static final String VCACLANDAR_VTIMEZONE = "BEGIN:VTIMEZONE\n" + "TZID:Asia/Seoul\n" + "TZURL:http://tzurl.org/zoneinfo-outlook/Asia/Seoul\n"
		+ "X-LIC-LOCATION:Asia/Seoul\n" + "BEGIN:STANDARD\n" + "TZOFFSETFROM:+0900\n" + "TZOFFSETTO:+0900\n" + "TZNAME:KST\n" + "DTSTART:19700101T000000\n"
		+ "END:STANDARD\n" + "END:VTIMEZONE" + "";
	private static final String DELEMETER = "⇨";
	private String name;
	private String summary;
	private String location;
	@Builder.Default private Boolean lunar = false;
	private List<LocalPeriod> periods;
	@JsonIgnore
	private CalendarComponent component;

	class LocalPeriod {
		@Getter
		@Setter
		private Date start;
		@Getter
		@Setter
		private Date end;

		public LocalPeriod(DateTime start, DateTime end) {
			this.start = start;
			this.end = end;
		}

		public LocalPeriod(DateTime start, DateTime end, boolean convertGmt2Kst) {
			if (!convertGmt2Kst) {
				this.start = start;
				this.end = end;
				return;
			}

			this.start = convertGmt2Kst(start);
			this.end = convertGmt2Kst(end);
		}

		public LocalPeriod(String start, String end) {
			this.start = Utility.parseDateTime(start);
			this.end = Utility.parseDateTime(end);
		}

		private Date convertGmt2Kst(DateTime date) {
			ZonedDateTime zdt = ZonedDateTime.ofInstant(date.toInstant(), ZoneId.of("Etc/UTC"));
			String string = zdt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
			return Utility.parseDateTime(string);
		}

	}

	public IcsComponentDomain(VCalendarComponentEntity entity) {
		BeanUtils.copyProperties(entity, this);
		//String text = String.format("BEGIN:VCALENDAR\n%s\nEND:VCALENDAR", entity.getContent().trim());
		String text = String.format("%s\n%s\n%s\n%s", VCACLANDAR_HEAD, VCACLANDAR_VTIMEZONE, entity.getContent().trim(), VCACLANDAR_FOOT);
		StringReader sin = new StringReader(text);
		CalendarBuilder builder = new CalendarBuilder();
		try {
			Calendar calendar = builder.build(sin);
			List<CalendarComponent> components = calendar.getComponents();
			if (components == null || components.size() < 2) {
				defaultIfNull();
				return;
			}
			CalendarComponent component = components.get(1);
			valueFromComponent(this, component);
		} catch (IOException e) {
			log.warn("{} IOException:: {}", entity, e.getLocalizedMessage());
		} catch (ParserException e) {
			log.warn("{} ParserException:: {}", entity, e.getLocalizedMessage());
		}
		defaultIfNull();
	}

	public IcsComponentDomain(CalendarComponent component) {
		setComponent(component);
		valueFromComponent(this, component);
		defaultIfNull();
	}

	public IcsComponentDomain(CalendarComponent component, Integer vcalendarId) {
		setComponent(component);
		setVcalendarId(vcalendarId);
		valueFromComponent(this, component);
		defaultIfNull();
	}

	public String getUid() {
		if (component == null) {
			log.warn("{} component is null - {}", Utility.indentMiddle(), this);
			return "";
		}

		Property property = component.getProperty("UID");
		if (property != null) {
			return property.getValue();
		}

		return "";
	}


	public void defaultIfNull() {
		super.defaultIfNull();
		if (getName() == null) {
			setName("");
		}
	}

	@Override
	public String toString() {
		return Utility.toStringJson(this, 32, 32);
	}

	public static VCalendarComponentEntity toEntity(IcsComponentDomain domain, boolean fromComponent) {
		VCalendarComponentEntity entity = new VCalendarComponentEntity();
		BeanUtils.copyProperties(domain, entity);
		if (fromComponent) {
			valueFromComponent(domain, domain.getComponent());
		}

		entity.defaultIfNull();
		return entity;
	}

	private static IcsComponentDomain valueFromComponent(IcsComponentDomain domain, CalendarComponent component) {
		if (domain == null || component == null) {
			log.warn("{} component is null - {}", Utility.indentMiddle(), domain);
			return domain;
		}

		domain.setComponent(component);
		domain.setContent(component.toString());
		domain.setName(component.getName());
		Property property = component.getProperty("SUMMARY");
		if (property != null) {
			domain.setSummary(property.getValue());
		}
		property = component.getProperty("LOCATION");
		if (property != null) {
			domain.setLocation(property.getValue());
		}

		property = component.getProperty("DTSTART");
		if (property != null) {
			domain.setStart(Utility.parseDateTime(property.getValue()));
		}

		Property propertyRRule = component.getProperty("RRULE");
		if (propertyRRule == null) {
			Property propertyEnd = component.getProperty("DTEND");
			if (propertyEnd != null) {
				domain.setEnd(Utility.parseDateTime(propertyEnd.getValue()));
			}
		} else {
			Recur recur = ((RRule)propertyRRule).getRecur();
			if (recur != null) {
				domain.setEnd(recur.getUntil());
			}
		}

		Property propertyLunar = component.getProperty("X-NAVER-LUNAR-DATE");
		domain.setLunar(propertyLunar != null && Boolean.parseBoolean(propertyLunar.getValue()));

		return domain;
	}

	public void periods(DateTime from, DateTime to) {
		log.trace("{} periods(『{}』, 『{}』)", Utility.indentStart(), from, to);
		long started = System.currentTimeMillis();

		if (component == null) {
			log.trace("{} 『NULL component』 periods(『{}』, 『{}』) - 『{}』", Utility.indentEnd(), from, to, Utility.toStringPastTimeReadable(started));
			return;
		}

		if (getLunar()) {
			try {
				KoreanLunarCalendar lcalendar = KoreanLunarCalendar.getInstance();
				java.util.Calendar scalendar = java.util.Calendar.getInstance();
				scalendar.setTime(from);
				lcalendar.setSolarDate(scalendar.get(java.util.Calendar.YEAR), scalendar.get(java.util.Calendar.MONTH) + 1, scalendar.get(java.util.Calendar.DAY_OF_MONTH));
				DateTime startLunarDateTime = new DateTime(Utility.parseDateTime(lcalendar.getLunarIsoFormat()));

				java.util.Calendar ecalendar = java.util.Calendar.getInstance();
				ecalendar.setTime(to);
				lcalendar.setSolarDate(ecalendar.get(java.util.Calendar.YEAR), ecalendar.get(java.util.Calendar.MONTH) + 1, ecalendar.get(java.util.Calendar.DAY_OF_MONTH));
				DateTime endLunarDateTime = new DateTime(Utility.parseDateTime(lcalendar.getLunarIsoFormat()));

				Period lunarPeriod = new Period(startLunarDateTime, endLunarDateTime);
				PeriodList lunarPeriods = component.calculateRecurrenceSet(lunarPeriod);

				if (lunarPeriods == null) {
					log.trace("{} 『NULL periods』 periods(『{}』, 『{}』) - 『{}』", Utility.indentEnd(), from, to, Utility.toStringPastTimeReadable(started));
					return;
				}

				List<LocalPeriod> list = new ArrayList<>();
				setPeriods(list);
				for (Period p : lunarPeriods) {
					java.util.Calendar pscalendar = java.util.Calendar.getInstance();
					pscalendar.setTime(p.getStart());
					java.util.Calendar pecalendar = java.util.Calendar.getInstance();
					pecalendar.setTime(p.getEnd());

					lcalendar.setLunarDate(pscalendar.get(java.util.Calendar.YEAR), pscalendar.get(java.util.Calendar.MONTH) + 1, pscalendar.get(java.util.Calendar.DAY_OF_MONTH), false);
					String start = lcalendar.getSolarIsoFormat();

					lcalendar.setLunarDate(pecalendar.get(java.util.Calendar.YEAR), pecalendar.get(java.util.Calendar.MONTH) + 1, pecalendar.get(java.util.Calendar.DAY_OF_MONTH), false);
					String end = lcalendar.getSolarIsoFormat();

					LocalPeriod localPeriod = new LocalPeriod(start, end);
					list.add(localPeriod);

					log.info("{} {} 『{} ~ {}』『{} ~ {}』『{} ~ {}』『{} ~ {}』", Utility.indentMiddle()
							, getSummary(), from, to, startLunarDateTime, endLunarDateTime, p.getStart(), p.getEnd(), start, end);
				}

				log.trace("{} 『SUCCESS』 periods(『{}』, 『{}』) - 『{}』", Utility.indentEnd(), from, to, Utility.toStringPastTimeReadable(started));
				return;
			} catch (Exception e) {
				log.error("Exception:: {}", e.getLocalizedMessage(), e);
			}
		}

		Period period = new Period(from, to);
		PeriodList periods = component.calculateRecurrenceSet(period);
		if (periods == null) {
			log.trace("{} 『NULL periods』 periods(『{}』, 『{}』) - 『{}』", Utility.indentEnd(), from, to, Utility.toStringPastTimeReadable(started));
			return;
		}

		List<LocalPeriod> list = new ArrayList<>();
		setPeriods(list);
		for (Period p : periods) {
			LocalPeriod localPeriod = new LocalPeriod(p.getStart().toString(), p.getEnd().toString());
			list.add(localPeriod);
		}

		log.trace("{} 『SUCCESS』 periods(『{}』, 『{}』) - 『{}』", Utility.indentEnd(), from, to, Utility.toStringPastTimeReadable(started));
	}

	public Date getDateEnd() {
		if (component == null) {
			return DEFAULT_END;
		}

		Property property = component.getProperty("DTEND");
		if (property == null) {
			return DEFAULT_END;
		}

		return Utility.parseDateTime(property.getValue());
	}

	public Date getLastModified() {
		if (component == null) {
			return DEFAULT_START;
		}

		Property property = component.getProperty("LAST-MODIFIED");
		if (property == null) {
			return DEFAULT_START;
		}

		return Utility.parseDateTime(property.getValue());
	}

	public String key() {
		return String.format("%s%s%s", getUid(), DELEMETER, getRecurrenceId());
	}

	private String getRecurrenceId() {
		if (component == null) {
			log.warn("{} component is null - {}", Utility.indentMiddle(), this);
			return "";
		}

		Property property = component.getProperty("RECURRENCE-ID");
		if (property != null) {
			return property.getValue();
		}

		return "";
	}

	public String getDescription() {
		if (component == null) {
			log.warn("{} component is null - {}", Utility.indentMiddle(), this);
			return "";
		}

		Property property = component.getProperty("DESCRIPTION");
		if (property != null) {
			return property.getValue();
		}

		return "";
	}

	public static IcsComponentDomain of(Optional<VCalendarComponentEntity> entity) {
		return new IcsComponentDomain(entity.get());
	}

}
