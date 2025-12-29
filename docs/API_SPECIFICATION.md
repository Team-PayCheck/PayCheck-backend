# WageManager API 명세서

## API 목록 (통합 표)

| 인덱스 | 기능 | Method | API Path | 설명 | FE 개발 현황 | BE 개발 현황 | 수정일 | JSON Example |
|--------|------|--------|----------|------|--------------|--------------|--------|--------------|
| **1. 인증 API** ||||||||
| 1.1 | 카카오 로그인 | POST | `/api/auth/kakao/login` | 카카오 소셜 로그인 (신규 사용자 자동 등록) | 대기 | 대기 | - | [링크](#11-카카오-로그인) |
| 1.2 | 로그아웃 | POST | `/api/auth/logout` | 로그아웃 및 토큰 무효화 | 대기 | 대기 | - | [링크](#12-로그아웃) |
| 1.3 | 토큰 갱신 | POST | `/api/auth/refresh` | Access Token 갱신 | 대기 | 대기 | - | [링크](#13-토큰-갱신) |
| **2. 사용자 API** ||||||||
| 2.1 | 내 정보 조회 | GET | `/api/users/me` | 로그인한 사용자 정보 | 대기 | 대기 | - | [링크](#21-내-정보-조회) |
| 2.2 | 내 정보 수정 | PUT | `/api/users/me` | 사용자 정보 수정 | 대기 | 대기 | - | [링크](#22-내-정보-수정) |
| 2.3 | 계좌 정보 수정 | PUT | `/api/users/me/account` | 근로자 계좌 정보 수정 | 대기 | 대기 | - | [링크](#23-계좌-정보-수정) |
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
| 12.1 | 내 설정 조회 | GET | `/api/settings/me` | 로그인한 사용자의 알림 설정 조회 | 대기 | 완료 | 2025-12-10 | [링크](#121-내-설정-조회) |
| 12.2 | 내 설정 수정 | PUT | `/api/settings/me` | 로그인한 사용자의 알림 설정 수정 | 대기 | 완료 | 2025-12-10 | [링크](#122-내-설정-수정) |

---

## JSON Examples

### 1.1 카카오 로그인

**Request:**
```json
{
  "kakaoAccessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response (고용주):**
```json
{
  "success": true,
  "data": {
    "id": 1001,
    "kakaoId": "123456789",
    "name": "김철수",
    "userType": "EMPLOYER",
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "createdAt": "2025-12-10T14:30:00"
  }
}
```

**Response (근로자):**
```json
{
  "success": true,
  "data": {
    "id": 2001,
    "kakaoId": "987654321",
    "name": "이영희",
    "userType": "WORKER",
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "createdAt": "2025-12-10T14:30:00"
  }
}
```

---

### 1.2 로그아웃

**Request:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response:**
```json
{
  "success": true,
  "message": "로그아웃되었습니다."
}
```

---

### 1.3 토큰 갱신

**Request:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

---

### 2.1 내 정보 조회

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1001,
    "kakaoId": "123456789",
    "name": "김철수",
    "phone": "010-8543-2179",
    "userType": "EMPLOYER",
    "profileImageUrl": "https://example.com/image/001.jpg",
    "createdAt": "2025-12-10T14:30:00",
    "updatedAt": "2025-12-15T09:00:00"
  }
}
```

---

### 2.2 내 정보 수정

**Request:**
```json
{
  "name": "김철수",
  "phone": "010-8543-2179",
  "profileImageUrl": "https://example.com/image/002.jpg"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1001,
    "kakaoId": "123456789",
    "name": "김철수",
    "phone": "010-8543-2179",
    "userType": "EMPLOYER",
    "profileImageUrl": "https://example.com/image/002.jpg",
    "createdAt": "2025-12-10T14:30:00",
    "updatedAt": "2025-12-15T10:30:00"
  }
}
```

---

### 2.3 계좌 정보 수정

**Request:**
```json
{
  "bankName": "국민은행",
  "accountNumber": "123-4567890-12"
}
```

**Response:**
```json
{
  "success": true,
  "message": "계좌 정보가 수정되었습니다."
}
```

---

### 3.1 사업장 등록

**Request:**
```json
{
  "businessNumber": "123-45-67890",
  "businessName": "홍대카페",
  "name": "홍대점",
  "address": "서울시 마포구 홍익로 123",
  "colorCode": "#FF5733",
  "isLessThanFiveEmployees": false
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 101,
    "businessNumber": "123-45-67890",
    "businessName": "홍대카페",
    "name": "홍대점",
    "address": "서울시 마포구 홍익로 123",
    "colorCode": "#FF5733",
    "isLessThanFiveEmployees": false,
    "isActive": true,
    "createdAt": "2025-12-15T09:00:00",
    "updatedAt": "2025-12-15T09:00:00"
  }
}
```

---

### 3.2 사업장 목록

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 101,
      "businessNumber": "123-45-67890",
      "businessName": "홍대카페",
      "name": "홍대점",
      "address": "서울시 마포구 홍익로 123",
      "colorCode": "#FF5733",
      "isLessThanFiveEmployees": false,
      "isActive": true,
      "workerCount": 5,
      "createdAt": "2025-12-10T09:00:00",
      "updatedAt": "2025-12-15T09:00:00"
    },
    {
      "id": 102,
      "businessNumber": "456-78-91011",
      "businessName": "강남편의점",
      "name": "강남역점",
      "address": "서울시 강남구 강남대로 456",
      "colorCode": "#3366FF",
      "isLessThanFiveEmployees": false,
      "isActive": true,
      "workerCount": 12,
      "createdAt": "2025-11-15T10:30:00",
      "updatedAt": "2025-12-12T14:30:00"
    },
    {
      "id": 103,
      "businessNumber": "789-01-23456",
      "businessName": "서초카페",
      "name": "서초점",
      "address": "서울시 서초구 서초대로 789",
      "colorCode": "#33FF66",
      "isLessThanFiveEmployees": true,
      "isActive": false,
      "workerCount": 18,
      "createdAt": "2025-10-20T11:00:00",
      "updatedAt": "2025-12-01T15:00:00"
    }
  ]
}
```

---

### 3.3 사업장 상세

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 101,
    "businessNumber": "123-45-67890",
    "businessName": "홍대카페",
    "name": "홍대점",
    "address": "서울시 마포구 홍익로 123",
    "colorCode": "#FF5733",
    "isLessThanFiveEmployees": false,
    "isActive": true,
    "workerCount": 5,
    "createdAt": "2025-12-10T09:00:00",
    "updatedAt": "2025-12-15T09:00:00"
  }
}
```

---

### 3.4 사업장 수정

**Request:**
```json
{
  "name": "홍대점",
  "address": "서울시 마포구 홍익로 150",
  "colorCode": "#FF5733"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 101,
    "businessNumber": "123-45-67890",
    "businessName": "홍대카페",
    "name": "홍대점",
    "address": "서울시 마포구 홍익로 150",
    "colorCode": "#FF5733",
    "isLessThanFiveEmployees": false,
    "isActive": true,
    "createdAt": "2025-12-10T09:00:00",
    "updatedAt": "2025-12-15T10:30:00"
  }
}
```

---

### 3.5 사업장 비활성화

**Response:**
```json
{
  "success": true,
  "message": "사업장이 비활성화되었습니다."
}
```

---

### 4.1 근로자 추가

**Request:**
```json
{
  "workerCode": "WORKER001",
  "hourlyWage": 10030,
  "workSchedules": [
    {
      "dayOfWeek": 1,
      "startTime": "09:00",
      "endTime": "18:00"
    },
    {
      "dayOfWeek": 2,
      "startTime": "09:00",
      "endTime": "18:00"
    },
    {
      "dayOfWeek": 3,
      "startTime": "09:00",
      "endTime": "18:00"
    },
    {
      "dayOfWeek": 4,
      "startTime": "09:00",
      "endTime": "18:00"
    },
    {
      "dayOfWeek": 5,
      "startTime": "09:00",
      "endTime": "18:00"
    },
    {
      "dayOfWeek": 6,
      "startTime": "10:00",
      "endTime": "15:00"
    }
  ],
  "contractStartDate": "2025-12-15",
  "contractEndDate": null,
  "paymentDay": 25,
  "payrollDeductionType": "NONE"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "contractId": 201,
    "workerId": 2001,
    "workerName": "박민준",
    "workerCode": "WORKER001",
    "workplaceId": 101,
    "workplaceName": "홍대점",
    "hourlyWage": 10030,
    "workSchedules": [
      {
        "dayOfWeek": 1,
        "startTime": "09:00",
        "endTime": "18:00"
      },
      {
        "dayOfWeek": 2,
        "startTime": "09:00",
        "endTime": "18:00"
      },
      {
        "dayOfWeek": 3,
        "startTime": "09:00",
        "endTime": "18:00"
      },
      {
        "dayOfWeek": 4,
        "startTime": "09:00",
        "endTime": "18:00"
      },
      {
        "dayOfWeek": 5,
        "startTime": "09:00",
        "endTime": "18:00"
      },
      {
        "dayOfWeek": 6,
        "startTime": "10:00",
        "endTime": "15:00"
      }
    ],
    "contractStartDate": "2025-12-15",
    "contractEndDate": null,
    "paymentDay": 25,
    "payrollDeductionType": "NONE",
    "contractStatus": "ONGOING",
    "createdAt": "2025-12-15T09:30:00",
    "updatedAt": "2025-12-15T09:30:00"
  }
}
```

---

### 4.2 근로자 목록

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "contractId": 201,
      "workerId": 2001,
      "workerName": "박민준",
      "workerCode": "WORKER001",
      "hourlyWage": 10030,
      "paymentDay": 25,
      "contractStatus": "ONGOING",
      "contractStartDate": "2025-12-15",
      "contractEndDate": null,
      "createdAt": "2025-12-15T09:30:00",
      "updatedAt": "2025-12-15T09:30:00"
    },
    {
      "contractId": 202,
      "workerId": 2002,
      "workerName": "최수진",
      "workerCode": "WORKER002",
      "hourlyWage": 11000,
      "paymentDay": 25,
      "contractStatus": "ONGOING",
      "contractStartDate": "2025-11-01",
      "contractEndDate": null,
      "createdAt": "2025-11-01T10:00:00",
      "updatedAt": "2025-12-10T14:30:00"
    },
    {
      "contractId": 203,
      "workerId": 2003,
      "workerName": "윤지현",
      "workerCode": "WORKER003",
      "hourlyWage": 15000,
      "paymentDay": 25,
      "contractStatus": "ENDED",
      "contractStartDate": "2025-10-01",
      "contractEndDate": "2025-12-10",
      "createdAt": "2025-10-01T09:00:00",
      "updatedAt": "2025-12-10T15:45:00"
    },
    {
      "contractId": 204,
      "workerId": 2004,
      "workerName": "이영희",
      "workerCode": "WORKER004",
      "hourlyWage": 12000,
      "paymentDay": 20,
      "contractStatus": "SCHEDULED",
      "contractStartDate": "2025-12-20",
      "contractEndDate": null,
      "createdAt": "2025-12-15T11:00:00",
      "updatedAt": "2025-12-15T11:00:00"
    }
  ]
}
```

---

### 4.3 계약 상세

**Response:**
```json
{
  "success": true,
  "data": {
    "contractId": 201,
    "workerId": 2001,
    "workerName": "박민준",
    "workerCode": "WORKER001",
    "workplaceId": 101,
    "workplaceName": "홍대점",
    "hourlyWage": 10030,
    "workSchedules": [
      {
        "dayOfWeek": 1,
        "startTime": "09:00",
        "endTime": "18:00"
      },
      {
        "dayOfWeek": 2,
        "startTime": "09:00",
        "endTime": "18:00"
      },
      {
        "dayOfWeek": 3,
        "startTime": "09:00",
        "endTime": "18:00"
      },
      {
        "dayOfWeek": 4,
        "startTime": "09:00",
        "endTime": "18:00"
      },
      {
        "dayOfWeek": 5,
        "startTime": "09:00",
        "endTime": "18:00"
      },
      {
        "dayOfWeek": 6,
        "startTime": "10:00",
        "endTime": "15:00"
      }
    ],
    "contractStartDate": "2025-12-15",
    "contractEndDate": null,
    "paymentDay": 25,
    "payrollDeductionType": "NONE",
    "contractStatus": "ONGOING",
    "createdAt": "2025-12-15T09:30:00",
    "updatedAt": "2025-12-15T09:30:00"
  }
}
```

---

### 4.4 계약 수정

**Request:**
```json
{
  "hourlyWage": 11000,
  "paymentDay": 25
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "contractId": 201,
    "workerId": 2001,
    "workerName": "박민준",
    "workerCode": "WORKER001",
    "workplaceId": 101,
    "workplaceName": "홍대점",
    "hourlyWage": 11000,
    "workSchedules": [
      {
        "dayOfWeek": 1,
        "startTime": "09:00",
        "endTime": "18:00"
      },
      {
        "dayOfWeek": 2,
        "startTime": "09:00",
        "endTime": "18:00"
      },
      {
        "dayOfWeek": 3,
        "startTime": "09:00",
        "endTime": "18:00"
      },
      {
        "dayOfWeek": 4,
        "startTime": "09:00",
        "endTime": "18:00"
      },
      {
        "dayOfWeek": 5,
        "startTime": "09:00",
        "endTime": "18:00"
      },
      {
        "dayOfWeek": 6,
        "startTime": "10:00",
        "endTime": "15:00"
      }
    ],
    "contractStartDate": "2025-12-15",
    "contractEndDate": null,
    "paymentDay": 25,
    "payrollDeductionType": "NONE",
    "contractStatus": "ONGOING",
    "createdAt": "2025-12-15T09:30:00",
    "updatedAt": "2025-12-15T14:00:00"
  }
}
```

---

### 4.5 계약 종료

**Response:**
```json
{
  "success": true,
  "message": "계약이 종료되었습니다."
}
```

---

### 5.1 일정 등록

**Request:**
```json
{
  "contractId": 201,
  "workDate": "2025-12-15",
  "startTime": "09:00",
  "endTime": "18:00",
  "breakMinutes": 60,
  "memo": "정상 근무"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 301,
    "contractId": 201,
    "workDate": "2025-12-15",
    "startTime": "09:00",
    "endTime": "18:00",
    "breakMinutes": 60,
    "totalWorkHours": 8.0,
    "status": "PENDING_APPROVAL",
    "isModified": false,
    "memo": "정상 근무",
    "createdAt": "2025-12-15T09:30:00",
    "updatedAt": "2025-12-15T09:30:00"
  }
}
```

---

### 5.2 일정 일괄등록

**Request:**
```json
{
  "contractId": 201,
  "workDates": [
    "2025-12-16",
    "2025-12-17",
    "2025-12-18",
    "2025-12-19",
    "2025-12-20",
    "2025-12-21"
  ],
  "startTime": "09:00",
  "endTime": "18:00",
  "breakMinutes": 60,
  "memo": "일괄 등록"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "createdCount": 6
  }
}
```

---

### 5.3 근무기록 조회

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 301,
      "contractId": 201,
      "workerName": "박민준",
      "workplaceName": "홍대점",
      "workDate": "2025-12-15",
      "startTime": "09:00",
      "endTime": "18:00",
      "breakMinutes": 60,
      "totalWorkHours": 8.0,
      "status": "SCHEDULED",
      "isModified": false,
      "memo": "정상 근무",
      "createdAt": "2025-12-15T09:30:00",
      "updatedAt": "2025-12-15T09:30:00"
    },
    {
      "id": 302,
      "contractId": 201,
      "workerName": "박민준",
      "workplaceName": "홍대점",
      "workDate": "2025-12-20",
      "startTime": "09:00",
      "endTime": "18:00",
      "breakMinutes": 60,
      "totalWorkHours": 8.0,
      "status": "SCHEDULED",
      "isModified": false,
      "memo": "정상 근무",
      "createdAt": "2025-12-15T09:30:00",
      "updatedAt": "2025-12-15T09:30:00"
    },
    {
      "id": 303,
      "contractId": 202,
      "workerName": "최수진",
      "workplaceName": "홍대점",
      "workDate": "2025-12-15",
      "startTime": "10:00",
      "endTime": "19:00",
      "breakMinutes": 60,
      "totalWorkHours": 8.0,
      "status": "PENDING_APPROVAL",
      "isModified": false,
      "memo": "당일 등록",
      "createdAt": "2025-12-15T14:30:00",
      "updatedAt": "2025-12-15T14:30:00"
    },
    {
      "id": 304,
      "contractId": 201,
      "workerName": "박민준",
      "workplaceName": "홍대점",
      "workDate": "2025-12-10",
      "startTime": "09:00",
      "endTime": "18:00",
      "breakMinutes": 60,
      "totalWorkHours": 8.0,
      "status": "COMPLETED",
      "isModified": false,
      "memo": "정상 근무",
      "createdAt": "2025-12-10T09:30:00",
      "updatedAt": "2025-12-10T17:30:00"
    }
  ]
}
```

