package kr.andold.terran.ics.service;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;
import kr.andold.terran.ics.domain.IcsCalendarDomain;
import kr.andold.terran.ics.domain.IcsComponentDomain;
import kr.andold.terran.ics.domain.IcsParam;
import kr.andold.terran.ics.entity.VCalendarComponentEntity;
import kr.andold.terran.ics.entity.VCalendarEntity;
import kr.andold.terran.ics.repository.VCalendarComponentRepository;
import kr.andold.terran.ics.repository.VCalendarRepository;
import kr.andold.utils.Utility;
import lombok.extern.slf4j.Slf4j;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Version;
import net.fortuna.ical4j.validate.ValidationResult;

@Slf4j
@Service
public class IcsService {
	private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

	@Autowired
	private VCalendarComponentRepository vcalendarComponentRepository;
	@Autowired
	private VCalendarRepository vcalendarRepository;

	public IcsParam upload(MultipartFile file, Integer vcalendarId) {
		log.info("{} upload(..., {})", Utility.indentStart(), vcalendarId);
		long started = System.currentTimeMillis();

		try {
			String text = Utility.extractStringFromText(file.getInputStream()).trim();
			String contentType = file.getContentType();
			switch (contentType) {
				case "text/calendar":
					CalendarBuilder builder = new CalendarBuilder();
					Calendar calendar = builder.build(new StringReader(text));
					IcsParam result = differ(calendar, vcalendarId);

					log.info("{} {} - upload(..., {}) - {}", Utility.indentEnd(), result, vcalendarId, Utility.toStringPastTimeReadable(started));
					return result;
				case "application/json":
					break;

				default:
					break;
			}
		} catch (IOException e) {
			log.error("IOException:: {}", e.getLocalizedMessage(), e);
		} catch (ParserException e) {
			log.error("ParserException:: {}", e.getLocalizedMessage(), e);
		}

		log.info("{} {} - upload({}) - {}", Utility.indentEnd(), null, Utility.toStringJson(file, 32), Utility.toStringPastTimeReadable(started));
		return null;
	}

	public IcsParam differ(Calendar calendar, Integer vcalendarId) {
		ComponentList<CalendarComponent> components = calendar.getComponents();
		List<IcsComponentDomain> after = new ArrayList<IcsComponentDomain>();
		for (CalendarComponent component : components) {
			IcsComponentDomain domain = new IcsComponentDomain(component, vcalendarId);
			after.add(domain);
		}
		List<IcsComponentDomain> before = search(vcalendarId);
		return differ(before, after, vcalendarId);
	}

	private IcsParam differ(List<IcsComponentDomain> befores, List<IcsComponentDomain> afters, Integer vcalendarId) {
		List<IcsComponentDomain> creates = new ArrayList<IcsComponentDomain>();
		List<IcsComponentDomain> duplicates = new ArrayList<IcsComponentDomain>();
		List<IcsComponentDomain> updates = new ArrayList<IcsComponentDomain>();
		List<IcsComponentDomain> removes = new ArrayList<IcsComponentDomain>();

		Map<String, IcsComponentDomain> mapBySummaryStartEndBefore = makeMapBySummaryStartEnd(befores);
		Map<String, IcsComponentDomain> mapBySummaryStartEndAfter = makeMapBySummaryStartEnd(afters);
		for (String key : mapBySummaryStartEndBefore.keySet()) {
			IcsComponentDomain after = mapBySummaryStartEndAfter.get(key);
			IcsComponentDomain before = mapBySummaryStartEndBefore.get(key);
			if (after == null) {
				removes.add(before);
				continue;
			}

			//duplicates.addAll(before);
		}
		for (String key : mapBySummaryStartEndAfter.keySet()) {
			IcsComponentDomain before = mapBySummaryStartEndBefore.get(key);
			IcsComponentDomain after = mapBySummaryStartEndAfter.get(key);
			if (before == null) {
				creates.add(after);
				continue;
			}

			// update duplicate check by only last modified
			ZonedDateTime beforeLastModified = ZonedDateTime.ofInstant(before.getLastModified().toInstant(), Utility.ZONE_ID_KST);
			ZonedDateTime afterLastModified = ZonedDateTime.ofInstant(after.getLastModified().toInstant(), Utility.ZONE_ID_KST);
			if (beforeLastModified.isBefore(afterLastModified)) {
				updates.add(after);
				continue;
			}

			duplicates.add(before);
		}

		return IcsParam.builder().creates(creates).duplicates(duplicates).updates(updates).removes(removes).build();
	}

