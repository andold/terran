# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 개요

terran은 개인용 백엔드 API 모음(Spring Boot 3.2, Java 21, PostgreSQL)입니다. WAR로 빌드되어 외부 Tomcat에 context path `/terran`으로 배포됩니다. 커밋 메시지와 로그 메시지는 한국어로 작성합니다.

## 빌드 / 실행 / 테스트

- 의존성 `kr.andold:utils`는 GitHub Packages(`maven.pkg.github.com/andold/utils`)에서 받으므로 `~/.gradle/gradle.properties`에 `mavenUser` / `mavenPassword`가 있어야 빌드됩니다. `kr.andold.utils.*`(Utility, JobService, ZookeeperClient, CrudService, CrudList 등)는 이 외부 라이브러리의 클래스입니다.
- ANTLR 생성 코드(`src/main/java/kr/andold/terran/bhistory/antlr/`)는 gitignore 대상이므로 클린 체크아웃 후 컴파일 전에 먼저 생성해야 합니다: Windows는 저장소 루트에서 `antlr.bat`, Linux/Git Bash는 `bash antlr.sh`(스크립트 위치 기준으로 경로를 잡음). 문법 파일은 `src/main/resources/*.g4`입니다.
- 빌드: `./gradlew build -Pprofile=<dev|linux> -x test` → `build/libs/terran-0.0.1-SNAPSHOT.war`
- 테스트 전체: `./gradlew test`, 단일 테스트: `./gradlew test --tests "kr.andold.terran.service.UtilsTest.testPutMap"`

### 리소스 프로파일 (Spring profile이 아닌 Gradle 속성)

`build.gradle`이 `-Pprofile` 값으로 리소스 디렉터리를 교체합니다.
- `-Pprofile=dev` → `src/main/resources-dev` (테스트 DB, znode `/test/elect-terran`)
- `-Pprofile=linux` → `src/main/resources-linux` (운영 DB, znode `/elect-terran`)
- 지정 안 함(테스트/IDE 실행) → `src/test/resources` + `src/test/resources-local`(gitignore, 로컬 전용 `application.properties`). `src/test/resources-windows`가 그 템플릿 예시입니다.

각 프로파일 디렉터리의 `deploy-terran-*.sh`가 실제 배포 절차(ANTLR 생성 → gradle build → Tomcat 중지 → WAR 압축 해제 → Tomcat 시작)입니다.

### 테스트 주의

대부분의 테스트는 `@SpringBootTest`이며 실제 원격 PostgreSQL, ZooKeeper, Selenium/chromedriver에 연결하고 크롤링 등 부수효과가 있습니다. 순수 단위 테스트는 `UtilsTest`, `NotSpringBootTest` 정도입니다. 테스트를 돌리기 전에 어떤 외부 자원을 건드리는지 확인하세요.

## 아키텍처

### 도메인 모듈

`kr.andold.terran` 아래 기능별 패키지가 `controller / domain / entity / param / repository / service` 구조를 가집니다: `bhistory`(빅히스토리 연표, ANTLR로 날짜 파싱 + SVG 생성), `bookmark`, `ics`(iCalendar), `solar/mppt`(태양광 MPPT 크롤링), `tsdb`(시계열). 연락처(contact) 기능은 하위 패키지 없이 루트의 `controller/`, `entity/`, `service/` 등에 있습니다.

- Entity(JPA)와 Domain(DTO)은 분리되어 있고, Param은 검색 조건 + 결과를 함께 담는 요청/응답 객체입니다. 검색은 `*Specification` 클래스로 JPA Specification을 만듭니다.
- 최근 모듈(solar/mppt, tsdb)은 `solar/mppt/interfaces`의 `CrudController` / `CrudService`(search, update, download, upload) 패턴을 따릅니다. upload/batch 결과는 `CrudList`(creates/duplicates/updates/removes)로 표현됩니다.
- 스키마는 `spring.jpa.hibernate.ddl-auto=none`이므로 `src/main/resources/schema*.sql`을 수동으로 적용합니다.

### 스케줄링과 Job 큐

- `ScheduledTasks`, `SolarMpptScheduledTasks`의 `@Scheduled` 메서드는 직접 작업하지 않고 `JobInterface` 구현체(`*Job`)를 `JobService.getQueue2()/getQueue3()`에 넣습니다. `JobService.run()`이 1초마다 큐를 소비합니다. 타임아웃 시 크롬 드라이버 재시작 Job(`SolarMpptCrawlDetailsJob.RestartDriverJob`)을 등록합니다.
- Job 클래스는 `@SuperBuilder` + `@Service`인 동시에 builder로 생성되어 큐에 들어갑니다. `call()` 안에서 `ApplicationContextProvider.getBean(...)`으로 스프링 빈 인스턴스를 얻어 필드를 복사한 뒤 `main()`을 실행하는 패턴이므로, builder로 만든 인스턴스에서 `@Autowired` 필드를 직접 쓰지 마세요.

### 다중 인스턴스 / 리더 선출

여러 노드에서 동시에 실행되며, `ZookeeperClient`(utils의 래퍼)로 리더를 선출합니다. 주기 작업은 `zookeeperClient.isMaster()`일 때만 실행합니다. `ZookeeperClient.isTestEnvironment()`는 znode elect path에 `test`가 포함되는지로 판정하며, 개발/테스트 환경에서는 크롤링 계열 Job을 등록하지 않습니다(조건 방향을 반대로 넣는 실수가 과거에 있었음).

### 기타

- 기본 TimeZone은 `Application`에서 `Asia/Seoul`로 설정됩니다.
- 커스텀 설정 키(`application.data.path`, `application.zookeeper.*`, `application.selenium.*`)는 `META-INF/additional-spring-configuration-metadata.json`에 정의되어 있습니다.
- 로그는 `Utility.indentStart()/indentMiddle()/indentEnd()`와 `『』` 괄호로 값을 감싸는 형식을 일관되게 사용합니다.
