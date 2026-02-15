package kr.andold.terran.solar.mppt.interfaces;

public interface CrudService<Param, Domain, Entity> extends kr.andold.utils.persist.CrudService<Param, Domain, Entity> {
	Param searchWithPageable(Param param);

}
