# WageManager ERD (Entity Relationship Diagram)

## 엔티티 관계도

```mermaid
erDiagram
    User ||--o| Employer : "is_a"
    User ||--o| Worker : "is_a"
    User ||--o| UserSettings : "has"

    Employer ||--o{ Workplace : "owns"
    Workplace ||--o{ WorkerContract : "has"
    Worker ||--o{ WorkerContract : "works_under"

    WorkerContract ||--o{ WorkSchedule : "has"
    WorkerContract ||--o{ WorkRecord : "has"
    WorkerContract ||--o{ Salary : "generates"

    WorkRecord ||--o{ CorrectionRequest : "can_request"

    Salary ||--|| Payment : "paid_through"

    User ||--o{ Notification : "receives"

    User {
        bigint id PK
        string kakao_id UK "카카오 소셜 ID"
        string name "이름"
        string phone "전화번호"
        enum user_type "USER_TYPE(EMPLOYER, WORKER)"
        string profile_image_url "프로필 이미지 URL"
        datetime created_at
        datetime updated_at
    }

    Employer {
        bigint id PK
        bigint user_id FK "User ID"
        string business_name "사업체명"
        datetime created_at
        datetime updated_at
    }

    Worker {
        bigint id PK
        bigint user_id FK "User ID"
        string worker_code UK "근로자 고유 6자리 코드"
        string account_number "계좌번호 (암호화)"
        string bank_name "은행명"
        string kakao_pay_link "카카오페이 송금 링크"
        datetime created_at
        datetime updated_at
    }

    Workplace {
        bigint id PK
        bigint employer_id FK "Employer ID"
        string business_number UK "사업자등록번호"
        string name "사업장명"
        string address "주소"
        string phone "전화번호"
        string color_code "캘린더 색상 코드 (#RRGGBB)"
        boolean is_active "활성화 상태"
        datetime created_at
        datetime updated_at
    }

    WorkerContract {
        bigint id PK
        bigint workplace_id FK "Workplace ID"
        bigint worker_id FK "Worker ID"
        decimal hourly_wage "시급"
        string work_days "근무요일 (JSON: [1,2,3,4,5])"
        time work_start_time "근무 시작 시간"
        time work_end_time "근무 종료 시간"
        date contract_start_date "계약 시작일"
        date contract_end_date "계약 종료일 (nullable)"
        int payment_day "급여 지급일(매월 N일)"
        boolean is_active "활성화 상태"
        datetime created_at
        datetime updated_at
    }

    WorkSchedule {
        bigint id PK
        bigint contract_id FK "WorkerContract ID"
        date work_date "근무 날짜"
        time scheduled_start_time "예정 시작 시간"
        time scheduled_end_time "예정 종료 시간"
        decimal scheduled_hours "예정 근무 시간"
        enum status "STATUS(SCHEDULED, MODIFIED, CANCELLED)"
        string memo "메모"
        datetime created_at
        datetime updated_at
    }

    WorkRecord {
        bigint id PK
        bigint contract_id FK "WorkerContract ID"
        bigint schedule_id FK "WorkSchedule ID (nullable)"
        date work_date "근무 날짜"
        time actual_start_time "실제 시작 시간"
        time actual_end_time "실제 종료 시간"
        decimal total_hours "총 근무 시간"
        decimal regular_hours "일반 근무 시간"
        decimal overtime_hours "연장 근무 시간"
        decimal night_hours "야간 근무 시간"
        decimal holiday_hours "휴일 근무 시간"
        enum status "STATUS(CONFIRMED, PENDING, DISPUTED)"
        string memo "메모"
        datetime created_at
        datetime updated_at
    }

    CorrectionRequest {
        bigint id PK
        bigint work_record_id FK "WorkRecord ID"
        bigint requester_id FK "User ID (요청자)"
        date requested_work_date "요청 근무 날짜"
        time requested_start_time "요청 시작 시간"
        time requested_end_time "요청 종료 시간"
        string reason "사유"
        enum status "STATUS(PENDING, APPROVED, REJECTED)"
        bigint reviewer_id FK "User ID (검토자, nullable)"
        datetime reviewed_at "검토 일시"
        string review_comment "검토 코멘트"
        datetime created_at
        datetime updated_at
    }

    Salary {
        bigint id PK
        bigint contract_id FK "WorkerContract ID"
        int year "연도"
        int month "월"
        decimal total_work_hours "총 근무 시간"
        decimal base_pay "기본급"
        decimal overtime_pay "연장 수당"
        decimal night_pay "야간 수당"
        decimal holiday_pay "휴일 수당"
        decimal total_gross_pay "총 지급액(세전)"
        decimal national_pension "국민연금"
        decimal health_insurance "건강보험"
        decimal employment_insurance "고용보험"
        decimal income_tax "소득세"
        decimal local_income_tax "지방소득세"
        decimal total_deduction "총 공제액"
        decimal net_pay "실수령액"
        date payment_due_date "지급 예정일"
        datetime created_at
        datetime updated_at
    }

    Payment {
        bigint id PK
        bigint salary_id FK "Salary ID"
        enum payment_method "METHOD(KAKAO_PAY, BANK_TRANSFER, CASH)"
        enum status "STATUS(PENDING, COMPLETED, FAILED, HOLD)"
        datetime payment_date "송금 일시"
        string transaction_id "거래 ID"
        string failure_reason "실패 사유"
        datetime created_at
        datetime updated_at
    }

    Notification {
        bigint id PK
        bigint user_id FK "User ID"
        enum type "TYPE(SCHEDULE_CHANGE, CORRECTION_REQUEST, CORRECTION_RESPONSE, PAYMENT_DUE, PAYMENT_SUCCESS, PAYMENT_FAILED)"
        string title "제목"
        string message "내용"
        string link_url "관련 링크"
        boolean is_read "읽음 여부"
        datetime read_at "읽은 일시"
        datetime created_at
    }

    UserSettings {
        bigint id PK
        bigint user_id FK "User ID"
        boolean notification_enabled "알림 활성화"
        boolean push_enabled "푸시 알림"
        boolean email_enabled "이메일 알림"
        boolean sms_enabled "SMS 알림"
        boolean schedule_change_alert "일정 변경 알림"
        boolean payment_alert "송금 알림"
        boolean correction_request_alert "정정 요청 알림"
        datetime created_at
        datetime updated_at
    }
```