---

### 5.4 일정 수정

**Request:**
```json
{
  "startTime": "09:30",
  "endTime": "18:30",
  "breakMinutes": 60,
  "memo": "시간 변경"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 301,
    "contractId": 201,
    "workDate": "2025-12-15",
    "startTime": "09:30",
    "endTime": "18:30",
    "breakMinutes": 60,
    "totalWorkHours": 8.0,
    "status": "PENDING_APPROVAL",
    "isModified": true,
    "memo": "시간 변경",
    "createdAt": "2025-12-15T09:30:00",
    "updatedAt": "2025-12-15T10:00:00"
  }
}
```

---

### 5.5 근무 완료

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 301,
    "contractId": 201,
    "workDate": "2025-12-15",
    "startTime": "09:00",
    "endTime": "18:00",
    "breakMinutes": 60,
    "totalWorkHours": 8.0,
    "status": "COMPLETED",
    "isModified": false,
    "createdAt": "2025-12-15T09:30:00",
    "updatedAt": "2025-12-15T18:00:00"
  }
}
```

---

### 5.6 일정 삭제

**Response:**
```json
{
  "success": true,
  "message": "근무 일정이 삭제되었습니다."
}
```

---

### 6.1 요청 목록

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 401,
      "contractId": 201,
      "workRecordId": 301,
      "requesterId": 2001,
      "requesterName": "박민준",
      "originalWorkDate": "2025-12-10",
      "originalStartTime": "09:00",
      "originalEndTime": "18:00",
      "requestedWorkDate": "2025-12-10",
      "requestedStartTime": "09:30",
      "requestedEndTime": "18:30",
      "status": "PENDING",
      "reviewedAt": null,
      "createdAt": "2025-12-15T14:30:00",
      "updatedAt": "2025-12-15T14:30:00"
    },
    {
      "id": 402,
      "contractId": 202,
      "workRecordId": 302,
      "requesterId": 2002,
      "requesterName": "최수진",
      "originalWorkDate": "2025-12-12",
      "originalStartTime": "10:00",
      "originalEndTime": "19:00",
      "requestedWorkDate": "2025-12-12",
      "requestedStartTime": "10:00",
      "requestedEndTime": "19:30",
      "status": "PENDING",
      "reviewedAt": null,
      "createdAt": "2025-12-15T15:00:00",
      "updatedAt": "2025-12-15T15:00:00"
    },
    {
      "id": 403,
      "contractId": 201,
      "workRecordId": 303,
      "requesterId": 2001,
      "requesterName": "박민준",
      "originalWorkDate": "2025-12-05",
      "originalStartTime": "09:00",
      "originalEndTime": "18:00",
      "requestedWorkDate": "2025-12-05",
      "requestedStartTime": "09:00",
      "requestedEndTime": "17:00",
      "status": "APPROVED",
      "reviewedAt": "2025-12-14T10:30:00",
      "createdAt": "2025-12-13T16:00:00",
      "updatedAt": "2025-12-14T10:30:00"
    },
    {
      "id": 404,
      "contractId": 202,
      "workRecordId": 304,
      "requesterId": 2002,
      "requesterName": "최수진",
      "originalWorkDate": "2025-12-08",
      "originalStartTime": "10:00",
      "originalEndTime": "19:00",
      "requestedWorkDate": "2025-12-08",
      "requestedStartTime": "11:00",
      "requestedEndTime": "19:00",
      "status": "REJECTED",
      "reviewedAt": "2025-12-13T14:00:00",
      "createdAt": "2025-12-12T17:30:00",
      "updatedAt": "2025-12-13T14:00:00"
    }
  ]
}
```

