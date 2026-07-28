# Koslink Backend

> 반도체 산업 뉴스 자동 수집, AI 분석 및 종목 영향도 분석 백엔드 시스템

## 📋 개요

뉴스 크롤링부터 AI 분석까지 전 과정을 자동화한 백엔드 API 서버입니다.
- 네이버 뉴스 API를 통한 반도체 관련 뉴스 자동 수집
- LLM 기반 뉴스 필터링 (Spring AI)
- 외부 AI 분석 서버 연동을 통한 종목 영향도 분석
- 분석 결과 조회 REST API 제공

## 🛠️ 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 4.0.7 |
| ORM | Spring Data JPA (Hibernate) |
| Database | PostgreSQL 15.x |
| HTTP Client | Spring Cloud OpenFeign |
| AI | Spring AI (OpenAI) |
| Crawling | Jsoup |
| Cache | Caffeine |
| Build Tool | Gradle 8.x |

## 📁 프로젝트 구조

```
src/main/java/com/koslink/
├── config/                 # 설정
│   ├── CacheConfig         # Caffeine 캐시 설정
│   ├── CorsConfig          # CORS 설정
│   └── JacksonConfig       # ObjectMapper 빈 설정
│
├── news/                   # 뉴스 도메인 (핵심)
│   ├── controller/
│   │   └── NewsController  # REST API 엔드포인트
│   ├── service/
│   │   ├── NewsService     # 비즈니스 로직
│   │   └── NewsFilterService  # LLM 기반 뉴스 필터링
│   ├── entity/
│   │   ├── News            # 뉴스 엔티티
│   │   ├── AiResponse      # AI 분석 결과 엔티티
│   │   └── NewsStatus      # 뉴스 상태 ENUM
│   ├── repository/
│   │   ├── NewsRepository
│   │   └── AiResponseRepository
│   ├── converter/          # JPA Converter (JSONB ↔ DTO)
│   │   ├── NewsGraphConverter
│   │   ├── NewsSummaryConverter
│   │   ├── NewsSourceConverter
│   │   ├── OriginStocksConverter
│   │   └── RelatedStocksConverter
│   ├── dto/                # 요청/응답 DTO
│   ├── client/             # Feign Client
│   │   ├── NaverNewsClient      # 네이버 뉴스 검색 API
│   │   └── NewsAnalyzeClient    # AI 분석 서버
│   ├── crawler/
│   │   └── NaverNewsCrawler     # Jsoup 기반 뉴스 본문 크롤링
│   ├── scheduler/
│   │   └── NewsScheduler        # 30초마다 뉴스 수집
│   ├── cache/
│   │   └── ArticleFingerprint   # 중복 판별용 캐시
│   └── util/
│       ├── DateParser           # 날짜 파싱
│       ├── TitleSimilarity      # 제목 유사도 계산
│       └── NaverNewsUrlFilter   # 네이버 뉴스 URL 필터링
│
├── corpus/                 # 뉴스 코퍼스 백필
│   ├── controller/
│   ├── service/
│   ├── entity/
│   └── repository/
│
├── exception/              # 전역 예외 처리
│   ├── GlobalExceptionHandler
│   ├── NewsNotFoundException
│   └── NewsNotAnalyzedException
│
└── support/                # 공통 지원
    ├── HealthCheckController
    └── naver/
        └── NaverApiProperties
```

## 🚀 실행 방법

### 1. 환경 변수 설정

\`application.yml\` 또는 환경 변수로 설정:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/koscomdb
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false

  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4o-mini

naver:
  api:
    client-id: ${NAVER_CLIENT_ID}
    client-secret: ${NAVER_CLIENT_SECRET}

news:
  analyze:
    base-url: http://localhost:8000  # AI 분석 서버 주소
```

### 2. 데이터베이스 준비

```bash
# PostgreSQL 데이터베이스 생성
createdb koscomdb

# 스키마는 ddl-auto: create 로 자동 생성하거나
# 별도 DDL 스크립트 실행
```

