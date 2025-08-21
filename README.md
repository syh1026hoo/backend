# ETF Stock Platform - Backend

Spring Boot 기반 ETF 정보 제공 및 알림 시스템 백엔드 API 서버

## 🚀 주요 기능

- **ETF 데이터 관리**: 공공데이터포털 API 연동
- **관심종목 시스템**: 사용자별 관심종목 등록/관리
- **가격 알림 시스템**: 실시간 가격 변동 감시 및 알림
- **스케줄링**: 자동 데이터 동기화 및 알림 처리
- **REST API**: 완전한 RESTful API 제공

## 🛠 기술 스택

- **Framework**: Spring Boot 3.x
- **Database**: PostgreSQL + JPA/Hibernate
- **Scheduler**: Spring @Scheduled
- **API**: REST API + JSON
- **Build**: Gradle

## 📋 API 엔드포인트

### ETF 정보
- `GET /api/etf` - ETF 목록 조회
- `GET /api/etf/{isinCd}` - ETF 상세 정보

### 관심종목
- `POST /api/watchlist` - 관심종목 추가
- `GET /api/watchlist` - 관심종목 조회
- `DELETE /api/watchlist/{id}` - 관심종목 삭제

### 알림 시스템
- `POST /api/alerts/conditions` - 알림 조건 생성
- `GET /api/alerts/conditions` - 알림 조건 조회
- `GET /api/alerts` - 알림 목록 조회
- `PATCH /api/alerts/{id}/read` - 알림 읽음 처리

## 🔧 실행 방법

```bash
# 의존성 설치 및 빌드
./gradlew build

# 애플리케이션 실행
./gradlew bootRun

# 또는 JAR 파일 실행
java -jar build/libs/etfstock-0.0.1-SNAPSHOT.jar
```

## ⚙️ 환경 설정

`src/main/resources/application.properties` 파일에서 설정:

```properties
# 데이터베이스 설정
spring.datasource.url=jdbc:postgresql://localhost:5432/etfstock
spring.datasource.username=postgres
spring.datasource.password=password

# API 키 설정
external.api.data-go-kr.service-key=YOUR_API_KEY
```

## 📊 스케줄링

- **평일 16:00**: 일일 ETF 데이터 동기화 및 알림 확인
- **평일 08:30**: 보완 데이터 동기화
- **평일 09:00-15:30**: 30분마다 실시간 알림 감시
- **매일 00:00**: 오래된 데이터 정리

## 🗃 데이터베이스 스키마

- `users`: 사용자 정보
- `etf_info`: ETF 기본 정보
- `user_watchlist`: 관심종목
- `alert_conditions`: 알림 조건
- `alerts`: 알림 이력