	private Map<String, IcsComponentDomain> makeMapBySummaryStartEnd(List<IcsComponentDomain> domains) {
		Map<String, IcsComponentDomain> map = new HashMap<>();
		for (IcsComponentDomain domain : domains) {
			String key = String.format("%s.%s.%s", domain.getSummary(), SIMPLE_DATE_FORMAT.format(domain.getStart()),
				SIMPLE_DATE_FORMAT.format(domain.getDateEnd()));
			map.put(key, domain);
		}

		return map;
	}

	public int batch(IcsParam param) throws ParseException {
		if (param == null) {
			return 0;
		}

		int count = 0;
		List<IcsComponentDomain> creates = param.getCreates();
		List<IcsComponentDomain> updates = param.getUpdates();
		List<IcsComponentDomain> removes = param.getRemoves();

		if (creates != null) {
			List<IcsComponentDomain> created = create(creates, param.getVcalendarId());
			count += Utility.size(created);
		}
		if (removes != null) {
			count += remove(removes);
		}
		if (updates != null) {
			count += Utility.size(update(updates));
		}

		return count;
	}

	@Transactional
	@Modifying
	private List<?> update(List<IcsComponentDomain> updates) {
		Date date = new Date();
		List<VCalendarComponentEntity> entities = new ArrayList<VCalendarComponentEntity>();
		for (IcsComponentDomain domain : updates) {
			VCalendarComponentEntity entity = IcsComponentDomain.toEntity(domain, false);
			entity.setUpdated(date);
			entities.add(entity);
			entity.defaultIfNull();
		}

		try {
			List<VCalendarComponentEntity> list = vcalendarComponentRepository.saveAllAndFlush(entities);
			List<IcsComponentDomain> domains = new ArrayList<IcsComponentDomain>();
			for (VCalendarComponentEntity entity : list) {
				IcsComponentDomain domain = new IcsComponentDomain(entity);
				domains.add(domain);
			}

			return domains;
		} catch (Exception e) {
			log.warn("{} create(#{}) - Exception:: {}", Utility.indentMiddle(), Utility.size(updates), e.getLocalizedMessage());

			List<IcsComponentDomain> domains = new ArrayList<IcsComponentDomain>();
			for (VCalendarComponentEntity entity : entities) {
				try {
					VCalendarComponentEntity created = vcalendarComponentRepository.save(entity);
					IcsComponentDomain domain = new IcsComponentDomain(created);
					domains.add(domain);
				} catch (Exception f) {
					log.warn("{} create(#{}) - {} - Exception:: {}", Utility.indentMiddle(), Utility.size(updates), Utility.toStringJson(entity, 32, 32),
						f.getLocalizedMessage());
				}
			}
			vcalendarComponentRepository.flush();

			return domains;
		}
	}

	@Transactional
	@Modifying
	private int remove(List<IcsComponentDomain> removes) {
		List<VCalendarComponentEntity> entities = new ArrayList<VCalendarComponentEntity>();
		for (IcsComponentDomain domain : removes) {
			VCalendarComponentEntity entity = IcsComponentDomain.toEntity(domain, false);
			entities.add(entity);
		}

		try {
			vcalendarComponentRepository.deleteAll(entities);
			return Utility.size(entities);
		} catch (Exception e) {
			log.warn("{} remove(#{}) - Exception:: {}", Utility.indentMiddle(), Utility.size(removes), e.getLocalizedMessage());

			int count = 0;
			for (VCalendarComponentEntity entity : entities) {
				try {
					vcalendarComponentRepository.delete(entity);
					count++;
				} catch (Exception f) {
					log.warn("{} delete(#{}) - {} - Exception:: {}", Utility.indentMiddle(), Utility.size(removes), Utility.toStringJson(entity, 32, 32),
						f.getLocalizedMessage());
				}
			}
			vcalendarComponentRepository.flush();

			return count;
		}
	}