### 3. 애플리케이션 실행

```bash
# 개발 환경 실행
./gradlew bootRun

# 프로덕션 빌드
./gradlew build
java -jar build/libs/koslink-0.0.1-SNAPSHOT.jar
```

### 4. 테스트

```bash
./gradlew test
```

## 📡 API 명세

### 1. 뉴스 리스트 조회

```http
GET /api/v1/news?cursorId={cursorId}&size={size}
```

**Query Parameters**
- \`cursorId\` (optional): 커서 ID (페이지네이션)
- \`size\` (optional, default=20): 페이지 크기

**Response**
```json
{
  "items": [
    {
      "newsId": 123,
      "title": "삼성전자, HBM 양산 시작",
      "link": "https://n.news.naver.com/...",
      "pubDate": "2026-07-28T10:00:00",
      "description": "...",
      "status": "ANALYZED"
    }
  ],
  "nextCursor": 100,
  "hasNext": true
}
```

### 2. 뉴스 영향도 분석 조회

```http
GET /api/v1/news/{newsId}/impact
```

**Path Parameters**
- \`newsId\`: 뉴스 ID

**Response**
```json
{
  "newsSummary": ["요약 문장 1", "요약 문장 2"],
  "source": {
    "press": "전자신문",
    "publishedAt": "2026-07-28T10:00:00",
    "url": "https://..."
  },
  "originStocks": [
    {
      "ticker": "005930",
      "name": "삼성전자",
      "status": "up",
      "reason": "HBM 양산 개시"
    }
  ],
  "relatedStocks": [
    {
      "ticker": "000660",
      "name": "SK하이닉스",
      "status": "down",
      "relationLabel": "경쟁사",
      "relationPath": "삼성전자 → SK하이닉스",
      "propagation": "경쟁사 양산으로 시장 점유율 하락 우려"
    }
  ],
  "finalSummary": "...",
  "graph": {
    "newsId": "123",
    "originId": "005930",
    "nodes": [...],
    "edges": [...]
  }
}
```

**Error Responses**
- \`404 NOT_FOUND\`: 뉴스가 존재하지 않음
- \`400 BAD_REQUEST\`: 뉴스 분석이 아직 완료되지 않음

### 3. 네이버 뉴스 검색

```http
GET /api/v1/news/search?query={query}&display={display}&start={start}&sort={sort}
```

## 🔄 시스템 아키텍처

### 뉴스 수집 및 분석 플로우

```
┌─────────────────┐
│ NewsScheduler   │ (30초마다 실행)
│ @Scheduled      │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ NaverNewsClient │ (Feign)
│ 네이버 뉴스 검색 │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ URL 필터링      │ (네이버 뉴스만 추출)
│ NaverNewsUrlFilter
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ LLM 필터링      │ (Spring AI)
│ NewsFilterService│ (반도체 관련 뉴스만)
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 중복 제거       │ (Caffeine Cache)
│ ArticleFingerprint│ (제목 유사도 + 해시)
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 본문 크롤링     │ (Jsoup)
│ NaverNewsCrawler│
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ News 엔티티     │
│ DB 저장         │ (상태: CRAWLED)
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ NewsAnalyzeClient│ (Feign, 비동기)
│ AI 분석 서버    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ AiResponse      │
│ DB 저장         │ (JSONB 필드)
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ News 상태 업데이트│ (상태: ANALYZED)
└─────────────────┘
```

### JSONB 데이터 처리

AiResponse 엔티티는 PostgreSQL JSONB 타입으로 유연한 구조를 저장하고,
JPA AttributeConverter를 통해 자동으로 DTO 객체로 변환됩니다.

```
DB (JSONB)  →  Converter  →  DTO Object  →  Service  →  API Response
```

**Converter 구현 클래스**
- \`NewsGraphConverter\`: graph JSONB → NewsGraphDto
- \`NewsSummaryConverter\`: news_summary JSONB → List<String>
- \`NewsSourceConverter\`: source JSONB → NewsSourceDto
- \`OriginStocksConverter\`: origin_stocks JSONB → List<OriginStockDto>
- \`RelatedStocksConverter\`: related_stocks JSONB → List<RelatedStockDto>

## 🗄️ 데이터베이스 스키마

### NEWS 테이블
| 컬럼 | 타입 | 설명 |
|------|------|------|
| news_id | BIGSERIAL | PK |
| title | TEXT | 뉴스 제목 |
| link | TEXT | 뉴스 URL |
| description | TEXT | 뉴스 요약 |
| pub_date | TIMESTAMP | 발행일 |
| body | TEXT | 본문 |
| press | VARCHAR(100) | 언론사 |
| status | VARCHAR(20) | 상태 (PENDING/FILTERED/CRAWLED/ANALYZED) |
| created_at | TIMESTAMP | 생성일 |
| updated_at | TIMESTAMP | 수정일 |

### AI_RESPONSE 테이블
| 컬럼 | 타입 | 설명 |
|------|------|------|
| response_id | BIGSERIAL | PK |
| news_id | BIGINT | FK (NEWS) |
| news_summary | JSONB | 뉴스 요약 리스트 |
| source | JSONB | 출처 정보 |
| origin_stocks | JSONB | 원천 종목 리스트 |
| related_stocks | JSONB | 관련 종목 리스트 |
| final_summary | TEXT | 최종 요약 |
| graph | JSONB | 종목 관계 그래프 |
| derived_companies | JSONB | 파생 기업 |
| key_companies | JSONB | 핵심 기업 |
| created_at | TIMESTAMP | 생성일 |

### NEWS_CORPUS 테이블
- 뉴스 코퍼스 원시 데이터 (분석/학습용)

## 🔧 주요 설계 결정

### 1. JPA AttributeConverter 패턴
- JSONB 데이터를 서비스 레이어에서 수동 파싱하지 않고 JPA 레벨에서 자동 변환
- 코드 간소화 및 타입 안정성 확보

### 2. Caffeine 인메모리 캐시
- 뉴스 중복 판별을 위해 제목 해시 + 본문 일부를 캐싱
- DB 조회 없이 빠른 중복 체크

### 3. 커서 기반 페이지네이션
- Offset 방식 대신 커서(마지막 ID) 기반으로 다음 페이지 조회
- 대량 데이터에서도 일정한 성능 보장

### 4. Spring Cloud OpenFeign
- 네이버 뉴스 API, AI 분석 서버 등 외부 API를 선언적으로 호출
- 재시도, 타임아웃 등 HTTP 클라이언트 설정 간소화

### 5. 뉴스 상태 관리 (NewsStatus)
```
PENDING   → 뉴스 API에서 받아온 초기 상태
FILTERED  → LLM 필터링 통과
CRAWLED   → 본문 크롤링 완료
ANALYZED  → AI 분석 완료
```

## 🧪 테스트

### 주요 테스트 케이스
- **NewsScheduler**: 중복 제거, URL 필터링, 캐시 워밍업
- **NaverNewsUrlFilter**: 유효한 네이버 뉴스 URL 판별
- **TitleSimilarity**: 제목 유사도 계산 (Jaccard, Levenshtein)

### 실행
```bash
./gradlew test
```

## 📝 개발 규칙

### 커밋 컨벤션
```
feat: 새로운 기능
fix: 버그 수정
refactor: 리팩터링
test: 테스트 추가/수정
docs: 문서 수정
chore: 빌드/설정 변경
```

### 브랜치 전략
- \`main\`: 프로덕션
- \`feat/{issue-number}-{feature-name}\`: 기능 개발
- \`fix/{issue-number}-{bug-name}\`: 버그 수정

## 📞 문의

이슈 및 PR은 GitHub을 통해 관리합니다.
