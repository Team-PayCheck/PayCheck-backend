-- Issue #155: 주간 연장수당 계산 로직 수정에 따른 데이터 마이그레이션
-- OVERTIME_RATE이 1.5에서 0.5(가산분만)으로 변경됨
-- 기존 overtime_amount 값들을 1/3로 조정 (1.5 → 0.5 = 1/3)

UPDATE weekly_allowances
SET overtime_amount = overtime_amount / 3
WHERE overtime_amount > 0;