## 엔티티 설명

### 1. User (사용자)
- 고용주와 근로자의 공통 사용자 정보
- 카카오 소셜 로그인 연동
- user_type으로 역할 구분 (EMPLOYER, WORKER)
- 프로필 이미지 URL 추가

### 2. Employer (고용주)
- User를 상속받는 고용주 전용 정보
- 사업체 관련 정보 관리

### 3. Worker (근로자)
- User를 상속받는 근로자 전용 정보
- 6자리 고유 식별코드 자동 생성
- 급여 수령을 위한 계좌/카카오페이 정보
- 계좌번호는 AES-256 암호화 저장

### 4. Workplace (사업장)
- 고용주가 운영하는 사업장 정보
- 사업자등록번호로 유효성 검증
- **color_code**: 캘린더에서 근무지별 색상 구분을 위한 필드

### 5. WorkerContract (근로 계약)
- 사업장과 근로자 간의 근로 계약
- 시급, 근무 시간, 지급일 등 계약 조건
- work_days는 JSON 배열로 저장 (예: [1,2,3,4,5] = 월~금)

### 6. WorkSchedule (근무 일정)
- 예정된 근무 스케줄
- 계약 기반으로 자동 생성 또는 수동 등록
- **scheduled_hours**: 예정 근무 시간 추가
- **memo**: 일정 메모 추가

### 7. WorkRecord (근무 기록)
- 실제 출퇴근 기록
- 일반/연장/야간/휴일 근무 시간 자동 계산
- **memo**: 근무 메모 추가
- STATUS: CONFIRMED(확정), PENDING(대기), DISPUTED(분쟁)

### 8. CorrectionRequest (정정 요청)
- 근로자가 근무 기록 수정을 요청
- 고용주가 승인/반려 처리
- **requested_work_date**: 수정 요청 날짜 추가
- 화면에서 보이는 "시작 시간", "종료 시간", "사유" 필드 포함

### 9. Salary (급여)
- 월별 급여 정산 내역
- 기본급, 각종 수당, 4대 보험 및 세금 공제 포함
- **total_work_hours**: 총 근무 시간 추가
- **local_income_tax**: 지방소득세 추가

