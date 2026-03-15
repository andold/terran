package kr.andold.terran.service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ezvcard.VCard;
import kr.andold.utils.Utility;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Utils {
	private static Integer countNull = 0;
	private static Integer countInteger = 0;
	private static Integer countString = 0;
	private static Integer countList = 0;
	private static Integer countMap = 0;
	private static Integer countClass = 0;
	private static Integer countGetter = 0;

	public static List<Object> toList(List<?> objects) {
		log.info("{} toList(#{})", Utility.indentStart(), Utility.size(objects));
		long started = System.currentTimeMillis();

		List<Object> list = new ArrayList<Object>();
		Map<Object, Integer> mapCount = new HashMap<Object, Integer>();
		for (Object item : objects) {
			processPutMapOrAddList(null, list, null, item, mapCount);
		}

		log.info("{} #{} toList(#{}) - {}", Utility.indentEnd(), Utility.size(list), Utility.size(objects), Utility.toStringPastTimeReadable(started));
		return list;
	}

	public static void putMap(Map<String, Object> mapSource, String key, Object object) {
		log.info("{} putMap(#{}, {}, ...)", Utility.indentStart(), Utility.size(mapSource), key);
		long started = System.currentTimeMillis();

		countNull = 0;
		countInteger = 0;
		countString = 0;
		countList = 0;
		countMap = 0;
		countClass = 0;
		countGetter = 0;

		if (object == null) {
			log.info("{} putMap(#{}, {}, ...) - {}", Utility.indentEnd(), Utility.size(mapSource), key, Utility.toStringPastTimeReadable(started));
			return;
		}

		Map<Object, Integer> mapCount = new HashMap<Object, Integer>();
		putMap(mapSource, key, object, mapCount);
		log.info("{} putMap(#{}, {}, ...) - {}", Utility.indentEnd(), Utility.size(mapSource), key, Utility.toStringPastTimeReadable(started));
		log.info("{} 발생빈도 - #Null: {}, #Integer: {}, #String: {}, #List: {}, #Map: {}, #Class: {}, #Getter: {}", Utility.indentEnd(), countNull, countInteger,
			countString, countList, countMap, countClass, countGetter);
	}

	private static void putMap(Map<String, Object> mapSource, String key, Object object, Map<Object, Integer> mapCount) {
		if (processPutMapOrAddList(mapSource, null, key, object, mapCount)) {
			return;
		}
		if (processPutGetterOrAddGetter(mapSource, null, key, object, mapCount)) {
			return;
		}
		
		log.warn("{} unreachable!!! putMap(#{}, {}, {}, #{})", Utility.indentEnd(), Utility.size(mapSource), key, object, Utility.size(mapCount));
	}

	private static boolean processPutGetterOrAddGetter(Map<String, Object> mapSource, List<Object> list, String key, Object object,
		Map<Object, Integer> mapCount) {
		if (object == null) {
			countNull++;
			return true;
		}

		countGetter++;
		Integer count = mapCount.get(object);
		if (count == null) {
			count = 0;
		}
		if (count > 8) {
			return true;
		}

		mapCount.put(object, count + 1);

		Map<String, Object> mapChild = makeMapAsObject(object, mapCount);
		if (mapChild.isEmpty()) {
			return true;
		}

		if (mapSource != null && key != null) {
			mapSource.put(key, mapChild);
		}
		if (list != null) {
			list.add(mapChild);
		}

		return true;
	}

	private static boolean processPutMapOrAddList(Map<String, Object> mapSource, List<Object> list, String key, Object object, Map<Object, Integer> mapCount) {
		if (object == null) {
			countNull++;
			return true;
		}

		Integer count = mapCount.get(object);
		if (count == null) {
			count = 0;
		}
		if (count > 8) {
			return true;
		}

		if (object instanceof Integer) {
			countInteger++;
			mapCount.put(object, count + 1);
			if (mapSource != null && key != null) {
				mapSource.put(key, object);
			}
			if (list != null) {
				list.add(object);
			}

			return true;
		}

		if (object instanceof String) {
			countString++;
			if (mapSource != null && key != null) {
				mapSource.put(key, object);
			}
			if (list != null) {
				list.add(object);
			}
			return true;
		}

		if (object instanceof List) {
			countList++;
			mapCount.put(object, count + 1);
			List<?> objects = (List<?>)object;
			if (objects.isEmpty()) {
				return true;
			}

			List<Object> listChild = new ArrayList<Object>();
			for (Object item : objects) {
				processPutMapOrAddList(null, listChild, null, item, mapCount);
			}
			if (listChild.isEmpty()) {
				return true;
			}

			if (mapSource != null && key != null) {
				mapSource.put(key, listChild);
			}
			if (list != null) {
				list.add(listChild);
			}

			return true;
		}

		if (object instanceof Map) {
			countMap++;
			mapCount.put(object, count + 1);
			Map<?, ?> map = (Map<?, ?>)object;
			Map<String, Object> mapChild = new HashMap<String, Object>();
			for (Object keyItem : map.keySet()) {
				processPutMapOrAddList(mapChild, null, keyItem.toString(), map.get(keyItem), mapCount);
			}

			if (mapChild.isEmpty()) {
				return true;
			}

			if (mapSource != null && key != null) {
				mapSource.put(key, mapChild);
			}
			if (list != null) {
				list.add(mapChild);
			}

			return true;
		}

		if (object instanceof Class) {
			countClass++;
			mapCount.put(object, count + 1);
			return true;
		}

		if (processPutGetterOrAddGetter(mapSource, list, key, object, mapCount)) {
			return true;
		}

		return false;
	}

	static Map<String, Object> makeMapAsObject(Object value, Map<Object, Integer> mapCount) {
		Map<String, Object> mapChild = new HashMap<String, Object>();
		if (value == null) {
			return mapChild;
		}

		return makeMapAsObject(mapChild, value, mapCount);
	}
	public static Map<String, Object> makeMapAsObject(Map<String, Object> map, Object value, Map<Object, Integer> mapCount) {
		if (value == null) {
			return map;
		}

		String classSimpleName = value.getClass().getSimpleName();
		switch (classSimpleName) {
			case "Class":
				return map;
			default:
				break;
		}
		Method[] methods = value.getClass().getMethods();
		for (Method method : methods) {
			String name = method.getName();
			if (name.startsWith("get") && method.getParameterTypes().length == 0) {
				String mname = name.replaceFirst("get", "");
				try {
					final Object valueChild = method.invoke(value);
					putMap(map, mname, valueChild, mapCount);
				} catch (IllegalAccessException e) {
					log.warn("{} makeMapAsObject({}, #{}) - {}:{} - {}", Utility.indentMiddle(), value, Utility.size(mapCount), classSimpleName, name,
						e.getLocalizedMessage());
				} catch (InvocationTargetException e) {
					log.warn("{} makeMapAsObject({}, #{}) - {}:{} - {}", Utility.indentMiddle(), value, Utility.size(mapCount), classSimpleName, name,
						e.getLocalizedMessage());
				}
			}
		}

		return map;
	}

	public static Map<String, Object> toDomain(VCard vcard) {
		Map<String, Object> result = makeMapAsObject(vcard, new HashMap<Object, Integer>());
		return result;
	}

}
