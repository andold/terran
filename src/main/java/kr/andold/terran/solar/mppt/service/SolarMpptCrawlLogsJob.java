package kr.andold.terran.solar.mppt.service;

import java.net.URLDecoder;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import kr.andold.terran.ApplicationContextProvider;
import kr.andold.terran.service.JobService;
import kr.andold.terran.solar.mppt.domain.SolarMpptDomain;
import kr.andold.terran.solar.mppt.param.SolarMpptParam;
import kr.andold.utils.job.JobInterface;
import kr.andold.utils.job.STATUS;
import kr.andold.utils.persist.CrudList;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
@Service
public class SolarMpptCrawlLogsJob implements JobInterface {
	private static final Duration DEFAULT_TIMEOUT_DURATION = Duration.ofSeconds(4);
	private static final Duration DEFAULT_TIMEOUT_DURATION_LONG = Duration.ofMinutes(1);
	private static final String URL = "http://app.gz529.com/index.php/api/Machine/getMacCollecLogList?mac_id=6194&last_log_id=";

	@Builder.Default @Getter @Setter private Long timeout = 60L;
	@Getter @Setter private ZonedDateTime start;
	private static ChromeDriverWrapper driver = null;

	@Autowired private SolarMpptService service;

	@Override
	public STATUS call() throws Exception {
		log.info("{} SolarMpptCrawlLogsJob::call()", Utility.indentStart());
		long started = System.currentTimeMillis();

		SolarMpptCrawlLogsJob that = (SolarMpptCrawlLogsJob) ApplicationContextProvider.getBean(SolarMpptCrawlLogsJob.class);
//		SolarMpptDomain result = that.main(start);
		SolarMpptDomain result = that.main2(start);

		log.info("{} 『#{}』 SolarMpptCrawlLogsJob::call() - {}", Utility.indentEnd(), result, Utility.toStringPastTimeReadable(started));
		return STATUS.SUCCESS;
	}

	@NoArgsConstructor
	@Data
	private static class ChildListClazz {
		private String name;
		private String value_text;
		
	}
	@NoArgsConstructor
	@Data
	private static class ListClazz {
		private List<ChildListClazz> childList;
		private String createtime_text;
		
	}
	@NoArgsConstructor
	@Data
	private static class DataClazz {
		private Integer end_id;
		private List<ListClazz> list;
		
	}
	@NoArgsConstructor
	@Data
	private static class ResponseLogs {
		private Integer code;
		private DataClazz data;
		private String msg;
		private String time;
		
		public static ResponseLogs of(String text) {
			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.setSerializationInclusion(Include.NON_NULL);
			objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
			objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			try {
				return objectMapper.readValue(text, ResponseLogs.class);
			} catch (Exception e) {
				try {
					return objectMapper.readValue(URLDecoder.decode(text, "UTF-8"), ResponseLogs.class);
				} catch (Exception f) {
					e.printStackTrace();
					f.printStackTrace();
				}
			}

			return null;
		}
	}

