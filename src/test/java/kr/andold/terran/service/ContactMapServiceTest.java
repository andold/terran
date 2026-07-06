package kr.andold.terran.service;

import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import kr.andold.terran.domain.ContactMapDomain;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ContactMapServiceTest {
	@Autowired private ContactMapService service;

	@Test
	public void testCreateListContactMapDomain() {
		ContactMapDomain domain = ContactMapDomain.builder()
				.key("key")
				.value(8)
				.created(Date.from(Instant.parse("2023-03-15T22:39:30.00Z")))
				.updated(Date.from(Instant.parse("2023-03-15T22:39:30.00Z")))
				.build();
		ContactMapDomain created = service.create(domain);
		log.info("created: 『{}』", created);
	}

	@Test
	public void testUpdateIntegerContactMapDomain() {
		ContactMapDomain domain = ContactMapDomain.builder().id(1448).value(8).build();
		ContactMapDomain updated = service.update(domain.getId(), domain);
		log.info("updated: 『{}』", updated);
	}

}