---

### 6.2 요청 상세

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 401,
    "contractId": 201,
    "workRecordId": 301,
    "requesterId": 2001,
    "requesterName": "박민준",
    "originalWorkDate": "2025-12-10",
    "originalStartTime": "09:00",
    "originalEndTime": "18:00",
    "requestedWorkDate": "2025-12-10",
    "requestedStartTime": "09:30",
    "requestedEndTime": "18:30",
    "originalBreakMinutes": 60,
    "requestedBreakMinutes": 60,
    "status": "PENDING",
    "reviewedAt": null,
    "createdAt": "2025-12-15T14:30:00",
    "updatedAt": "2025-12-15T14:30:00"
  }
}
```

---

### 6.3 요청 승인

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 401,
    "contractId": 201,
    "workRecordId": 301,
    "requesterId": 2001,
    "requesterName": "박민준",
    "originalWorkDate": "2025-12-10",
    "originalStartTime": "09:00",
    "originalEndTime": "18:00",
    "requestedWorkDate": "2025-12-10",
    "requestedStartTime": "09:30",
    "requestedEndTime": "18:30",
    "status": "APPROVED",
    "reviewedAt": "2025-12-15T15:30:00",
    "createdAt": "2025-12-15T14:30:00",
    "updatedAt": "2025-12-15T15:30:00"
  }
}
```

