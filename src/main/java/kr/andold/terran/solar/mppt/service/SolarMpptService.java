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
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import kr.andold.terran.solar.mppt.domain.SolarMpptDomain;
import kr.andold.terran.solar.mppt.entity.SolarMpptEntity;
import kr.andold.terran.solar.mppt.interfaces.CrudService;
import kr.andold.terran.solar.mppt.param.SolarMpptParam;
import kr.andold.terran.solar.mppt.repository.SolarMpptRepository;
import kr.andold.terran.solar.mppt.repository.SolarMpptSpecification;
import kr.andold.utils.persist.CrudList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SolarMpptService implements CrudService<SolarMpptParam, SolarMpptDomain, SolarMpptEntity> {
	public static final String URL = "http://app.gz529.com/VuGvSgOfDB.php/index/login?login_type=user";
	public static final String USERNAME = "01068106479";
	public static final String PASSWORD = "123456";
	private static final Duration DEFAULT_TIMEOUT_DURATION = Duration.ofSeconds(8);
	private static final Duration DEFAULT_TIMEOUT_DURATION_LONG = Duration.ofMinutes(1);
	private static final Sort DEFAULT_SORT = Sort.by(Order.asc("base"));
	private static final Pageable DEFAULT_PAGEABLE = PageRequest.of(0, 1024 * 1024, DEFAULT_SORT);

	@Getter private static String webdriverPath;
	@Value("${application.selenium.webdriver.chrome.driver}")
	public void setWebdriverPath(String value) {
		log.info("{} setWebdriverPath(『{}』)", Utility.indentMiddle(), value);
		webdriverPath = value;
	}

	@Getter private static String userDataDir;
	@Value("${application.selenium.user.data.dir}")
	public void setUserDataDir(String value) {
		log.info("{} setUserDataDir(『{}』)", Utility.indentMiddle(), value);
		userDataDir = value;
	}

	@Getter private static String userDataPath;
	@Value("${application.data.path}")
	public void setUserDataPath(String value) {
		log.info("{} setUserDataPath(『{}』)", Utility.indentMiddle(), value);
		userDataPath = value;
	}

	@Autowired private SolarMpptRepository repository;
	
	private ChromeDriverWrapper driver = null;

	public SolarMpptParam crawl(SolarMpptParam param) {
		log.info("{} crawl(『{}』)", Utility.indentStart(), param);
		long started = System.currentTimeMillis();

		SolarMpptParam result = logs(param.getStart().toInstant().atZone(Utility.ZONE_ID_KST));

		log.info("{} 『{}』 crawl(『{}』) - {}", Utility.indentEnd(), result, param, Utility.toStringPastTimeReadable(started));
		return result;
	}

	private SolarMpptParam logs(ZonedDateTime minDate) {
		log.info("{} logs()", Utility.indentStart());
		long started = System.currentTimeMillis();

		try {
			ChromeDriverWrapper driver = driver();
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
					log.info("{} logs() - 『{}/{}』『{}』『{}』『{}』『{}』『{}』『{} < {}』『#{} {}』", Utility.indentMiddle()
							, cx, sizex, datetime, temperature, discharge, charge, voltage, datetime, minDate, Utility.size(domains), container);
				}

				log.info("{} logs() - 『{} < {}』『{} {}』", Utility.indentMiddle(), datetime, minDate, Utility.size(domains), container);
				CrudList<SolarMpptDomain> crud = put(domains);
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

			close();

			log.info("{} 『{}』 logs() - {}", Utility.indentEnd(), result, Utility.toStringPastTimeReadable(started));
			return result;
		} catch (Exception e) {
			log.error("Exception:: {}", e.getLocalizedMessage(), e);
		}

		close();

		log.info("{} 『{}』 logs() - {}", Utility.indentEnd(), null, Utility.toStringPastTimeReadable(started));
		return null;
	}

	private void close() {
		if (driver != null) {
			driver.quit();
			driver = null;
		}
	}

	private boolean navigateLogs() {
		log.info("{} navigateLogs()", Utility.indentStart());
		long started = System.currentTimeMillis();

		try {
			ChromeDriverWrapper driver = driver();
			driver.manage().timeouts().implicitlyWait(DEFAULT_TIMEOUT_DURATION);
			driver.switchTo().defaultContent();
			
			//*[@id="firstnav"]/div/ul/li[5]/a/span
			By BY_XPATH_USERNAME_X = By.xpath("//*[@id='firstnav']/div/ul/li[5]/a/span[contains(text(),'" + USERNAME + "')]");
			log.info("{} navigateLogs() - 『{}』『{}』", Utility.indentMiddle(), "USERNAME", driver.getText(BY_XPATH_USERNAME_X, Duration.ZERO));
			if (!driver.isDisplayed(BY_XPATH_USERNAME_X, Duration.ZERO)) {
				login(driver);
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

	public boolean login(ChromeDriverWrapper driver) {
		log.info("{} login()", Utility.indentStart());
		long started = System.currentTimeMillis();

		try {
			driver.manage().timeouts().implicitlyWait(DEFAULT_TIMEOUT_DURATION);
			
			//*[@id="firstnav"]/div/ul/li[5]/a/span	
			By BY_XPATH_USERNAME_X = By.xpath("//*[@id='firstnav']/div/ul/li[5]/a/span[contains(text(),'01068106479')]");
			log.info("{} login() - 『{}』『{}』", Utility.indentMiddle(), "USERNAME", driver.getText(BY_XPATH_USERNAME_X, Duration.ZERO));
			if (driver.isDisplayed(BY_XPATH_USERNAME_X, Duration.ZERO)) {
				log.info("{} 『{}』 login() - {}", Utility.indentEnd(), true, Utility.toStringPastTimeReadable(started));
				return true;
			}
			log.info("{} login() - 『{}』『{}』", Utility.indentMiddle(), "USERNAME", driver.getText(BY_XPATH_USERNAME_X, Duration.ZERO));

			driver.get(URL);
			log.info("{} login() - {}", Utility.indentMiddle(), URL);
			
			//	로그인 하라는 화면 또는 로그인 된 화면
			By BY_XPATH_START_LOADED = By.xpath("//*[@id='login-form' or @id='firstnav']");
			log.info("{} login() - 『{}』『{}』", Utility.indentMiddle(), "load", Utility.ellipsisEscape(driver.getText(BY_XPATH_START_LOADED, Duration.ZERO), 32, 32));
			driver.presenceOfElementLocated(BY_XPATH_START_LOADED, DEFAULT_TIMEOUT_DURATION_LONG);
			log.info("{} login() - 『{}』『{}』", Utility.indentMiddle(), "load", Utility.ellipsisEscape(driver.getText(BY_XPATH_START_LOADED, Duration.ZERO), 32, 32));

			if (driver.isDisplayed(BY_XPATH_USERNAME_X)) {
				log.info("{} 『{}』 login() - {}", Utility.indentEnd(), true, Utility.toStringPastTimeReadable(started));
				return true;
			}
			log.info("{} login() - 『{}』『{}』", Utility.indentMiddle(), "USERNAME", driver.getText(BY_XPATH_USERNAME_X, Duration.ZERO));

			driver.manage().timeouts().implicitlyWait(DEFAULT_TIMEOUT_DURATION);

			//*[@id="login-form"]
			By BY_XPATH_LOGIN_FORM = By.xpath("//*[@id='login-form']");
			log.info("{} login() - 『{}』『{}』", Utility.indentMiddle(), "login", Utility.escape(driver.getText(BY_XPATH_LOGIN_FORM, Duration.ZERO)));
			driver.presenceOfElementLocated(BY_XPATH_LOGIN_FORM, DEFAULT_TIMEOUT_DURATION_LONG);
			log.info("{} login() - 『{}』『{}』", Utility.indentMiddle(), "login", Utility.escape(driver.getText(BY_XPATH_LOGIN_FORM, Duration.ZERO)));

			//	//*[@id="pd-form-username"]
			By BY_XPATH_USERNAME = By.xpath("//*[@id='pd-form-username']");
			log.info("{} login() - 『{}』『{}』", Utility.indentMiddle(), "username", driver.getText(BY_XPATH_USERNAME, Duration.ZERO));
			WebElement username = driver.findElement(BY_XPATH_USERNAME);
			username.sendKeys(USERNAME);
			log.info("{} login() - 『{}』『{}』", Utility.indentMiddle(), "username", driver.getText(BY_XPATH_USERNAME, Duration.ZERO));

			//*[@id="pd-form-password"]
			By BY_XPATH_PASSWORD = By.xpath("//*[@id='pd-form-password']");
			log.info("{} login() - 『{}』『{}』", Utility.indentMiddle(), "password", driver.getText(BY_XPATH_PASSWORD, Duration.ZERO));
			WebElement password = driver.findElement(BY_XPATH_PASSWORD);
			password.sendKeys(PASSWORD);
			log.info("{} login() - 『{}』『{}』", Utility.indentMiddle(), "password", driver.getText(BY_XPATH_PASSWORD, Duration.ZERO));
			
			//	//*[@id="keeplogin"]
			By BY_XPATH_KEEP_LOGIN = By.xpath("//*[@id='keeplogin']");
			log.info("{} login() - 『{}』『{}』", Utility.indentMiddle(), "keeplogin", driver.getText(BY_XPATH_KEEP_LOGIN, Duration.ZERO));
			WebElement keepLogin = driver.findElement(BY_XPATH_KEEP_LOGIN);
			if (!keepLogin.isSelected()) {
				keepLogin.click();
			}
			log.info("{} login() - 『{}』『{}』", Utility.indentMiddle(), "keeplogin", driver.getText(BY_XPATH_KEEP_LOGIN, Duration.ZERO));

			//*[@id="login-form"]/div[5]/button
			By BY_XPATH_SUBMIT = By.xpath("//*[@id='login-form']/div/button[contains(@type,'submit')]");
			log.info("{} login() - 『{}』『{}』", Utility.indentMiddle(), "submit", driver.getText(BY_XPATH_SUBMIT, Duration.ZERO));
			driver.clickIfExist(BY_XPATH_SUBMIT);
			log.info("{} login() - 『{}』『{}』", Utility.indentMiddle(), "submit", driver.getText(BY_XPATH_SUBMIT, Duration.ZERO));

			deviceList(driver);
			modeEnglish(driver);

			log.info("{} 『{}』 login() - {}", Utility.indentEnd(), true, Utility.toStringPastTimeReadable(started));
			return true;
		} catch (Exception e) {
			log.error("Exception:: {}", e.getLocalizedMessage(), e);
		}

		log.info("{} 『{}』 login() - {}", Utility.indentEnd(), false, Utility.toStringPastTimeReadable(started));
		return false;
	}

	//	Devices List
	public void deviceList(ChromeDriverWrapper driver) {
		//	Toggle navigation	//*[@id="firstnav"]/a/span
		By BY_XPATH_TOGGLE_NAVIGATION = By.xpath("//*[@id='firstnav']/a/span[contains(text(),'Toggle navigation')]");
		log.info("{} login() - 『{}』『{}』", Utility.indentMiddle(), "Toggle navigation", driver.getText(BY_XPATH_TOGGLE_NAVIGATION, Duration.ZERO));
		driver.waitUntilExist(BY_XPATH_TOGGLE_NAVIGATION, true, (int) (DEFAULT_TIMEOUT_DURATION.getSeconds() * 1000));
		log.info("{} login() - 『{}』『{}』", Utility.indentMiddle(), "Toggle navigation", driver.getText(BY_XPATH_TOGGLE_NAVIGATION, Duration.ZERO));

		//	Devices List	//*[@id="tabs"]/div/aside[1]/div/section/ul/li/ul/li/a/span[1]
		By BY_XPATH_DEVICES_LIST_LEFT_MENU = By.xpath("//*[@id='tabs']/div/aside[1]/div/section/ul/li/ul/li/a/span[1]");
		log.info("{} modeEnglish(...) - 『{}』『{}』", Utility.indentMiddle(), "Devices List", driver.getText(BY_XPATH_DEVICES_LIST_LEFT_MENU, Duration.ZERO));
		driver.presenceOfElementLocated(BY_XPATH_DEVICES_LIST_LEFT_MENU, DEFAULT_TIMEOUT_DURATION);
		driver.clickIfExist(BY_XPATH_DEVICES_LIST_LEFT_MENU);
		log.info("{} modeEnglish(...) - 『{}』『{}』", Utility.indentMiddle(), "Devices List", driver.getText(BY_XPATH_DEVICES_LIST_LEFT_MENU, Duration.ZERO));

		//	Toggle navigation	//*[@id="firstnav"]/a/span
		log.info("{} login() - 『{}』『{}』", Utility.indentMiddle(), "Toggle navigation", driver.getText(BY_XPATH_TOGGLE_NAVIGATION, Duration.ZERO));
		driver.waitUntilExist(BY_XPATH_TOGGLE_NAVIGATION, true, (int) (DEFAULT_TIMEOUT_DURATION.getSeconds() * 1000));
		log.info("{} login() - 『{}』『{}』", Utility.indentMiddle(), "Toggle navigation", driver.getText(BY_XPATH_TOGGLE_NAVIGATION, Duration.ZERO));
	}

	//	English
	public void modeEnglish(ChromeDriverWrapper driver) {
		//	Home		//*[@id="firstnav"]/div/ul/li[1]/a/text()
		By BY_XPATH_HOME = By.xpath("//*[@id='firstnav']/div/ul/li[1]/a[contains(text(),'Home')]");
		log.info("{} modeEnglish(...) - 『{}』『{}』", Utility.indentMiddle(), "Home", driver.getText(BY_XPATH_HOME, Duration.ZERO));
		if (driver.isDisplayed(BY_XPATH_HOME, Duration.ZERO)) {
			return;
		}
		log.info("{} modeEnglish(...) - 『{}』『{}』", Utility.indentMiddle(), "Home", driver.getText(BY_XPATH_HOME, Duration.ZERO));
		
		//	fa fa-language	//*[@id="firstnav"]/div/ul/li[3]/a/i
		By BY_XPATH_LANGUAGE_ICON = By.xpath("//*[@id='firstnav']/div/ul/li[3]/a/i[contains(@class,'fa-language')]");
		log.info("{} modeEnglish(...) - 『{}』『{}』", Utility.indentMiddle(), "language", driver.getText(BY_XPATH_LANGUAGE_ICON, Duration.ZERO));
		driver.presenceOfElementLocated(BY_XPATH_LANGUAGE_ICON, DEFAULT_TIMEOUT_DURATION);
		driver.clickIfExist(BY_XPATH_LANGUAGE_ICON);
		log.info("{} modeEnglish(...) - 『{}』『{}』", Utility.indentMiddle(), "language", driver.getText(BY_XPATH_LANGUAGE_ICON, Duration.ZERO));

		//	English	//*[@id="firstnav"]/div/ul/li[3]/ul/li[2]/a
		By BY_XPATH_LANGUAGE_ENGLISH = By.xpath("//*[@id='firstnav']/div/ul/li[3]/ul/li[2]/a[contains(text(),'English')]");
		log.info("{} modeEnglish(...) - 『{}』『{}』", Utility.indentMiddle(), "English", driver.getText(BY_XPATH_LANGUAGE_ENGLISH, Duration.ZERO));
		driver.presenceOfElementLocated(BY_XPATH_LANGUAGE_ENGLISH, DEFAULT_TIMEOUT_DURATION);
		driver.clickIfExist(BY_XPATH_LANGUAGE_ENGLISH);
		log.info("{} modeEnglish(...) - 『{}』『{}』", Utility.indentMiddle(), "English", driver.getText(BY_XPATH_LANGUAGE_ENGLISH, Duration.ZERO));
		
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

	private ChromeDriverWrapper driver() {
		if (this.driver != null) {
			return this.driver;
		}

		this.driver = createDriver(true, "general");

		return this.driver;
	}

	public static ChromeDriverWrapper createDriver(boolean fHeadless, String postfix) {
		log.info("{} createDriver({}) - 『{}』", Utility.indentStart(), fHeadless, getWebdriverPath());
		long started = System.currentTimeMillis();

		System.setProperty("webdriver.chrome.driver", getWebdriverPath());
		ChromeOptions options = new ChromeOptions();
		options.addArguments("--disable-background-networking");	//	timeout 관련
		options.addArguments("--disable-blink-features=AutomationControlled");
		options.addArguments("--disable-dev-shm-usage");
		options.addArguments("--disable-features=NetworkService");	//	timeout 관련
		options.addArguments("--disable-gpu");
		options.addArguments("--disable-infobars");
		options.addArguments("--disable-popup-blocking");
		if (fHeadless) {
			options.addArguments("--headless=new");	//	new:: timeout 관련
		}
		options.addArguments("--no-sandbox");
		options.addArguments("--remote-allow-origins=*");
		options.addArguments(String.format("--user-data-dir=%s-%s", getUserDataDir(), postfix));
		options.addArguments("--window-position=0,0");
		options.addArguments(String.format("--window-size=%d,%d", 1920 * 1, 1090 * 1 - 256));
		options.setPageLoadStrategy(PageLoadStrategy.NONE);
		ChromeDriverWrapper chromeDriver = new ChromeDriverWrapper(options);

		log.info("{} 『{}』 ChromeDriverWrapper({}) - 『{}:{}』 - {}", Utility.indentEnd(), chromeDriver, fHeadless, getWebdriverPath(), getUserDataDir(), Utility.toStringPastTimeReadable(started));
		return chromeDriver;
	}

	@Modifying
	public SolarMpptParam update(SolarMpptDomain domain) {
		SolarMpptEntity entity = toEntity(domain);
		SolarMpptEntity updated = repository.saveAndFlush(entity);
		return toParam(updated);
	}

	private SolarMpptParam toParam(SolarMpptEntity entity) {
		SolarMpptParam param = new SolarMpptParam();
		BeanUtils.copyProperties(entity, param);
		return param;
	}

	@Override
	public CrudList<SolarMpptDomain> put(List<SolarMpptDomain> afters) {
		if (afters == null || afters.isEmpty()) {
			return CrudList.<SolarMpptDomain>builder().build();
		}

		Date start = afters.get(0).getBase();
		Date end = afters.get(0).getBase();
		for (SolarMpptDomain domain: afters) {
			start = Utility.min(start, domain.getBase());
			end = Utility.max(end, domain.getBase());
		}
		SolarMpptParam param = SolarMpptParam.builder()
				.start(start)
				.end(Date.from(end.toInstant().plusSeconds(1)))
				.build();
		List<SolarMpptDomain> befores = search(param);
		CrudList<SolarMpptDomain> list = differ(befores, afters);
		list.setRemoves(null);
		batch(list);
		return list;
	}

	@Modifying
	@Override
	public List<SolarMpptDomain> update(List<SolarMpptDomain> domains) {
		List<SolarMpptEntity> entities = toEntities(domains);
		List<SolarMpptEntity> updated = repository.saveAllAndFlush(entities);
		return toDomains(updated);
	}

	@Override
	public SolarMpptDomain toDomain(String text) {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setSerializationInclusion(Include.NON_NULL);
		objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		try {
			return objectMapper.readValue(text, SolarMpptDomain.class);
		} catch (Exception e) {
			try {
				return objectMapper.readValue(URLDecoder.decode(text, "UTF-8"), SolarMpptDomain.class);
			} catch (Exception f) {
				e.printStackTrace();
				f.printStackTrace();
			}
		}

		return null;
	}

	@Override
	public SolarMpptDomain toDomain(SolarMpptEntity entity) {
		SolarMpptDomain domain = new SolarMpptDomain();
		BeanUtils.copyProperties(entity, domain);
		return domain;
	}

	@Override
	public SolarMpptEntity toEntity(SolarMpptDomain domain) {
		SolarMpptEntity entity = new SolarMpptEntity();
		BeanUtils.copyProperties(domain, entity);
		return entity;
	}

	@Override
	public int compare(SolarMpptDomain after, SolarMpptDomain before) {
		return after.getBase().compareTo(before.getBase());
	}

	@Override
	public void prepareCreate(SolarMpptDomain domain) {
		Date date = new Date();
		domain.setId(null);
		if (domain.getBase() == null) {
			domain.setBase(date);
		}
		if (domain.getCharge() == null) {
			domain.setCharge(-1f);
		}
		if (domain.getDischarge() == null) {
			domain.setDischarge(-1f);
		}
		if (domain.getTemperature() == null) {
			domain.setTemperature(-274f);
		}
		if (domain.getVoltage() == null) {
			domain.setVoltage(-1f);
		}
		domain.setCreated(date);
		domain.setUpdated(date);
	}

	@Override
	public void prepareUpdate(SolarMpptDomain before, SolarMpptDomain after) {
		if (before == null || after == null) {
			return;
		}

		Float temperature = after.getTemperature();
		if (temperature != null && temperature > 0) {
			before.setTemperature(temperature);
		}
		Float discharge = after.getDischarge();
		if (discharge != null && discharge > 0) {
			before.setDischarge(discharge);
		}
		Float charge = after.getCharge();
		if (charge != null && charge > 0) {
			before.setCharge(charge);
		}
		Float voltage = after.getVoltage();
		if (voltage != null && voltage > 0) {
			before.setVoltage(voltage);
		}

		before.setUpdated(new Date());
	}

	@Override
	public String key(SolarMpptDomain domain) {
		return String.format("%tF %<tR", domain.getBase());
	}

	@Modifying
	@Override
	public int remove(List<SolarMpptDomain> domains) {
		List<SolarMpptEntity> entities = toEntities(domains);
		repository.deleteAll(entities);
		repository.flush();
		return Utility.size(entities);
	}

	@Modifying
	@Override
	public List<SolarMpptDomain> create(List<SolarMpptDomain> domains) {
		for (SolarMpptDomain domain: domains) {
			prepareCreate(domain);
		}
		List<SolarMpptEntity> entities = toEntities(domains);
		List<SolarMpptEntity> created = repository.saveAllAndFlush(entities);
		return toDomains(created);
	}

	@Override
	public List<SolarMpptDomain> search(SolarMpptParam param) {
		SolarMpptParam result = searchWithPageable(param);
		List<SolarMpptDomain> domains = result.getCrud().getDuplicates();
		return domains;
	}

	@Override
	public SolarMpptParam searchWithPageable(SolarMpptParam param) {
		log.info("{} search(『{}』)", Utility.indentStart(), param);
		long started = System.currentTimeMillis();

		if (param == null) {
			Page<SolarMpptEntity> paged = repository.findAll(DEFAULT_PAGEABLE);
			SolarMpptParam result = SolarMpptParam.builder()
					.crud(CrudList.<SolarMpptDomain>builder()
							.duplicates(toDomains(paged.getContent()))
							.build())
					.build();

			log.info("{} 『{}』 search(『{}』) - {}", Utility.indentEnd(), result, param, Utility.toStringPastTimeReadable(started));
			return result;
		}

		Page<SolarMpptEntity> paged = repository.findAll(SolarMpptSpecification.searchWith(param), param);
		log.info("{} 『{}』 search(『{}』) - {}", Utility.indentMiddle(), paged, param, Utility.toStringPastTimeReadable(started));
		List<SolarMpptDomain> domains = toDomains(paged.getContent());
		log.info("{} 『#{}』 search(『{}』) - {}", Utility.indentMiddle(), Utility.size(domains), param, Utility.toStringPastTimeReadable(started));
		SolarMpptParam result = SolarMpptParam.builder()
				.crud(CrudList.<SolarMpptDomain>builder()
						.duplicates(domains)
						.build())
				.build();

		log.info("{} 『{}』 search(『{}』) - {}", Utility.indentEnd(), result, param, Utility.toStringPastTimeReadable(started));
		return result;
	}


	public int backup() {
		log.info("{} backup()", Utility.indentStart());
		long started = System.currentTimeMillis();

		String fullpath = getUserDataPath();
		List<SolarMpptDomain> domains = search(null);
		String json = Utility.toStringJsonLine(domains);
		Utility.write(String.format("%s/solar-mppt.json", fullpath), json);
		int result = Utility.size(domains);

		log.info("{} 『{}』 backup() - {}", Utility.indentEnd(), result, Utility.toStringPastTimeReadable(started));
		return result;
	}

	public CrudList<SolarMpptDomain> upload(MultipartFile file) {
		log.info("{} upload({})", Utility.indentStart(), Utility.toStringJson(file, 64, 32));
		long started = System.currentTimeMillis();

		try {
			String text = Utility.extractStringFromText(file.getInputStream());
			CrudList<SolarMpptDomain> result = CrudService.super.upload(text);

			log.info("{} {} - upload({}) - {}", Utility.indentEnd(), result, Utility.toStringJson(file, 32, 32), Utility.toStringPastTimeReadable(started));
			return result;
		} catch (Exception e) {
		}

		log.info("{} {} - upload({}) - {}", Utility.indentEnd(), "EXCEPTION", Utility.toStringJson(file, 32, 32), Utility.toStringPastTimeReadable(started));
		return null;
	}

}
