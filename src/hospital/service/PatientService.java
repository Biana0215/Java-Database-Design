package hospital.service;

import hospital.dao.PatientDao;
import hospital.entity.Patient;
import hospital.util.DBUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 患者业务类：负责建档、查询与资料修改，证件号唯一冲突转换为中文业务提示。
 */
public class PatientService {

    private final PatientDao patientDao = new PatientDao();

    /**
     * 按编号查询患者，不存在时抛出业务异常。
     */
    public Patient get(int patientId) {
        if (patientId <= 0) {
            throw new BusinessException("患者编号非法：" + patientId);
        }
        try (Connection conn = DBUtil.getConnection()) {
            Patient patient = patientDao.findById(conn, patientId);
            if (patient == null) {
                throw new BusinessException("患者不存在");
            }
            return patient;
        } catch (SQLException e) {
            throw new BusinessException("查询患者失败：" + e.getMessage(), e);
        }
    }

    /**
     * 按身份证号查询患者，未建档时抛出业务异常。
     */
    public Patient getByIdCard(String idCard) {
        String value = idCard == null ? "" : idCard.trim();
        if (value.isEmpty()) {
            throw new BusinessException("证件号不能为空");
        }
        try (Connection conn = DBUtil.getConnection()) {
            Patient patient = patientDao.findByIdCard(conn, value);
            if (patient == null) {
                throw new BusinessException("该证件号未建档");
            }
            return patient;
        } catch (SQLException e) {
            throw new BusinessException("查询患者失败：" + e.getMessage(), e);
        }
    }

    /**
     * 按姓名、证件号或电话模糊查询患者，keyword 为空时返回前 200 条。
     */
    public List<Patient> search(String keyword) {
        String value = keyword == null ? null : keyword.trim();
        if (value != null && value.isEmpty()) {
            value = null;
        }
        try (Connection conn = DBUtil.getConnection()) {
            return patientDao.search(conn, value);
        } catch (SQLException e) {
            throw new BusinessException("查询患者失败：" + e.getMessage(), e);
        }
    }

    /**
     * 新建患者档案，返回患者编号；证件号重复时提示“该证件号已建档”。
     */
    public int create(Patient p) {
        if (p == null) {
            throw new BusinessException("患者信息不能为空");
        }
        String name = requireText(p.getPatientName(), "患者姓名不能为空");
        String idCard = trimToNull(p.getIdCard());
        String gender = normalizeGender(p.getGender());

        p.setPatientName(name);
        p.setIdCard(idCard);
        p.setGender(gender);
        p.setPhone(trimToNull(p.getPhone()));
        p.setAddress(trimToNull(p.getAddress()));
        if (p.getCreateTime() == null) {
            p.setCreateTime(LocalDateTime.now());
        }

        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            if (idCard != null && patientDao.findByIdCard(conn, idCard) != null) {
                throw new BusinessException("该证件号已建档");
            }
            int patientId = patientDao.insert(conn, p);
            if (patientId <= 0) {
                throw new SQLException("未取回新增患者的编号");
            }

            conn.commit();
            p.setPatientId(patientId);
            return patientId;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
            }
            if (isUniqueViolation(e)) {
                throw new BusinessException("该证件号已建档", e);
            }
            throw new BusinessException("新建患者失败：" + e.getMessage(), e);
        } finally {
            DBUtil.close(conn);
        }
    }

    /**
     * 修改患者资料，只允许改姓名、性别、出生日期、电话和地址，证件号与建档时间不变。
     */
    public void update(Patient p) {
        if (p == null) {
            throw new BusinessException("患者信息不能为空");
        }
        if (p.getPatientId() <= 0) {
            throw new BusinessException("患者编号非法：" + p.getPatientId());
        }
        String name = requireText(p.getPatientName(), "患者姓名不能为空");
        String gender = normalizeGender(p.getGender());

        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            Patient existing = patientDao.findById(conn, p.getPatientId());
            if (existing == null) {
                throw new BusinessException("患者不存在");
            }

            existing.setPatientName(name);
            existing.setGender(gender);
            existing.setBirthDate(p.getBirthDate());
            existing.setPhone(trimToNull(p.getPhone()));
            existing.setAddress(trimToNull(p.getAddress()));
            if (!patientDao.update(conn, existing)) {
                throw new BusinessException("患者信息修改失败，请刷新后重试");
            }

            conn.commit();
            p.setPatientName(name);
            p.setGender(gender);
            p.setPhone(existing.getPhone());
            p.setAddress(existing.getAddress());
            p.setIdCard(existing.getIdCard());
            p.setCreateTime(existing.getCreateTime());
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
            }
            if (isUniqueViolation(e)) {
                throw new BusinessException("该证件号已建档", e);
            }
            throw new BusinessException("修改患者失败：" + e.getMessage(), e);
        } finally {
            DBUtil.close(conn);
        }
    }

    /**
     * 校验文本必填并去除首尾空白。
     */
    private String requireText(String value, String message) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw new BusinessException(message);
        }
        return trimmed;
    }

    /**
     * 校验性别：允许为空，非空时只能是“男/女”。
     */
    private String normalizeGender(String gender) {
        String value = trimToNull(gender);
        if (value != null && !"男".equals(value) && !"女".equals(value)) {
            throw new BusinessException("性别值非法：" + value);
        }
        return value;
    }

    /**
     * 可空文本去空白，空串转 null。
     */
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 判断是否为唯一约束冲突。
     */
    private static boolean isUniqueViolation(SQLException e) {
        return "23505".equals(e.getSQLState());
    }
}
