package kr.andold.terran.bhistory.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import kr.andold.terran.bhistory.domain.BigHistorySearchRequest;
import kr.andold.terran.bhistory.domain.Chronology;
import kr.andold.terran.bhistory.domain.ChronologyColumn;
import kr.andold.terran.bhistory.domain.ChronologyRow;
import kr.andold.terran.bhistory.domain.ScalableVectorGraphics;
import kr.andold.terran.bhistory.entity.BigHistoryEntity;
import kr.andold.terran.bhistory.repository.BigHistoryRepository;
import kr.andold.terran.bhistory.repository.BigHistorySpecification;
import kr.andold.utils.Utility;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class BigHistoryService {
	private double VERSION = 0.1;

	@Autowired
	private BigHistoryRepository repository;
	@Autowired
	private ScalableVectorGraphicsService svgService;

	public Page<BigHistoryEntity> list(Pageable pageable, BigHistorySearchRequest form) {
		log.info("{} list({}, {})", Utility.indentStart(), pageable, Utility.toStringJson(form));
		long started = System.currentTimeMillis();

		Page<BigHistoryEntity> paged = repository.findAll(BigHistorySpecification.searchWith(form), pageable);

		log.info("{} #{} - list({}, {}) - {}", Utility.indentEnd(), "", pageable, Utility.toStringJson(form), Utility.toStringPastTimeReadable(started));
		return paged;
	}

	@Transactional
	@Modifying
	public BigHistoryEntity create(BigHistoryEntity bigHistory) {
		log.info("{} create({})", Utility.indentStart(), Utility.toStringJson(bigHistory));
		long started = System.currentTimeMillis();

		bigHistory.setId(null);
		bigHistory.defaultIfNull();
		BigHistoryEntity created = repository.saveAndFlush(bigHistory);

		log.info("{} {} - create({}) - {}", Utility.indentEnd(), Utility.toStringJson(created), Utility.toStringJson(bigHistory), Utility.toStringPastTimeReadable(started));
		return created;
	}

	public BigHistoryEntity read(Integer id, BigHistorySearchRequest request) {
		log.info("{} read({})", Utility.indentStart(), id);
		long started = System.currentTimeMillis();

		if (id == null) {
			log.warn("{} {} - read({}) - {}", Utility.indentEnd(), null, id, Utility.toStringPastTimeReadable(started));
			return null;
		}

		Optional<BigHistoryEntity> found = repository.findById(id);
		if (found.isEmpty()) {
			log.warn("{} NODATA - read({}, {}) - {}", Utility.indentEnd(), id, request, Utility.toStringPastTimeReadable(started));
			return null;
		}

		BigHistoryEntity bigHistory = found.get();
		
		if (request == null || request.getExpandChildren() == null || !request.getExpandChildren()) {
			log.info("{} {} - read({}) - {}", Utility.indentEnd(), Utility.toStringJson(bigHistory, 32), id, Utility.toStringPastTimeReadable(started));
			return bigHistory;
		}
		
		log.info("{} {} - read({}) - {}", Utility.indentEnd(), Utility.toStringJson(found, 32), id, Utility.toStringPastTimeReadable(started));
		return bigHistory;
	}

	@Transactional
	@Modifying
	public BigHistoryEntity delete(Integer id) {
		log.info("{} delete({})", Utility.indentStart(), id);
		long started = System.currentTimeMillis();

		if (id == null) {
			log.warn("{} {}:NULL_PARAMETER - delete({}) - {}", Utility.indentEnd(), null, id, Utility.toStringPastTimeReadable(started));
			return null;
		}

		BigHistoryEntity found = read(id, null);
		if (found == null) {
			log.warn("{} {}:NULL_DATA - delete({}) - {}", Utility.indentEnd(), Utility.toStringJson(found, 32), id, Utility.toStringPastTimeReadable(started));
			return null;
		}

		repository.delete(found);

		log.info("{} {} - delete({}) - {}", Utility.indentEnd(), Utility.toStringJson(found, 32), id, Utility.toStringPastTimeReadable(started));
		return found;
	}

	@Transactional
	@Modifying
	public BigHistoryEntity update(Integer id, BigHistoryEntity form) {
		log.info("{} update({}, {})", Utility.indentStart(), id, Utility.toStringJson(form, 32));
		long started = System.currentTimeMillis();

		BigHistoryEntity bigHistory = read(id, null);
		if (bigHistory == null) {
			form.setUpdated(new Date());
			repository.saveAndFlush(form);
			log.info("{} #{} - update({}, {}) - {}", Utility.indentEnd(), Utility.toStringJson(form, 32), id, Utility.toStringJson(form, 32), Utility.toStringPastTimeReadable(started));
			return form;
		}
		if (Utility.copyPropertiesNotNull(form, bigHistory)) {
			bigHistory.setUpdated(new Date());
			repository.flush();
		}

		log.info("{} #{} - update({}, {}) - {}", Utility.indentEnd(), Utility.toStringJson(bigHistory, 32), id, Utility.toStringJson(form, 32), Utility.toStringPastTimeReadable(started));
		return bigHistory;
	}

	public Chronology chronology(BigHistorySearchRequest form) {
		log.info("{} chronology({})", Utility.indentStart(), Utility.toStringJson(form));
		long started = System.currentTimeMillis();

		if (form == null) {
			form = new BigHistorySearchRequest();
		}

		if (form.getStart() == null) {
			form.setStart(Utility.UNIVERSE_AGE - 1000d * Utility.ONE_SOLAR_YEAR);
		}
		List<BigHistoryEntity> list = search(form);
		if (list == null) {
			log.info("{} {}:NO_DATA - chronology({}) - {}", Utility.indentEnd(), null, Utility.toStringJson(form), Utility.toStringPastTimeReadable(started));
			return null;
		}

		Chronology chronology = makeChronology(list);

		log.info("{} {} - chronology({}) - {}", Utility.indentEnd(), Utility.toStringJson(chronology, 32), Utility.toStringJson(form), Utility.toStringPastTimeReadable(started));
		return chronology;
	}

	private Chronology makeChronology(List<BigHistoryEntity> list) {
		log.info("{} makeChronology(#{})", Utility.indentStart(), Utility.size(list));
		long started = System.currentTimeMillis();

		if (list == null) {
			log.info("{} {}:NULL_PARAMETER - makeChronology(#{}) - {}", Utility.indentEnd(), null, Utility.size(list), Utility.toStringPastTimeReadable(started));
			return null;
		}

		//	mapTime: {시각, start or end의 모든 사건}
		Map<Double, List<BigHistoryEntity>> mapTime = new HashMap<Double, List<BigHistoryEntity>>();
		for (int cx = 0, sizex = list.size(); cx < sizex; cx++) {
			BigHistoryEntity bigHistory = list.get(cx);
			if (bigHistory == null || bigHistory.getStart() == null || bigHistory.getEnd() == null) {
				continue;
			}

			Double start = bigHistory.getStart();
			List<BigHistoryEntity> listBigHistory = mapTime.get(start);
			if (listBigHistory == null) {
				listBigHistory = new ArrayList<BigHistoryEntity>();
				mapTime.put(start, listBigHistory);
			}
			listBigHistory.add(bigHistory);

			Double end = bigHistory.getEnd();
			listBigHistory = mapTime.get(end);
			if (listBigHistory == null) {
				listBigHistory = new ArrayList<BigHistoryEntity>();
				mapTime.put(end, listBigHistory);
			}
		}

		//	keyList: 정렬된 시각
		List<Double> keyList = new ArrayList<>(mapTime.keySet());
		Collections.sort(keyList);

		//	항목을 y축 시간순으로 정렬
		Chronology chronology = new Chronology();
		List<ChronologyRow> rows = new ArrayList<ChronologyRow>();
		chronology.setRows(rows);
		for (int cx = 0, sizex = keyList.size(); cx < sizex; cx++) {
			Double start = keyList.get(cx);
			if (start == null) {
				continue;
			}

			List<BigHistoryEntity> listBigHistory = mapTime.get(start);
			ChronologyRow row = new ChronologyRow();
			rows.add(row);
			row.setTitle(start);
			List<ChronologyColumn> columns = new ArrayList<ChronologyColumn>();
			row.setColumns(columns);
			if (listBigHistory == null || listBigHistory.isEmpty()) {
				continue;
			}

			makeChronologyColumn(chronology, row, columns, keyList, cx, start, listBigHistory);
		}

		//	항목을 x축으로 겹치지 않게 정렬
		int max = sortX(rows);
		chronology.setColspan(max);

		log.info("{} {} - makeChronology(#{}) - {}", Utility.indentEnd(), Utility.toStringJson(chronology, 32), Utility.size(list), Utility.toStringPastTimeReadable(started));
		return chronology;
	}

	/**
	 * 항목을 x축으로 겹치지 않게 정렬
	 */
	private int sortX(List<ChronologyRow> rows) {
		int max = 0;
		Map<String, ChronologyColumn> map = new HashMap<String, ChronologyColumn>();
		for (int cx = 0, sizex = rows.size(); cx < sizex; cx++) {
			List<ChronologyColumn> columns = rows.get(cx).getColumns();
			for (int cy = 0, sizey = columns.size(); cy < sizey; cy++) {
				int colspan = findFirst(map, cx);
				ChronologyColumn column = columns.get(cy);
				column.setColspan(colspan + 1);
				max = Math.max(max, colspan + 1);
				map.put(String.format("%d.%d", cx, colspan), column);
				fillX(rows, cx + 1, column, map, colspan);
			}
		}

		return max;
	}

	/**
	 * @author andold
	 * @since 2021-12-07
	 */
	private void fillX(List<ChronologyRow> rows, int index, ChronologyColumn column, Map<String, ChronologyColumn> map, int colspan) {
		for (int cx = index, sizex = rows.size(); cx < sizex; cx++) {
			ChronologyRow row = rows.get(cx);
			Double title = row.getTitle();
			if (title >= column.getBigHistory().getEnd()) {
				break;
			}

			map.put(String.format("%d.%d", cx, colspan), column);
		}
	}

	private int findFirst(Map<String, ChronologyColumn> map, int row) {
		for (int cx = 0; cx < 16; cx++) {
			ChronologyColumn the = map.get(String.format("%d.%d", row, cx));
			if (the == null) {
				return cx;
			}
		}

		return 0;
	}

	private void makeChronologyColumn(Chronology chronology, ChronologyRow row, List<ChronologyColumn> columns, List<Double> keyList, int indexStart, Double start, List<BigHistoryEntity> listBigHistory) {
		for (int cx = 0, sizex = listBigHistory.size(); cx < sizex; cx++) {
			BigHistoryEntity bigHistory = listBigHistory.get(cx);
			if (bigHistory == null) {
				continue;
			}

			Double end = bigHistory.getEnd();
			if (end == null) {
				continue;
			}

			int rowspan = 0;
			for (int cy = indexStart + 1, sizey = keyList.size(); cy < sizey; cy++) {
				if (end < keyList.get(cy)) {
					break;
				}

				rowspan++;
			}

			ChronologyColumn column = new ChronologyColumn();
			columns.add(column);
			column.setRowspan(rowspan);
			column.setColspan(columns.size());
			column.setBigHistory(bigHistory);
		}
	}

	public List<BigHistoryEntity> search(BigHistorySearchRequest form) {
		log.info("{} search({})", Utility.indentStart(), Utility.toStringJson(form));
		long started = System.currentTimeMillis();

		if (form == null) {
			List<BigHistoryEntity> list = repository.findAll();
			log.info("{} #{} - search({}) - {}", Utility.indentEnd(), Utility.size(list), Utility.toStringJson(form), Utility.toStringPastTimeReadable(started));
			return list;
		}

//		List<BigHistoryEntity> list = repository.search(form);
		List<BigHistoryEntity> list = repository.findAll(BigHistorySpecification.searchWith(form));

		log.info("{} #{} - search({}) - {}", Utility.indentEnd(), Utility.size(list), Utility.toStringJson(form), Utility.toStringPastTimeReadable(started));
		return list;
	}

	public Map<String, List<BigHistoryEntity>> upload(MultipartFile file, String content) {
		log.info("{} upload({}, {})", Utility.indentStart(), "...", Utility.ellipsis(content, 64));
		long started = System.currentTimeMillis();

		if ((file == null || file.isEmpty()) && (content == null || content.isEmpty())) {
			log.info("{} #{} upload({}, {}) - {}", Utility.indentEnd(), -1, "...", Utility.ellipsis(content, 64), Utility.toStringPastTimeReadable(started));
			return new HashMap<String, List<BigHistoryEntity>>();
		}

		List<BigHistoryEntity> listBefore = search(null);
		if (file == null || file.isEmpty()) {
			List<BigHistoryEntity> listAfter = parse(content);
			Map<String, List<BigHistoryEntity>> map = synchronize(listAfter, listBefore);

			log.info("{} #{} upload({}, {}) - {}", Utility.indentEnd(), Utility.size(map), "...", Utility.ellipsis(content, 64), Utility.toStringPastTimeReadable(started));
			return map;
		}

		try {
			String text = Utility.extractStringFromText(file.getInputStream());
			List<BigHistoryEntity> listAfter = parse(text);
			Map<String, List<BigHistoryEntity>> map = synchronize(listAfter, listBefore);

			log.info("{} #{} upload({}, {}) - {}", Utility.indentEnd(), Utility.size(map), "...", Utility.ellipsis(content, 64), Utility.toStringPastTimeReadable(started));
			return map;
		} catch (IOException e) {
		}

		log.info("{} #{} upload({}, {}) - {}", Utility.indentEnd(), null, "...", Utility.ellipsis(content, 64), Utility.toStringPastTimeReadable(started));
		return null;
	}

	private Map<String, List<BigHistoryEntity>> synchronize(List<BigHistoryEntity> listAfter, List<BigHistoryEntity> listBefore) {
		if (VERSION > 0.0) {
			return synchronizeById(listAfter, listBefore);
		}

		Map<String, List<BigHistoryEntity>> map = new HashMap<String, List<BigHistoryEntity>>();
		List<BigHistoryEntity> listCreate = new ArrayList<BigHistoryEntity>();
		List<BigHistoryEntity> listRead = new ArrayList<BigHistoryEntity>();
		List<BigHistoryEntity> listUpdate = new ArrayList<BigHistoryEntity>();
		List<BigHistoryEntity> listDelete = new ArrayList<BigHistoryEntity>();

		Map<String, BigHistoryEntity> mapAfter = makeMapByStartEndTitle(listAfter);
		Map<String, BigHistoryEntity> mapBefore = makeMapByStartEndTitle(listBefore);
		for (String key : mapBefore.keySet()) {
			BigHistoryEntity after = mapAfter.get(key);
			BigHistoryEntity before = mapBefore.get(key);
			if (after == null) {
				listDelete.add(before);
				continue;
			}

			boolean dirty = false;
			if (Utility.compare(before.getTitle(), after.getTitle()) != 0) {
				dirty = true;
			}
			if (Utility.compare(before.getDescription(), after.getDescription()) != 0) {
				dirty = true;
			}
			//			if (Utility.compare(before.getParentId(), after.getParentId()) != 0) {			dirty = true;	}
			if (dirty) {
				listUpdate.add(before);
				BeanUtils.copyProperties(after, before, "id", "created");
				continue;
			}

			listRead.add(before);
		}

		for (String key : mapAfter.keySet()) {
			BigHistoryEntity after = mapAfter.get(key);
			BigHistoryEntity before = mapBefore.get(key);
			if (before == null) {
				listCreate.add(after);
				continue;
			}
		}

		List<BigHistoryEntity> listCreated = create(listCreate);
		List<BigHistoryEntity> listUpdated = update(listUpdate);
		map.put("listRead", listRead);
		map.put("listCreate", listCreated);
		map.put("listUpdate", listUpdated);
		map.put("listDelete", listDelete);
		return map;
	}
	private Map<String, List<BigHistoryEntity>> synchronizeById(List<BigHistoryEntity> listAfter, List<BigHistoryEntity> listBefore) {
		Map<String, List<BigHistoryEntity>> map = new HashMap<String, List<BigHistoryEntity>>();
		List<BigHistoryEntity> listCreate = new ArrayList<BigHistoryEntity>();
		List<BigHistoryEntity> listRead = new ArrayList<BigHistoryEntity>();
		List<BigHistoryEntity> listUpdate = new ArrayList<BigHistoryEntity>();
		List<BigHistoryEntity> listDelete = new ArrayList<BigHistoryEntity>();

		Map<String, BigHistoryEntity> mapAfter = makeMapById(listAfter);
		Map<String, BigHistoryEntity> mapBefore = makeMapById(listBefore);
		for (String key : mapBefore.keySet()) {
			BigHistoryEntity after = mapAfter.get(key);
			BigHistoryEntity before = mapBefore.get(key);
			if (after == null) {
				listDelete.add(before);
				continue;
			}

			boolean dirty = false;
			if (Utility.compare(before.getStart(), after.getStart()) != 0) {
				dirty = true;
			}
			if (Utility.compare(before.getEnd(), after.getEnd()) != 0) {
				dirty = true;
			}
			if (Utility.compare(before.getTitle(), after.getTitle()) != 0) {
				dirty = true;
			}
			if (Utility.compare(before.getDescription(), after.getDescription()) != 0) {
				dirty = true;
			}
			if (dirty) {
				listUpdate.add(before);
				BeanUtils.copyProperties(after, before, "id", "created");
				continue;
			}

			listRead.add(before);
		}

		for (String key : mapAfter.keySet()) {
			BigHistoryEntity after = mapAfter.get(key);
			BigHistoryEntity before = mapBefore.get(key);
			if (before == null) {
				listCreate.add(after);
				continue;
			}
		}

		List<BigHistoryEntity> listCreated = create(listCreate);
		List<BigHistoryEntity> listUpdated = update(listUpdate);
		map.put("listRead", listRead);
		map.put("listCreate", listCreated);
		map.put("listUpdate", listUpdated);
		map.put("listDelete", listDelete);
		return map;
	}

	private List<BigHistoryEntity> update(List<BigHistoryEntity> list) {
		log.info("{} update(#{})", Utility.indentStart(), Utility.size(list));
		long started = System.currentTimeMillis();

		if (list == null) {
			log.warn("{} {}:NULL_PARAMETER - update(#{}) - {}", Utility.indentEnd(), null, Utility.size(list), Utility.toStringPastTimeReadable(started));
			return null;
		}

		List<BigHistoryEntity> listUpdated = new ArrayList<BigHistoryEntity>();
		for (int cx = 0, sizex = list.size(); cx < sizex; cx++) {
			BigHistoryEntity bigHistory = list.get(cx);
			BigHistoryEntity updated = update(bigHistory.getId(), bigHistory);
			listUpdated.add(updated);
		}

		log.info("{} #{} - update({}) - {}", Utility.indentEnd(), Utility.size(listUpdated), Utility.size(list), Utility.toStringPastTimeReadable(started));
		return listUpdated;
	}

	private List<BigHistoryEntity> create(List<BigHistoryEntity> list) {
		log.info("{} create(#{})", Utility.indentStart(), Utility.size(list));
		long started = System.currentTimeMillis();

		if (list == null) {
			log.warn("{} {}:NULL_PARAMETER - create(#{}) - {}", Utility.indentEnd(), null, Utility.size(list), Utility.toStringPastTimeReadable(started));
			return null;
		}

		List<BigHistoryEntity> listCreated = new ArrayList<BigHistoryEntity>();
		for (int cx = 0, sizex = list.size(); cx < sizex; cx++) {
			BigHistoryEntity bigHistory = list.get(cx);
			BigHistoryEntity created = create(bigHistory);
			listCreated.add(created);
		}

		log.info("{} #{} - create({}) - {}", Utility.indentEnd(), Utility.size(listCreated), Utility.size(list), Utility.toStringPastTimeReadable(started));
		return listCreated;
	}

	private Map<String, BigHistoryEntity> makeMapByStartEndTitle(List<BigHistoryEntity> list) {
		Map<String, BigHistoryEntity> map = new HashMap<String, BigHistoryEntity>();
		if (list == null) {
			return map;
		}

		for (int cx = 0, sizex = list.size(); cx < sizex; cx++) {
			BigHistoryEntity bigHistory = list.get(cx);
			if (bigHistory == null) {
				continue;
			}

			String key = String.format("%6f.%6f.%s", bigHistory.getStart(), bigHistory.getEnd(), bigHistory.getTitle());
			map.put(key, bigHistory);
		}

		return map;
	}

	private Map<String, BigHistoryEntity> makeMapById(List<BigHistoryEntity> list) {
		Map<String, BigHistoryEntity> map = new HashMap<String, BigHistoryEntity>();
		if (list == null) {
			return map;
		}

		for (int cx = 0, sizex = list.size(); cx < sizex; cx++) {
			BigHistoryEntity bigHistory = list.get(cx);
			if (bigHistory == null) {
				continue;
			}

			String key = String.format("%d", bigHistory.getId());
			map.put(key, bigHistory);
		}

		return map;
	}

	private List<BigHistoryEntity> parse(String content) {
		log.info("{} parse({})", Utility.indentStart(), Utility.ellipsis(content, 64));
		long started = System.currentTimeMillis();

		List<BigHistoryEntity> listBigHistory = extractClassesFromJsonString(content, BigHistoryEntity.class);

		log.info("{} #{} parse({}) - {}", Utility.indentEnd(), Utility.size(listBigHistory), Utility.ellipsis(content, 64), Utility.toStringPastTimeReadable(started));
		return listBigHistory;
	}

	public ScalableVectorGraphics svg(BigHistorySearchRequest form) {
		log.info("{} svg({})", Utility.indentStart(), Utility.toStringJson(form));
		long started = System.currentTimeMillis();

		if (form == null) {
			form = new BigHistorySearchRequest();
			log.warn("{} svg({})", Utility.indentMiddle(), Utility.toStringJson(form));
		}

		if (form.getStart() == null) {
			form.setStart(Utility.UNIVERSE_AGE - 1000d * Utility.ONE_SOLAR_YEAR);
		}
		if (form.getRem() == null) {
			form.setRem(16);
		}
		if (form.getWidth() == null) {
			form.setWidth(1198);
		}

		Integer width = form.getWidth();
		List<BigHistoryEntity> list = search(form);
		if (list == null) {
			log.info("{} {}:NO_DATA - svg({}) - {}", Utility.indentEnd(), null, Utility.toStringJson(form), Utility.toStringPastTimeReadable(started));
			return null;
		}

		ScalableVectorGraphics svg = svgService.makeView(list, form.getRem(), width);

		log.info("{} {} - svg({}) - {}", Utility.indentEnd(), Utility.toStringJson(svg, 32), Utility.toStringJson(form), Utility.toStringPastTimeReadable(started));
		return svg;
	}

	public ScalableVectorGraphics svg2(BigHistorySearchRequest form) {
		log.info("{} svg2({})", Utility.indentStart(), Utility.toStringJson(form));
		long started = System.currentTimeMillis();

		if (form == null) {
			form = new BigHistorySearchRequest();
			log.warn("{} svg({})", Utility.indentMiddle(), Utility.toStringJson(form));
		}

		if (form.getStart() == null) {
			form.setStart(Utility.UNIVERSE_AGE - 1000d * Utility.ONE_SOLAR_YEAR);
		}
		if (form.getRem() == null) {
			form.setRem(16);
		}
		if (form.getWidth() == null) {
			form.setWidth(1198);
		}

		Integer width = form.getWidth();
		List<BigHistoryEntity> list = search(form);
		if (list == null) {
			log.info("{} {}:NO_DATA - svg2({}) - {}", Utility.indentEnd(), null, Utility.toStringJson(form), Utility.toStringPastTimeReadable(started));
			return null;
		}

		ScalableVectorGraphics svg = svgService.makeView2(list, form.getRem(), width);

		log.info("{} {} - svg2({}) - {}", Utility.indentEnd(), Utility.toStringJson(svg, 32), Utility.toStringJson(form), Utility.toStringPastTimeReadable(started));
		return svg;
	}

	public static <T> List<T> extractClassesFromJsonString(String content, Class<T> classParameter) {
		if (content == null) {
			return null;
		}

		List<T> listClass = new ArrayList<T>();
		String[] lines = content.split("\n");
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		for (int cx = 0; cx < lines.length; cx++) {
			try {
				T the = objectMapper.readValue(lines[cx], classParameter);
				listClass.add(the);
			} catch (JsonProcessingException e) {
				log.warn("{} extractClassesFromJsonString: line number: {} - {}", Utility.indent(), cx, lines[cx]);
			}
		}

		return listClass;
	}

}
