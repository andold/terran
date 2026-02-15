package kr.andold.terran.tsdb.service;

import java.net.URLDecoder;
import java.util.Date;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import kr.andold.terran.solar.mppt.interfaces.CrudService;
import kr.andold.terran.solar.mppt.service.Utility;
import kr.andold.terran.tsdb.domain.TsdbDomain;
import kr.andold.terran.tsdb.entity.TsdbEntity;
import kr.andold.terran.tsdb.param.TsdbParam;
import kr.andold.terran.tsdb.repository.TsdbRepository;
import kr.andold.terran.tsdb.repository.TsdbSpecification;
import kr.andold.utils.persist.CrudList;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TsdbService implements CrudService<TsdbParam, TsdbDomain, TsdbEntity> {
	private static final Sort DEFAULT_SORT = Sort.by(Order.asc("base"));
	private static final Pageable DEFAULT_PAGEABLE = PageRequest.of(0, 1024 * 1024, DEFAULT_SORT);
	private static final String GROUP_DEFAULT = "그룹";
	private static final String MEMBER_DEFAULT = "멤버";

	@Autowired private TsdbRepository repository;

	@Modifying
	public TsdbParam update(TsdbDomain domain) {
		TsdbEntity entity = toEntity(domain);
		TsdbEntity updated = repository.saveAndFlush(entity);
		return toParam(updated);
	}

	private TsdbParam toParam(TsdbEntity entity) {
		TsdbParam param = new TsdbParam();
		BeanUtils.copyProperties(entity, param);
		return param;
	}

	@Override
	public List<TsdbDomain> update(List<TsdbDomain> domains) {
		List<TsdbEntity> entities = toEntities(domains);
		List<TsdbEntity> updated = repository.saveAllAndFlush(entities);
		return toDomains(updated);
	}

	@Override
	public TsdbDomain toDomain(String text) {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setSerializationInclusion(Include.NON_NULL);
		objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		try {
			return objectMapper.readValue(text, TsdbDomain.class);
		} catch (Exception e) {
			try {
				return objectMapper.readValue(URLDecoder.decode(text, "UTF-8"), TsdbDomain.class);
			} catch (Exception f) {
				e.printStackTrace();
				f.printStackTrace();
			}
		}

		return null;
	}

	@Override
	public TsdbDomain toDomain(TsdbEntity entity) {
		TsdbDomain domain = new TsdbDomain();
		BeanUtils.copyProperties(entity, domain);
		return domain;
	}

	@Override
	public TsdbEntity toEntity(TsdbDomain domain) {
		TsdbEntity entity = new TsdbEntity();
		BeanUtils.copyProperties(domain, entity);
		return entity;
	}

	@Override
	public int compare(TsdbDomain after, TsdbDomain before) {
		int compared = key(after).compareTo(key(before));
		if (compared != 0) {
			return compared;
		}

		return Utility.compare(after.getValue(), before.getValue());
	}

	@Override
	public void prepareCreate(TsdbDomain domain) {
		Date date = new Date();
		domain.setId(null);
		if (domain.getBase() == null) {
			domain.setBase(date);
		}
		if (domain.getGroup() == null) {
			domain.setGroup(GROUP_DEFAULT);
		}
		if (domain.getMember() == null) {
			domain.setMember(MEMBER_DEFAULT);
		}
		if (domain.getBase() == null) {
			domain.setBase(date);
		}
		if (domain.getValue() == null) {
			domain.setValue("");
		}
		domain.setCreated(date);
		domain.setUpdated(date);
	}

	@Override
	public void prepareUpdate(TsdbDomain before, TsdbDomain after) {
		if (before == null || after == null) {
			return;
		}

		Utility.copyPropertiesNotNull(after, before);
		before.setUpdated(new Date());
	}

	@Override
	public String key(TsdbDomain domain) {
		return String.format("%tF %<tR.%s.%s", domain.getBase(), domain.getGroup(), domain.getMember());
	}

	@Override
	public List<TsdbDomain> search(TsdbParam param) {
		TsdbParam result = searchWithPageable(param);
		List<TsdbDomain> domains = result.getCrud().getDuplicates();
		return domains;
	}

	@Override
	public int remove(List<TsdbDomain> domains) {
		List<TsdbEntity> entities = toEntities(domains);
		repository.deleteAll(entities);
		repository.flush();
		return Utility.size(entities);
	}

	@Override
	public List<TsdbDomain> create(List<TsdbDomain> domains) {
		for (TsdbDomain domain: domains) {
			prepareCreate(domain);
		}
		List<TsdbEntity> entities = toEntities(domains);
		List<TsdbEntity> created = repository.saveAllAndFlush(entities);
		return toDomains(created);
	}

	@Override
	public TsdbParam searchWithPageable(TsdbParam param) {
		log.info("{} search(『{}』)", Utility.indentStart(), param);
		long started = System.currentTimeMillis();

		if (param == null) {
			Page<TsdbEntity> paged = repository.findAll(DEFAULT_PAGEABLE);
			TsdbParam result = TsdbParam.builder()
					.crud(CrudList.<TsdbDomain>builder()
							.duplicates(toDomains(paged.getContent()))
							.build())
					.build();

			log.info("{} 『{}』 search(『{}』) - {}", Utility.indentEnd(), result, param, Utility.toStringPastTimeReadable(started));
			return result;
		}

		Page<TsdbEntity> paged = repository.findAll(TsdbSpecification.searchWith(param), param);
		log.info("{} 『{}』 search(『{}』) - {}", Utility.indentMiddle(), paged, param, Utility.toStringPastTimeReadable(started));
		List<TsdbDomain> domains = toDomains(paged.getContent());
		log.info("{} 『#{}』 search(『{}』) - {}", Utility.indentMiddle(), Utility.size(domains), param, Utility.toStringPastTimeReadable(started));
		TsdbParam result = TsdbParam.builder()
				.crud(CrudList.<TsdbDomain>builder()
						.duplicates(domains)
						.build())
				.build();

		log.info("{} 『{}』 search(『{}』) - {}", Utility.indentEnd(), result, param, Utility.toStringPastTimeReadable(started));
		return result;
	}

	public CrudList<TsdbDomain> upload(MultipartFile file) {
		log.info("{} upload({})", Utility.indentStart(), Utility.toStringJson(file, 64, 32));
		long started = System.currentTimeMillis();

		try {
			String text = Utility.extractStringFromText(file.getInputStream());
			CrudList<TsdbDomain> result = CrudService.super.upload(text);

			log.info("{} {} - upload({}) - {}", Utility.indentEnd(), result, Utility.toStringJson(file, 32, 32), Utility.toStringPastTimeReadable(started));
			return result;
		} catch (Exception e) {
		}

		log.info("{} {} - upload({}) - {}", Utility.indentEnd(), "EXCEPTION", Utility.toStringJson(file, 32, 32), Utility.toStringPastTimeReadable(started));
		return null;
	}

}
