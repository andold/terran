package kr.andold.terran.bhistory.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import kr.andold.utils.Utility;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ZookeeperClient extends kr.andold.utils.ZookeeperClient {
	@Getter private static String zookeeperConnectString;
	@Value("${application.zookeeper.connect.string}")
	public void setZookeeperConnectString(String value) {
		log.info("{} setZookeeperConnectString(『{}』)", Utility.indentMiddle(), value);
		zookeeperConnectString = value;
	}

	@Getter private static String zookeeperZnodeElectPath;
	@Value("${application.zookeeper.znode.elect.path}")
	public void setZookeeperZnodeElectPath(String value) {
		log.info("{} setZookeeperZnodeElectPath(『{}』)", Utility.indentMiddle(), value);
		zookeeperZnodeElectPath = value;
	}

	public void run() {
		log.info("{} run() - 『{}』『{}』", Utility.indentStart(), zookeeperConnectString, zookeeperZnodeElectPath);

		super.run(zookeeperConnectString, zookeeperZnodeElectPath);
		
		log.info("{} run() - 『{}』『{}』", Utility.indentEnd(), zookeeperConnectString, zookeeperZnodeElectPath);
	}

}
