package kr.andold.terran.solar.mppt.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import kr.andold.terran.ApplicationContextProvider;
import kr.andold.terran.service.JobService;
import kr.andold.terran.service.ZookeeperClient;
import kr.andold.terran.solar.mppt.domain.SolarMpptDomain;
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
public class SolarMpptCrawlDetailsJob implements JobInterface {
	@Builder.Default @Getter @Setter private Long timeout = 60L * 3;

	private static final String USERNAME = "01068106479";
	private static final Duration DEFAULT_TIMEOUT_DURATION = Duration.ofSeconds(4);
	private static final Duration DEFAULT_TIMEOUT_DURATION_LONG = Duration.ofMinutes(1);
	private static final By BY_XPATH_FRAME_1ST = By.xpath("//div[contains(@class,'content-wrapper tab-content tab-addtabs')]/div[contains(@class,'active')]/iframe");
	private static final By BY_XPATH_FRAME_2ND = By.xpath("//div[contains(@class,'layui-layer-content')]/iframe");

	@Autowired private SolarMpptService service;

	private static ChromeDriverWrapper driver = null;

	@Builder
	public static class RestartDriverJob implements JobInterface {
		@Builder.Default @Getter private Long timeout = 60L;

		@Override
		public STATUS call() throws Exception {
			log.info("{} RestartDriverJob::call()", Utility.indentStart());
			long started = System.currentTimeMillis();

			SolarMpptCrawlDetailsJob that = (SolarMpptCrawlDetailsJob) ApplicationContextProvider.getBean(SolarMpptCrawlDetailsJob.class);
			that.clean();

			log.info("{} 『{}』 RestartDriverJob::call() - {}", Utility.indentEnd(), STATUS.SUCCESS, Utility.toStringPastTimeReadable(started));
			return STATUS.SUCCESS;
		}

		public static void regist(ConcurrentLinkedDeque<JobInterface> deque) {
			log.info("{} RestartDriverJob::regist(『#{}』)", Utility.indentStart(), Utility.size(deque));

			if (containsOrModify(JobService.getQueue0())) {
				log.info("{} ALREADY-IN-0 RestartDriverJob::regist(『#{}』)", Utility.indentEnd(), Utility.size(deque));
				return;
			}
			if (containsOrModify(JobService.getQueue1())) {
				log.info("{} ALREADY-IN-1 RestartDriverJob::regist(『#{}』)", Utility.indentEnd(), Utility.size(deque));
				return;
			}
			if (containsOrModify(JobService.getQueue2())) {
				log.info("{} ALREADY-IN-2 RestartDriverJob::regist(『#{}』)", Utility.indentEnd(), Utility.size(deque));
				return;
			}
			if (containsOrModify(JobService.getQueue3())) {
				log.info("{} ALREADY-IN-3 RestartDriverJob::regist(『#{}』)", Utility.indentEnd(), Utility.size(deque));
				return;
			}

			deque.addLast(RestartDriverJob.builder().build());
			log.info("{} REGISTERED RestartDriverJob::regist(『#{}』)", Utility.indentEnd(), Utility.size(deque));
		}

		private static boolean containsOrModify(ConcurrentLinkedDeque<JobInterface> deque) {
			for (JobInterface job : deque) {
				if (containsOrModify(job)) {
					return true;
				}
			}
			return false;
		}

		private static boolean containsOrModify(JobInterface job) {
			if (!(job instanceof RestartDriverJob)) {
				return false;
			}

			return true;
		}

	}

	@Override
	public STATUS call() throws Exception {
		log.info("{} SolarMpptCrawlDetailsJob::call()", Utility.indentStart());
		long started = System.currentTimeMillis();

		SolarMpptCrawlDetailsJob that = (SolarMpptCrawlDetailsJob) ApplicationContextProvider.getBean(SolarMpptCrawlDetailsJob.class);
		SolarMpptDomain result = that.main();

		log.info("{} 『#{}』 SolarMpptCrawlDetailsJob::call() - {}", Utility.indentEnd(), result, Utility.toStringPastTimeReadable(started));
		return STATUS.SUCCESS;
	}

