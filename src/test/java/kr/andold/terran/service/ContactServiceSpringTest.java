package kr.andold.terran.service;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import kr.andold.terran.domain.ContactDomain;
import kr.andold.terran.param.ContactParam;
import kr.andold.utils.Utility;
import kr.andold.utils.persist.CrudList;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ContactServiceSpringTest {
	@Autowired private ContactService service;

	@Test
	public void uploadGoogleVcard() {
		String filename = "contacts.vcf";
		String text = Utility.readClassPathFile(filename);
		log.info("text: 『{}』", Utility.ellipsisEscape(text, 64));

		ContactParam result = service.uploadVcard(text);
		log.info("result: 『{}』", result);

		CrudList<ContactDomain> crud = CrudList.<ContactDomain>builder()
				.creates(result.getCreates())
				.updates(result.getUpdates())
				.removes(result.getRemoves())
				.build();
		int changed = service.put(crud);
		log.info("changed: 『{}』", changed);
	}

	@Test
	public void testDownloadVcard() {
		List<ContactDomain> domains = service.search(null);
		StringBuffer stringBuffer = new StringBuffer("");
		for (ContactDomain domain: domains) {
			String vcard = domain.toStringVcard(16);
			stringBuffer.append(vcard);
			log.info("result: 『{}』", vcard);
		}

		String result = service.downloadVCard(16);
		log.info("result: 『{}』", Utility.ellipsisEscape(result, 64, 64));
	}

	@Test
	public void testDownload() {
		String result = service.download();
		log.info("result: 『{}』", Utility.ellipsisEscape(result, 64, 64));
	}

	@Test
	public void testUpdate() {
		ContactDomain domain = ContactDomain.builder().id(1032).value(8).build();
		ContactDomain result = service.update(domain.getId(), domain);
		log.info("result: 『{}』", result);
	}

	@Test
	public void testUploadJson() {
		String filename = "list-contact-20240826.json";
		String text = Utility.readClassPathFile(filename);
		log.info("text: 『{}』", Utility.ellipsisEscape(text, 64));

		ContactParam result = service.uploadJson(text);
		log.info("result: 『{}』", result);

		CrudList<ContactDomain> crud = CrudList.<ContactDomain>builder()
				.creates(result.getCreates())
				.updates(result.getUpdates())
				.removes(result.getRemoves())
				.build();
		log.info("crud: 『{}』", crud);
//		int changed = service.put(crud);
//		log.info("changed: 『{}』", changed);
	}

	@Test
	public void uploadVcard() {
		String filename = "list-contact-20230313.vcf";
		String text = Utility.readClassPathFile(filename);
		log.info("text: 『{}』", Utility.ellipsisEscape(text, 64));

		ContactParam result = service.uploadVcard(text);
		log.info("result: 『{}』", result);

		CrudList<ContactDomain> crud = CrudList.<ContactDomain>builder()
				.creates(result.getCreates())
				.updates(result.getUpdates())
				.removes(result.getRemoves())
				.build();
		int changed = service.put(crud);
		log.info("changed: 『{}』", changed);
	}


}