	protected SolarMpptParam main2(ZonedDateTime start) {
		log.info("{} SolarMpptCrawlLogsJob::main2({})", Utility.indentStart(), start);
		long started = System.currentTimeMillis();

		try {
			driver();
			driver.manage().timeouts().implicitlyWait(DEFAULT_TIMEOUT_DURATION);
			driver.switchTo().defaultContent();
			
			//*[@id="firstnav"]/div/ul/li[5]/a/span
			By BY_XPATH_USERNAME_X = By.xpath("//*[@id='firstnav']/div/ul/li[5]/a/span[contains(text(),'" + SolarMpptService.USERNAME + "')]");
			log.info("{} SolarMpptCrawlLogsJob::main2() - 『{}』『{}』", Utility.indentMiddle(), "USERNAME", driver.getText(BY_XPATH_USERNAME_X, Duration.ZERO));
			if (!driver.isDisplayed(BY_XPATH_USERNAME_X, Duration.ZERO)) {
				service.login(driver);
			}
			log.info("{} SolarMpptCrawlLogsJob::main2() - 『{}』『{}』", Utility.indentMiddle(), "USERNAME", driver.getText(BY_XPATH_USERNAME_X, Duration.ZERO));

			driver.manage().timeouts().implicitlyWait(DEFAULT_TIMEOUT_DURATION);

			By BY_XPATH_HTML = By.xpath("/html");
			log.debug("{} SolarMpptCrawlLogsJob::main2({}) - 『{}』『{}』", Utility.indentMiddle(), start, "HTML", Utility.ellipsisEscape(driver.getText(BY_XPATH_HTML, Duration.ZERO), 32, 32));
			ZonedDateTime previous = ZonedDateTime.now();
			ZonedDateTime current = ZonedDateTime.now();
			for (int cx = 0, logId = 0; cx < 1024 && current.isAfter(start); cx++) {
				String url = String.format("%s%d", URL, logId);
				log.debug("{} SolarMpptCrawlLogsJob::main2({}) - 『{}』『{}』", Utility.indentMiddle(), start, "HTML", Utility.ellipsisEscape(driver.getText(BY_XPATH_HTML, Duration.ZERO), 32, 32));
				String mark = Double.toString(Math.random());
				driver.setText(BY_XPATH_USERNAME_X, mark, 1000 * 8);
				driver.get(url);
				driver.waitUntilTextNotInclude(BY_XPATH_HTML, 1000 * 8, mark);
				log.debug("{} SolarMpptCrawlLogsJob::main2({}) - 『{}』『{}』", Utility.indentMiddle(), start, "HTML", Utility.ellipsisEscape(driver.getText(BY_XPATH_HTML, Duration.ZERO), 32, 32));

//				String text = driver.getPageSource();
				String text = driver.getText(BY_XPATH_HTML);
				ResponseLogs rl = ResponseLogs.of(text);
				logId = rl.getData().getEnd_id();
				List<ListClazz> logs = rl.getData().getList();
				ListClazz lastLog = logs.get(logs.size() - 1);
				current = ZonedDateTime.parse(lastLog.getCreatetime_text(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.of("Asia/Shanghai")));

				log.debug("{} SolarMpptCrawlLogsJob::main2({}) - 『{}』『{}』", Utility.indentMiddle(), start, Utility.ellipsisEscape(text, 32, 32), Utility.toStringJson(rl, 32, 32));
				log.debug("{} SolarMpptCrawlLogsJob::main2({}) - 『previous:{}』『current:{}』『logId:{}』", Utility.indentMiddle(), start, previous, current, logId);
				if (previous.isEqual(current)) {
					//	infinite loop
					break;
				}
				previous = current;
			}

			log.info("{} 『{}』 main2({}) - {}", Utility.indentEnd(), null, start, Utility.toStringPastTimeReadable(started));
			return null;
		} catch (Exception e) {
			log.error("Exception:: {}", e.getLocalizedMessage(), e);
		}

		log.info("{} 『{}』 SolarMpptCrawlLogsJob::main2({}) - {}", Utility.indentEnd(), null, start, Utility.toStringPastTimeReadable(started));
		return null;
	}