### 10. Payment (송금)
- 급여 송금 내역 및 상태 관리
- 카카오페이, 계좌이체 등 다양한 방식 지원
- **HOLD**: 보류 상태 추가 (화면의 "보류" 상태)

### 11. Notification (알림)
- 사용자별 알림 내역
- **CORRECTION_RESPONSE**: 정정 요청 응답 알림 타입 추가
- 일정 변경, 정정 요청, 송금 등 다양한 이벤트

### 12. UserSettings (사용자 설정) - 신규 추가
- 사용자별 알림 설정 관리
- 푸시, 이메일, SMS 알림 개별 설정
- 알림 유형별 활성화/비활성화 설정

## 주요 관계

1. **User ↔ Employer/Worker**: 상속 관계 (Single Table or Joined)
2. **User → UserSettings**: 1:1 (사용자 설정)
3. **Employer → Workplace**: 1:N (한 고용주가 여러 사업장 운영)
4. **Workplace ↔ Worker → WorkerContract**: N:M (다대다 관계를 계약으로 해소)
5. **WorkerContract → WorkSchedule/WorkRecord/Salary**: 1:N
6. **WorkRecord → CorrectionRequest**: 1:N
7. **Salary ↔ Payment**: 1:1

## 화면 설계 반영 사항

### 고용주 화면 반영
1. **주간/월간 캘린더**: WorkSchedule, WorkRecord 엔티티로 구현
2. **근무지별 색상**: Workplace.color_code 필드 추가
3. **월급 관리**: Salary 엔티티의 월별 데이터 조회
4. **근무 통계**: Salary의 year, month 기반 집계 데이터

### 근로자 화면 반영
1. **근무 일정 확인**: WorkSchedule 조회
2. **수정 요청**: CorrectionRequest 엔티티 (날짜, 시작/종료 시간, 사유)
3. **월별 급여 통계**: Salary 데이터를 차트로 시각화
4. **알림 설정**: UserSettings 엔티티로 관리

## 인덱스 전략

- User: kakao_id (UK), user_type
- Worker: worker_code (UK), user_id
- Workplace: business_number (UK), employer_id, is_active
- WorkerContract: workplace_id, worker_id, is_active
- WorkSchedule: contract_id, work_date, status
- WorkRecord: contract_id, work_date, status
- CorrectionRequest: work_record_id, requester_id, status
- Salary: contract_id, (year, month) composite index
- Payment: salary_id, status
- Notification: user_id, is_read, created_at
- UserSettings: user_id (UK)

## 보안 고려사항

- 급여 데이터: AES-256 암호화
- 계좌번호: AES-256 암호화 저장
- 통신: HTTPS (TLS 1.3)
- 접근 제어:
  - 고용주는 자신의 사업장 데이터만 접근
  - 근로자는 자신의 근무 기록만 접근
  - JWT 기반 인증 및 권한 관리
- 민감 정보 로깅 금지
- 최소 1년 이상 이력 로그 보관

## API 설계 고려사항

### 고용주 API
- `GET /api/employer/workplaces` - 근무지 목록 조회
- `GET /api/employer/workplaces/{id}/workers` - 근무지별 근로자 조회
- `GET /api/employer/schedules?workplace_id={id}&year={year}&month={month}` - 캘린더 조회
- `PUT /api/employer/schedules/{id}` - 근무 일정 수정
- `GET /api/employer/salaries?workplace_id={id}&year={year}&month={month}` - 급여 관리
- `GET /api/employer/correction-requests` - 정정 요청 목록
- `PUT /api/employer/correction-requests/{id}` - 정정 요청 승인/반려

### 근로자 API
- `GET /api/worker/schedules?year={year}&month={month}` - 근무 일정 조회
- `GET /api/worker/work-records?year={year}&month={month}` - 근무 기록 조회
- `POST /api/worker/correction-requests` - 정정 요청 생성
- `GET /api/worker/salaries?year={year}` - 월별 급여 통계
- `GET /api/worker/payments` - 송금 내역 조회

### 공통 API
- `GET /api/notifications` - 알림 목록
- `PUT /api/notifications/{id}/read` - 알림 읽음 처리
- `GET /api/user/settings` - 사용자 설정 조회
- `PUT /api/user/settings` - 사용자 설정 수정
