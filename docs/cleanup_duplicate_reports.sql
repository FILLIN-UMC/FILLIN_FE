-- ============================================================
-- FILLIN 제보 중복 데이터 정리 스크립트
-- ============================================================
-- 사용법: MySQL 클라이언트에서 DB 연결 후 실행
--   mysql -u [사용자] -p [DB명] < cleanup_duplicate_reports.sql
--
-- 또는 MySQL Workbench, DBeaver 등에서 직접 실행
--
-- 주의: 실행 전 반드시 DB 백업 권장
-- 백엔드 코드 수정 없이 DB에서 직접 중복 제거
--
-- 중복 기준: member_id, address, latitude, longitude, title 동일
-- 각 그룹에서 report_id가 가장 작은 1건만 유지, 나머지 삭제
-- ============================================================

-- ========== 1단계: 중복 제보 확인 (먼저 실행) ==========
-- member_id, address, latitude, longitude, title이 동일한 제보 그룹
-- duplicate_count > 1 이면 중복 존재
SELECT 
    member_id, address, latitude, longitude, title,
    COUNT(*) as duplicate_count,
    GROUP_CONCAT(report_id ORDER BY report_id) as report_ids
FROM report
WHERE status = 'PUBLISHED'
GROUP BY member_id, IFNULL(address, ''), latitude, longitude, IFNULL(title, '')
HAVING COUNT(*) > 1;

-- ========== 2단계: 삭제 대상 report_id 확인 (선택) ==========
-- 실제 삭제 전 삭제될 report_id 목록 확인
/*
SELECT r.report_id, r.address, r.title
FROM report r
INNER JOIN (
    SELECT member_id, IFNULL(address,'') a, latitude, longitude, IFNULL(title,'') t, MIN(report_id) as keep_id
    FROM report WHERE status = 'PUBLISHED'
    GROUP BY member_id, IFNULL(address,''), latitude, longitude, IFNULL(title,'')
    HAVING COUNT(*) > 1
) dup ON r.member_id = dup.member_id 
    AND IFNULL(r.address,'') = dup.a 
    AND r.latitude = dup.latitude 
    AND r.longitude = dup.longitude 
    AND IFNULL(r.title,'') = dup.t
WHERE r.report_id != dup.keep_id;
*/

-- ========== 3단계: 중복 제보 삭제 (실행) ==========
-- 트랜잭션으로 감싸서 안전하게 실행

START TRANSACTION;

-- 3-1. 삭제할 report_id를 임시 테이블에 저장
CREATE TEMPORARY TABLE IF NOT EXISTS report_ids_to_delete AS
SELECT r.report_id
FROM report r
INNER JOIN (
    SELECT member_id, address, latitude, longitude, title, MIN(report_id) as keep_id
    FROM report
    WHERE status = 'PUBLISHED'
    GROUP BY member_id, address, latitude, longitude, title
    HAVING COUNT(*) > 1
) dup ON r.member_id = dup.member_id 
    AND IFNULL(r.address, '') = IFNULL(dup.address, '')
    AND r.latitude = dup.latitude 
    AND r.longitude = dup.longitude 
    AND IFNULL(r.title, '') = IFNULL(dup.title, '')
WHERE r.report_id != dup.keep_id
  AND r.status = 'PUBLISHED';

-- 3-2. 해당 제보의 feedback 삭제
DELETE FROM feedback 
WHERE report_id IN (SELECT report_id FROM report_ids_to_delete);

-- 3-3. 해당 제보의 likes 삭제
DELETE FROM likes 
WHERE report_id IN (SELECT report_id FROM report_ids_to_delete);

-- 3-4. 중복 제보 삭제
DELETE FROM report 
WHERE report_id IN (SELECT report_id FROM report_ids_to_delete);

-- 3-5. 삭제된 행 수 확인
SELECT ROW_COUNT() AS deleted_reports_count;

COMMIT;

-- 임시 테이블 정리 (세션 종료 시 자동 삭제됨)
DROP TEMPORARY TABLE IF EXISTS report_ids_to_delete;