	public static void regist(ConcurrentLinkedDeque<JobInterface> deque) {
		if (containsOrModify(JobService.getQueue0())) {
			return;
		}
		if (containsOrModify(JobService.getQueue1())) {
			return;
		}
		if (containsOrModify(JobService.getQueue2())) {
			return;
		}
		if (containsOrModify(JobService.getQueue3())) {
			return;
		}

		deque.addLast(SolarMpptCrawlDetailsJob.builder().build());
	}

	private static boolean containsOrModify(ConcurrentLinkedDeque<JobInterface> deque) {
		for (JobInterface job : deque) {
			if (containsOrModify(job)) {
				return true;
			}
		}
		return false;
	}

	private static boolean containsOrModify(JobInterface job) {
		if (!(job instanceof SolarMpptCrawlDetailsJob)) {
			return false;
		}

		return true;
	}

	protected SolarMpptDomain main() throws Exception {
		log.info("{} SolarMpptCrawlDetailsJob::main()", Utility.indentStart());
		long started = System.currentTimeMillis();

		try {
			driver();
			driver.manage().timeouts().implicitlyWait(DEFAULT_TIMEOUT_DURATION_LONG);

			if (notDetails()) {
				navigateDetails();
			}

			driver.manage().timeouts().implicitlyWait(DEFAULT_TIMEOUT_DURATION);

			driver.switchTo().defaultContent();
			driver.switchTo().frame(driver.findElement(BY_XPATH_FRAME_1ST));
			driver.switchTo().frame(driver.findElement(BY_XPATH_FRAME_2ND));

			for (int cx = 0; cx < 2; cx++) {
				//	Temperature	<div class="list-type p-1"><div class="w-100 title text-center one-line">temperature</div> <div class="w-100 text-center desc text-bold one-line">20.5℃</div></div>
				By BY_XPATH_TEMPERATURE = By.xpath("//*[@id='main']//th[contains(@class,'el-descriptions-item__label') and contains(text(),'Temperature')]/following-sibling::*[1]");
				log.debug("{} SolarMpptCrawlDetailsJob::main() - 『{}』『{}』", Utility.indentMiddle(), "Temperature", driver.getText(BY_XPATH_TEMPERATURE, Duration.ZERO));
				driver.waitUntilTextMatch(BY_XPATH_TEMPERATURE, "[^\\-]+");
				driver.waitUntilTextMatch(BY_XPATH_TEMPERATURE, "[0-9\\.]+.+");
				String temperatureString = driver.getText(BY_XPATH_TEMPERATURE).replaceAll("[^0-9\\.]", "");
				Float temperature = Utility.parseFloat(temperatureString, null);
				log.debug("{} SolarMpptCrawlDetailsJob::main() - 『{}』『{}』", Utility.indentMiddle(), "Temperature", driver.getText(BY_XPATH_TEMPERATURE, Duration.ZERO));

				//	Discharge Current
				By BY_XPATH_DISCHARGE = By.xpath("//*[@id='main']//th[contains(@class,'el-descriptions-item__label') and contains(text(),'Discharge Current')]/following-sibling::*[1]");
				log.debug("{} SolarMpptCrawlDetailsJob::main() - 『{}』『{}』", Utility.indentMiddle(), "Discharge Current", driver.getText(BY_XPATH_DISCHARGE, Duration.ZERO));
				driver.waitUntilTextMatch(BY_XPATH_DISCHARGE, "[^\\-]+");
				String dischargeString = driver.getText(BY_XPATH_DISCHARGE).replaceAll("[^0-9\\.]", "");
				Float discharge = Utility.parseFloat(dischargeString, null);
				log.debug("{} SolarMpptCrawlDetailsJob::main() - 『{}』『{}』", Utility.indentMiddle(), "Discharge Current", driver.getText(BY_XPATH_DISCHARGE, Duration.ZERO));

				//	Charging current
				By BY_XPATH_CHARGE = By.xpath("//*[@id='main']//th[contains(@class,'el-descriptions-item__label') and contains(text(),'Charging current')]/following-sibling::*[1]");
				log.debug("{} SolarMpptCrawlDetailsJob::main() - 『{}』『{}』", Utility.indentMiddle(), "Charging Current", driver.getText(BY_XPATH_CHARGE, Duration.ZERO));
				driver.waitUntilTextMatch(BY_XPATH_CHARGE, "[^\\-]+");
				String chargeString = driver.getText(BY_XPATH_CHARGE).replaceAll("[^0-9\\.]", "");
				Float charge = Utility.parseFloat(chargeString, null);
				log.debug("{} SolarMpptCrawlDetailsJob::main() - 『{}』『{}』", Utility.indentMiddle(), "Charging Current", driver.getText(BY_XPATH_CHARGE, Duration.ZERO));

				//	Battery voltage
				By BY_XPATH_VOLTAGE = By.xpath("//*[@id='main']//th[contains(@class,'el-descriptions-item__label') and contains(text(),'Battery voltage')]/following-sibling::*[1]");
				log.debug("{} SolarMpptCrawlDetailsJob::main() - 『{}』『{}』", Utility.indentMiddle(), "Battery voltage", driver.getText(BY_XPATH_VOLTAGE, Duration.ZERO));
				driver.waitUntilTextMatch(BY_XPATH_VOLTAGE, "[^\\-]+");
				String voltageString = driver.getText(BY_XPATH_VOLTAGE).replaceAll("[^0-9\\.]", "");
				Float voltage = Utility.parseFloat(voltageString, null);
				log.debug("{} SolarMpptCrawlDetailsJob::main() - 『{}』『{}』", Utility.indentMiddle(), "Battery voltage", driver.getText(BY_XPATH_VOLTAGE, Duration.ZERO));

				if (Utility.isNullAll(temperature, discharge, charge, voltage)) {
					continue;
				}

				LocalTime lt = LocalTime.now();
				LocalDateTime ldt = LocalDateTime.of(LocalDate.now(), LocalTime.of(lt.getHour(), lt.getMinute() / 5 * 5));
				Date date = Date.from(ldt.toInstant(Utility.ZONE_OFFSET_KST));

				List<SolarMpptDomain> domains = new ArrayList<>();
				SolarMpptDomain domain = SolarMpptDomain.builder()
						.base(date)
						.temperature(temperature)
						.discharge(discharge)
						.charge(charge)
						.voltage(voltage)
						.build();
				domains.add(domain);
				service.create(domains);

				JobService.getQueue2().add(SolarMpptTsdbCreateJob.builder().domain(domain).build());
				
				log.info("{} 『{}』 SolarMpptCrawlDetailsJob::main() - {}", Utility.indentEnd(), domain, Utility.toStringPastTimeReadable(started));
				return domain;
			}
		} catch (Exception e) {
			log.error("Exception:: {}", e.getLocalizedMessage(), e);
		}

		log.info("{} 『{}』 SolarMpptCrawlDetailsJob::main() - {}", Utility.indentEnd(), null, Utility.toStringPastTimeReadable(started));
		return null;
	}