---

### 6.4 요청 반려

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 401,
    "contractId": 201,
    "workRecordId": 301,
    "requesterId": 2001,
    "requesterName": "박민준",
    "originalWorkDate": "2025-12-10",
    "originalStartTime": "09:00",
    "originalEndTime": "18:00",
    "requestedWorkDate": "2025-12-10",
    "requestedStartTime": "09:30",
    "requestedEndTime": "18:30",
    "status": "REJECTED",
    "reviewedAt": "2025-12-15T15:30:00",
    "createdAt": "2025-12-15T14:30:00",
    "updatedAt": "2025-12-15T15:30:00"
  }
}
```

---

### 7.1 급여 목록

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 501,
      "contractId": 201,
      "workerId": 2001,
      "workerName": "박민준",
      "workplaceId": 101,
      "workplaceName": "홍대점",
      "year": 2025,
      "month": 11,
      "basePay": 440132,
      "overtimePay": 15090,
      "nightPay": 0,
      "holidayPay": 0,
      "deduction": 22000,
      "netPay": 433222,
      "createdAt": "2025-12-05T09:00:00",
      "updatedAt": "2025-12-05T09:00:00"
    },
    {
      "id": 502,
      "contractId": 202,
      "workerId": 2002,
      "workerName": "최수진",
      "workplaceId": 101,
      "workplaceName": "홍대점",
      "year": 2025,
      "month": 11,
      "basePay": 484000,
      "overtimePay": 20000,
      "nightPay": 5500,
      "holidayPay": 11000,
      "deduction": 25000,
      "netPay": 495500,
      "createdAt": "2025-12-05T09:00:00",
      "updatedAt": "2025-12-05T09:00:00"
    },
    {
      "id": 503,
      "contractId": 201,
      "workerId": 2001,
      "workerName": "박민준",
      "workplaceId": 101,
      "workplaceName": "홍대점",
      "year": 2025,
      "month": 12,
      "basePay": 380264,
      "overtimePay": 0,
      "nightPay": 0,
      "holidayPay": 0,
      "deduction": 19000,
      "netPay": 361264,
      "createdAt": "2025-12-10T09:00:00",
      "updatedAt": "2025-12-15T10:30:00"
    },
    {
      "id": 504,
      "contractId": 202,
      "workerId": 2002,
      "workerName": "최수진",
      "workplaceId": 101,
      "workplaceName": "홍대점",
      "year": 2025,
      "month": 12,
      "basePay": 418000,
      "overtimePay": 8800,
      "nightPay": 0,
      "holidayPay": 0,
      "deduction": 21500,
      "netPay": 405300,
      "createdAt": "2025-12-10T09:00:00",
      "updatedAt": "2025-12-15T10:30:00"
    }
  ]
}
```

