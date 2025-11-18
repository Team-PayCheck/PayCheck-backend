# WageManager API 명세서

## API 목록 (통합 표)

| 인덱스 | 기능 | Method | API Path | 설명 | FE 개발 현황 | BE 개발 현황 | 수정일 | JSON Example |
|--------|------|--------|----------|------|--------------|--------------|--------|--------------|
| **1. 인증 API** ||||||||
| 1.1 | 카카오 로그인 | POST | `/api/auth/kakao/login` | 카카오 소셜 로그인 | 대기 | 대기 | - | [링크](#11-카카오-로그인) |
| 1.2 | 로그아웃 | POST | `/api/auth/logout` | 로그아웃 및 토큰 무효화 | 대기 | 대기 | - | [링크](#12-로그아웃) |
| 1.3 | 토큰 갱신 | POST | `/api/auth/refresh` | Access Token 갱신 | 대기 | 대기 | - | [링크](#13-토큰-갱신) |
| **2. 사용자 API** ||||||||
| 2.1 | 회원가입 | POST | `/api/users/register` | 신규 사용자 등록 | 대기 | 대기 | - | [링크](#21-회원가입) |
| 2.2 | 내 정보 조회 | GET | `/api/users/me` | 로그인한 사용자 정보 | 대기 | 대기 | - | [링크](#22-내-정보-조회) |
| 2.3 | 내 정보 수정 | PUT | `/api/users/me` | 사용자 정보 수정 | 대기 | 대기 | - | [링크](#23-내-정보-수정) |
| 2.4 | 계좌 정보 수정 | PUT | `/api/users/me/account` | 근로자 계좌 정보 수정 | 대기 | 대기 | - | [링크](#24-계좌-정보-수정) |
| **3. 고용주 - 사업장** ||||||||
| 3.1 | 사업장 등록 | POST | `/api/employer/workplaces` | 새 사업장 등록 | 대기 | 대기 | - | [링크](#31-사업장-등록) |
| 3.2 | 사업장 목록 | GET | `/api/employer/workplaces` | 내 사업장 목록 조회 | 대기 | 대기 | - | [링크](#32-사업장-목록) |
| 3.3 | 사업장 상세 | GET | `/api/employer/workplaces/{id}` | 사업장 상세 정보 | 대기 | 대기 | - | [링크](#33-사업장-상세) |
| 3.4 | 사업장 수정 | PUT | `/api/employer/workplaces/{id}` | 사업장 정보 수정 | 대기 | 대기 | - | [링크](#34-사업장-수정) |
| 3.5 | 사업장 비활성화 | DELETE | `/api/employer/workplaces/{id}` | 사업장 비활성화 | 대기 | 대기 | - | [링크](#35-사업장-비활성화) |
| **4. 고용주 - 근로자** ||||||||
| 4.1 | 근로자 추가 | POST | `/api/employer/workplaces/{workplace_id}/workers` | 코드로 근로자 추가 | 대기 | 대기 | - | [링크](#41-근로자-추가) |
| 4.2 | 근로자 목록 | GET | `/api/employer/workplaces/{workplace_id}/workers` | 사업장 근로자 목록 | 대기 | 대기 | - | [링크](#42-근로자-목록) |
| 4.3 | 계약 상세 | GET | `/api/employer/contracts/{id}` | 계약 상세 정보 | 대기 | 대기 | - | [링크](#43-계약-상세) |
| 4.4 | 계약 수정 | PUT | `/api/employer/contracts/{id}` | 계약 정보 수정 | 대기 | 대기 | - | [링크](#44-계약-수정) |
| 4.5 | 계약 종료 | DELETE | `/api/employer/contracts/{id}` | 계약 종료 | 대기 | 대기 | - | [링크](#45-계약-종료) |
| **5. 고용주 - 근무일정** ||||||||
| 5.1 | 일정 등록 | POST | `/api/employer/work-records` | 근무 일정 등록 | 대기 | 대기 | - | [링크](#51-일정-등록) |
| 5.2 | 일정 일괄등록 | POST | `/api/employer/work-records/batch` | 여러 일정 일괄 등록 | 대기 | 대기 | - | [링크](#52-일정-일괄등록) |
| 5.3 | 근무기록 조회 | GET | `/api/employer/work-records` | 캘린더 근무 기록 조회 | 대기 | 대기 | - | [링크](#53-근무기록-조회) |
| 5.4 | 일정 수정 | PUT | `/api/employer/work-records/{id}` | 근무 시간 수정 | 대기 | 대기 | - | [링크](#54-일정-수정) |
| 5.5 | 근무 완료 | PUT | `/api/employer/work-records/{id}/complete` | 근무 완료 처리 | 대기 | 대기 | - | [링크](#55-근무-완료) |
| 5.6 | 일정 삭제 | DELETE | `/api/employer/work-records/{id}` | 근무 일정 삭제 | 대기 | 대기 | - | [링크](#56-일정-삭제) |
| **6. 고용주 - 정정요청** ||||||||
| 6.1 | 요청 목록 | GET | `/api/employer/correction-requests` | 정정 요청 목록 | 대기 | 대기 | - | [링크](#61-요청-목록) |
| 6.2 | 요청 상세 | GET | `/api/employer/correction-requests/{id}` | 정정 요청 상세 | 대기 | 대기 | - | [링크](#62-요청-상세) |
| 6.3 | 요청 승인 | PUT | `/api/employer/correction-requests/{id}/approve` | 정정 요청 승인 | 대기 | 대기 | - | [링크](#63-요청-승인) |
| 6.4 | 요청 반려 | PUT | `/api/employer/correction-requests/{id}/reject` | 정정 요청 반려 | 대기 | 대기 | - | [링크](#64-요청-반려) |
| **7. 고용주 - 급여** ||||||||
| 7.1 | 급여 목록 | GET | `/api/employer/salaries` | 월별 급여 목록 | 대기 | 대기 | - | [링크](#71-급여-목록) |
| 7.2 | 급여 상세 | GET | `/api/employer/salaries/{id}` | 급여 상세 정보 | 대기 | 대기 | - | [링크](#72-급여-상세) |
| 7.3 | 급여 계산 | POST | `/api/employer/salaries/calculate` | 급여 계산 | 대기 | 대기 | - | [링크](#73-급여-계산) |
| 7.4 | 급여 송금 | POST | `/api/employer/payments` | 급여 송금 처리 | 대기 | 대기 | - | [링크](#74-급여-송금) |
| **8. 근로자 - 근무일정** ||||||||
| 8.1 | 일정 조회 | GET | `/api/worker/work-records` | 월별 근무 일정 조회 | 대기 | 대기 | - | [링크](#81-일정-조회) |
| 8.2 | 기록 상세 | GET | `/api/worker/work-records/{id}` | 근무 기록 상세 | 대기 | 대기 | - | [링크](#82-기록-상세) |
| 8.3 | 근무 완료 | PUT | `/api/worker/work-records/{id}/complete` | 근무 완료 처리 | 대기 | 대기 | - | [링크](#83-근무-완료) |
| **9. 근로자 - 정정요청** ||||||||
| 9.1 | 요청 생성 | POST | `/api/worker/correction-requests` | 정정 요청 생성 | 대기 | 대기 | - | [링크](#91-요청-생성) |
| 9.2 | 내 요청 목록 | GET | `/api/worker/correction-requests` | 내 정정 요청 목록 | 대기 | 대기 | - | [링크](#92-내-요청-목록) |
| 9.3 | 요청 상세 | GET | `/api/worker/correction-requests/{id}` | 내 정정 요청 상세 조회 | 대기 | 대기 | - | [링크](#93-요청-상세) |
| 9.4 | 요청 취소 | DELETE | `/api/worker/correction-requests/{id}` | 정정 요청 취소 | 대기 | 대기 | - | [링크](#94-요청-취소) |
| **10. 근로자 - 급여** ||||||||
| 10.1 | 급여 목록 | GET | `/api/worker/salaries` | 연도별 급여 목록 | 대기 | 대기 | - | [링크](#101-급여-목록) |
| 10.2 | 급여 상세 | GET | `/api/worker/salaries/{id}` | 급여 상세 정보 | 대기 | 대기 | - | [링크](#102-급여-상세) |
| 10.3 | 송금 내역 | GET | `/api/worker/payments` | 송금 내역 조회 | 대기 | 대기 | - | [링크](#103-송금-내역) |
| **11. 공통 - 알림** ||||||||
| 11.1 | 알림 목록 | GET | `/api/notifications` | 알림 목록 조회 | 대기 | 대기 | - | [링크](#111-알림-목록) |
| 11.2 | SSE 알림 구독 | GET | `/api/notifications/stream` | 실시간 알림 구독 (SSE) | 대기 | 대기 | - | [링크](#112-sse-알림-구독) |
| 11.3 | 읽지 않은 알림 개수 | GET | `/api/notifications/unread-count` | 읽지 않은 알림 개수 조회 | 대기 | 대기 | - | [링크](#113-읽지-않은-알림-개수) |
| 11.4 | 알림 읽음 | PUT | `/api/notifications/{id}/read` | 알림 읽음 처리 | 대기 | 대기 | - | [링크](#114-알림-읽음) |
| 11.5 | 전체 읽음 | PUT | `/api/notifications/read-all` | 모든 알림 읽음 | 대기 | 대기 | - | [링크](#115-전체-읽음) |
| 11.6 | 알림 삭제 | DELETE | `/api/notifications/{id}` | 알림 삭제 | 대기 | 대기 | - | [링크](#116-알림-삭제) |
| **12. 공통 - 설정** ||||||||
| 12.1 | 설정 조회 | GET | `/api/settings` | 사용자 설정 조회 | 대기 | 대기 | - | [링크](#121-설정-조회) |
| 12.2 | 설정 수정 | PUT | `/api/settings` | 사용자 설정 수정 | 대기 | 대기 | - | [링크](#122-설정-수정) |

---

## JSON Examples

### 1.1 카카오 로그인
**Request:** `{"kakao_access_token": "token"}` → **Response:** `{"success": true, "data": {"access_token": "jwt...", "user": {...}}}`

### 1.2 로그아웃
**Request:** `{"refresh_token": "jwt..."}` → **Response:** `{"success": true, "message": "로그아웃되었습니다."}`

### 1.3 토큰 갱신
**Request:** `{"refresh_token": "jwt..."}` → **Response:** `{"success": true, "data": {"access_token": "new_jwt..."}}`

### 2.1 회원가입
**Request:** `{"name": "박성호", "user_type": "EMPLOYER", ...}` → **Response:** `{"success": true, "data": {"user_id": 1, ...}}`

### 2.2 내 정보 조회
**Response:** `{"success": true, "data": {"id": 1, "name": "박성호", "user_type": "EMPLOYER"}}`

### 2.3 내 정보 수정
**Request:** `{"name": "박성호", "phone": "010-1234-5679"}` → **Response:** `{"success": true}`

### 2.4 계좌 정보 수정
**Request:** `{"bank_name": "국민은행", "account_number": "1234-56-789"}` → **Response:** `{"success": true}`

### 3.1 사업장 등록
**Request:** `{"business_number": "123-45-67890", "name": "홍대점", ...}` → **Response:** `{"success": true, "data": {"id": 1, ...}}`

### 3.2 사업장 목록
**Response:** `{"success": true, "data": [{"id": 1, "name": "홍대점", "worker_count": 5}]}`

### 3.3 사업장 상세
**Response:** `{"success": true, "data": {"id": 1, "business_number": "123-45-67890", ...}}`

### 3.4 사업장 수정
**Request:** `{"name": "홍대점", "address": "서울시..."}` → **Response:** `{"success": true}`

### 3.5 사업장 비활성화
**Response:** `{"success": true, "message": "사업장이 비활성화되었습니다."}`

### 4.1 근로자 추가
**Request:** `{"worker_code": "ABC123", "hourly_wage": 10000, ...}` → **Response:** `{"success": true, "data": {"contract_id": 1, ...}}`

### 4.2 근로자 목록
**Response:** `{"success": true, "data": [{"contract_id": 1, "worker": {"name": "김민지"}, ...}]}`

### 4.3 계약 상세
**Response:** `{"success": true, "data": {"contract_id": 1, "hourly_wage": 10000, ...}}`

### 4.4 계약 수정
**Request:** `{"hourly_wage": 11000, "payment_day": 28}` → **Response:** `{"success": true}`

### 4.5 계약 종료
**Response:** `{"success": true, "message": "계약이 종료되었습니다."}`

### 5.1 일정 등록
**Request:** `{"contract_id": 1, "work_date": "2025-11-01", "start_time": "09:00", "end_time": "14:00"}` → **Response:** `{"success": true, "data": {"id": 1, "status": "SCHEDULED", "is_modified": false}}`

### 5.2 일정 일괄등록
**Request:** `{"contract_id": 1, "work_records": [{...}, {...}]}` → **Response:** `{"success": true, "data": {"created_count": 20}}`

### 5.3 근무기록 조회
**Query:** `?workplace_id=1&year=2025&month=11` → **Response:** `{"success": true, "data": [{"id": 1, "status": "COMPLETED", "is_modified": false, ...}]}`

### 5.4 일정 수정
**Request:** `{"start_time": "10:00", "end_time": "15:00"}` → **Response:** `{"success": true, "data": {"is_modified": true}}`

### 5.5 근무 완료
**Response:** `{"success": true, "data": {"id": 1, "status": "COMPLETED", "total_hours": 5.0}}`

### 5.6 일정 삭제
**Response:** `{"success": true, "message": "근무 일정이 삭제되었습니다."}`

### 6.1 요청 목록
**Response:** `{"success": true, "data": [{"id": 1, "requester": {"name": "김민지"}, "status": "PENDING", ...}]}`

### 6.2 요청 상세
**Response:** `{"success": true, "data": {"id": 1, "requested_end_time": "14:30", "reason": "...", ...}}`

### 6.3 요청 승인
**Request:** `{"review_comment": "확인했습니다"}` → **Response:** `{"success": true, "data": {"status": "APPROVED"}}`

### 6.4 요청 반려
**Request:** `{"review_comment": "..."}` → **Response:** `{"success": true, "data": {"status": "REJECTED"}}`

### 7.1 급여 목록
**Query:** `?workplace_id=1&year=2025&month=11` → **Response:** `{"success": true, "data": [{"salary_id": 1, "net_pay": 1008000, ...}]}`

### 7.2 급여 상세
**Response:** `{"success": true, "data": {"salary_id": 1, "base_pay": 1000000, "net_pay": 1008000, ...}}`

### 7.3 급여 계산
**Request:** `{"contract_id": 1, "year": 2025, "month": 11}` → **Response:** `{"success": true, "data": {"salary_id": 1, "net_pay": 1008000}}`

### 7.4 급여 송금
**Request:** `{"salary_id": 1, "payment_method": "KAKAO_PAY"}` → **Response:** `{"success": true, "data": {"payment_id": 1, "status": "COMPLETED"}}`

### 8.1 일정 조회
**Query:** `?year=2025&month=11` → **Response:** `{"success": true, "data": [{"id": 1, "status": "COMPLETED", "is_modified": false, ...}]}`

### 8.2 기록 상세
**Response:** `{"success": true, "data": {"id": 1, "total_hours": 5.0, "status": "COMPLETED", ...}}`

### 8.3 근무 완료
**Response:** `{"success": true, "data": {"id": 1, "status": "COMPLETED"}}`

### 9.1 요청 생성
**Request:** `{"work_record_id": 1, "requested_end_time": "14:30", "reason": "..."}` → **Response:** `{"success": true, "data": {"id": 1, "status": "PENDING"}}`

### 9.2 내 요청 목록
**Response:** `{"success": true, "data": [{"id": 1, "status": "PENDING", ...}]}`

### 9.3 요청 상세
**Response:** `{"success": true, "data": {"id": 1, "work_record_id": 1, "requested_end_time": "14:30", "reason": "손님이 많아서 30분 연장 근무했습니다", "status": "PENDING", ...}}`

### 9.4 요청 취소
**Response:** `{"success": true, "message": "정정 요청이 취소되었습니다."}`

### 10.1 급여 목록
**Query:** `?year=2025` → **Response:** `{"success": true, "data": [{"salary_id": 1, "month": 11, "net_pay": 1008000, ...}]}`

### 10.2 급여 상세
**Response:** `{"success": true, "data": {"salary_id": 1, "total_work_hours": 110, "net_pay": 1008000, ...}}`

### 10.3 송금 내역
**Response:** `{"success": true, "data": [{"payment_id": 1, "status": "COMPLETED", "payment_date": "2025-11-25", ...}]}`

### 11.1 알림 목록
**Query:** `?is_read=false&page=1` → **Response:** `{"success": true, "data": {"notifications": [...], "unread_count": 12}}`

### 11.2 SSE 알림 구독
**실시간 알림을 수신하기 위한 Server-Sent Events 연결**
- **Content-Type:** `text/event-stream`
- **연결 유지:** Keep-alive 방식으로 실시간 알림 수신
- **이벤트 타입:**
  - `notification`: 새로운 알림 발생 시 전송
    - **Data:** `{"id": 123, "type": "CORRECTION_REQUEST", "title": "정정 요청", "message": "김민지님이 근무 시간 정정을 요청했습니다.", "created_at": "2025-11-18T14:30:00", "is_read": false}`
  - `unread_count`: 읽지 않은 알림 개수 업데이트
    - **Data:** `{"unread_count": 5}`
- **재연결:** 연결이 끊어지면 클라이언트에서 자동 재연결 권장

### 11.3 읽지 않은 알림 개수
**Response:** `{"success": true, "data": 5}`

### 11.4 알림 읽음
**Response:** `{"success": true, "message": "알림이 읽음 처리되었습니다."}`

### 11.5 전체 읽음
**Response:** `{"success": true, "message": "모든 알림이 읽음 처리되었습니다."}`

### 11.6 알림 삭제
**Response:** `{"success": true, "message": "알림이 삭제되었습니다."}`

### 12.1 설정 조회
**Response:** `{"success": true, "data": {"notification_enabled": true, "push_enabled": true, ...}}`

### 12.2 설정 수정
**Request:** `{"notification_enabled": true, "push_enabled": false, ...}` → **Response:** `{"success": true}`

---

## 에러 응답

**형식:** `{"success": false, "error": {"code": "ERROR_CODE", "message": "..."}}`

**주요 에러 코드:** `UNAUTHORIZED` (401), `FORBIDDEN` (403), `NOT_FOUND` (404), `VALIDATION_ERROR` (400), `DUPLICATE_ENTRY` (409)

---

## 인증
**Header:** `Authorization: Bearer {access_token}`

---

## 변경 이력
- v1.0 (2025-11-01): 초안 작성
- v1.1 (2025-11-01): 하나의 통합 표로 정리, is_modified 필드 반영
- v1.2 (2025-11-06): 근로자용 정정 요청 상세 조회 API 추가 (9.3)