	@Transactional
	@Modifying
	public IcsComponentDomain remove(Integer id) {
		Optional<VCalendarComponentEntity> entity = read(id);
		if (entity.isPresent()) {
			vcalendarComponentRepository.deleteById(id);
			return IcsComponentDomain.of(entity);
		}
		
		return null;
	}

	private Optional<VCalendarComponentEntity> read(Integer id) {
		return vcalendarComponentRepository.findById(id);
	}

	@Transactional
	@Modifying
	private List<IcsComponentDomain> create(List<IcsComponentDomain> creates, Integer vcalendarId) {
		List<VCalendarComponentEntity> entities = new ArrayList<VCalendarComponentEntity>();
		for (IcsComponentDomain domain : creates) {
			VCalendarComponentEntity entity = IcsComponentDomain.toEntity(domain, false);
			entities.add(entity);
			entity.setId(null);
			if (vcalendarId != null) {
				entity.setVcalendarId(vcalendarId);
			}
			entity.defaultIfNull();
		}

		try {
			List<VCalendarComponentEntity> list = vcalendarComponentRepository.saveAllAndFlush(entities);
			List<IcsComponentDomain> domains = new ArrayList<IcsComponentDomain>();
			for (VCalendarComponentEntity entity : list) {
				IcsComponentDomain domain = new IcsComponentDomain(entity);
				domains.add(domain);
			}

			return domains;
		} catch (Exception e) {
			log.warn("{} create(#{}) - Exception:: {}", Utility.indentMiddle(), Utility.size(creates), e.getLocalizedMessage());

			List<IcsComponentDomain> domains = new ArrayList<IcsComponentDomain>();
			for (VCalendarComponentEntity entity : entities) {
				try {
					VCalendarComponentEntity created = vcalendarComponentRepository.save(entity);
					IcsComponentDomain domain = new IcsComponentDomain(created);
					domains.add(domain);
				} catch (Exception f) {
					log.warn("{} create(#{}) - {} - Exception:: {}", Utility.indentMiddle(), Utility.size(creates), Utility.toStringJson(entity, 32, 32),
						f.getLocalizedMessage());
				}
			}
			vcalendarComponentRepository.flush();

			return domains;
		}
	}

	public List<IcsComponentDomain> search(IcsParam param) {
		List<VCalendarComponentEntity> entities;
		DateTime from;
		DateTime to;
		if (param == null) {
			entities = vcalendarComponentRepository.findAll();
			from = new DateTime(IcsComponentDomain.DEFAULT_START);
			to = new DateTime(IcsComponentDomain.DEFAULT_END);
		} else {
			param.prepareForSearch();
//			entities = vcalendarComponentRepository.search(param);
			entities = vcalendarComponentRepository.findAllByContentContainsAndVcalendarIdAndEndGreaterThanEqualAndStartLessThan(param.getKeyword(), param.getVcalendarId(), param.getStart(), param.getEnd());
			from = new DateTime(param.getStart() == null ? IcsComponentDomain.DEFAULT_START : param.getStart());
			to = new DateTime(param.getEnd() == null ? IcsComponentDomain.DEFAULT_END : param.getEnd());
		}

		List<IcsComponentDomain> domains = new ArrayList<IcsComponentDomain>();
		for (VCalendarComponentEntity entity : entities) {
			IcsComponentDomain domain = new IcsComponentDomain(entity);
			domains.add(domain);

			CalendarComponent component = domain.getComponent();
			if (component == null) {
				continue;
			}

			domain.periods(from, to);
		}

		return domains;
	}

	private List<IcsComponentDomain> search(Integer vcalendarId) {
		List<VCalendarComponentEntity> entities = vcalendarComponentRepository.findAllByVcalendarId(vcalendarId);
		List<IcsComponentDomain> domains = new ArrayList<IcsComponentDomain>();
		for (VCalendarComponentEntity entity : entities) {
			IcsComponentDomain domain = new IcsComponentDomain(entity);
			domains.add(domain);
		}

		return domains;
	}