	protected SolarMpptDomain main(ZonedDateTime minDate) throws Exception {
		log.info("{} SolarMpptCrawlLogsJob::main()", Utility.indentStart());
		long started = System.currentTimeMillis();

		try {
			driver();
			driver.manage().timeouts().implicitlyWait(DEFAULT_TIMEOUT_DURATION_LONG);

			SolarMpptParam result = SolarMpptParam.builder()
					.crud(CrudList.<SolarMpptDomain>builder().build())
					.build();
			CrudList<SolarMpptDomain> container = result.getCrud();

			if (notLogs()) {
				navigateLogs();
			}

			driver.manage().timeouts().implicitlyWait(DEFAULT_TIMEOUT_DURATION);

			By BY_XPATH_FRAME_X = By.xpath("//div[contains(@class,'tab-content')]/div[contains(@class,'active')]/iframe");
			By BY_XPATH_FRAME_Y = By.xpath("//div[contains(@class,'layui-layer-content')]/iframe");
			driver.switchTo().defaultContent();
			driver.switchTo().frame(driver.findElement(BY_XPATH_FRAME_X));
			driver.switchTo().frame(driver.findElement(BY_XPATH_FRAME_Y));

			int cx = 0;
			boolean keepGoing = true;
			ZonedDateTime previousDatetimeLast = ZonedDateTime.now();
			for (ZonedDateTime datetime = ZonedDateTime.now(); keepGoing && datetime.isAfter(minDate);) {
				driver.manage().timeouts().implicitlyWait(DEFAULT_TIMEOUT_DURATION_LONG);

				//	//*[@id="productIndex"]/div/div[1]
				By BY_XPATH_CONTENTS = By.xpath("//*[@id='productIndex']/div/div[contains(@class,'list-content')]/div");
				log.info("{} logs() - 『{}』『{}』", Utility.indentMiddle(), "content", Utility.ellipsisEscape(driver.getText(BY_XPATH_CONTENTS, Duration.ZERO), 128));
				List<WebElement> contents = driver.findElements(BY_XPATH_CONTENTS);
				int sizex = contents.size();
				List<SolarMpptDomain> domains = new ArrayList<>();
				for (; cx < sizex; cx++) {
					contents = driver.findElements(BY_XPATH_CONTENTS, DEFAULT_TIMEOUT_DURATION_LONG);
					WebElement content = contents.get(cx);
					log.info("{} logs() - 『{}/{}』『{}』『{}』", Utility.indentMiddle(), cx, sizex, "content", Utility.ellipsisEscape(content.getText(), 64, 64));

					driver.manage().timeouts().implicitlyWait(DEFAULT_TIMEOUT_DURATION);

					//	scroll to
					((JavascriptExecutor)driver).executeScript("arguments[0].scrollIntoView(true);", content);

					//	<div class="text-bold log-time  ">2024-11-25 12:48</div>
					By BY_XPATH_DATE_TIME = By.xpath("div[contains(@class,'log-time')]");
					log.info("{} logs() - 『{}/{}』『{}』『{}』", Utility.indentMiddle(), cx, sizex, "datetime", driver.getText(content, BY_XPATH_DATE_TIME, Duration.ZERO));
					if (!driver.isDisplayed(content, BY_XPATH_DATE_TIME)) {
						log.info("{} 『{}』 logs() - {}", Utility.indentMiddle(), "시각이 없습니다", Utility.toStringPastTimeReadable(started));
						continue;
					}
					datetime = ZonedDateTime.parse(driver.getText(content, BY_XPATH_DATE_TIME), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.of("Asia/Shanghai")));
					
					//	<div class="list-type p-1"><div class="w-100 title text-center one-line">temperature</div> <div class="w-100 text-center desc text-bold one-line">20.5℃</div></div>
					By BY_XPATH_TEMPERATURE = By.xpath("div[contains(@class,'list-dom')]/div/div[contains(@class,'title') and contains(text(),'temperature')]/following-sibling::*[1]");
					Float temperature = Utility.parseFloat(driver.getText(content, BY_XPATH_TEMPERATURE, Duration.ZERO).replaceAll("[^0-9\\.]", ""), null);

					//	discharge
					By BY_XPATH_DISCHARGE = By.xpath("div[contains(@class,'list-dom')]/div/div[contains(@class,'title') and contains(text(),'Discharge Current')]/following-sibling::*[1]");
					Float discharge = Utility.parseFloat(driver.getText(content, BY_XPATH_DISCHARGE, Duration.ZERO).replaceAll("[^0-9\\.]", ""), null);

					//	charge
					By BY_XPATH_CHARGE = By.xpath("div[contains(@class,'list-dom')]/div/div[contains(@class,'title') and contains(text(),'Charging current')]/following-sibling::*[1]");
					Float charge = Utility.parseFloat(driver.getText(content, BY_XPATH_CHARGE, Duration.ZERO).replaceAll("[^0-9\\.]", ""), null);

					//	voltage
					By BY_XPATH_VOLTAGE = By.xpath("div[contains(@class,'list-dom')]/div/div[contains(@class,'title') and contains(text(),'Current battery voltage')]/following-sibling::*[1]");
					Float voltage = Utility.parseFloat(driver.getText(content, BY_XPATH_VOLTAGE, Duration.ZERO).replaceAll("[^0-9\\.]", ""), null);
					
					if (Utility.isNullAll(temperature, discharge, charge, voltage)) {
						log.info("{} 『{}』 logs() - {}", Utility.indentMiddle(), "측정 데이터가 아닙니다", Utility.toStringPastTimeReadable(started));
						continue;
					}
					
					SolarMpptDomain domain = SolarMpptDomain.builder()
							.base(Date.from(datetime.toInstant()))
							.temperature(temperature)
							.discharge(discharge)
							.charge(charge)
							.voltage(voltage)
							.build();
					domains.add(domain);
					JobService.getQueue3().add(SolarMpptTsdbCreateJob.builder().domain(domain).build());
					log.info("{} logs() - 『{}/{}』『{}』『{}』『{}』『{}』『{}』『{} < {}』『#{} {}』", Utility.indentMiddle()
							, cx, sizex, datetime, temperature, discharge, charge, voltage, datetime, minDate, Utility.size(domains), container);
				}

				log.info("{} logs() - 『{} < {}』『{} {}』", Utility.indentMiddle(), datetime, minDate, Utility.size(domains), container);
				CrudList<SolarMpptDomain> crud = service.put(domains);
				crud.setRemoves(new ArrayList<>());
				container.add(crud);
				log.info("{} logs() - 『{} < {}』『{} {}』『{}』", Utility.indentMiddle(), datetime, minDate, Utility.size(domains), container, crud);

				log.info("{} MISSION COMPLETE logs() - 『{} vs {}』", Utility.indentMiddle(), datetime, minDate);
				if (datetime.isBefore(minDate)) {
					break;
				}

				log.info("{} INFINITE LOOP logs() - 『{} vs {}』", Utility.indentMiddle(), datetime, previousDatetimeLast);
				if (datetime.isEqual(previousDatetimeLast)) {
					break;
				}
				
				previousDatetimeLast = datetime;

				driver.switchTo().defaultContent();
				driver.switchTo().frame(driver.findElement(BY_XPATH_FRAME_X));
				driver.switchTo().frame(driver.findElement(BY_XPATH_FRAME_Y));

				// more button //*[@id="productIndex"]/div/div[2]
				By BY_XPATH_MORE = By.xpath("//*[@id='productIndex']//div[contains(@class,'more-btn')]");
				log.info("{} logs() - 『{}/{}』『{}』『{}』", Utility.indentMiddle(), datetime, minDate, "Loading more", driver.getText(BY_XPATH_MORE, Duration.ZERO));
				//	scroll to
				((JavascriptExecutor)driver).executeScript("arguments[0].scrollIntoView(true);", driver.findElement(BY_XPATH_MORE));
				driver.clickIfExist(BY_XPATH_MORE);
				log.info("{} logs() - 『{}/{}』『{}』『{}』", Utility.indentMiddle(), datetime, minDate, "Loading more", driver.getText(BY_XPATH_MORE, Duration.ZERO));

				driver.switchTo().defaultContent();
				driver.switchTo().frame(driver.findElement(BY_XPATH_FRAME_X));
				driver.switchTo().frame(driver.findElement(BY_XPATH_FRAME_Y));
				log.info("{} logs() - 『{}』『{}』", Utility.indentMiddle(), "content", Utility.ellipsisEscape(driver.getText(BY_XPATH_CONTENTS, Duration.ZERO), 128));

				keepGoing = driver.numberOfElementsToBeMoreThan(BY_XPATH_CONTENTS, sizex, DEFAULT_TIMEOUT_DURATION_LONG);

				driver.manage().timeouts().implicitlyWait(DEFAULT_TIMEOUT_DURATION);
				log.info("{} 『{}』 logs() - 『{}』『{}』", Utility.indentMiddle(), keepGoing, "content", Utility.ellipsisEscape(driver.getText(BY_XPATH_CONTENTS, Duration.ZERO), 128));
			}

			log.info("{} 『{}』 logs() - {}", Utility.indentEnd(), result, Utility.toStringPastTimeReadable(started));
			return result;
		} catch (Exception e) {
			log.error("Exception:: {}", e.getLocalizedMessage(), e);
		}