---

### 7.2 급여 상세

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 501,
    "contractId": 201,
    "workerId": 2001,
    "workerName": "박민준",
    "workplaceId": 101,
    "workplaceName": "홍대점",
    "year": 2025,
    "month": 11,
    "totalWorkHours": 44.0,
    "hourlyWage": 10030,
    "basePay": 440132,
    "overtimePay": 15090,
    "nightPay": 0,
    "holidayPay": 0,
    "totalAllowance": 15090,
    "deduction": 22000,
    "netPay": 433222,
    "paymentStatus": "PENDING",
    "createdAt": "2025-12-05T09:00:00",
    "updatedAt": "2025-12-05T09:00:00"
  }
}
```

---

### 7.3 급여 계산

**Request:**
```json
{
  "contractId": 201,
  "year": 2025,
  "month": 12
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 505,
    "contractId": 201,
    "workerId": 2001,
    "workerName": "박민준",
    "year": 2025,
    "month": 12,
    "basePay": 380264,
    "overtimePay": 0,
    "nightPay": 0,
    "holidayPay": 0,
    "netPay": 361264
  }
}
```

---

### 7.4 급여 송금

**Request:**
```json
{
  "salaryId": 501,
  "paymentMethod": "BANK_TRANSFER"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 601,
    "salaryId": 501,
    "contractId": 201,
    "workerId": 2001,
    "workerName": "박민준",
    "amount": 433222,
    "paymentMethod": "BANK_TRANSFER",
    "status": "COMPLETED",
    "paymentDate": "2025-12-15T09:00:00",
    "createdAt": "2025-12-15T09:00:00",
    "updatedAt": "2025-12-15T09:00:00"
  }
}
```

---

### 8.1 일정 조회

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 305,
      "contractId": 201,
      "workDate": "2025-12-20",
      "startTime": "09:00",
      "endTime": "18:00",
      "breakMinutes": 60,
      "totalWorkHours": 8.0,
      "status": "SCHEDULED",
      "isModified": false,
      "memo": "정상 근무",
      "createdAt": "2025-12-15T09:30:00",
      "updatedAt": "2025-12-15T09:30:00"
    },
    {
      "id": 306,
      "contractId": 201,
      "workDate": "2025-12-25",
      "startTime": "09:00",
      "endTime": "18:00",
      "breakMinutes": 60,
      "totalWorkHours": 8.0,
      "status": "SCHEDULED",
      "isModified": false,
      "memo": "정상 근무",
      "createdAt": "2025-12-15T09:30:00",
      "updatedAt": "2025-12-15T09:30:00"
    },
    {
      "id": 307,
      "contractId": 201,
      "workDate": "2025-12-15",
      "startTime": "09:00",
      "endTime": "18:00",
      "breakMinutes": 60,
      "totalWorkHours": 8.0,
      "status": "PENDING_APPROVAL",
      "isModified": false,
      "memo": "당일 기록",
      "createdAt": "2025-12-15T14:30:00",
      "updatedAt": "2025-12-15T14:30:00"
    },
    {
      "id": 308,
      "contractId": 201,
      "workDate": "2025-12-10",
      "startTime": "09:00",
      "endTime": "18:00",
      "breakMinutes": 60,
      "totalWorkHours": 8.0,
      "status": "COMPLETED",
      "isModified": false,
      "memo": "완료됨",
      "createdAt": "2025-12-10T09:30:00",
      "updatedAt": "2025-12-10T17:30:00"
    }
  ]
}
```

