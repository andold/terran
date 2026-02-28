package kr.andold.terran.bookmark.domain;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import kr.andold.terran.bookmark.entity.Bookmark;
import kr.andold.terran.bookmark.service.BookmarkService;
import kr.andold.utils.Utility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookmarkParam {
	private static final String DELEMETER_KEY = "⇨";

	private String title;
	private String url;
	private String description;
	private Integer count;
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	@Temporal(TemporalType.TIMESTAMP)
	private Date created;
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	@Temporal(TemporalType.TIMESTAMP)
	private Date updated;

	private List<BookmarkParam> children;

	public static BookmarkParam of(Bookmark bookmark, Map<Integer, Bookmark> mapBookmark) {
		BookmarkParam param = new BookmarkParam();
		BeanUtils.copyProperties(bookmark, param);
		param.setChildren(new ArrayList<>());
		return param;
	}

	public static BookmarkParam of(String text) {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setSerializationInclusion(Include.NON_NULL);
		objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		try {
			return objectMapper.readValue(text, BookmarkParam.class);
		} catch (Exception e) {
			try {
				return objectMapper.readValue(URLDecoder.decode(text, "UTF-8"), BookmarkParam.class);
			} catch (Exception f) {
				e.printStackTrace();
				f.printStackTrace();
			}
		}

		return null;
	}

	public static BookmarkParam of(List<Bookmark> bookmarks) {
		Map<Integer, Bookmark> mapBookmark = BookmarkService.makeMap(bookmarks);

		Bookmark rootBookmark = BookmarkService.root(mapBookmark);
		if (rootBookmark == null) {
			rootBookmark = BookmarkService.rootDefault();
			mapBookmark.put(rootBookmark.getId(), rootBookmark);
		}

		Map<Integer, BookmarkParam> map = new HashMap<>();
		for (Bookmark bookmark : mapBookmark.values()) {
			BookmarkParam param = BookmarkParam.of(bookmark, mapBookmark);
			map.put(bookmark.getId(), param);
		}
		BookmarkParam root = map.get(rootBookmark.getId());
		for (Bookmark bookmark : mapBookmark.values()) {
			Integer id = bookmark.getId();
			Integer pid = bookmark.getPid();
			BookmarkParam param = map.get(id);
			if (id.equals(rootBookmark.getId())) {
				continue;
			}

			BookmarkParam parent = map.get(pid);
			if (parent == null) {
				parent = root;
			}

			parent.getChildren().add(param);
		}

		return root;
	}

	public static Map<String, BookmarkParam> makeMapBookmarks(BookmarkParam root) {
		Map<String, BookmarkParam> map = new HashMap<>();
		if (root == null) {
			return map;
		}
		
		String title = root.getTitle();
		map.put(title, root);
		for (BookmarkParam child : root.getChildren()) {
			put(map, title, child);
		}
		return map;
	}

	private static void put(Map<String, BookmarkParam> map, String prefix, BookmarkParam item) {
		if (map == null || item == null) {
			return;
		}
		
		String title = item.getTitle();
		String key = String.format("%s%s%s", prefix, DELEMETER_KEY, title);
		map.put(key, item);
		for (BookmarkParam child : item.getChildren()) {
			put(map, key, child);
		}
	}

	public static String key(Bookmark bookmark, Map<Integer, Bookmark> mapBookmark) {
		if (bookmark == null || mapBookmark == null) {
			return "잡다한 인연들";
		}

		String title = bookmark.getTitle();
		if (bookmark.getId().equals(bookmark.getPid())) {
			return title;
		}

		return String.format("%s%s%s", key(mapBookmark.get(bookmark.getPid()), mapBookmark), DELEMETER_KEY, title);
	}

	public static boolean isSame(Bookmark bookmark, BookmarkParam bookmarkParam, Map<Integer, Bookmark> mapBookmark) {
		if (Utility.compare(bookmark.getUrl(), bookmarkParam.getUrl()) != 0) {
			return false;
		}
		if (Utility.compare(bookmark.getDescription(), bookmarkParam.getDescription()) != 0) {
			return false;
		}

		return true;
	}

	public static void overwrite(Bookmark before, BookmarkParam after) {
		before.setUrl(after.getUrl());
		before.setDescription(after.getDescription());
		before.setCount(Math.max(before.getCount() == null ? 0 : before.getCount(), after.getCount()));
	}

	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@Data
	static public class BookmarkDifferResult {
		private List<BookmarkResultCreate> creates;
		private List<BookmarkParam> duplicates;
		private List<Bookmark> updates;
		private List<Bookmark> removes;
	}

	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@Data
	static public class BookmarkResultCreate {
		private String title;
		private String url;
		private String description;
		private Integer count;
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
		@Temporal(TemporalType.TIMESTAMP)
		private Date created;
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
		@Temporal(TemporalType.TIMESTAMP)
		private Date updated;

		public static BookmarkResultCreate of(BookmarkParam bookmarkParam, String key) {
			BookmarkResultCreate param = new BookmarkResultCreate();
			BeanUtils.copyProperties(bookmarkParam, param, "title");
			param.setTitle(key);
			return param;
		}

		public Bookmark toEntity(Map<String, Bookmark> map) {
			String key = getTitle();
			Bookmark entity = map.get(key);
			if (entity != null) {
				return entity;
			}
			
			Integer pid = 0;
			int position = key.lastIndexOf(DELEMETER_KEY);
			String title = key.substring(position + 1);
			if (position >= 0) {
				String path = key.substring(0, position);
				Bookmark parent = map.get(path);
				if (parent != null) {
					pid = parent.getId();
				}
			}

			entity = new Bookmark();
			BeanUtils.copyProperties(this, entity, "title");
			entity.setTitle(title);
			entity.setPid(pid);
			return entity;
		}
	}

}