	private ChromeDriverWrapper driver() {
		log.info("{} driver() - 『{}』", Utility.indentStart(), driver);
		long started = System.currentTimeMillis();

		if (driver == null) {
			driver = SolarMpptService.createDriver(!ZookeeperClient.isTestEnvironment(), "details");
		}

		log.info("{} driver() - 『{}』 - {}", Utility.indentEnd(), driver, Utility.toStringPastTimeReadable(started));
		return driver;
	}

	public void clean() {
		log.info("{} clean() - 『{}』", Utility.indentStart(), driver);
		long started = System.currentTimeMillis();

		if (driver != null) {
			driver.quit();
			driver = null;
		}

		driver = SolarMpptService.createDriver(!ZookeeperClient.isTestEnvironment(), "details");
		driver.get(SolarMpptService.URL);
//		driver.navigate().refresh();

		log.info("{} clean() - 『{}』 - {}", Utility.indentEnd(), driver, Utility.toStringPastTimeReadable(started));
	}

	private boolean navigateDetails() {
		log.info("{} navigateDetails()", Utility.indentStart());
		long started = System.currentTimeMillis();

		try {
			ChromeDriverWrapper driver = driver();
			driver.manage().timeouts().implicitlyWait(DEFAULT_TIMEOUT_DURATION);
			driver.switchTo().defaultContent();
			
			//*[@id="firstnav"]/div/ul/li[5]/a/span
			By BY_XPATH_USERNAME_X = By.xpath("//*[@id='firstnav']/div/ul/li[5]/a/span[contains(text(),'" + USERNAME + "')]");
			log.info("{} navigateDetails() - 『{}』『{}』", Utility.indentMiddle(), "USERNAME", driver.getText(BY_XPATH_USERNAME_X, Duration.ZERO));
			if (!driver.isDisplayed(BY_XPATH_USERNAME_X, Duration.ZERO)) {
				service.login(driver);
			}
			log.info("{} navigateDetails() - 『{}』『{}』", Utility.indentMiddle(), "USERNAME", driver.getText(BY_XPATH_USERNAME_X, Duration.ZERO));

			driver.manage().timeouts().implicitlyWait(DEFAULT_TIMEOUT_DURATION);

			modeEnglish(driver);
			closeDeviceDetails(driver);	//	close before reopen
			wipeBrowserCache(driver);

			log.info("{} navigateDetails() - 『{}』『{}』", Utility.indentMiddle(), "iframe 1st", driver.getText(BY_XPATH_FRAME_1ST, Duration.ZERO));
			driver.switchTo().defaultContent();
			driver.switchTo().frame(driver.findElement(BY_XPATH_FRAME_1ST, DEFAULT_TIMEOUT_DURATION));
			log.info("{} navigateDetails() - 『{}』『{}』", Utility.indentMiddle(), "iframe 1st", driver.getText(BY_XPATH_FRAME_1ST, Duration.ZERO));

			By BY_XPATH_BUTTON_DETAILS = By.xpath("//*[@id='productIndex']//span[contains(text(),'Details')]/..");
			log.info("{} navigateDetails() - 『{}』『{}』", Utility.indentMiddle(), "Details", driver.getText(BY_XPATH_BUTTON_DETAILS, Duration.ZERO));
			driver.presenceOfElementLocated(BY_XPATH_BUTTON_DETAILS, DEFAULT_TIMEOUT_DURATION);
			driver.clickIfExist(BY_XPATH_BUTTON_DETAILS);
			log.info("{} navigateDetails() - 『{}』『{}』", Utility.indentMiddle(), "Details", driver.getText(BY_XPATH_BUTTON_DETAILS, Duration.ZERO));

			//*[@id="con_466197140234"]/iframe	/VuGvSgOfDB.php/usermac/machine/index?lang=en&addtabs=1
			log.info("{} navigateDetails() - 『{}』『{}』", Utility.indentMiddle(), "iframe 2nd", driver.getText(BY_XPATH_FRAME_2ND, Duration.ZERO));
			driver.switchTo().defaultContent();
			log.info("{} navigateDetails() - 『{}』『{}』", Utility.indentMiddle(), "iframe 1st", driver.getText(BY_XPATH_FRAME_1ST, Duration.ZERO));
			driver.presenceOfElementLocated(BY_XPATH_FRAME_1ST, DEFAULT_TIMEOUT_DURATION);
			log.info("{} navigateDetails() - 『{}』『{}』", Utility.indentMiddle(), "iframe 1st", driver.getText(BY_XPATH_FRAME_1ST, Duration.ZERO));
			driver.switchTo().frame(driver.findElement(BY_XPATH_FRAME_1ST, DEFAULT_TIMEOUT_DURATION));
			log.info("{} navigateDetails() - 『{}』『{}』", Utility.indentMiddle(), "iframe 2nd", driver.getText(BY_XPATH_FRAME_2ND, Duration.ZERO));
			driver.presenceOfElementLocated(BY_XPATH_FRAME_2ND, DEFAULT_TIMEOUT_DURATION_LONG);
			log.info("{} navigateDetails() - 『{}』『{}』", Utility.indentMiddle(), "iframe 2nd", driver.getText(BY_XPATH_FRAME_2ND, Duration.ZERO));
			driver.switchTo().frame(driver.findElement(BY_XPATH_FRAME_2ND, DEFAULT_TIMEOUT_DURATION_LONG));
			log.info("{} navigateDetails() - 『{}』『{}』", Utility.indentMiddle(), "iframe 2nd", driver.getText(BY_XPATH_FRAME_2ND, Duration.ZERO));

			log.info("{} 『{}』 navigateLogs() - {}", Utility.indentEnd(), true, Utility.toStringPastTimeReadable(started));
			return true;
		} catch (Exception e) {
			log.error("Exception:: {}", e.getLocalizedMessage(), e);
		}

		log.info("{} 『{}』 navigateLogs() - {}", Utility.indentEnd(), false, Utility.toStringPastTimeReadable(started));
		return false;
	}

