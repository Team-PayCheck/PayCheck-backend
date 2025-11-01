# WageManager 사용자 플로우

## 목차
1. [고용주 플로우](#고용주-플로우)
2. [근로자 플로우](#근로자-플로우)
3. [통합 시나리오](#통합-시나리오)

---

## 고용주 플로우

### 사례: 박성호 사장님 (카페 운영자, 아르바이트생 5명 고용)

#### 1️⃣ 회원가입 및 초기 설정

```
1. 카카오 소셜 로그인으로 회원가입
   - 이름: 박성호
   - 전화번호: 010-1234-5678
   - 역할: 고용주(EMPLOYER)

2. 사업장 등록
   - 사업자등록번호: 123-45-67890 (유효성 검증)
   - 사업장명: (주)카페모카
   - 지점명: 홍대점
   - 주소: 서울시 마포구 홍익로 123
   - 색상: #FF6B6B (캘린더에서 빨간색으로 표시)
```

**데이터베이스 변화:**
```sql
-- User 테이블
INSERT INTO User (kakao_id, name, phone, user_type)
VALUES ('kakao_12345', '박성호', '010-1234-5678', 'EMPLOYER');

-- Employer 테이블
INSERT INTO Employer (user_id, phone)
VALUES (1, '010-1234-5678');

-- Workplace 테이블
INSERT INTO Workplace (employer_id, business_number, business_name, name, address, color_code)
VALUES (1, '123-45-67890', '(주)카페모카', '홍대점', '서울시 마포구 홍익로 123', '#FF6B6B');
```

---

#### 2️⃣ 근로자 등록 및 계약 체결

```
박성호 사장님이 아르바이트생 김민지를 등록합니다.

1. 김민지의 고유 식별코드 입력: ABC123
   - 시스템이 근로자 정보 조회

2. 근로 계약 정보 입력
   - 시급: 10,000원
   - 근무요일: [1,2,3,4,5] (월~금)
   - 계약 시작일: 2025-11-01
   - 급여 지급일: 매월 25일
```

**데이터베이스 변화:**
```sql
-- WorkerContract 테이블
INSERT INTO WorkerContract
  (workplace_id, worker_id, hourly_wage, work_days, contract_start_date, payment_day, is_active)
VALUES
  (1, 1, 10000, '[1,2,3,4,5]', '2025-11-01', 25, true);
```

---

#### 3️⃣ 근무 일정 등록

```
박성호 사장님이 11월 한 달 근무 일정을 등록합니다.

예시: 2025년 11월 1일 (금요일)
- 근무자: 김민지
- 시작 시간: 09:00
- 종료 시간: 14:00
- 예정 근무 시간: 5시간
```

**데이터베이스 변화:**
```sql
-- WorkSchedule 테이블
INSERT INTO WorkSchedule
  (contract_id, work_date, scheduled_start_time, scheduled_end_time, scheduled_hours, status)
VALUES
  (1, '2025-11-01', '09:00', '14:00', 5.0, 'SCHEDULED');
```

---

#### 4️⃣ 근무 기록 확정 및 정정 요청 처리

```
시나리오: 김민지가 11월 1일 실제 근무 후, 시간 정정을 요청했습니다.

1. 초기 근무 기록 (자동 생성 또는 고용주 입력)
   - 실제 시작: 09:00
   - 실제 종료: 14:00
   - 총 근무: 5시간

2. 김민지의 정정 요청
   - 요청 내용: "실제로 14:30까지 근무했습니다"
   - 요청 시작: 09:00
   - 요청 종료: 14:30
   - 사유: "손님이 많아서 30분 연장 근무했습니다"

3. 박성호 사장님의 승인
   - 상태: APPROVED
   - 검토 코멘트: "확인했습니다. 감사합니다"
```

**데이터베이스 변화:**
```sql
-- 1. 초기 WorkRecord
INSERT INTO WorkRecord
  (contract_id, schedule_id, work_date, actual_start_time, actual_end_time,
   total_hours, regular_hours, status)
VALUES
  (1, 1, '2025-11-01', '09:00', '14:00', 5.0, 5.0, 'PENDING');

-- 2. CorrectionRequest 생성
INSERT INTO CorrectionRequest
  (work_record_id, requester_id, requested_work_date, requested_start_time,
   requested_end_time, reason, status)
VALUES
  (1, 2, '2025-11-01', '09:00', '14:30', '손님이 많아서 30분 연장 근무했습니다', 'PENDING');

-- 3. 승인 후 업데이트
UPDATE CorrectionRequest
SET status = 'APPROVED',
    reviewer_id = 1,
    reviewed_at = NOW(),
    review_comment = '확인했습니다. 감사합니다'
WHERE id = 1;

UPDATE WorkRecord
SET actual_end_time = '14:30',
    total_hours = 5.5,
    regular_hours = 5.5,
    status = 'CONFIRMED'
WHERE id = 1;
```

---

#### 5️⃣ 월급 계산 및 정산

```
11월 30일, 시스템이 자동으로 김민지의 월급을 계산합니다.

계산 내역:
- 총 근무 시간: 110시간
- 기본급: 1,100,000원 (110시간 × 10,000원)
- 연장 수당: 50,000원
- 야간 수당: 30,000원
- 총 지급액(세전): 1,180,000원
- 4대 보험: -120,000원
- 소득세: -20,000원
- 지방소득세: -2,000원
- 총 공제액: -142,000원
- 실수령액: 1,038,000원
```

**데이터베이스 변화:**
```sql
-- Salary 테이블
INSERT INTO Salary
  (contract_id, year, month, total_work_hours, base_pay, overtime_pay, night_pay,
   total_gross_pay, four_major_insurance, income_tax, local_income_tax,
   total_deduction, net_pay, payment_due_date)
VALUES
  (1, 2025, 11, 110, 1100000, 50000, 30000,
   1180000, 120000, 20000, 2000,
   142000, 1038000, '2025-11-25');
```

---

#### 6️⃣ 급여 송금

```
11월 25일, 박성호 사장님이 김민지에게 급여를 송금합니다.

1. 송금 화면에서 "카카오페이 송금" 선택
2. 김민지의 카카오페이 링크 열림
3. 송금 완료 후 상태 업데이트
```

**데이터베이스 변화:**
```sql
-- Payment 테이블
INSERT INTO Payment
  (salary_id, payment_method, status, payment_date, transaction_id)
VALUES
  (1, 'KAKAO_PAY', 'COMPLETED', NOW(), 'KP20251125ABC123');

-- Notification 생성 (근로자에게)
INSERT INTO Notification
  (user_id, type, title, message, is_read)
VALUES
  (2, 'PAYMENT_SUCCESS', '급여 입금 완료',
   '1,038,000원이 입금되었습니다.', false);
```

---

## 근로자 플로우

### 사례: 김민지 (24세, 대학생, 카페 아르바이트)

#### 1️⃣ 회원가입 및 코드 발급

```
1. 카카오 소셜 로그인으로 회원가입
   - 이름: 김민지
   - 전화번호: 010-9876-5432
   - 역할: 근로자(WORKER)

2. 시스템이 자동으로 고유 식별코드 생성
   - 코드: ABC123

3. 계좌 정보 입력 (선택)
   - 은행: 카카오뱅크
   - 계좌번호: 3333-12-1234567 (암호화 저장)
   - 카카오페이 링크: https://qr.kakaopay.com/abc123
```

**데이터베이스 변화:**
```sql
-- User 테이블
INSERT INTO User (kakao_id, name, phone, user_type)
VALUES ('kakao_67890', '김민지', '010-9876-5432', 'WORKER');

-- Worker 테이블
INSERT INTO Worker
  (user_id, worker_code, account_number, bank_name, kakao_pay_link)
VALUES
  (2, 'ABC123', 'ENCRYPTED(3333-12-1234567)', '카카오뱅크',
   'https://qr.kakaopay.com/abc123');
```

---

#### 2️⃣ 근무 일정 확인

```
김민지가 11월 첫째 주 일정을 확인합니다.

캘린더 화면:
- 11월 1일 (금): 09:00~14:00 (5시간) - 홍대점 (빨간색)
- 11월 4일 (월): 09:00~14:00 (5시간) - 홍대점 (빨간색)
- 11월 5일 (화): 09:00~14:00 (5시간) - 홍대점 (빨간색)
- 11월 6일 (수): 09:00~14:00 (5시간) - 홍대점 (빨간색)
- 11월 7일 (목): 09:00~14:00 (5시간) - 홍대점 (빨간색)
```

**API 호출:**
```
GET /api/worker/schedules?year=2025&month=11

Response:
[
  {
    "work_date": "2025-11-01",
    "scheduled_start_time": "09:00",
    "scheduled_end_time": "14:00",
    "scheduled_hours": 5.0,
    "workplace": {
      "name": "홍대점",
      "color_code": "#FF6B6B"
    },
    "status": "SCHEDULED"
  },
  ...
]
```

---

#### 3️⃣ 근무 시간 정정 요청

```
시나리오: 11월 1일, 김민지가 실제로 30분 더 근무했습니다.

1. 근무 기록 화면에서 11월 1일 선택
2. "수정 요청" 버튼 클릭
3. 정정 요청 양식 작성
   - 날짜: 2025-11-01
   - 시작 시간: 09:00
   - 종료 시간: 14:30 (기존 14:00 → 14:30)
   - 사유: "손님이 많아서 30분 연장 근무했습니다"
4. 요청 전송
```

**API 호출:**
```
POST /api/worker/correction-requests

Request Body:
{
  "work_record_id": 1,
  "requested_work_date": "2025-11-01",
  "requested_start_time": "09:00",
  "requested_end_time": "14:30",
  "reason": "손님이 많아서 30분 연장 근무했습니다"
}

Response:
{
  "id": 1,
  "status": "PENDING",
  "message": "정정 요청이 전송되었습니다."
}
```

**알림 발생:**
```sql
-- Notification 생성 (고용주에게)
INSERT INTO Notification
  (user_id, type, title, message, link_url, is_read)
VALUES
  (1, 'CORRECTION_REQUEST', '근무 시간 정정 요청',
   '김민지님이 11월 1일 근무 시간 정정을 요청했습니다.',
   '/correction-requests/1', false);
```

---

#### 4️⃣ 월별 급여 통계 확인

```
김민지가 2025년 월별 급여 통계를 확인합니다.

통계 화면 (막대 그래프):
- 1월: 950,000원
- 2월: 980,000원
- 3월: 1,020,000원
- ...
- 11월: 1,038,000원

상세 내역 (11월):
- 총 근무 시간: 110시간
- 기본급: 1,100,000원
- 연장 수당: 50,000원
- 야간 수당: 30,000원
- 총 지급액(세전): 1,180,000원
- 4대 보험: -120,000원
- 소득세: -20,000원
- 지방소득세: -2,000원
- 실수령액: 1,038,000원
```

**API 호출:**
```
GET /api/worker/salaries?year=2025

Response:
[
  {
    "month": 1,
    "net_pay": 950000,
    "total_work_hours": 95
  },
  ...
  {
    "month": 11,
    "net_pay": 1038000,
    "total_work_hours": 110,
    "detail": {
      "base_pay": 1100000,
      "overtime_pay": 50000,
      "night_pay": 30000,
      "total_gross_pay": 1180000,
      "four_major_insurance": 120000,
      "income_tax": 20000,
      "local_income_tax": 2000,
      "total_deduction": 142000
    }
  }
]
```

---

#### 5️⃣ 송금 확인 및 알림

```
11월 25일, 김민지가 급여 입금 알림을 받습니다.

알림 내용:
- 제목: "급여 입금 완료"
- 메시지: "1,038,000원이 입금되었습니다."
- 시간: 2025-11-25 14:32

송금 내역 화면:
- 근무지: 홍대점
- 지급월: 2025년 11월
- 지급액: 1,038,000원
- 송금 방법: 카카오페이
- 상태: 완료
- 송금일: 2025-11-25
```

---

## 통합 시나리오

### 시나리오: 일정 변경 및 실시간 알림

```
상황: 11월 10일, 박성호 사장님이 갑자기 김민지의 11월 15일 근무 시간을 변경합니다.

1. 고용주 측 (박성호 사장님)
   - 캘린더에서 11월 15일 선택
   - 근무 시간 변경: 09:00~14:00 → 10:00~15:00
   - 메모 추가: "재고 정리 예정"
   - 저장 버튼 클릭

2. 시스템 처리
   - WorkSchedule 업데이트
   - 상태를 MODIFIED로 변경
   - 근로자에게 알림 전송

3. 근로자 측 (김민지)
   - 푸시 알림 수신: "근무 일정이 변경되었습니다"
   - 앱에서 확인:
     * 변경 전: 09:00~14:00
     * 변경 후: 10:00~15:00
     * 사장님 메모: "재고 정리 예정"
```

**데이터베이스 변화:**
```sql
-- WorkSchedule 업데이트
UPDATE WorkSchedule
SET scheduled_start_time = '10:00',
    scheduled_end_time = '15:00',
    status = 'MODIFIED',
    memo = '재고 정리 예정',
    updated_at = NOW()
WHERE id = 15;

-- Notification 생성
INSERT INTO Notification
  (user_id, type, title, message, link_url, is_read)
VALUES
  (2, 'SCHEDULE_CHANGE', '근무 일정 변경',
   '11월 15일 근무 시간이 10:00~15:00로 변경되었습니다.',
   '/schedules/2025/11', false);
```

---

### 시나리오: 다중 사업장 관리

```
상황: 박성호 사장님이 강남에 두 번째 지점을 오픈합니다.

1. 새 사업장 등록
   - 사업자등록번호: 987-65-43210
   - 사업장명: (주)카페모카
   - 지점명: 강남점
   - 주소: 서울시 강남구 테헤란로 456
   - 색상: #4ECDC4 (청록색)

2. 강남점에 새 근로자 등록
   - 이름: 이영희
   - 식별코드: DEF456
   - 시급: 11,000원
   - 근무요일: [1,2,3,4,5,6] (월~토)

3. 캘린더에서 두 지점 확인
   - 홍대점 일정: 빨간색 (#FF6B6B)
   - 강남점 일정: 청록색 (#4ECDC4)
   - 필터 기능: 지점별, 근로자별 조회 가능
```

**데이터베이스 변화:**
```sql
-- 새 Workplace 추가
INSERT INTO Workplace
  (employer_id, business_number, business_name, name, address, color_code)
VALUES
  (1, '987-65-43210', '(주)카페모카', '강남점',
   '서울시 강남구 테헤란로 456', '#4ECDC4');

-- 새 근로자 계약
INSERT INTO WorkerContract
  (workplace_id, worker_id, hourly_wage, work_days, contract_start_date, payment_day)
VALUES
  (2, 3, 11000, '[1,2,3,4,5,6]', '2025-12-01', 25);
```

---

## 주요 플로우 요약

### 고용주 주요 기능
1. ✅ 사업장 등록 및 관리
2. ✅ 근로자 등록 (식별코드 기반)
3. ✅ 근무 일정 생성 및 수정
4. ✅ 근무 기록 확정
5. ✅ 정정 요청 승인/반려
6. ✅ 급여 자동 계산 확인
7. ✅ 송금 처리
8. ✅ 다중 사업장 관리

### 근로자 주요 기능
1. ✅ 고유 식별코드 발급
2. ✅ 근무 일정 조회
3. ✅ 근무 기록 확인
4. ✅ 근무 시간 정정 요청
5. ✅ 월별 급여 통계 확인
6. ✅ 송금 내역 확인
7. ✅ 실시간 알림 수신

### 시스템 자동화
1. ✅ 급여 자동 계산 (연장/야간/휴일 수당 포함)
2. ✅ 4대 보험 및 세금 자동 공제
3. ✅ 실시간 알림 발송
4. ✅ 근무 일정 자동 생성 (계약 기반)
5. ✅ 데이터 암호화 (계좌번호, 급여 정보)

---

## 예외 처리 시나리오

### 1. 송금 실패

```
상황: 카카오페이 송금이 실패했습니다.

1. Payment 상태를 FAILED로 업데이트
2. failure_reason 기록: "카카오페이 서버 오류"
3. 고용주와 근로자 모두에게 알림 발송
4. 고용주가 재시도 또는 다른 방법 선택
```

```sql
UPDATE Payment
SET status = 'FAILED',
    failure_reason = '카카오페이 서버 오류'
WHERE id = 1;

-- 알림 생성
INSERT INTO Notification (user_id, type, title, message)
VALUES
  (1, 'PAYMENT_FAILED', '송금 실패', '김민지님에게 송금이 실패했습니다.'),
  (2, 'PAYMENT_FAILED', '입금 실패', '급여 입금이 실패했습니다. 사장님에게 문의해주세요.');
```

---

### 2. 정정 요청 반려

```
상황: 고용주가 근로자의 정정 요청을 반려합니다.

1. 고용주가 정정 요청 검토
2. 반려 사유 입력: "CCTV 확인 결과 14:00에 퇴근하셨습니다"
3. 상태를 REJECTED로 업데이트
4. 근로자에게 알림 발송
```

```sql
UPDATE CorrectionRequest
SET status = 'REJECTED',
    reviewer_id = 1,
    reviewed_at = NOW(),
    review_comment = 'CCTV 확인 결과 14:00에 퇴근하셨습니다'
WHERE id = 1;

-- 알림 생성
INSERT INTO Notification
  (user_id, type, title, message, link_url)
VALUES
  (2, 'CORRECTION_RESPONSE', '정정 요청 반려',
   '11월 1일 근무 시간 정정 요청이 반려되었습니다.',
   '/correction-requests/1');
```

---

## 데이터 흐름도

```
[고용주] --등록--> [사업장] --등록--> [근로자 계약]
                                          |
                                          v
                                    [근무 일정]
                                          |
                                          v
                                    [근무 기록] <--요청-- [정정 요청]
                                          |                     |
                                          v                     v
                                      [급여 계산]          [승인/반려]
                                          |
                                          v
                                        [송금]
                                          |
                                          v
                                  [알림] --> [근로자]
```

---

## 보안 및 권한 관리

### 접근 제어 규칙

```
고용주:
- 자신이 등록한 사업장만 조회/수정 가능
- 자신의 사업장에 속한 근로자만 조회/수정 가능
- 자신의 사업장 급여/송금 내역만 조회 가능

근로자:
- 자신의 근무 일정만 조회 가능
- 자신의 근무 기록만 조회/정정 요청 가능
- 자신의 급여/송금 내역만 조회 가능

시스템:
- 급여 데이터 AES-256 암호화
- 계좌번호 AES-256 암호화
- HTTPS 통신 (TLS 1.3)
- JWT 토큰 기반 인증
```
