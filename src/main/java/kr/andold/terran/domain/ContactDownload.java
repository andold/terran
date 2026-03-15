package kr.andold.terran.domain;

import java.util.ArrayList;
import java.util.List;

import kr.andold.utils.Utility;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ContactDownload {
	private String fn;
	private Integer value;
	private List<FieldMap> maps;
	
	@SuperBuilder
	@NoArgsConstructor
	@AllArgsConstructor
	@Data
	public static class FieldMap {
		private String field;
		private Integer value;

		public static FieldMap of(ContactMapDomain map) {
			return FieldMap.builder()
					.field(map.getKey())
					.value(map.getValue())
					.build();
		}

		public static ContactMapDomain to(FieldMap map) {
			return ContactMapDomain.builder()
					.key(map.getField())
					.value(map.getValue())
					.build();
		}
	}

	public static ContactDownload of(ContactDomain contact) {
		List<FieldMap> maps = new ArrayList<>();
		for (ContactMapDomain map : contact.getMaps()) {
			maps.add(FieldMap.of(map));
		}

		ContactDownload download = ContactDownload.builder()
				.fn(contact.getFn())
				.value(contact.getValue())
				.maps(maps)
				.build();

		return download;
	}

	@Override
	public String toString() {
		return Utility.toStringJson(this);
	}

	public ContactDomain to() {
		List<ContactMapDomain> maps = new ArrayList<>();
		for (FieldMap map : getMaps()) {
			maps.add(FieldMap.to(map));
		}
		ContactDomain domain = ContactDomain.builder()
				.fn(getFn())
				.value(getValue())
				.maps(maps)
				.build();

		return domain;
	}

}
