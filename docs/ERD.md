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

    WorkerContract ||--o{ WorkRecord : "has"
    WorkerContract ||--o{ Salary : "generates"
    WorkerContract ||--o{ WeeklyAllowance : "has"

    WeeklyAllowance ||--o{ WorkRecord : "includes"

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
        string phone "전화번호"
        datetime created_at
        datetime updated_at
    }

    Worker {
        bigint id PK
        bigint user_id FK "User ID"
        string worker_code UK "근로자 고유 6자리 코드"
        string bank_name "은행명"
        string account_number "계좌번호 (암호화)"
        datetime created_at
        datetime updated_at
    }

    Workplace {
        bigint id PK
        bigint employer_id FK "Employer ID"
        string business_number UK "사업자등록번호"
        string business_name "사업장명"
        string name "지점명/별칭"
        string address "주소"
        string color_code "캘린더 색상 코드 (#RRGGBB)"
        boolean is_less_than_five_employees "5인 미만 사업장 여부"
        boolean is_active "활성화 상태"
        datetime created_at
        datetime updated_at
    }

    WorkerContract {
        bigint id PK
        bigint workplace_id FK "Workplace ID"
        bigint worker_id FK "Worker ID"
        decimal hourly_wage "시급"
        string work_schedules "근무 스케줄 (JSON: [{dayOfWeek, startTime, endTime}, ...])"
        date contract_start_date "계약 시작일"
        date contract_end_date "계약 종료일 (nullable)"
        int payment_day "급여 지급일(매월 N일)"
        enum payroll_deduction_type "급여공제유형 (FREELANCER, PART_TIME_NONE 등)"
        boolean is_active "활성화 상태"
        datetime created_at
        datetime updated_at
    }

    WorkRecord {
        bigint id PK
        bigint contract_id FK "WorkerContract ID"
        bigint weekly_allowance_id FK "WeeklyAllowance ID"
        date work_date "근무 날짜"
        time start_time "시작 시간"
        time end_time "종료 시간"
        int break_minutes "휴식 시간 (분)"
        int total_work_minutes "실제 근무 시간 (분)"
        decimal total_hours "총 근무 시간"
        decimal regular_hours "일반 근무 시간"
        decimal night_hours "야간 근무 시간"
        decimal holiday_hours "휴일 근무 시간"
        decimal base_salary "당일 기본급"
        decimal night_salary "당일 야간수당"
        decimal holiday_salary "당일 휴일수당"
        decimal total_salary "당일 총급여"
        enum status "STATUS(SCHEDULED, COMPLETED, DELETED)"
        boolean is_modified "수정 여부"
        string memo "메모"
        datetime created_at
        datetime updated_at
    }

    WeeklyAllowance {
        bigint id PK
        bigint contract_id FK "WorkerContract ID"
        decimal total_work_hours "주간 총 근무 시간"
        decimal weekly_paid_leave_amount "주휴수당 금액"
        decimal overtime_hours "연장근로 시간 (주 40시간 초과)"
        decimal overtime_amount "연장수당 금액"
        datetime created_at
        datetime updated_at
    }

    CorrectionRequest {
        bigint id PK
        enum type "TYPE(CREATE, UPDATE, DELETE)"
        bigint work_record_id FK "WorkRecord ID (UPDATE/DELETE용, nullable)"
        bigint contract_id FK "WorkerContract ID (CREATE용, nullable)"
        bigint requester_id FK "User ID (요청자)"
        date original_work_date "원본 근무 날짜 (UPDATE/DELETE용)"
        time original_start_time "원본 시작 시간 (UPDATE/DELETE용)"
        time original_end_time "원본 종료 시간 (UPDATE/DELETE용)"
        date requested_work_date "요청 근무 날짜"
        time requested_start_time "요청 시작 시간"
        time requested_end_time "요청 종료 시간"
        int requested_break_minutes "요청 휴식시간 (분)"
        string requested_memo "요청 메모"
        enum status "STATUS(PENDING, APPROVED, REJECTED)"
        bigint reviewer_id FK "User ID (검토자, nullable)"
        datetime reviewed_at "검토 일시"
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
        decimal four_major_insurance "4대 보험 (국민연금+건강보험+고용보험+산재보험)"
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
        bigint salary_id FK UK "Salary ID (1:1 관계)"
        enum payment_method "METHOD(TOSS_DEEP_LINK, BANK_TRANSFER, CASH)"
        enum status "STATUS(PENDING, COMPLETED, FAILED)"
        datetime payment_date "송금 일시"
        string transaction_id "거래 ID"
        string failure_reason "실패 사유"
        datetime created_at
        datetime updated_at
    }

    Notification {
        bigint id PK
        bigint user_id FK "User ID"
        enum type "TYPE(SCHEDULE_CREATED, CORRECTION_REQUEST, CORRECTION_RESPONSE, PAYMENT_SUCCESS 등)"
        string title "제목"
        enum action_type "ACTION_TYPE(NONE, VIEW_WORK_RECORD, VIEW_CORRECTION 등)"
        string action_data "액션 데이터 (JSON 형식)"
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
        boolean schedule_change_alert_enabled "일정 변경 알림"
        boolean payment_alert_enabled "송금 알림"
        boolean correction_request_alert_enabled "정정 요청 알림"
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
- **phone**: 고용주 연락처 (모든 사업장에 공통 사용)

### 3. Worker (근로자)
- User를 상속받는 근로자 전용 정보
- 6자리 고유 식별코드 자동 생성
- 급여 수령을 위한 계좌 정보 (토스 딥링크는 서버에서 계좌 정보를 바탕으로 자동 생성)
- 계좌번호는 AES-256 암호화 저장

### 4. Workplace (사업장)
- 고용주가 운영하는 사업장 정보
- 사업자등록번호로 유효성 검증
- **business_name**: 사업장명 (법인명 등)
- **name**: 지점명 또는 별칭 (예: "홍대점", "강남점")
- **color_code**: 캘린더에서 근무지별 색상 구분을 위한 필드
- **is_less_than_five_employees**: 5인 미만 사업장 여부 (4대보험 가입 의무가 다름)
- 한 고용주가 여러 사업장을 운영할 수 있음

### 5. WorkerContract (근로 계약)
- 사업장과 근로자 간의 근로 계약
- 시급, 근무요일, 지급일 등 계약 조건
- **work_schedules**: JSON 형식으로 저장 (예: [{"dayOfWeek": 1, "startTime": "09:00", "endTime": "18:00"}, ...])
  - dayOfWeek: 1=월요일 ~ 7=일요일
  - 요일별로 다른 시작/종료 시간 설정 가능
- **payroll_deduction_type**: 급여 공제 유형
  - FREELANCER: 소득세만 (3.3%)
  - PART_TIME_NONE: 공제 없음
  - PART_TIME_TAX_ONLY: 소득세만
  - PART_TIME_TAX_AND_INSURANCE: 소득세 + 4대보험

### 6. WorkRecord (근무 기록 및 일정)
- **WorkSchedule 통합**: 예정된 근무 일정과 실제 근무 기록을 하나의 엔티티로 관리
- **start_time/end_time**: 근무 시간 (등록 시 예정 시간, 수정 시 실제 시간으로 업데이트)
- **break_minutes**: 휴식 시간 (분 단위)
- **total_work_minutes**: 실제 근무 시간 (분 단위)
- **total_hours**: 총 근무 시간 (start_time ~ end_time - break_minutes 기준으로 자동 계산)
- 일반/야간/휴일 근무 시간 자동 계산
- **급여 필드**: base_salary(기본급), night_salary(야간수당), holiday_salary(휴일수당), total_salary(당일 총급여)
- **STATUS**:
  - SCHEDULED: 예정 (근무 전)
  - COMPLETED: 완료 (근무 후)
  - DELETED: 삭제됨 (소프트 삭제)
- **is_modified**: 수정 여부 (근무 전/후 상관없이 시간 변경 시 true)
- **weekly_allowance_id**: 주간 수당 집계를 위한 WeeklyAllowance 참조
- **주의**: 연장수당(overtime)은 WorkRecord가 아닌 WeeklyAllowance에서 주 단위로 계산됨

### 7. WeeklyAllowance (주간 수당)
- 주 단위로 근무 기록을 집계하여 주휴수당 및 연장수당 계산
- **total_work_hours**: 주간 총 근무 시간 (WorkRecord 합산)
- **weekly_paid_leave_amount**: 주휴수당 금액
  - 주 15시간 이상 근무 시 지급
  - 계산식: (1주 소정근로 시간 / 40) × 8 × 시급
- **overtime_hours**: 연장근로 시간 (주 40시간 초과 시)
- **overtime_amount**: 연장수당 금액
  - 계산식: 초과 시간 × 기본시급 × 1.5배율
- WorkRecord와 1:N 관계로 해당 주의 모든 근무 기록을 참조

### 8. CorrectionRequest (정정 요청)
- 근무 기록의 생성/수정/삭제를 요청하는 통합 시스템
- **type**: 요청 유형
  - CREATE: 근무 기록 생성 요청 (workRecord가 없을 때)
  - UPDATE: 근무 기록 수정 요청 (기존 workRecord 수정)
  - DELETE: 근무 기록 삭제 요청
- **work_record_id**: UPDATE/DELETE 요청의 대상 근무 기록
- **contract_id**: CREATE 요청 시 사용 (workRecord가 아직 없으므로 계약 참조)
- 원본 정보(original_*)와 요청 정보(requested_*)를 모두 저장하여 변경사항 추적
- 고용주가 승인/반려 처리

### 9. Salary (급여)
- 월별 급여 정산 내역
- 기본급, 각종 수당, 4대 보험 및 세금 공제 포함
- **total_work_hours**: 총 근무 시간
- **four_major_insurance**: 4대 보험 통합 (국민연금+건강보험+고용보험+산재보험)
- **local_income_tax**: 지방소득세
- **급여 공제 계산**: `DeductionCalculator` 클래스를 통해 자동 계산
  - `PayrollDeductionType` enum으로 4가지 급여 공제 유형 지원
  - 4대 보험 및 세금 자동 계산

### 10. Payment (송금)
- 급여 송금 내역 및 상태 관리
- 토스 딥링크, 계좌이체 등 다양한 방식 지원
- **상태**: PENDING(대기), COMPLETED(완료), FAILED(실패)
- **자동 실패 처리**: 매일 자정 스케줄러(`PaymentAutoFailScheduler`)가 실행되어 지급 예정일(payment_due_date)이 경과한 PENDING 상태의 결제를 자동으로 FAILED 처리

### 11. Notification (알림)
- 사용자별 알림 내역
- **type**: 알림 유형
  - SCHEDULE_CREATED, SCHEDULE_APPROVAL_REQUEST, SCHEDULE_APPROVED, SCHEDULE_REJECTED, SCHEDULE_DELETED
  - CORRECTION_REQUEST, CORRECTION_RESPONSE
  - PAYMENT_SUCCESS, PAYMENT_FAILED
- **action_type**: 알림 클릭 시 수행할 액션 유형
  - NONE: 액션 없음
  - VIEW_WORK_RECORD: 근무 기록 상세 보기
  - VIEW_CORRECTION: 정정 요청 상세 보기
  - 등
- **action_data**: 액션 수행에 필요한 데이터를 JSON 형식으로 저장
  - 예: `{"workRecordId": 123}`, `{"correctionRequestId": 456}`

### 12. UserSettings (사용자 설정)
- 사용자별 알림 설정 관리
- 푸시, 이메일, SMS 알림 개별 설정
- 알림 유형별 활성화/비활성화 설정

## 주요 관계

1. **User ↔ Employer/Worker**: 상속 관계 (Single Table or Joined)
2. **User → UserSettings**: 1:1 (사용자 설정)
3. **Employer → Workplace**: 1:N (한 고용주가 여러 사업장 운영)
4. **Workplace ↔ Worker → WorkerContract**: N:M (다대다 관계를 계약으로 해소)
5. **WorkerContract → WorkRecord/Salary/WeeklyAllowance**: 1:N
6. **WeeklyAllowance → WorkRecord**: 1:N (주 단위 근무 기록 집계)
7. **WorkRecord → CorrectionRequest**: 1:N
8. **Salary ↔ Payment**: 1:1

## 화면 설계 반영 사항

### 고용주 화면 반영
1. **주간/월간 캘린더**: WorkRecord 엔티티로 구현 (상태별 표시: SCHEDULED/COMPLETED, 수정 여부: is_modified)
2. **근무지별 색상**: Workplace.color_code 필드 추가
3. **근무 일정 등록**: 고용주가 WorkRecord를 SCHEDULED 상태로 생성
4. **근무 일정 수정**: 시간 수정 시 is_modified를 true로 설정
5. **월급 관리**: Salary 엔티티의 월별 데이터 조회
6. **근무 통계**: Salary의 year, month 기반 집계 데이터

### 근로자 화면 반영
1. **근무 일정 확인**: WorkRecord 조회 (status별 필터링)
2. **근무 완료 처리**: 근무 완료 시 COMPLETED 상태로 변경
3. **수정 요청**: CorrectionRequest 엔티티 (날짜, 시작/종료 시간, 사유), 승인 시 is_modified true로 변경
4. **월별 급여 통계**: Salary 데이터를 차트로 시각화
5. **알림 설정**: UserSettings 엔티티로 관리

## 인덱스 전략

- User: kakao_id (UK), user_type
- Worker: worker_code (UK), user_id
- Workplace: business_number (UK), employer_id, is_active
- WorkerContract: workplace_id, worker_id, is_active
- WorkRecord: contract_id, work_date, status (복합 인덱스: contract_id + work_date + status)
- WeeklyAllowance: contract_id
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
- `GET /api/employer/work-records?workplace_id={id}&year={year}&month={month}` - 캘린더 조회
- `POST /api/employer/work-records` - 근무 일정 등록 (SCHEDULED 상태로 생성)
- `PUT /api/employer/work-records/{id}` - 근무 시간 수정 (is_modified를 true로 설정)
- `PUT /api/employer/work-records/{id}/complete` - 근무 완료 처리 (SCHEDULED → COMPLETED)
- `GET /api/employer/salaries?workplace_id={id}&year={year}&month={month}` - 급여 관리
- `GET /api/employer/correction-requests` - 정정 요청 목록
- `PUT /api/employer/correction-requests/{id}` - 정정 요청 승인/반려 (승인 시 is_modified를 true로 설정)

### 근로자 API
- `GET /api/worker/work-records?year={year}&month={month}` - 근무 일정 및 기록 조회
- `PUT /api/worker/work-records/{id}/complete` - 근무 완료 처리 (SCHEDULED → COMPLETED)
- `POST /api/worker/correction-requests` - 정정 요청 생성 (근무 완료 후 수정 요청)
- `GET /api/worker/salaries?year={year}` - 월별 급여 통계
- `GET /api/worker/payments` - 송금 내역 조회

### 공통 API
- `GET /api/notifications` - 알림 목록
- `PUT /api/notifications/{id}/read` - 알림 읽음 처리
- `GET /api/user/settings` - 사용자 설정 조회
- `PUT /api/user/settings` - 사용자 설정 수정