---

### 8.2 기록 상세

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 308,
    "contractId": 201,
    "workerId": 2001,
    "workerName": "박민준",
    "workplaceId": 101,
    "workplaceName": "홍대점",
    "workDate": "2025-12-10",
    "startTime": "09:00",
    "endTime": "18:00",
    "breakMinutes": 60,
    "totalWorkHours": 8.0,
    "status": "COMPLETED",
    "isModified": false,
    "memo": "완료됨",
    "createdAt": "2025-12-10T09:30:00",
    "updatedAt": "2025-12-10T17:30:00"
  }
}
```

---

### 8.3 근무 완료

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 307,
    "contractId": 201,
    "workDate": "2025-12-15",
    "startTime": "09:00",
    "endTime": "18:00",
    "breakMinutes": 60,
    "totalWorkHours": 8.0,
    "status": "COMPLETED",
    "isModified": false,
    "createdAt": "2025-12-15T14:30:00",
    "updatedAt": "2025-12-15T18:00:00"
  }
}
```

---

### 9.1 요청 생성

**Request:**
```json
{
  "workRecordId": 304,
  "requestedWorkDate": "2025-12-10",
  "requestedStartTime": "09:30",
  "requestedEndTime": "18:30"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 405,
    "contractId": 201,
    "workRecordId": 304,
    "originalWorkDate": "2025-12-10",
    "originalStartTime": "09:00",
    "originalEndTime": "18:00",
    "originalBreakMinutes": 60,
    "requestedWorkDate": "2025-12-10",
    "requestedStartTime": "09:30",
    "requestedEndTime": "18:30",
    "status": "PENDING",
    "reviewedAt": null,
    "createdAt": "2025-12-15T14:30:00",
    "updatedAt": "2025-12-15T14:30:00"
  }
}
```

---