	//	English
	private void modeEnglish(ChromeDriverWrapper driver) {
		//	Home		//*[@id="firstnav"]/div/ul/li[1]/a/text()
		By BY_XPATH_HOME = By.xpath("//*[@id='firstnav']/div/ul/li[1]/a[contains(text(),'Home')]");
		log.info("{} modeEnglish(...) - 『{}』『{}』", Utility.indentMiddle(), "Home", driver.getText(BY_XPATH_HOME, Duration.ZERO));
		if (driver.isDisplayed(BY_XPATH_HOME, Duration.ZERO)) {
			return;
		}
		log.info("{} modeEnglish(...) - 『{}』『{}』", Utility.indentMiddle(), "Home", driver.getText(BY_XPATH_HOME, Duration.ZERO));
		
		//	fa fa-language	//*[@id="firstnav"]/div/ul/li[3]/a/i
		By BY_XPATH_LANGUAGE_ICON = By.xpath("//*[@id='firstnav']/div/ul/li[3]/a/i[contains(@class,'fa-language')]");
		log.info("{} navigateDetails() - 『{}』『{}』", Utility.indentMiddle(), "language", driver.getText(BY_XPATH_LANGUAGE_ICON, Duration.ZERO));
		driver.presenceOfElementLocated(BY_XPATH_LANGUAGE_ICON, DEFAULT_TIMEOUT_DURATION);
		driver.clickIfExist(BY_XPATH_LANGUAGE_ICON);
		log.info("{} navigateDetails() - 『{}』『{}』", Utility.indentMiddle(), "language", driver.getText(BY_XPATH_LANGUAGE_ICON, Duration.ZERO));

		//	English	//*[@id="firstnav"]/div/ul/li[3]/ul/li[2]/a
		By BY_XPATH_LANGUAGE_ENGLISH = By.xpath("//*[@id='firstnav']/div/ul/li[3]/ul/li[2]/a[contains(text(),'English')]");
		log.info("{} navigateDetails() - 『{}』『{}』", Utility.indentMiddle(), "English", driver.getText(BY_XPATH_LANGUAGE_ENGLISH, Duration.ZERO));
		driver.presenceOfElementLocated(BY_XPATH_LANGUAGE_ENGLISH, DEFAULT_TIMEOUT_DURATION);
		driver.clickIfExist(BY_XPATH_LANGUAGE_ENGLISH);
		log.info("{} navigateDetails() - 『{}』『{}』", Utility.indentMiddle(), "English", driver.getText(BY_XPATH_LANGUAGE_ENGLISH, Duration.ZERO));
		
	}

