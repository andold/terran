package kr.andold.terran.bookmark.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;
import kr.andold.terran.bookmark.domain.BookmarkParam;
import kr.andold.terran.bookmark.domain.BookmarkParam.BookmarkDifferResult;
import kr.andold.terran.bookmark.domain.BookmarkParam.BookmarkResultCreate;
import kr.andold.terran.bookmark.domain.BookmarkParameter;
import kr.andold.terran.bookmark.entity.Bookmark;
import kr.andold.terran.bookmark.repository.BookmarkRepository;
import kr.andold.utils.Utility;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class BookmarkService {
	private static final Integer ROOT_ID = 53755;
	private static final Bookmark ROOT_DEFAULT = Bookmark.builder().id(ROOT_ID).title("잡다한 인연들").url("http://andold.iptime.org/").description("잡다한 인연들").pid(
		ROOT_ID).count(0).created(new Date()).updated(new Date()).build();

	@Autowired
	private BookmarkRepository repository;

	public List<Bookmark> list() {
		log.info("{} list()", Utility.indentStart());
		long started = System.currentTimeMillis();

		List<Bookmark> list = repository.findByOrderByPidAscIdAsc();

		log.info("{} #{} - list() - {}", Utility.indentEnd(), Utility.size(list), Utility.toStringPastTimeReadable(started));
		return list;
	}

	@Transactional
	@Modifying
	public Bookmark create(Bookmark bookmark) {
		log.info("{} create({})", Utility.indentStart(), bookmark);
		long started = System.currentTimeMillis();

		if (bookmark == null) {
			log.info("{} {} - create({}) - {}", Utility.indentEnd(), null, bookmark, Utility.toStringPastTimeReadable(started));
			return null;
		}

		Date date = new Date();
		//bookmark.setId(null);
		if (bookmark.getDescription() == null) {
			bookmark.setDescription("");
		}
		if (bookmark.getCreated() == null) {
			bookmark.setCreated(date);
		}
		if (bookmark.getUpdated() == null) {
			bookmark.setUpdated(date);
		}
		Bookmark created = repository.saveAndFlush(bookmark);

		log.info("{} {} - create({}) - {}", Utility.indentEnd(), created, bookmark, Utility.toStringPastTimeReadable(started));
		return created;
	}

	public List<Bookmark> create(List<Bookmark> list) {
		log.info("{} create(#{})", Utility.indentStart(), Utility.size(list));
		long started = System.currentTimeMillis();

		list.forEach(bookmark -> {
			Date date = new Date();
			//			if (bookmark.getId() == null) {
			//				bookmark.setId(0);
			//			}
			if (bookmark.getDescription() == null) {
				bookmark.setDescription("");
			}
			if (bookmark.getCreated() == null) {
				bookmark.setCreated(date);
			}
			if (bookmark.getUpdated() == null) {
				bookmark.setUpdated(date);
			}
		});
		List<Bookmark> created = repository.saveAllAndFlush(list);

		log.info("{} #{} create(#{}) - {}", Utility.indentEnd(), created, Utility.size(list), Utility.toStringPastTimeReadable(started));
		return created;
	}

	public Bookmark read(Integer id) {
		log.info("{} read({})", Utility.indentStart(), id);
		long started = System.currentTimeMillis();

		if (id == null) {
			log.warn("{} {} read({}) - {}", Utility.indentEnd(), null, id, Utility.toStringPastTimeReadable(started));
			return null;
		}

		BookmarkParameter request = new BookmarkParameter();
		request.setId(id);
		List<Bookmark> list = search(request);
		if (list == null) {
			log.warn("{} {} read({}) - {}", Utility.indentEnd(), null, id, Utility.toStringPastTimeReadable(started));
			return null;
		}

		Bookmark bookmark = null;
		for (int cx = 0, sizex = list.size(); cx < sizex; cx++) {
			Bookmark theBookmark = list.get(cx);
			if (theBookmark == null) {
				continue;
			}

			if (bookmark == null) {
				bookmark = theBookmark;
				continue;
			}

			repository.delete(theBookmark);
			log.warn("{} {} read({}) - {}", Utility.indentMiddle(), null, id, Utility.toStringPastTimeReadable(started));
		}

		log.info("{} {} read({}) - {}", Utility.indentEnd(), bookmark, id, Utility.toStringPastTimeReadable(started));
		return bookmark;
	}

	public Bookmark root() {
		List<Bookmark> bookmarks = search(null);
		return root(bookmarks);
	}

	private Bookmark root(List<Bookmark> bookmarks) {
		for (Bookmark bookmark : bookmarks) {
			Integer id = bookmark.getId();
			Integer pid = bookmark.getPid();

			if (isRoot(id, pid)) {
				return bookmark;
			}
		}

		return null;
	}

	public static Bookmark root(Map<Integer, Bookmark> map) {
		for (Bookmark bookmark : map.values()) {
			Integer id = bookmark.getId();
			Integer pid = bookmark.getPid();

			if (isRoot(id, pid)) {
				return bookmark;
			}
		}

		return null;
	}

	public static boolean isRoot(Integer id, Integer pid) {
		return id == null || pid == null || id.equals(0) || pid.equals(0) || id.equals(ROOT_ID) || id.equals(pid);
	}

	public static Bookmark rootDefault() {
		return ROOT_DEFAULT;
	}

	private List<Bookmark> children(Bookmark bookmark, Map<Integer, Bookmark> map) {
		List<Bookmark> result = new ArrayList<Bookmark>();
		if (bookmark == null || map == null) {
			return result;
		}
		
		Integer id = bookmark.getId();
		for (Bookmark cx : map.values()) {
			if (cx.getPid().equals(id)) {
				if (cx.getId().equals(id)) {
					// self
					continue;
				}
				
				result.add(cx);
			}
		}

		return result;
	}

	public int differByContent(List<Bookmark> listAfter, List<Bookmark> listCreate, List<Bookmark> listUpdate, List<Bookmark> listDelete) {
		List<Bookmark> listBefore = list();
		Map<Integer, Bookmark> mapAfter = makeMap(listAfter);
		Map<Integer, Bookmark> mapBefore = makeMap(listBefore);

		return differ(mapBefore, mapAfter, listCreate, listDelete, listUpdate);
	}

	public int synchronize(List<Bookmark> listAfter, List<Bookmark> listCreate, List<Bookmark> listUpdate, List<Bookmark> listDelete) {
		log.info("{} synchronize(#{}, #{}, #{}, #{})", Utility.indentStart(), Utility.size(listAfter), Utility.size(listCreate), Utility.size(listDelete),
			Utility.size(listUpdate));
		long started = System.currentTimeMillis();

		List<Bookmark> listBefore = list();
		Map<Integer, Bookmark> mapAfter = makeMap(listAfter);
		Map<Integer, Bookmark> mapBefore = makeMap(listBefore);

		int result = synchronize(mapBefore, mapAfter, listCreate, listDelete, listUpdate);

		log.info("{} {} synchronize(#{}, #{}, #{}, #{}) - {}", Utility.indentEnd(), result, Utility.size(listAfter), Utility.size(listCreate),
			Utility.size(listDelete), Utility.size(listUpdate), Utility.toStringPastTimeReadable(started));
		return result;
	}

	private int synchronize(Map<Integer, Bookmark> mapBefore, Map<Integer, Bookmark> mapAfter, List<Bookmark> listCreate, List<Bookmark> listDelete,
		List<Bookmark> listUpdate) {
		log.info("{} synchronize(#{}, #{}, #{}, #{}, #{})", Utility.indentStart(), Utility.size(mapBefore), Utility.size(mapAfter), Utility.size(listCreate),
			Utility.size(listDelete), Utility.size(listUpdate));
		long started = System.currentTimeMillis();

		if (mapBefore == null || mapAfter == null || listCreate == null || listDelete == null || listUpdate == null) {
			log.info("{} #{} synchronize(#{}, #{}, #{}, #{}, #{}) - {}", Utility.indentEnd(), -1, Utility.size(mapBefore), Utility.size(mapAfter),
				Utility.size(listCreate), Utility.size(listDelete), Utility.size(listUpdate), Utility.toStringPastTimeReadable(started));
			return -1;
		}

		for (Integer key : mapBefore.keySet()) {
			Bookmark after = mapAfter.get(key);
			Bookmark before = mapBefore.get(key);
			if (after == null) {
				listDelete.add(before);
				continue;
			}

			if (after.getId() == 54410) {
				log.debug("{} {} .vs. {}", Utility.indentMiddle(), after, before);
			}
			if (after.getUpdated() == null) {
				continue;
			}

			if (equivalent(before, after)) {
				continue;
			}

			log.info("{} updated before: {}", Utility.indentMiddle(), before);
			log.info("{} updated after : {}", Utility.indentMiddle(), after);
			merge(before, after, 2);
			listUpdate.add(before);
		}

		log.info("{} synchronize(#{}, #{}, #{}, #{}, #{}) - {}", Utility.indentMiddle(), Utility.size(mapBefore), Utility.size(mapAfter),
			Utility.size(listCreate), Utility.size(listDelete), Utility.size(listUpdate), Utility.toStringPastTimeReadable(started));
		for (Integer key : mapAfter.keySet()) {
			Bookmark after = mapAfter.get(key);
			Bookmark before = mapBefore.get(key);
			if (before == null) {
				listCreate.add(after);
				continue;
			}
		}

		log.info("{} synchronize(#{}, #{}, #{}, #{}, #{}) - {}", Utility.indentEnd(), Utility.size(mapBefore), Utility.size(mapAfter), Utility.size(listCreate),
			Utility.size(listDelete), Utility.size(listUpdate), Utility.toStringPastTimeReadable(started));
		return Utility.size(listCreate) + Utility.size(listDelete) + Utility.size(listUpdate);
	}

	private int differ(Map<Integer, Bookmark> mapBefore, Map<Integer, Bookmark> mapAfter, List<Bookmark> listCreate, List<Bookmark> listDelete,
		List<Bookmark> listUpdate) {
		log.info("{} differ(#{}, #{}, #{}, #{}, #{})", Utility.indentStart(), Utility.size(mapBefore), Utility.size(mapAfter), Utility.size(listCreate),
			Utility.size(listDelete), Utility.size(listUpdate));
		long started = System.currentTimeMillis();

		if (mapBefore == null || mapAfter == null || listCreate == null || listDelete == null || listUpdate == null) {
			log.info("{} #{} differ(#{}, #{}, #{}, #{}, #{}) - {}", Utility.indentEnd(), -1, Utility.size(mapBefore), Utility.size(mapAfter),
				Utility.size(listCreate), Utility.size(listDelete), Utility.size(listUpdate), Utility.toStringPastTimeReadable(started));
			return -1;
		}

		for (Integer key : mapBefore.keySet()) {
			Bookmark after = mapAfter.get(key);
			Bookmark before = mapBefore.get(key);
			if (after == null) {
				listDelete.add(before);
				continue;
			}

			if (!equivalent(before, after)) {
				log.info("{} updated before: {}", Utility.indentMiddle(), before);
				log.info("{} updated after : {}", Utility.indentMiddle(), after);
				if (before.getUpdated() == null || before.getUpdated().before(after.getUpdated())) {
					merge(before, after, 1);
					listUpdate.add(before);
				}
			}
		}

		log.info("{} differ(#{}, #{}, #{}, #{}, #{}) - {}", Utility.indentMiddle(), Utility.size(mapBefore), Utility.size(mapAfter), Utility.size(listCreate),
			Utility.size(listDelete), Utility.size(listUpdate), Utility.toStringPastTimeReadable(started));
		for (Integer key : mapAfter.keySet()) {
			Bookmark after = mapAfter.get(key);
			Bookmark before = mapBefore.get(key);
			if (before == null) {
				listCreate.add(after);
				continue;
			}
		}

		log.info("{} differ(#{}, #{}, #{}, #{}, #{}) - {}", Utility.indentEnd(), Utility.size(mapBefore), Utility.size(mapAfter), Utility.size(listCreate),
			Utility.size(listDelete), Utility.size(listUpdate), Utility.toStringPastTimeReadable(started));
		return Utility.size(listCreate) + Utility.size(listDelete) + Utility.size(listUpdate);
	}

	private Bookmark merge(Bookmark left, Bookmark right, int degree) {
		left.setId(right.getId());
		left.setTitle(right.getTitle());
		left.setUrl(right.getUrl());
		left.setDescription(right.getDescription());
		left.setPid(right.getPid());
		left.setCreated(Utility.min(left.getCreated(), right.getCreated()));
		if (degree < 1) {
			return left;
		}

		left.setCount(Utility.max(left.getCount(), right.getCount()));
		left.setUpdated(Utility.max(left.getUpdated(), right.getUpdated()));
		left.setStatus(right.getStatus());

		return left;
	}

	private boolean equivalent(Bookmark left, Bookmark right) {
		if (left == null && right == null) {
			return true;
		}

		if (left == null || right == null) {
			return false;
		}

		if (Utility.compare(left.getTitle(), right.getTitle()) != 0) {
			return false;
		}
		if (Utility.compare(left.getUrl(), right.getUrl()) != 0) {
			return false;
		}
		if (Utility.compare(left.getDescription(), right.getDescription()) != 0) {
			return false;
		}
		if (Utility.compare(left.getPid(), right.getPid()) != 0) {
			return false;
		}
		if (Utility.compare(left.getId(), right.getId()) != 0) {
			return false;
		}

		return true;
	}

	public static Map<Integer, Bookmark> makeMap(List<Bookmark> list) {
		Map<Integer, Bookmark> map = new HashMap<Integer, Bookmark>();
		if (list == null) {
			return map;
		}

		for (int cx = 0, sizex = list.size(); cx < sizex; cx++) {
			Bookmark object = list.get(cx);
			if (object == null) {
				continue;
			}

			map.put(object.getId(), object);
		}

		return map;
	}

	private Map<String, Bookmark> makeMapByKey(List<Bookmark> bookmarks, Map<Integer, Bookmark> mapBookmark) {
		Map<String, Bookmark> map = new HashMap<String, Bookmark>();
		for (Bookmark bookmark : bookmarks) {
			String key = BookmarkParam.key(bookmark, mapBookmark);
			map.put(key, bookmark);
		}
		return map;
	}

	public Bookmark update(Integer id, Bookmark bookmark, Boolean force) {
		log.info("{} update(#{}, {})", Utility.indentStart(), id, bookmark);
		long started = System.currentTimeMillis();

		if (id == null || bookmark == null) {
			return null;
		}

		Bookmark base = read(id);
		if (base == null) {
			return null;
		}

		if (force != null && force.booleanValue()) {
			BeanUtils.copyProperties(bookmark, base, "sid", "id");
			update(base);

			log.info("{} {} update(#{}, {}) - {}", Utility.indentEnd(), base, id, bookmark, Utility.toStringPastTimeReadable(started));
			return base;
		}

		boolean dirty = copyNotNullExcludeId(base, bookmark);

		if (dirty) {
			update(base);
		}

		log.info("{} {} update(#{}, {}) - {}", Utility.indentEnd(), base, id, bookmark, Utility.toStringPastTimeReadable(started));
		return base;
	}

	public Bookmark updateCountIncrease(Integer id) {
		log.info("{} updateCountIncrease({})", Utility.indentStart(), id);
		long started = System.currentTimeMillis();

		Bookmark base = read(id);
		if (base == null) {
			return null;
		}

		if (!StringUtils.hasText(base.getUrl())) {
			if (base.getCount() == null) {
				base.setCount(1);
			} else {
				base.setCount(base.getCount().intValue() + 1);
			}

			Bookmark updated = update(base);

			log.info("{} 『{}』 updateCountIncrease({}) - {}", Utility.indentEnd(), updated, id, Utility.toStringPastTimeReadable(started));
			return updated;
		}

		String url = base.getUrl().strip();
		List<Bookmark> bookmarks = search(null);
		List<Bookmark> container = new ArrayList<>();
		for (Bookmark bookmark: bookmarks) {
			if (bookmark == null || !StringUtils.hasText(bookmark.getUrl())) {
				continue;
			}
			
			String urlThis = bookmark.getUrl().strip();
			if (url.contains(urlThis)) {
				if (bookmark.getCount() == null) {
					bookmark.setCount(1);
				} else {
					bookmark.setCount(bookmark.getCount().intValue() + 1);
				}

				Bookmark updated = update(bookmark);
				container.add(updated);
			}
		}

		log.info("{} 『#{}』 updateCountIncrease({}) - {}", Utility.indentEnd(), Utility.size(container), id, Utility.toStringPastTimeReadable(started));
		return base;
	}

	private boolean copyNotNullExcludeId(Bookmark base, Bookmark bookmark) {
		if (base == null || bookmark == null) {
			return false;
		}

		boolean dirty = false;
		if (bookmark.getTitle() != null) {
			base.setTitle(bookmark.getTitle());
			dirty = true;
		}
		if (bookmark.getUrl() != null) {
			base.setUrl(bookmark.getUrl());
			dirty = true;
		}
		if (bookmark.getDescription() != null) {
			base.setDescription(bookmark.getDescription());
			dirty = true;
		}
		if (bookmark.getPid() != null) {
			base.setPid(bookmark.getPid());
			dirty = true;
		}
		if (bookmark.getCount() != null) {
			base.setCount(bookmark.getCount());
			dirty = true;
		}
		if (bookmark.getCreated() != null) {
			base.setCreated(bookmark.getCreated());
			dirty = true;
		}
		if (bookmark.getUpdated() != null) {
			base.setUpdated(bookmark.getUpdated());
			dirty = true;
		}

		return dirty;
	}

	@Transactional
	@Modifying
	private Bookmark update(Bookmark bookmark) {
		log.info("{} update()", Utility.indentStart(), bookmark);
		long started = System.currentTimeMillis();

		if (bookmark == null || bookmark.getId() == null) {
			log.info("{} NULL ATTRIBUTE:{} update({})", Utility.indentEnd(), null, bookmark, Utility.toStringPastTimeReadable(started));
			return null;
		}

		bookmark.setUpdated(new Date());
		Bookmark updated = repository.saveAndFlush(bookmark);

		log.info("{} {} update({})", Utility.indentEnd(), updated, bookmark, Utility.toStringPastTimeReadable(started));
		return updated;
	}

	public void update(List<Bookmark> list, long timestamp) {
		log.info("{} update(#{}, {})", Utility.indentStart(), Utility.size(list), Utility.toStringTimestamp(timestamp));

		if (list == null || list.isEmpty()) {
			return;
		}

		for (int cx = 0, sizex = list.size(); cx < sizex; cx++) {
			Bookmark object = list.get(cx);
			log.debug("{} list[{}/{}] = {} - update(...)", Utility.indentMiddle(), cx, sizex, object);
			if (object == null) {
				continue;
			}

			update(object);
		}

		log.info("{} update(#{}, {})", Utility.indentEnd(), Utility.size(list), Utility.toStringTimestamp(timestamp));
	}

	public boolean delete(Integer id) {
		delete(read(id));

		return true;
	}

	public void delete(List<Bookmark> list) {
		for (int cx = 0, sizex = list.size(); cx < sizex; cx++) {
			Bookmark ojbect = list.get(cx);
			if (ojbect == null) {
				continue;
			}

			delete(ojbect);
		}
	}

	@Transactional
	@Modifying
	private void delete(Bookmark bookmark) {
		repository.delete(bookmark);
	}

	public List<Bookmark> search(BookmarkParameter parameter) {
		log.info("{} search({})", Utility.indentStart(), parameter);
		long started = System.currentTimeMillis();

		List<Bookmark> bookmarks = (parameter == null) ? list() : repository.search(parameter);

		log.info("{} #{} - search({}) - {}", Utility.indentEnd(), Utility.size(bookmarks), parameter, Utility.toStringPastTimeReadable(started));
		return bookmarks;
	}

	public Integer aggreagateCount() {
		List<Bookmark> bookmarks = search(null);
		Map<Integer, Bookmark> map = makeMap(bookmarks);
		Bookmark root = root(map);
		return aggreagateCount(root, map);
	}

	private Integer aggreagateCount(Bookmark bookmark, Map<Integer, Bookmark> map) {
		if (bookmark == null || map == null) {
			return 0;
		}

		List<Bookmark> children = children(bookmark, map);
		if (children == null || children.isEmpty()) {
			return bookmark.getCount() == null ? 0 : bookmark.getCount().intValue();
		}

		Integer count = 0;
		for (int cx = 0, sizex = children.size(); cx < sizex; cx++) {
			count += aggreagateCount(children.get(cx), map);
		}

		if (Utility.compare(count, bookmark.getCount()) == 0) {
			return count;
		}

		bookmark.setCount(count);
		update(bookmark);

		return count;
	}

	public long decreaseCountHalf() {
		List<Bookmark> bookmarks = search(null);
		Map<Integer, Bookmark> map = makeMap(bookmarks);
		Bookmark root = root(map);
		return decreaseCountHalf(root, map);
	}

	private Integer decreaseCountHalf(Bookmark bookmark, Map<Integer, Bookmark> map) {
		if (bookmark == null) {
			return 0;
		}

		List<Bookmark> children = children(bookmark, map);
		if (children == null || children.isEmpty()) {
			bookmark.setCount(bookmark.getCount() == null ? 0 : bookmark.getCount().intValue() / 2);
			update(bookmark);

			return bookmark.getCount().intValue();
		}

		Integer count = 0;
		for (int cx = 0, sizex = children.size(); cx < sizex; cx++) {
			count += decreaseCountHalf(children.get(cx), map);
		}

		bookmark.setCount(bookmark.getCount() == null ? 0 : count);
		update(bookmark);

		return bookmark.getCount().intValue();
	}

	public BookmarkParam download() {
		List<Bookmark> bookmarks = list();
		BookmarkParam root = BookmarkParam.of(bookmarks);
		return root;
	}

	public static String readFileFromText(MultipartFile multipartFile) {
		log.debug("{} readFile({})", Utility.indentStart(), multipartFile);
		long started = System.currentTimeMillis();

		if (multipartFile == null) {
			log.warn("{} {} readFile({}) - {}", Utility.indentEnd(), null, multipartFile, Utility.toStringPastTimeReadable(started));
			return null;
		}

		try {
			String string = Utility.extractStringFromText(multipartFile.getInputStream());
			log.debug("{} #{} readFile({}) - {}", Utility.indentEnd(), Utility.length(string), multipartFile, Utility.toStringPastTimeReadable(started));
			return string;
		} catch (IOException e) {
		}

		log.warn("{} {} readFile({}) - {}", Utility.indentEnd(), null, multipartFile, Utility.toStringPastTimeReadable(started));
		return null;
	}

	public BookmarkDifferResult upload(MultipartFile file) {
		log.info("{} upload(...)", Utility.indentStart());
		String content = readFileFromText(file);
		BookmarkParam after = BookmarkParam.of(content);
		BookmarkDifferResult result = differ(after);
		log.info("{} #{} upload(...)", Utility.indentEnd(), Utility.toStringJson(result, 32, 32));
		return result;
	}

	private BookmarkDifferResult differ(BookmarkParam afterBookmarkParamRoot) {
		List<Bookmark> beforeBookmarks = list();
		BookmarkParam beforeBookmarkParam = BookmarkParam.of(beforeBookmarks);

		Map<Integer, Bookmark> mapBookmark = BookmarkService.makeMap(beforeBookmarks);
		Map<String, BookmarkParam> mapKeyBefore = BookmarkParam.makeMapBookmarks(beforeBookmarkParam);
		Map<String, BookmarkParam> mapKeyAfter = BookmarkParam.makeMapBookmarks(afterBookmarkParamRoot);

		List<BookmarkResultCreate> creates = new ArrayList<BookmarkResultCreate>();
		List<BookmarkParam> duplicates = new ArrayList<BookmarkParam>();
		List<Bookmark> updates = new ArrayList<Bookmark>();
		List<Bookmark> removes = new ArrayList<Bookmark>();

		for (Bookmark beforeBookmark : beforeBookmarks) {
			String key = BookmarkParam.key(beforeBookmark, mapBookmark);
			BookmarkParam afterBookmarkParam = mapKeyAfter.get(key);
			if (afterBookmarkParam == null) {
				removes.add(beforeBookmark);
				continue;
			}

			if (BookmarkParam.isSame(beforeBookmark, afterBookmarkParam, mapBookmark)) {
				duplicates.add(afterBookmarkParam);
				continue;
			}

			BookmarkParam.overwrite(beforeBookmark, afterBookmarkParam);
			updates.add(beforeBookmark);
		}
		for (String key : mapKeyAfter.keySet()) {
			BookmarkParam after = mapKeyAfter.get(key);
			BookmarkParam before = mapKeyBefore.get(key);
			if (before == null) {
				creates.add(BookmarkResultCreate.of(after, key));
				continue;
			}
		}

		return BookmarkDifferResult.builder().creates(creates).duplicates(duplicates).updates(updates).removes(removes).build();
	}

	public int batch(BookmarkDifferResult param) {
		if (param == null) {
			return 0;
		}

		int p = 0;
		List<BookmarkResultCreate> creates = param.getCreates();
		List<Bookmark> updates = param.getUpdates();
		List<Bookmark> removes = param.getRemoves();

		if (creates != null) {
			p += Utility.size(createBookmarkResultCreate(creates));
		}
		if (removes != null) {
			p += Utility.size(remove(removes));
		}
		if (updates != null) {
			p += Utility.size(update(updates, new Date()));
		}

		return p;
	}

	private List<?> createBookmarkResultCreate(List<BookmarkResultCreate> creates) {
		List<Bookmark> bookmarks = list();
		Map<Integer, Bookmark> mapBookmark = makeMap(bookmarks);
		Map<String, Bookmark> map = makeMapByKey(bookmarks, mapBookmark);

		List<Bookmark> result = new ArrayList<>();
		creates.sort((a, b) -> a.getTitle().compareTo(b.getTitle()));
		for (BookmarkResultCreate item : creates) {
			Bookmark creating = item.toEntity(map);
			Bookmark created = create(creating);
			result.add(created);
			map.put(BookmarkParam.key(created, mapBookmark), created);
		}

		return result;
	}

	private List<?> update(List<Bookmark> updates, Date date) {
		log.info("{} update(#{}, {})", Utility.indentStart(), Utility.size(updates), date);

		if (updates == null) {
			return updates;
		}

		for (Bookmark bookmark : updates) {
			fillDefault(bookmark);
			bookmark.setUpdated(date);
		}

		List<Bookmark> updated = repository.saveAllAndFlush(updates);

		log.info("{} update(#{}, {})", Utility.indentEnd(), Utility.size(updated), date);
		return updated;
	}

	private void fillDefault(Bookmark bookmark) {
		if (bookmark == null) {
			return;
		}

		Date now = new Date();
		if (bookmark.getTitle() == null) {
			bookmark.setTitle("");
		}
		if (bookmark.getUrl() == null) {
			bookmark.setUrl("");
		}
		if (bookmark.getDescription() == null) {
			bookmark.setDescription("");
		}
		if (bookmark.getPid() == null) {
			bookmark.setPid(0);
		}
		if (bookmark.getCreated() == null) {
			bookmark.setCreated(now);
		}
		if (bookmark.getUpdated() == null) {
			bookmark.setUpdated(now);
		}
	}

	@Modifying
	private List<?> remove(List<Bookmark> removes) {
		repository.deleteAll(removes);
		repository.flush();
		return removes;
	}

	public BookmarkDifferResult deduplicate() {
		List<Bookmark> bookmarks = list();
		Map<Integer, Bookmark> mapBookmark = BookmarkService.makeMap(bookmarks);
		List<BookmarkResultCreate> creates = new ArrayList<BookmarkResultCreate>();
		List<Bookmark> updates = new ArrayList<Bookmark>();
		List<Bookmark> removes = new ArrayList<Bookmark>();

		Bookmark root = root(mapBookmark);
		if (root == null) {
			// 루트 노드가 없을 경우
			root = rootDefault();
			creates.add(BookmarkResultCreate.of(BookmarkParam.of(root, mapBookmark), root.getTitle()));
		}
		
		if (!root.getPid().equals(root.getId())) {
			// 루트 링크가 잘못되어 있는 경우
			root.setPid(root.getId());
			updates.add(root);
		}

		Map<String, Bookmark> mapUnique = new HashMap<String, Bookmark>();
		for (Bookmark bookmark : bookmarks) {
			String key = BookmarkParam.key(bookmark, mapBookmark);
			Bookmark previous = mapUnique.get(key);
			if (previous == null) {
				mapUnique.put(key, bookmark);
				continue;
			}

			removes.add(bookmark);
		}
		
		// pid가 무효한 경우, 루트로 수정
		for (Bookmark bookmark : mapUnique.values()) {
			if (mapBookmark.get(bookmark.getPid()) == null) {
				bookmark.setPid(root.getId());
				updates.add(bookmark);
			}
		}

		return BookmarkDifferResult.builder().creates(creates).updates(updates).removes(removes).build();
	}

}
