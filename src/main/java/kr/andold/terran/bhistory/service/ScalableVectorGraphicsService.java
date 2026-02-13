package kr.andold.terran.bhistory.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import kr.andold.terran.bhistory.domain.ScalableVectorGraphics;
import kr.andold.terran.bhistory.domain.ScalableVectorGraphicsEvent;
import kr.andold.terran.bhistory.domain.ScalableVectorGraphicsTimeLine;
import kr.andold.terran.bhistory.entity.BigHistoryEntity;
import kr.andold.utils.Utility;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ScalableVectorGraphicsService {
	public ScalableVectorGraphics makeView(List<BigHistoryEntity> list, Integer rem, Integer width) {
		log.info("{} makeView(#{}, {}, {})", Utility.indentStart(), Utility.size(list), rem, width);
		long started = System.currentTimeMillis();

		if (list == null) {
			log.info("{} {}:NULL_PARAMETER - makeView(#{}, {}, {}) - {}", Utility.indentEnd(), null, Utility.size(list), rem, width, Utility.toStringPastTimeReadable(started));
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

		ScalableVectorGraphics svg = new ScalableVectorGraphics();

		List<ScalableVectorGraphicsEvent> listEvent = new ArrayList<ScalableVectorGraphicsEvent>();
		svg.setListEvent(listEvent);

		//	시각 표시
		Map<Double, Integer> mapTimeY = placeTimeLine(svg, listEvent, keyList, rem, width);
//		Map<Double, Integer> mapTimeY = placeTimeLine(svg, keyList, rem, width);

		//	항목을 y축 시간순으로 정렬
		placeOrderByParentScalableVectorGraphics(svg, list, listEvent, mapTimeY, rem, width);	//	parent 우선

		//	height 계산
		Integer height = calculateHeight(listEvent, width);

		svg.setWidth(width);
		svg.setHeight(height);

		log.info("{} {} - makeView(#{}, {}, {}) - {}", Utility.indentEnd(), Utility.toStringJson(svg, 32), Utility.size(list), rem, width, Utility.toStringPastTimeReadable(started));
		return svg;
	}

	/**
	 * parent 순으로
	 */
	private void placeOrderByParentScalableVectorGraphics(ScalableVectorGraphics svg, List<BigHistoryEntity> list, List<ScalableVectorGraphicsEvent> listEvent, Map<Double, Integer> mapTimeY, Integer rem, Integer width) {
		for (BigHistoryEntity bigHistory: list) {
			placeScalableVectorGraphics(svg, bigHistory, mapTimeY, listEvent, rem, width);
		}
	}

	private void placeScalableVectorGraphics(ScalableVectorGraphics svg, BigHistoryEntity bigHistory, Map<Double, Integer> mapTimeY, List<ScalableVectorGraphicsEvent> listEvent, Integer rem, Integer width) {
		log.debug("{} placeScalableVectorGraphics(#{}, #{}, {}, {})", Utility.indentStart(), Utility.toStringJson(bigHistory, 32), Utility.size(mapTimeY), Utility.size(listEvent), rem, width);
		long started = System.currentTimeMillis();

		Double start = bigHistory.getStart();
		Double end = bigHistory.getEnd();

		ScalableVectorGraphicsEvent event = new ScalableVectorGraphicsEvent();
		event.setBigHistory(bigHistory);
		Integer startY = mapTimeY.get(start);
		Integer endY = mapTimeY.get(end);
		event.setDuration(endY - startY);
		if (start < end) {
			event.setY(startY);
		} else {
			event.setY(startY - rem);
		}

		// calculate text dy
		Integer dy = Math.max(3 * rem, mapTimeY.get(end) - mapTimeY.get(start));
		event.setDy(dy);

		// calculate dx
		Integer dx = calculateDx(dy, rem, event.getBigHistory().getTitle());
		event.setDx(dx);
		
		//	find x
		Integer x = findHorizontal(svg, listEvent, rem * 3, event.getY(), dx, dy, width);
		event.setX(x);

		writeTextPath(event);
		listEvent.add(event);
		log.debug("{} placeScalableVectorGraphics(#{}, #{}, {}, {}) - {}", Utility.indentEnd(), Utility.toStringJson(bigHistory, 32), Utility.size(mapTimeY), Utility.size(listEvent), rem, width, Utility.toStringPastTimeReadable(started));
	}

	private int findHorizontal(ScalableVectorGraphics svg, List<ScalableVectorGraphicsEvent> listEvent, Integer x, Integer y, Integer dx, Integer dy, Integer width) {
		for (int cx = x; cx < width * 2; cx += 16) {
			if (intersect(svg, cx, y, dx , dy)) {
				continue;
			}
			
			return cx;
		}

		return 0;
	}

	private boolean intersect(ScalableVectorGraphics svg, int x, int y, int dx, int dy) {
		List<ScalableVectorGraphicsEvent> listEvent = svg.getListEvent();
		for (int cx = 0, sizex = listEvent.size(); cx < sizex; cx++) {
			ScalableVectorGraphicsEvent event = listEvent.get(cx);
			if (event == null) {
				continue;
			}

			if (intersect(x + 1, dx - 2, event.getX(), event.getDx()) && intersect(y + 1, Math.max(0, dy - 2), event.getY(), event.getDy())) {
				return true;
			}
		}

		List<ScalableVectorGraphicsTimeLine> listTimeLine = svg.getListTimeLine();
		if (listTimeLine == null) {
			return false;
		}

		for (int cx = 0, sizex = listTimeLine.size(); cx < sizex; cx++) {
			ScalableVectorGraphicsTimeLine timeLine = listTimeLine.get(cx);
			if (timeLine == null) {
				continue;
			}

			if (intersect(x + 1, dx - 2, timeLine.getX(), timeLine.getDx()) && intersect(y + 1, Math.max(0, dy - 2), timeLine.getY(), timeLine.getDy())) {
				return true;
			}
		}

		return false;
	}

	private boolean intersect(Integer x, Integer dx, Integer y, Integer dy) {
		return !(x > y + dy || x + dx < y);
	}

	public static Integer cut(int base, Integer slice) {
		return (base / slice) * slice;
	}
	private Integer calculateDx(Integer dy, Integer rem, String title) {
		Integer min = 16;
		Integer slice = 16;
		Integer length = cut(Math.max(((title.getBytes().length + title.length() + slice - 1) / 3), min), slice);

		if (dy < length / 4 * rem) {
			return cut(length / 3 * rem + rem * 3, slice);
		}

		return cut(length / 3 * rem / dy + rem * 4, slice);
	}

	/**
	 * 시각 표시. 사건 기준으로.
	 */
	private Map<Double, Integer> placeTimeLine(ScalableVectorGraphics svg, List<ScalableVectorGraphicsEvent> listEvent, List<Double> keyList, Integer rem, Integer width) {
		log.info("{} placeTimeLine({}, #{}, #{}, {}, {})", Utility.indentStart(), Utility.toStringJson(svg, 32), Utility.size(listEvent), Utility.size(keyList), rem, width);
		long started = System.currentTimeMillis();
	
		Map<Double, Integer> map = new HashMap<Double, Integer>();
		if (keyList.isEmpty()) {
			return map;
		}

		Double previousTime = keyList.get(0);
		Integer previousY = 0;
		for (int cx = 0, sizex = keyList.size(); cx < sizex; cx++) {
			Double time = keyList.get(cx);
			if (time == null) {
				continue;
			}

			Integer y = previousY + rem * 2;
			if (time < previousTime + Utility.ONE_SOLAR_YEAR * 10) {
				y = previousY + (rem * 2);
			} else if (time < previousTime + Utility.ONE_SOLAR_YEAR * 100) {
				y = previousY + (rem * 3);
			} else if (time < previousTime + Utility.ONE_SOLAR_YEAR * 1000) {
				y = previousY + (rem * 4);
			} else if (time < previousTime + Utility.ONE_SOLAR_YEAR * 10000) {
				y = previousY + (rem * 5);
			} else {
				y = previousY + (rem * 6);
			}

			ScalableVectorGraphicsEvent event = new ScalableVectorGraphicsEvent();
			listEvent.add(event);
			event.setX(0);
			event.setY(y);
			map.put(time, y);
			event.setDx(7 * rem);
			event.setDy(2 * rem);
			BigHistoryEntity bigHistory = new BigHistoryEntity();
			event.setBigHistory(bigHistory);
			bigHistory.setId(Integer.MAX_VALUE / 2 + cx);
			bigHistory.setTitle(BigHistoryEntity.toTextSpanTime(time, rem / 2));

			writeTextPath(event);
			
			previousTime = time;
			previousY = y;
		}
		
		log.info("{} placeTimeLine({}, #{}, #{}, {}, {}) - {}", Utility.indentEnd(), Utility.toStringJson(svg, 32), Utility.size(listEvent), Utility.size(keyList), rem, width, Utility.toStringPastTimeReadable(started));
		return map;
	}

	@AllArgsConstructor
	class TimeSlot {
		@Getter @Setter private Double end;
		@Getter @Setter private Double unit;
		@Getter @Setter private Integer multiple;
		@Getter @Setter private Integer offset;
		@Getter @Setter private Integer length;
	}

	private Map<Double, Integer> placeTimeLine(ScalableVectorGraphics svg, List<Double> keyList, Integer rem, Integer width) {
		log.info("{} placeTimeLine({}, #{}, {}, {})", Utility.indentStart(), Utility.toStringJson(svg, 32), Utility.size(keyList), rem, width);
		long started = System.currentTimeMillis();
	
		List<TimeSlot> table = new ArrayList<TimeSlot>();
		table.add(new TimeSlot(0d * Math.pow(10d, 0d) * Utility.ONE_SOLAR_YEAR, 0d, 0, rem, 0));	//	0초
		table.add(new TimeSlot(1d * Math.pow(10d, 0d) * Utility.ONE_SOLAR_YEAR, 0d, 8, rem, 50));	//	1초
		table.add(new TimeSlot(1d * Math.pow(10d, 8d) * Utility.ONE_SOLAR_YEAR, Math.pow(10d, 8d) * Utility.ONE_SOLAR_YEAR, 8, -0, 16));	//	1억년

		table.add(new TimeSlot(Utility.UNIVERSE_AGE - 50d * Math.pow(10d, 8d) * Utility.ONE_SOLAR_YEAR, Math.pow(10d, 8d) * Utility.ONE_SOLAR_YEAR, 2, -0, -0));	//	태양계
		table.add(new TimeSlot(Utility.UNIVERSE_AGE - 5.42d * Math.pow(10d, 8d) * Utility.ONE_SOLAR_YEAR, Math.pow(10d, 7d) * Utility.ONE_SOLAR_YEAR, 2, -0, -0));	//	현생누대
		table.add(new TimeSlot(Utility.UNIVERSE_AGE - 6600d * Math.pow(10d, 4d) * Utility.ONE_SOLAR_YEAR, Math.pow(10d, 6d) * Utility.ONE_SOLAR_YEAR, 1, -0, -0));	//	신생대
		table.add(new TimeSlot(Utility.UNIVERSE_AGE - 2000d * Utility.ONE_SOLAR_YEAR, Math.pow(10d, 5d) * Utility.ONE_SOLAR_YEAR, 1, -0, -0));	//	고조선
		table.add(new TimeSlot(Utility.UNIVERSE_AGE - 700d * Utility.ONE_SOLAR_YEAR, Math.pow(10d, 5d) * Utility.ONE_SOLAR_YEAR, 2, -0, -0));	//	탈레스
		table.add(new TimeSlot(Utility.UNIVERSE_AGE + 1392d * Utility.ONE_SOLAR_YEAR, Math.pow(10d, 1d) * Utility.ONE_SOLAR_YEAR, 10, -0, -0));	//	조선
		table.add(new TimeSlot(Utility.UNIVERSE_AGE + 2050d * Utility.ONE_SOLAR_YEAR, Utility.ONE_SOLAR_YEAR, 2, -0, -0));

		table.add(new TimeSlot(Utility.UNIVERSE_AGE + Math.pow(10d, 200d) * Utility.ONE_SOLAR_YEAR, -0d, 4, -0, -0));

		//	calculate length
		table.get(1).setLength(table.get(1).getLength() * table.get(1).getMultiple());
		table.get(2).setLength(table.get(2).getLength() * table.get(2).getMultiple());
		for (int cx = 3, sizex = table.size() - 1; cx < sizex; cx++) {
			TimeSlot prev = table.get(cx - 1);
			TimeSlot slot = table.get(cx);
			int length = (int)((slot.getEnd() - prev.getEnd()) / slot.getUnit()) * slot.getMultiple();
			slot.setLength(length);
		}
		//	calculate offset
		for (int cx = 0, sizex = table.size() - 1; cx < sizex; cx++) {
			TimeSlot slot = table.get(cx);
			TimeSlot next = table.get(cx + 1);
			int offset = slot.getOffset() + slot.getLength();
			next.setOffset(offset);
		}
		Map<Double, Integer> map = new HashMap<Double, Integer>();
		int offset = findVirtical(table, keyList.get(0));
		for (int cx = 0, sizex = keyList.size(); cx < sizex; cx++) {
			Double time = keyList.get(cx);
			if (time == null) {
				continue;
			}

			int y = findVirtical(table, time);
			
			map.put(time, y - offset + rem);
		}

		List<ScalableVectorGraphicsTimeLine> listTimeLine = new ArrayList<ScalableVectorGraphicsTimeLine>();
		svg.setListTimeLine(listTimeLine);
		for (int cx = 0, sizex = keyList.size() - 1; cx < sizex; cx++) {
			Double currentTime = keyList.get(cx);
			Double nextTime = keyList.get(cx + 1);
			Integer currentY = map.get(currentTime);
			Integer nextY = map.get(nextTime);
			String title = String.format("%s ~ %s (%s)", BigHistoryEntity.toHtmlTime(currentTime), BigHistoryEntity.toHtmlTime(nextTime), BigHistoryEntity.toHtmlDuration(nextTime - currentTime));
			ScalableVectorGraphicsTimeLine timeLine = new ScalableVectorGraphicsTimeLine(0, currentY, rem * 2, nextY - currentY, title);
			listTimeLine.add(timeLine);
		}

		log.info("{} placeTimeLine({}, #{}, {}, {}) - {}", Utility.indentEnd(), Utility.toStringJson(svg, 32), Utility.size(keyList), rem, width, Utility.toStringPastTimeReadable(started));
		return map;
	}

	private int findVirtical(List<TimeSlot> table, Double time) {
		time = BigHistoryEntity.significant(time, 12);
		if (time == 0d) {	//	0초
			return table.get(0).getOffset();
		}

		if (time < 1d) {	//	1초
			return table.get(1).getOffset() + (50 + (int)Math.log10(time)) * table.get(1).getMultiple();
		}

		if (time < 1d * Math.pow(10d, 8d) * Utility.ONE_SOLAR_YEAR) {	//	1억년
			return table.get(2).getOffset() + (int)Math.log10(time) * table.get(2).getMultiple();
		}

		for (int cy = 3, sizey = table.size() - 1; cy < sizey; cy++) {
			TimeSlot prev = table.get(cy - 1);
			TimeSlot slot = table.get(cy);
			if (time < slot.getEnd()) {
				return slot.getOffset() + (int)((time - prev.getEnd()) / slot.getUnit()) * slot.getMultiple();
			}
		}

		return table.get(table.size() - 1).getOffset() + (int)Math.log10(time) * table.get(table.size() - 1).getMultiple();
	}

	private void writeTextPath(ScalableVectorGraphicsEvent event) {
		Integer x = event.getX();
		Integer y = event.getY();
		Integer dx = event.getDx();
		Integer dy = event.getDy();

		if (dx >= dy) {	//	가로쓰기
			String path = String.format("M %d, %d H %d", x + 8, y + 22, x + dx - 8);
			for (int cx = 38; cx < dy; cx += 16) {
				path += String.format(" M %d, %d H %d", x + 16, y + cx, x + dx - 16);
			}
			
			event.setVertical(false);
			event.setTextPath(path);

			return;
		}

		//	세로쓰기
		String path = String.format("M %d, %d V %d ", x + 16, y + 8, y + dy - 8);
		for (int cx = 32; cx < dx; cx += 16) {
			path += String.format(" M %d, %d V %d", x + cx, y + 16, y + dy - 16);
		}
		
		event.setVertical(true);
		event.setTextPath(path);
	}

	private Integer calculateHeight(List<ScalableVectorGraphicsEvent> listEvent, Integer width) {
		log.info("{} calculateHeight(#{}, {})", Utility.indentStart(), Utility.size(listEvent), width);
		long started = System.currentTimeMillis();
	
		Integer height = 128;
		for (int cx = 0, sizex = listEvent.size(); cx < sizex; cx++) {
			ScalableVectorGraphicsEvent event = listEvent.get(cx);
			if (event == null) {
				continue;
			}
			
			height = Math.max(height, event.getY() + event.getDy());
		}

		log.info("{} {} - calculateHeight(#{}, {}) - {}", Utility.indentEnd(), height, Utility.size(listEvent), width, Utility.toStringPastTimeReadable(started));
		return height;
	}

	public ScalableVectorGraphics makeView2(List<BigHistoryEntity> list, Integer rem, Integer width) {
		log.info("{} makeView2(#{}, {}, {})", Utility.indentStart(), Utility.size(list), rem, width);
		long started = System.currentTimeMillis();

		if (list == null || list.isEmpty()) {
			log.info("{} {}:NULL_OR_EMPTY - makeView2(#{}, {}, {}) - {}", Utility.indentEnd(), null, Utility.size(list), rem, width, Utility.toStringPastTimeReadable(started));
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

		ScalableVectorGraphics svg = new ScalableVectorGraphics();

		List<ScalableVectorGraphicsEvent> listEvent = new ArrayList<ScalableVectorGraphicsEvent>();
		svg.setListEvent(listEvent);

		//	시각 표시
		Map<Double, Integer> mapTimeY = placeTimeLine(svg, keyList, rem, width);

		//	항목을 y축 시간순으로 정렬
		placeOrderByParentScalableVectorGraphics(svg, list, listEvent, mapTimeY, rem, width);	//	parent 우선

		//	height 계산
		Integer height = calculateHeight(listEvent, width);

		svg.setWidth(width);
		svg.setHeight(height);

		log.info("{} {} - makeView2(#{}, {}, {}) - {}", Utility.indentEnd(), Utility.toStringJson(svg, 32), Utility.size(list), rem, width, Utility.toStringPastTimeReadable(started));
		return svg;
	}

}