	private void closeDeviceDetails(ChromeDriverWrapper driver) {
		log.info("{} closeDeviceDetails(...)", Utility.indentStart());
		try {
			//*[@id="con_466197140234"]/iframe	/VuGvSgOfDB.php/usermac/machine/index?lang=en&addtabs=1
			log.info("{} closeDeviceDetails() - 『{}』『{}』", Utility.indentMiddle(), "iframe 1st", driver.getText(BY_XPATH_FRAME_1ST, Duration.ZERO));
			driver.switchTo().defaultContent();
			driver.switchTo().frame(driver.findElement(BY_XPATH_FRAME_1ST, DEFAULT_TIMEOUT_DURATION));
			log.info("{} closeDeviceDetails() - 『{}』『{}』", Utility.indentMiddle(), "iframe 1st", driver.getText(BY_XPATH_FRAME_1ST, Duration.ZERO));

			//	Device details	//	close before reopen	//*[@id="layui-layer2"]
			By BY_XPATH_CONTAINER_2ND_FRAME = By.xpath("//*[contains(@class,'layui-layer-iframe')]");
			log.debug("{} closeDeviceDetails() - 『{}』『{}』", Utility.indentMiddle(), "iframe 2nd", driver.getText(BY_XPATH_CONTAINER_2ND_FRAME, Duration.ZERO));
			if (driver.isDisplayed(BY_XPATH_CONTAINER_2ND_FRAME)) {
				//	close icon	//*[@id="layui-layer5"]/span[1]/a[3]
				By BY_XPATH_CLOSE_ICON = By.xpath("//*[contains(@class,'layui-layer-iframe')]/span[contains(@class,'layui-layer-setwin')]/a[contains(@class,'layui-layer-close')]");
				log.debug("{} closeDeviceDetails() - 『{}』『{}』", Utility.indentMiddle(), "CLOSE", driver.getText(BY_XPATH_CLOSE_ICON, Duration.ZERO));
				driver.clickIfExist(BY_XPATH_CLOSE_ICON);
				log.debug("{} closeDeviceDetails() - 『{}』『{}』", Utility.indentMiddle(), "CLOSE", driver.getText(BY_XPATH_CLOSE_ICON, Duration.ZERO));
			}
		} catch (Exception e) {
			log.error("Exception:: {}", e.getLocalizedMessage(), e);
		}
		log.info("{} closeDeviceDetails(...)", Utility.indentEnd());
	}

