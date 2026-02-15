package kr.andold.terran.tsdb.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import kr.andold.terran.solar.mppt.domain.SolarMpptDomain;
import kr.andold.terran.tsdb.entity.TsdbEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TsdbDomain extends TsdbEntity {
	public static final String TSDB_GROUP_BASIS = "mppt-controller";
	public static final String TSDB_GROUP_1H = "mppt-controller.1hour";
	public static final String TSDB_GROUP_1D = "mppt-controller.1day";
	public static final String TSDB_MEMBER_TEMPERATURE = "temperature";
	public static final String TSDB_MEMBER_DISCHARGE = "discharge";
	public static final String TSDB_MEMBER_CHARGE = "charge";
	public static final String TSDB_MEMBER_VOLTAGE = "voltage";
	public static final String TSDB_MEMBER_MEMBER_POSTFIX_MIN = "Min";

	@Getter @Setter private String dummy;

	@Override
	public String toString() {
		return super.toString();
	}

	public static List<TsdbDomain> of(SolarMpptDomain solarMpptDomain) {
		Date date = solarMpptDomain.getBase();
		List<TsdbDomain> tsdbDomains = new ArrayList<>();
		if (solarMpptDomain.getTemperature() >= 0) {
			tsdbDomains.add(TsdbDomain.builder().group(TSDB_GROUP_BASIS).base(date).member(TSDB_MEMBER_TEMPERATURE).value(solarMpptDomain.getTemperature().toString()).build());
		}
		if (solarMpptDomain.getDischarge() >= 0) {
			tsdbDomains.add(TsdbDomain.builder().group(TSDB_GROUP_BASIS).base(date).member(TSDB_MEMBER_DISCHARGE).value(solarMpptDomain.getDischarge().toString()).build());
		}
		if (solarMpptDomain.getCharge() >= 0) {
			tsdbDomains.add(TsdbDomain.builder().group(TSDB_GROUP_BASIS).base(date).member(TSDB_MEMBER_CHARGE).value(solarMpptDomain.getCharge().toString()).build());
		}
		if (solarMpptDomain.getVoltage() >= 0) {
			tsdbDomains.add(TsdbDomain.builder().group(TSDB_GROUP_BASIS).base(date).member(TSDB_MEMBER_VOLTAGE).value(solarMpptDomain.getVoltage().toString()).build());
		}

		return tsdbDomains;
	}

}
