package kr.andold.terran.ics.domain;

import org.springframework.beans.BeanUtils;

import kr.andold.terran.ics.entity.VCalendarEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IcsCalendarDomain extends VCalendarEntity {
	private String keyword;

	public IcsCalendarDomain(VCalendarEntity entity) {
		BeanUtils.copyProperties(entity, this);
	}

	public static VCalendarEntity toEntity(IcsCalendarDomain domain) {
		VCalendarEntity entity = new VCalendarEntity();
		BeanUtils.copyProperties(domain, entity);
		return entity;
	}

	public void defaultIfNull() {
		super.defaultIfNull();
	}

}