	private void wipeBrowserCache(ChromeDriverWrapper driver) {
		log.info("{} wipeBrowserCache(...)", Utility.indentStart());
		try {
			driver.switchTo().defaultContent();

			//	 Wipe cache	//*[@id="firstnav"]/div/ul/li[2]
			By BY_XPATH_WIPE_CACHE = By.xpath("//*[@id='firstnav']/div/ul/li[2]");
			log.info("{} navigateDetails() - 『{}』『{}』", Utility.indentMiddle(), "Wipe cache", driver.getText(BY_XPATH_WIPE_CACHE, Duration.ZERO));
			//	 Wipe browser cache	//*[@id="firstnav"]/div/ul/li[2]/ul/li[6]/a/text()
			By BY_XPATH_WIPE_BROWSER_CACHE = By.xpath("//*[@id='firstnav']/div/ul/li[2]/ul/li[6]/a[contains(text(),'Wipe browser cache')]");
			log.info("{} navigateDetails() - 『{}』『{}』", Utility.indentMiddle(), "Wipe browser cache", driver.getText(BY_XPATH_WIPE_BROWSER_CACHE, Duration.ZERO));
			
			//	show Wipe browser cache link
			for (int cx = 0, sizex = 3; cx < sizex; cx++) {
				log.debug("{} wipeBrowserCache(...) - 『#{}/{}』『{}』", Utility.indentMiddle(), cx, sizex, "show Wipe browser cache link");
				if (driver.isDisplayed(BY_XPATH_WIPE_BROWSER_CACHE, Duration.ZERO)) {
					break;
				}
	
				driver.clickIfExist(BY_XPATH_WIPE_CACHE);
				log.info("{} navigateDetails() - 『{}』『{}』", Utility.indentMiddle(), "Wipe cache", driver.getTextEscape(BY_XPATH_WIPE_CACHE, Duration.ZERO));
				log.info("{} navigateDetails() - 『{}』『{}』", Utility.indentMiddle(), "Wipe browser cache", driver.getText(BY_XPATH_WIPE_BROWSER_CACHE, Duration.ZERO));
			}
			
			driver.clickIfExist(BY_XPATH_WIPE_BROWSER_CACHE);
		} catch (Exception e) {
			log.error("Exception:: {}", e.getLocalizedMessage(), e);
		}
		log.info("{} wipeBrowserCache(...)", Utility.indentEnd());
	}