### 9.2 내 요청 목록

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 405,
      "contractId": 201,
      "workRecordId": 304,
      "originalWorkDate": "2025-12-10",
      "originalStartTime": "09:00",
      "originalEndTime": "18:00",
      "requestedWorkDate": "2025-12-10",
      "requestedStartTime": "09:30",
      "requestedEndTime": "18:30",
      "status": "PENDING",
      "reviewedAt": null,
      "createdAt": "2025-12-15T14:30:00",
      "updatedAt": "2025-12-15T14:30:00"
    },
    {
      "id": 406,
      "contractId": 201,
      "workRecordId": 305,
      "originalWorkDate": "2025-12-08",
      "originalStartTime": "09:00",
      "originalEndTime": "18:00",
      "requestedWorkDate": "2025-12-08",
      "requestedStartTime": "10:00",
      "requestedEndTime": "19:00",
      "status": "PENDING",
      "reviewedAt": null,
      "createdAt": "2025-12-14T16:00:00",
      "updatedAt": "2025-12-14T16:00:00"
    },
    {
      "id": 407,
      "contractId": 201,
      "workRecordId": 303,
      "originalWorkDate": "2025-12-05",
      "originalStartTime": "09:00",
      "originalEndTime": "18:00",
      "requestedWorkDate": "2025-12-05",
      "requestedStartTime": "09:00",
      "requestedEndTime": "17:00",
      "status": "APPROVED",
      "reviewedAt": "2025-12-10T14:30:00",
      "createdAt": "2025-12-09T15:00:00",
      "updatedAt": "2025-12-10T14:30:00"
    },
    {
      "id": 408,
      "contractId": 201,
      "workRecordId": 306,
      "originalWorkDate": "2025-12-01",
      "originalStartTime": "09:00",
      "originalEndTime": "18:00",
      "requestedWorkDate": "2025-12-01",
      "requestedStartTime": "14:30",
      "requestedEndTime": "22:00",
      "status": "REJECTED",
      "reviewedAt": "2025-12-05T11:00:00",
      "createdAt": "2025-12-04T17:00:00",
      "updatedAt": "2025-12-05T11:00:00"
    }
  ]
}
```

---

### 9.3 요청 상세

**Response (PENDING):**
```json
{
  "success": true,
  "data": {
    "id": 405,
    "contractId": 201,
    "workRecordId": 304,
    "originalWorkDate": "2025-12-10",
    "originalStartTime": "09:00",
    "originalEndTime": "18:00",
    "originalBreakMinutes": 60,
    "requestedWorkDate": "2025-12-10",
    "requestedStartTime": "09:30",
    "requestedEndTime": "18:30",
    "requestedBreakMinutes": 60,
    "status": "PENDING",
    "reviewedAt": null,
    "createdAt": "2025-12-15T14:30:00",
    "updatedAt": "2025-12-15T14:30:00"
  }
}
```

**Response (APPROVED/REJECTED):**
```json
{
  "success": true,
  "data": {
    "id": 407,
    "contractId": 201,
    "workRecordId": 303,
    "originalWorkDate": "2025-12-05",
    "originalStartTime": "09:00",
    "originalEndTime": "18:00",
    "originalBreakMinutes": 60,
    "requestedWorkDate": "2025-12-05",
    "requestedStartTime": "09:00",
    "requestedEndTime": "17:00",
    "requestedBreakMinutes": 60,
    "status": "APPROVED",
    "reviewedAt": "2025-12-10T14:30:00",
    "createdAt": "2025-12-09T15:00:00",
    "updatedAt": "2025-12-10T14:30:00"
  }
}
```

---

### 9.4 요청 취소

**Response:**
```json
{
  "success": true,
  "message": "정정 요청이 취소되었습니다."
}
```

---

### 10.1 급여 목록

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 501,
      "contractId": 201,
      "workerName": "박민준",
      "year": 2025,
      "month": 10,
      "basePay": 440132,
      "overtimePay": 10000,
      "nightPay": 5000,
      "holidayPay": 0,
      "deduction": 22000,
      "netPay": 433132,
      "createdAt": "2025-11-05T09:00:00",
      "updatedAt": "2025-11-05T09:00:00"
    },
    {
      "id": 502,
      "contractId": 201,
      "workerName": "박민준",
      "year": 2025,
      "month": 11,
      "basePay": 440132,
      "overtimePay": 15090,
      "nightPay": 0,
      "holidayPay": 0,
      "deduction": 22000,
      "netPay": 433222,
      "createdAt": "2025-12-05T09:00:00",
      "updatedAt": "2025-12-05T09:00:00"
    },
    {
      "id": 503,
      "contractId": 201,
      "workerName": "박민준",
      "year": 2025,
      "month": 12,
      "basePay": 380264,
      "overtimePay": 0,
      "nightPay": 0,
      "holidayPay": 0,
      "deduction": 19000,
      "netPay": 361264,
      "createdAt": "2025-12-10T09:00:00",
      "updatedAt": "2025-12-15T10:30:00"
    }
  ]
}
```

---

### 10.2 급여 상세

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 502,
    "contractId": 201,
    "workerId": 2001,
    "workerName": "박민준",
    "workplaceId": 101,
    "workplaceName": "홍대점",
    "year": 2025,
    "month": 11,
    "totalWorkHours": 44.0,
    "hourlyWage": 10030,
    "basePay": 440132,
    "overtimePay": 15090,
    "nightPay": 0,
    "holidayPay": 0,
    "totalAllowance": 15090,
    "deduction": 22000,
    "netPay": 433222,
    "paymentStatus": "PENDING",
    "createdAt": "2025-12-05T09:00:00",
    "updatedAt": "2025-12-05T09:00:00"
  }
}
```

---

### 10.3 송금 내역

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 601,
      "salaryId": 501,
      "contractId": 201,
      "workerName": "박민준",
      "year": 2025,
      "month": 10,
      "amount": 433132,
      "paymentMethod": "BANK_TRANSFER",
      "status": "COMPLETED",
      "paymentDate": "2025-10-25T09:00:00",
      "createdAt": "2025-10-25T09:00:00",
      "updatedAt": "2025-10-25T09:00:00"
    },
    {
      "id": 602,
      "salaryId": 502,
      "contractId": 201,
      "workerName": "박민준",
      "year": 2025,
      "month": 11,
      "amount": 433222,
      "paymentMethod": "BANK_TRANSFER",
      "status": "COMPLETED",
      "paymentDate": "2025-11-25T09:00:00",
      "createdAt": "2025-11-25T09:00:00",
      "updatedAt": "2025-11-25T09:00:00"
    },
    {
      "id": 603,
      "salaryId": 503,
      "contractId": 201,
      "workerName": "박민준",
      "year": 2025,
      "month": 12,
      "amount": 361264,
      "paymentMethod": "BANK_TRANSFER",
      "status": "PENDING",
      "paymentDate": null,
      "createdAt": "2025-12-10T09:00:00",
      "updatedAt": "2025-12-15T10:30:00"
    }
  ]
}
```

---

### 11.1 알림 목록

