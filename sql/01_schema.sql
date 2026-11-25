-- 在 hospital 库执行。此脚本重建下列八张表，已有数据会被删除。
-- 业务上不物理删除基础数据，一律通过 status 停用；外键显式限制删除被引用记录。
BEGIN;

DROP TABLE IF EXISTS public.charge;
DROP TABLE IF EXISTS public.registration;
DROP TABLE IF EXISTS public.slot;
DROP TABLE IF EXISTS public.schedule;
DROP TABLE IF EXISTS public.sys_user;
DROP TABLE IF EXISTS public.patient;
DROP TABLE IF EXISTS public.doctor;
DROP TABLE IF EXISTS public.dept;

CREATE TABLE public.dept (
    dept_id SERIAL PRIMARY KEY,
    dept_name VARCHAR(60) NOT NULL UNIQUE,
    dept_phone VARCHAR(20),
    location VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT '启用' CHECK (status IN ('启用', '停用'))
);

CREATE TABLE public.doctor (
    doctor_id SERIAL PRIMARY KEY,
    doctor_name VARCHAR(60) NOT NULL,
    dept_id INT NOT NULL REFERENCES public.dept (dept_id) ON DELETE RESTRICT,
    title VARCHAR(60) NOT NULL CHECK (title IN ('主任医师', '副主任医师', '主治医师', '住院医师')),
    reg_fee NUMERIC(8,2) NOT NULL CHECK (reg_fee > 0),
    phone VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT '在职' CHECK (status IN ('在职', '离职'))
);

CREATE TABLE public.patient (
    patient_id SERIAL PRIMARY KEY,
    patient_name VARCHAR(60) NOT NULL,
    gender VARCHAR(10) CHECK (gender IN ('男', '女')),
    birth_date DATE,
    id_card VARCHAR(30) UNIQUE,
    phone VARCHAR(20),
    address VARCHAR(200),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE public.schedule (
    schedule_id SERIAL PRIMARY KEY,
    doctor_id INT NOT NULL REFERENCES public.doctor (doctor_id) ON DELETE RESTRICT,
    work_date DATE NOT NULL,
    period VARCHAR(20) NOT NULL CHECK (period IN ('上午', '下午')),
    slot_total INT NOT NULL CHECK (slot_total > 0),
    status VARCHAR(20) DEFAULT '正常' CHECK (status IN ('正常', '停诊')),
    CONSTRAINT uq_schedule_doctor_date_period UNIQUE (doctor_id, work_date, period)
);

CREATE TABLE public.slot (
    slot_id SERIAL PRIMARY KEY,
    schedule_id INT NOT NULL REFERENCES public.schedule (schedule_id) ON DELETE RESTRICT,
    seq_no INT NOT NULL CHECK (seq_no > 0),
    status VARCHAR(20) DEFAULT '可挂' CHECK (status IN ('可挂', '已挂', '停用')),
    CONSTRAINT uq_slot_schedule_seq UNIQUE (schedule_id, seq_no)
);

CREATE INDEX idx_slot_schedule_status ON public.slot (schedule_id, status);

CREATE TABLE public.registration (
    reg_id SERIAL PRIMARY KEY,
    patient_id INT NOT NULL REFERENCES public.patient (patient_id) ON DELETE RESTRICT,
    slot_id INT NOT NULL UNIQUE REFERENCES public.slot (slot_id) ON DELETE RESTRICT,
    reg_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reg_type VARCHAR(20) NOT NULL CHECK (reg_type IN ('普通', '专家')),
    reg_fee NUMERIC(8,2) NOT NULL,
    status VARCHAR(20) DEFAULT '已挂号' CHECK (status IN ('已挂号', '已就诊', '已退号')),
    operator_id INT,
    refund_time TIMESTAMP,
    refund_reason VARCHAR(200)
);

CREATE INDEX idx_reg_patient ON public.registration (patient_id);
CREATE INDEX idx_reg_time ON public.registration (reg_time);
CREATE INDEX idx_reg_status ON public.registration (status);

CREATE TABLE public.charge (
    charge_id SERIAL PRIMARY KEY,
    reg_id INT NOT NULL UNIQUE REFERENCES public.registration (reg_id) ON DELETE RESTRICT,
    amount NUMERIC(8,2) NOT NULL CHECK (amount >= 0),
    pay_method VARCHAR(20) CHECK (pay_method IN ('现金', '微信', '支付宝', '银行卡')),
    pay_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    refund_time TIMESTAMP,
    status VARCHAR(20) DEFAULT '已收费' CHECK (status IN ('已收费', '已退费')),
    operator_id INT
);

CREATE TABLE public.sys_user (
    user_id SERIAL PRIMARY KEY,
    username VARCHAR(30) NOT NULL UNIQUE,
    pwd_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('挂号员', '医生', '管理员')),
    doctor_id INT REFERENCES public.doctor (doctor_id),
    status VARCHAR(20) DEFAULT '启用' CHECK (status IN ('启用', '停用'))
);

ALTER TABLE public.registration ADD CONSTRAINT fk_registration_operator
    FOREIGN KEY (operator_id) REFERENCES public.sys_user (user_id);

ALTER TABLE public.charge ADD CONSTRAINT fk_charge_operator
    FOREIGN KEY (operator_id) REFERENCES public.sys_user (user_id);

COMMIT;