	public List<IcsCalendarDomain> searchCalendar(IcsParam param) {
		List<VCalendarEntity> entities = vcalendarRepository.findAll();

		List<IcsCalendarDomain> domains = new ArrayList<IcsCalendarDomain>();
		for (VCalendarEntity entity : entities) {
			IcsCalendarDomain domain = new IcsCalendarDomain(entity);
			domains.add(domain);
		}

		return domains;
	}

	@Transactional
	@Modifying
	public IcsCalendarDomain createCalendar(IcsCalendarDomain param) {
		param.defaultIfNull();
		VCalendarEntity entity = IcsCalendarDomain.toEntity(param);
		VCalendarEntity created = vcalendarRepository.save(entity);
		IcsCalendarDomain domain = new IcsCalendarDomain(created);
		return domain;
	}

	public String downloadIcs(Integer vcalendarId) {
		List<IcsComponentDomain> domains = search(vcalendarId);
		ComponentList<CalendarComponent> components = new ComponentList<CalendarComponent>();
		for (IcsComponentDomain domain : domains) {
			CalendarComponent component = domain.getComponent();
			try {
				ValidationResult result = component.validate();
				if (result.hasErrors()) {
					log.warn("{} ERROR::{} downloadIcs({}) - {}", Utility.indentMiddle(), result, vcalendarId, component);
					continue;
				}

				ComponentList<CalendarComponent> only = new ComponentList<CalendarComponent>();
				only.add(component);
				Calendar calendar = new Calendar(properties(), only);
				StringWriter sw = new StringWriter();
				CalendarOutputter outputter = new CalendarOutputter();
				outputter.output(calendar, sw);

				components.add(component);
			} catch (Exception e) {
				log.warn("{} Exception::{} downloadIcs({}) - {}", Utility.indentMiddle(), e.getLocalizedMessage(), vcalendarId, component);
			}
		}

		Calendar calendar = new Calendar(properties(), components);
		StringWriter sw = new StringWriter();
		try {
			CalendarOutputter outputter = new CalendarOutputter();
			outputter.output(calendar, sw);
			return sw.toString();
		} catch (Exception e) {
			log.warn("{} Exception::{} downloadIcs({})", Utility.indentMiddle(), e.getLocalizedMessage(), vcalendarId, e);
		}

		return "";
	}

	private PropertyList<Property> properties() {
		PropertyList<Property> properties = new PropertyList<Property>();
		properties.add(new ProdId("andold"));
		Version version = new Version();
		version.setValue(Version.VALUE_2_0);
		properties.add(version);
		return properties;
	}

	public IcsParam deduplicate(Integer vcalendarId) {
		log.info("{} deduplicate({})", Utility.indentStart(), vcalendarId);
		List<IcsComponentDomain> domains = search(vcalendarId);
		Map<String, IcsComponentDomain> map = new HashMap<>();
		Map<String, IcsComponentDomain> mapDuplicate = new HashMap<>();
		List<IcsComponentDomain> removes = new ArrayList<IcsComponentDomain>();
		for (IcsComponentDomain domain : domains) {
			String key = domain.key();
			IcsComponentDomain before = map.get(key);
			if (before == null) {
				map.put(key, domain);
				continue;
			}

			Date currentLastModified = domain.getLastModified();
			Date beforeLastModified = before.getLastModified();
			int compared = Utility.compare(currentLastModified, beforeLastModified);
			if (compared < 0) {
				removes.add(domain);
				mapDuplicate.put(key, before);
				continue;
			}
			if (compared > 0) {
				removes.add(before);
				map.put(key, domain);
				mapDuplicate.put(key, domain);
				continue;
			}
		}

		log.info("{} deduplicate({}) - {}", Utility.indentEnd(), vcalendarId);
		return IcsParam.builder().duplicates(new ArrayList<>(mapDuplicate.values())).removes(removes).build();
	}

}