**Response:**
```json
{
  "success": true,
  "data": {
    "notifications": [
      {
        "id": 701,
        "userId": 1001,
        "type": "SCHEDULE_CHANGE",
        "title": "근무 일정 변경",
        "message": "박민준님의 근무 일정이 변경되었습니다.",
        "referenceId": 301,
        "isRead": false,
        "createdAt": "2025-12-15T14:30:00",
        "updatedAt": "2025-12-15T14:30:00"
      },
      {
        "id": 702,
        "userId": 2001,
        "type": "PAYMENT_SUCCESS",
        "title": "급여 송금 완료",
        "message": "2025년 11월 급여가 송금되었습니다.",
        "referenceId": 601,
        "isRead": true,
        "createdAt": "2025-12-10T09:00:00",
        "updatedAt": "2025-12-10T09:30:00"
      },
      {
        "id": 703,
        "userId": 1001,
        "type": "CORRECTION_REQUEST",
        "title": "정정 요청 생성",
        "message": "박민준님이 근무 시간 정정을 요청했습니다.",
        "referenceId": 405,
        "isRead": false,
        "createdAt": "2025-12-15T14:30:00",
        "updatedAt": "2025-12-15T14:30:00"
      },
      {
        "id": 704,
        "userId": 2001,
        "type": "CORRECTION_RESPONSE",
        "title": "정정 요청 처리",
        "message": "정정 요청이 승인되었습니다.",
        "referenceId": 407,
        "isRead": true,
        "createdAt": "2025-12-10T14:30:00",
        "updatedAt": "2025-12-10T14:45:00"
      }
    ],
    "unreadCount": 2
  }
}
```

---

### 11.2 SSE 알림 구독

**Connection Established:**
```
GET /api/notifications/stream HTTP/1.1
Content-Type: text/event-stream
```

**Event Type 1: notification**
```
event: notification
data: {
  "id": 705,
  "userId": 1001,
  "type": "CORRECTION_REQUEST",
  "title": "정정 요청 생성",
  "message": "최수진님이 근무 시간 정정을 요청했습니다.",
  "referenceId": 406,
  "isRead": false,
  "createdAt": "2025-12-15T15:00:00"
}
```

**Event Type 2: unread_count**
```
event: unread_count
data: {"unread_count": 3}
```

**Event Type 3: notification (다른 타입)**
```
event: notification
data: {
  "id": 706,
  "userId": 2002,
  "type": "PAYMENT_SUCCESS",
  "title": "급여 송금 완료",
  "message": "2025년 12월 급여가 송금되었습니다.",
  "referenceId": 603,
  "isRead": false,
  "createdAt": "2025-12-15T15:15:00"
}
```

---

### 11.3 읽지 않은 알림 개수

**Response:**
```json
{
  "success": true,
  "data": 5
}
```

---

### 11.4 알림 읽음

**Response:**
```json
{
  "success": true,
  "message": "알림이 읽음 처리되었습니다."
}
```

---

### 11.5 전체 읽음

**Response:**
```json
{
  "success": true,
  "message": "모든 알림이 읽음 처리되었습니다."
}
```

---

### 11.6 알림 삭제

**Response:**
```json
{
  "success": true,
  "message": "알림이 삭제되었습니다."
}
```

---

### 12.1 내 설정 조회

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "userId": 1001,
    "notificationEnabled": true,
    "pushEnabled": true,
    "emailEnabled": false,
    "smsEnabled": false,
    "scheduleChangeAlertEnabled": true,
    "paymentAlertEnabled": true,
    "correctionRequestAlertEnabled": true,
    "createdAt": "2025-12-10T14:30:00",
    "updatedAt": "2025-12-10T14:30:00"
  }
}
```

---

### 12.2 내 설정 수정

**Request:**
```json
{
  "notificationEnabled": true,
  "pushEnabled": false,
  "emailEnabled": true,
  "smsEnabled": false,
  "scheduleChangeAlertEnabled": true,
  "paymentAlertEnabled": true,
  "correctionRequestAlertEnabled": false
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "userId": 1001,
    "notificationEnabled": true,
    "pushEnabled": false,
    "emailEnabled": true,
    "smsEnabled": false,
    "scheduleChangeAlertEnabled": true,
    "paymentAlertEnabled": true,
    "correctionRequestAlertEnabled": false,
    "createdAt": "2025-12-10T14:30:00",
    "updatedAt": "2025-12-15T10:00:00"
  }
}
```

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
- v1.3 (2025-12-09): 회원가입 API 제거 (카카오 로그인에서 자동 처리), 카카오 로그인 설명 보강
- v1.4 (2025-12-10): 사용자 설정 API 구현 완료 (12.1-12.2), 알림 설정 관리 기능 추가
- v1.5 (2025-12-11): 정정 요청 API 간소화 - reviewComment와 reason 필드 제거, 승인/반려 시 요청 본문 불필요, UserSettings 회원가입 시 자동 생성
- v1.6 (2025-12-29): 엔티티 구조 업데이트 반영
  - WorkRecord에 breakMinutes, totalWorkMinutes, 급여 필드(baseSalary, nightSalary, holidaySalary, totalSalary) 추가
  - WorkRecordStatus에 DELETED 상태 추가
  - CorrectionRequest에 type 필드 추가 (CREATE, UPDATE, DELETE)
  - Notification에 actionType, actionData 필드 추가
  - Workplace에 isLessThanFiveEmployees 필드 추가
  - WorkerContract에 payrollDeductionType 필드 추가