		log.info("{} 『{}』 SolarMpptCrawlLogsJob::main() - {}", Utility.indentEnd(), null, Utility.toStringPastTimeReadable(started));
		return null;
	}

	private boolean navigateLogs() {
		log.info("{} navigateLogs()", Utility.indentStart());
		long started = System.currentTimeMillis();

		try {
			ChromeDriverWrapper driver = driver();
			driver.manage().timeouts().implicitlyWait(DEFAULT_TIMEOUT_DURATION);
			driver.switchTo().defaultContent();
			
			//*[@id="firstnav"]/div/ul/li[5]/a/span
			By BY_XPATH_USERNAME_X = By.xpath("//*[@id='firstnav']/div/ul/li[5]/a/span[contains(text(),'" + SolarMpptService.USERNAME + "')]");
			log.info("{} navigateLogs() - 『{}』『{}』", Utility.indentMiddle(), "USERNAME", driver.getText(BY_XPATH_USERNAME_X, Duration.ZERO));
			if (!driver.isDisplayed(BY_XPATH_USERNAME_X, Duration.ZERO)) {
				service.login(driver);
			}
			log.info("{} navigateLogs() - 『{}』『{}』", Utility.indentMiddle(), "USERNAME", driver.getText(BY_XPATH_USERNAME_X, Duration.ZERO));

			driver.manage().timeouts().implicitlyWait(DEFAULT_TIMEOUT_DURATION);

			//	Devices List	//*[@id="tabs"]/div/aside[1]/div/section/ul/li/ul/li/a/span[1]
			By BY_XPATH_DEVICES_LIST = By.xpath("//*[@id='tabs']/div/aside[1]/div/section/ul/li/ul/li/a/span[contains(text(),'Devices List')]");
			log.info("{} navigateLogs() - 『{}』『{}』", Utility.indentMiddle(), "Devices List", driver.getText(BY_XPATH_DEVICES_LIST, Duration.ZERO));
			if (!driver.isDisplayed(BY_XPATH_DEVICES_LIST, Duration.ZERO)) {
				//*[@id="firstnav"]/div/ul/li[3]/a/i
				By BY_XPATH_LANGUAGE_ICON = By.xpath("//*[@id='firstnav']/div/ul/li[3]/a/i[contains(@class,'fa-language')]");
				log.info("{} login() - 『{}』『{}』", Utility.indentMiddle(), "language", driver.getText(BY_XPATH_LANGUAGE_ICON, Duration.ZERO));
				driver.clickIfExist(BY_XPATH_LANGUAGE_ICON);
				log.info("{} login() - 『{}』『{}』", Utility.indentMiddle(), "language", driver.getText(BY_XPATH_LANGUAGE_ICON, Duration.ZERO));
				
				//	English	//*[@id="firstnav"]/div/ul/li[3]/ul/li[2]/a
				By BY_XPATH_LANGUAGE_ENGLISH = By.xpath("//*[@id='firstnav']/div/ul/li[3]/ul/li[2]/a[contains(text(),'English')]");
				log.info("{} navigateLogs() - 『{}』『{}』", Utility.indentMiddle(), "English", driver.getText(BY_XPATH_LANGUAGE_ENGLISH, Duration.ZERO));
				driver.clickIfExist(BY_XPATH_LANGUAGE_ENGLISH);
				log.info("{} navigateLogs() - 『{}』『{}』", Utility.indentMiddle(), "English", driver.getText(BY_XPATH_LANGUAGE_ENGLISH, Duration.ZERO));
			}
			driver.presenceOfElementLocated(BY_XPATH_DEVICES_LIST);
			log.info("{} navigateLogs() - 『{}』『{}』", Utility.indentMiddle(), "Devices List", driver.getText(BY_XPATH_DEVICES_LIST, Duration.ZERO));

			//*[@id="con_466197140234"]/iframe	/VuGvSgOfDB.php/usermac/machine/index?lang=en&addtabs=1
			By BY_XPATH_FRAME_X = By.xpath("//div[contains(@class,'content-wrapper')]/div[contains(@class,'active')]/iframe");
			log.info("{} navigateLogs() - 『{}』『{}』", Utility.indentMiddle(), "iframe", driver.getText(BY_XPATH_FRAME_X, Duration.ZERO));
			driver.switchTo().defaultContent();
			driver.switchTo().frame(driver.findElement(BY_XPATH_FRAME_X));
			log.info("{} navigateLogs() - 『{}』『{}』", Utility.indentMiddle(), "iframe", driver.getText(BY_XPATH_FRAME_X, Duration.ZERO));
			
			//	//*[@id="productIndex"]/div[3]/div[1]/div/div/div/div[2]/div/div[4]/button[1]/span
			By BY_XPATH_BUTTON_LOGS = By.xpath("//*[@id='productIndex']//span[contains(text(),'Logs')]/..");
			log.info("{} navigateLogs() - 『{}』『{}』", Utility.indentMiddle(), "Logs", driver.getText(BY_XPATH_BUTTON_LOGS, Duration.ZERO));
			driver.presenceOfElementLocated(BY_XPATH_BUTTON_LOGS);
			driver.clickIfExist(BY_XPATH_BUTTON_LOGS);
			log.info("{} navigateLogs() - 『{}』『{}』", Utility.indentMiddle(), "Logs", driver.getText(BY_XPATH_BUTTON_LOGS, Duration.ZERO));
			
			//*[@id="con_466197140234"]/iframe	/VuGvSgOfDB.php/usermac/machine/index?lang=en&addtabs=1
			By BY_XPATH_FRAME_Y = By.xpath("//div[contains(@class,'layui-layer-content')]/iframe");
			log.info("{} navigateLogs() - 『{}』『{}』", Utility.indentMiddle(), "iframe", driver.getText(BY_XPATH_FRAME_Y, Duration.ZERO));
			driver.switchTo().defaultContent();
			log.info("{} navigateLogs() - 『{}』『{}』", Utility.indentMiddle(), "iframe x", driver.getText(BY_XPATH_FRAME_X, Duration.ZERO));
			driver.presenceOfElementLocated(BY_XPATH_FRAME_X);
			log.info("{} navigateLogs() - 『{}』『{}』", Utility.indentMiddle(), "iframe x", driver.getText(BY_XPATH_FRAME_X, Duration.ZERO));
			driver.switchTo().frame(driver.findElement(BY_XPATH_FRAME_X));
			log.info("{} navigateLogs() - 『{}』『{}』", Utility.indentMiddle(), "iframe y", driver.getText(BY_XPATH_FRAME_Y, Duration.ZERO));
			driver.presenceOfElementLocated(BY_XPATH_FRAME_Y, DEFAULT_TIMEOUT_DURATION_LONG);
			log.info("{} navigateLogs() - 『{}』『{}』", Utility.indentMiddle(), "iframe y", driver.getText(BY_XPATH_FRAME_Y, Duration.ZERO));
			driver.switchTo().frame(driver.findElement(BY_XPATH_FRAME_Y));
			log.info("{} navigateLogs() - 『{}』『{}』", Utility.indentMiddle(), "iframe", driver.getText(BY_XPATH_FRAME_Y, Duration.ZERO));

			log.info("{} 『{}』 navigateLogs() - {}", Utility.indentEnd(), true, Utility.toStringPastTimeReadable(started));
			return true;
		} catch (Exception e) {
			log.error("Exception:: {}", e.getLocalizedMessage(), e);
		}

		log.info("{} 『{}』 navigateLogs() - {}", Utility.indentEnd(), false, Utility.toStringPastTimeReadable(started));
		return false;
	}

	private boolean notLogs() {
		log.info("{} notLogs()", Utility.indentStart());
		long started = System.currentTimeMillis();

		try {
			ChromeDriverWrapper driver = driver();
			driver.manage().timeouts().implicitlyWait(Duration.ZERO);

			driver.switchTo().defaultContent();

			//	//*[@id="con_1341"]/iframe
			By BY_XPATH_FRAME = By.xpath("//*[@id='con_1341']/iframe");
			log.info("{} notLogs() - 『{}』『{}』", Utility.indentMiddle(), "iframe", driver.getText(BY_XPATH_FRAME, Duration.ZERO));
			if (!driver.isDisplayed(BY_XPATH_FRAME)) {
				log.info("{} 『{}』 notLogs() - {}", Utility.indentEnd(), true, Utility.toStringPastTimeReadable(started));
				return true;
			}
			WebElement frame = driver.findElement(BY_XPATH_FRAME);
			log.info("{} notLogs() - 『{}』『{}』", Utility.indentMiddle(), "iframe", driver.getText(BY_XPATH_FRAME, Duration.ZERO));
			driver.switchTo().frame(frame);

			//	//*[@id="main"]
			By BY_XPATH_CONTENT = By.xpath("//*[@id='main']");
			log.info("{} notLogs() - 『{}』『{}』", Utility.indentMiddle(), "main", driver.getText(BY_XPATH_CONTENT, Duration.ZERO));
			if (!driver.isDisplayed(BY_XPATH_CONTENT)) {
				log.info("{} 『{}』 notLogs() - {}", Utility.indentEnd(), true, Utility.toStringPastTimeReadable(started));
				return true;
			}
			log.info("{} notLogs() - 『{}』『{}』", Utility.indentMiddle(), "main", driver.getText(BY_XPATH_CONTENT, Duration.ZERO));

			log.info("{} 『{}』 notLogs() - {}", Utility.indentEnd(), false, Utility.toStringPastTimeReadable(started));
			return false;
		} catch (Exception e) {
			log.error("Exception:: {}", e.getLocalizedMessage(), e);
		}

		log.info("{} 『{}』 notLogs() - {}", Utility.indentEnd(), true, Utility.toStringPastTimeReadable(started));
		return true;
	}

	private static ChromeDriverWrapper driver() {
		log.info("{} driver() - 『{}』", Utility.indentStart(), driver);
		long started = System.currentTimeMillis();

		if (driver == null) {
			driver = SolarMpptService.createDriver(true, "logs");
		}

		log.info("{} driver() - 『{}』 - {}", Utility.indentEnd(), driver, Utility.toStringPastTimeReadable(started));
		return driver;
	}

}
