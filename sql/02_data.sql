-- 在 hospital 库执行：先运行 01_schema.sql，再向空表导入演示数据。
BEGIN;

INSERT INTO public.dept (dept_name, dept_phone, location)
VALUES
    ('内科',       '010-55550001', '门诊楼一层'),
    ('外科',       '010-55550002', '门诊楼一层'),
    ('儿科',       '010-55550003', '门诊楼一层'),
    ('妇科',       '010-55550004', '门诊楼二层'),
    ('眼科',       '010-55550005', '门诊楼二层'),
    ('耳鼻喉科',   '010-55550006', '门诊楼二层'),
    ('口腔科',     '010-55550007', '门诊楼三层'),
    ('皮肤科',     '010-55550008', '门诊楼三层'),
    ('骨科',       '010-55550009', '门诊楼三层'),
    ('康复医学科', '010-55550010', '门诊楼四层');

INSERT INTO public.doctor (doctor_name, dept_id, title, reg_fee)
SELECT seed.doctor_name, d.dept_id,
       CASE seed.level_no
           WHEN 1 THEN '主任医师' WHEN 2 THEN '副主任医师'
           WHEN 3 THEN '主治医师' ELSE '住院医师'
       END,
       CASE seed.level_no WHEN 1 THEN 50 WHEN 2 THEN 35 WHEN 3 THEN 20 ELSE 10 END
FROM (VALUES
    ('内科', '张明远', 1), ('内科', '王思宁', 2), ('内科', '李文博', 3), ('内科', '赵雨桐', 4),
    ('外科', '陈立新', 1), ('外科', '周海峰', 2), ('外科', '孙嘉怡', 3), ('外科', '吴子墨', 4),
    ('儿科', '郑雪梅', 1), ('儿科', '何晓燕', 2), ('儿科', '林若曦', 3), ('儿科', '高宇轩', 4),
    ('妇科', '许静芳', 1), ('妇科', '胡慧敏', 2), ('妇科', '罗佳琪', 3), ('妇科', '梁婉清', 4),
    ('眼科', '宋启航', 1), ('眼科', '蒋美玲', 2), ('眼科', '谢书涵', 3), ('眼科', '唐语晨', 4),
    ('耳鼻喉科', '冯志平', 1), ('耳鼻喉科', '邓雅琴', 2), ('耳鼻喉科', '韩佳宁', 3), ('耳鼻喉科', '马亦辰', 4),
    ('口腔科', '曹建华', 1), ('口腔科', '曾丽娟', 2), ('口腔科', '彭思源', 3), ('口腔科', '萧子瑜', 4),
    ('皮肤科', '程文娟', 1), ('皮肤科', '袁敏华', 2), ('皮肤科', '潘雨薇', 3), ('皮肤科', '田宇辰', 4),
    ('骨科', '杜志强', 1), ('骨科', '叶振宇', 2), ('骨科', '苏俊杰', 3), ('骨科', '魏嘉树', 4),
    ('康复医学科', '沈安康', 1), ('康复医学科', '吕清和', 2),
    ('康复医学科', '戴嘉乐', 3), ('康复医学科', '顾星辰', 4)
) AS seed (dept_name, doctor_name, level_no)
JOIN public.dept AS d ON d.dept_name = seed.dept_name
ORDER BY d.dept_id, seed.level_no;

-- 四个教学账号使用初始密码 123456 的 SHA-256 小写十六进制摘要。
INSERT INTO public.sys_user (username, pwd_hash, role, doctor_id)
SELECT seed.username,
       '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92',
       seed.role, d.doctor_id
FROM (VALUES ('admin', '管理员', NULL), ('guahao', '挂号员', NULL),
             ('guahao2', '挂号员', NULL), ('doctor1', '医生', '张明远'))
     AS seed (username, role, doctor_name)
LEFT JOIN public.doctor AS d ON d.doctor_name = seed.doctor_name;

-- 编号和地址使用虚构样例；挂号记录产生前已完成建档。
INSERT INTO public.patient (patient_name, gender, birth_date, id_card, phone, address, create_time)
SELECT names.patient_name,
       CASE WHEN MOD(g.n, 2) = 0 THEN '女' ELSE '男' END,
       DATE '1950-01-01' + g.n * 83,
       'DEMO' || LPAD(g.n::VARCHAR, 6, '0'),
       '000' || LPAD(g.n::VARCHAR, 8, '0'),
       '示例社区' || (1 + MOD(g.n - 1, 10))::VARCHAR || '号楼',
       CURRENT_DATE - 30
FROM generate_series(1, 300) AS g(n)
JOIN (VALUES
    (1, '张伟'), (2, '王芳'), (3, '李娜'), (4, '刘洋'), (5, '陈强'),
    (6, '杨静'), (7, '赵磊'), (8, '黄敏'), (9, '周杰'), (10, '吴燕'),
    (11, '徐勇'), (12, '孙丽'), (13, '胡军'), (14, '朱婷'), (15, '高峰'),
    (16, '林霞'), (17, '何涛'), (18, '郭玲'), (19, '马超'), (20, '罗娟'),
    (21, '梁斌'), (22, '宋雪'), (23, '郑刚'), (24, '谢梅'), (25, '韩鹏'),
    (26, '唐倩'), (27, '冯健'), (28, '于萍'), (29, '董浩'), (30, '许慧')
) AS names (name_no, patient_name) ON names.name_no = 1 + MOD(g.n - 1, 30)
ORDER BY g.n;