	private boolean notDetails() {
		log.info("{} notDetails()", Utility.indentStart());
		long started = System.currentTimeMillis();

		try {
			ChromeDriverWrapper driver = driver();
			driver.manage().timeouts().implicitlyWait(Duration.ZERO);

			driver.switchTo().defaultContent();

			//	//*[@id="con_1341"]/iframe
			By BY_XPATH_FRAME = By.xpath("//*[@id='con_1341']/iframe");
			log.info("{} notDetails() - 『{}』『{}』", Utility.indentMiddle(), "iframe", driver.getText(BY_XPATH_FRAME, Duration.ZERO));
			if (!driver.isDisplayed(BY_XPATH_FRAME)) {
				log.info("{} 『{}』 notDetails() - {}", Utility.indentEnd(), true, Utility.toStringPastTimeReadable(started));
				return true;
			}
			WebElement frame = driver.findElement(BY_XPATH_FRAME);
			log.info("{} notDetails() - 『{}』『{}』", Utility.indentMiddle(), "iframe", driver.getText(BY_XPATH_FRAME, Duration.ZERO));
			driver.switchTo().frame(frame);

			//	//*[@id="layui-layer-iframe2"]
			By BY_XPATH_FRAME_INNER = By.xpath("//*[@id='layui-layer-iframe2']");
			log.info("{} notDetails() - 『{}』『{}』", Utility.indentMiddle(), "iframe inner", driver.getText(BY_XPATH_FRAME, Duration.ZERO));
			if (!driver.isDisplayed(BY_XPATH_FRAME_INNER)) {
				log.info("{} 『{}』 notDetails() - {}", Utility.indentEnd(), true, Utility.toStringPastTimeReadable(started));
				return true;
			}
			WebElement frameInner = driver.findElement(BY_XPATH_FRAME_INNER);
			log.info("{} notDetails() - 『{}』『{}』", Utility.indentMiddle(), "iframe inner", driver.getText(BY_XPATH_FRAME_INNER, Duration.ZERO));
			driver.switchTo().frame(frameInner);

			//	//*[@id="main"]
			By BY_XPATH_CONTENT = By.xpath("//*[@id='main']");
			log.info("{} notDetails() - 『{}』『{}』", Utility.indentMiddle(), "main", driver.getText(BY_XPATH_CONTENT, Duration.ZERO));
			if (!driver.isDisplayed(BY_XPATH_CONTENT)) {
				log.info("{} 『{}』 notDetails() - {}", Utility.indentEnd(), true, Utility.toStringPastTimeReadable(started));
				return true;
			}
			log.info("{} notDetails() - 『{}』『{}』", Utility.indentMiddle(), "main", driver.getText(BY_XPATH_CONTENT, Duration.ZERO));

			log.info("{} 『{}』 notDetails() - {}", Utility.indentEnd(), false, Utility.toStringPastTimeReadable(started));
			return false;
		} catch (Exception e) {
			log.error("Exception:: {}", e.getLocalizedMessage(), e);
		}

		log.info("{} 『{}』 notDetails() - {}", Utility.indentEnd(), true, Utility.toStringPastTimeReadable(started));
		return true;
	}

}
