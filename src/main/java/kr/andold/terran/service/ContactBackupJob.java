package kr.andold.terran.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import kr.andold.terran.ApplicationContextProvider;
import kr.andold.utils.Utility;
import kr.andold.utils.job.JobInterface;
import kr.andold.utils.job.STATUS;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
@Service
public class ContactBackupJob implements JobInterface {
	@Builder.Default @Getter @Setter private Long timeout = 60L;

	@Autowired private ContactService service;

	@Override
	public STATUS call() throws Exception {
		log.info("{} ContactBackupJob::call()", Utility.indentStart());
		long started = System.currentTimeMillis();

		ContactBackupJob that = (ContactBackupJob) ApplicationContextProvider.getBean(ContactBackupJob.class);
		STATUS result = that.main();

		log.info("{} {} ContactBackupJob::call() - {}", Utility.indentEnd(), result, Utility.toStringPastTimeReadable(started));
		return result;
	}

	protected STATUS main() {
		log.info("{} ContactBackupJob::main()", Utility.indentStart());
		long started = System.currentTimeMillis();

		String json = service.download();
		String userDataPath = TerranService.getApplicationDataPath();

		String jsonfilename = String.format("%s/contacts.json", userDataPath);
		Utility.write(jsonfilename, json);

		String vcard = service.downloadVCard(16);
		String vcardfilename = String.format("%s/contacts.vcf", userDataPath);
		Utility.write(vcardfilename, vcard);

		String vcard8 = service.downloadVCard(8);
		String vcard8filename = String.format("%s/contacts8.vcf", userDataPath);
		Utility.write(vcard8filename, vcard8);
		
		log.info("{} {} ContactBackupJob::main() - {}", Utility.indentEnd(), STATUS.SUCCESS, Utility.toStringPastTimeReadable(started));
		return STATUS.SUCCESS;
		
	}
	
}