-- 40 位医生各排一班，前 20 位各加一班；张明远有当天及次日排班。
WITH ranked_doctors AS (
    SELECT doctor_id, ROW_NUMBER() OVER (ORDER BY doctor_id) AS doctor_no
    FROM public.doctor
)
INSERT INTO public.schedule (doctor_id, work_date, period, slot_total, status)
SELECT d.doctor_id,
       CURRENT_DATE + CASE WHEN g.n = 1 THEN 0 WHEN g.n = 41 THEN 1 ELSE MOD(g.n - 1, 5) - 2 END,
       CASE WHEN g.n <= 40 THEN '上午' ELSE '下午' END,
       30,
       CASE WHEN g.n IN (18, 47) THEN '停诊' ELSE '正常' END
FROM generate_series(1, 60) AS g(n)
JOIN ranked_doctors AS d ON d.doctor_no = 1 + MOD(g.n - 1, 40)
ORDER BY g.n;

INSERT INTO public.slot (schedule_id, seq_no)
SELECT s.schedule_id, g.n
FROM public.schedule AS s
CROSS JOIN generate_series(1, 30) AS g(n)
WHERE g.n <= s.slot_total
ORDER BY s.schedule_id, g.n;

-- 按号源序号轮流为各排班造单，因此每个排班都保留未预约号源。
WITH candidates AS (
    SELECT sl.slot_id, sl.seq_no, s.work_date, s.status AS schedule_status,
           d.title, d.reg_fee,
           ROW_NUMBER() OVER (ORDER BY sl.seq_no, s.schedule_id) AS sample_no
    FROM public.slot AS sl
    JOIN public.schedule AS s ON s.schedule_id = sl.schedule_id
    JOIN public.doctor AS d ON d.doctor_id = s.doctor_id
    ORDER BY sl.seq_no, s.schedule_id
    LIMIT 1000
),
patients AS (
    SELECT patient_id, ROW_NUMBER() OVER (ORDER BY patient_id) AS patient_no
    FROM public.patient
),
examples AS (
    SELECT c.*,
           CASE
               WHEN c.work_date = CURRENT_DATE AND c.schedule_status = '正常'
                    AND MOD(c.seq_no, 5) = 0 THEN '已退号'
               WHEN c.work_date < CURRENT_DATE AND c.schedule_status = '正常'
                    AND MOD(c.seq_no, 4) <> 0 THEN '已就诊'
               ELSE '已挂号'
           END AS reg_status,
           CASE
               WHEN c.work_date < CURRENT_DATE THEN c.work_date + INTERVAL '8 hours'
               WHEN c.work_date = CURRENT_DATE THEN GREATEST(
                   CAST(CURRENT_DATE AS TIMESTAMP), CURRENT_TIMESTAMP - INTERVAL '20 minutes')
               ELSE CURRENT_TIMESTAMP - INTERVAL '20 minutes'
           END AS booked_at
    FROM candidates AS c
)
INSERT INTO public.registration
    (patient_id, slot_id, reg_time, reg_type, reg_fee, status, operator_id, refund_time, refund_reason)
SELECT p.patient_id, e.slot_id, e.booked_at,
       CASE WHEN e.title IN ('主任医师', '副主任医师') THEN '专家' ELSE '普通' END,
       e.reg_fee, e.reg_status, u.user_id,
       CASE WHEN e.reg_status = '已退号' THEN CURRENT_TIMESTAMP END,
       CASE WHEN e.reg_status = '已退号' THEN '行程调整' END
FROM examples AS e
JOIN patients AS p ON p.patient_no = 1 + MOD(e.sample_no - 1, 300)
-- 每三张挂号单依次分配给 guahao、guahao、guahao2。
JOIN public.sys_user AS u ON u.username =
    CASE WHEN MOD(e.sample_no, 3) = 0 THEN 'guahao2' ELSE 'guahao' END
ORDER BY e.sample_no;

-- 停诊只停用未被占用的号源；退号示例仅来自正常排班。
UPDATE public.slot AS sl
SET status = states.slot_status
FROM (
    SELECT x.slot_id,
           CASE WHEN r.status IN ('已挂号', '已就诊') THEN '已挂'
                WHEN s.status = '停诊' THEN '停用'
                ELSE '可挂' END AS slot_status
    FROM public.slot AS x
    JOIN public.schedule AS s ON s.schedule_id = x.schedule_id
    LEFT JOIN public.registration AS r ON r.slot_id = x.slot_id
) AS states
WHERE sl.slot_id = states.slot_id AND sl.status <> states.slot_status;

-- 部分已挂号单保留待收费状态；已退号样例同时保留对应退款记录。
INSERT INTO public.charge (reg_id, amount, pay_method, pay_time, refund_time, status, operator_id)
SELECT r.reg_id, r.reg_fee,
       CASE MOD(sl.seq_no, 4)
           WHEN 0 THEN '现金' WHEN 1 THEN '微信' WHEN 2 THEN '支付宝' ELSE '银行卡'
       END,
       r.reg_time,
       CASE WHEN r.status = '已退号' THEN r.refund_time END,
       CASE WHEN r.status = '已退号' THEN '已退费' ELSE '已收费' END,
       r.operator_id
FROM public.registration AS r
JOIN public.slot AS sl ON sl.slot_id = r.slot_id
WHERE r.status <> '已挂号' OR MOD(sl.seq_no, 6) <> 0
ORDER BY r.reg_id;

COMMIT;
