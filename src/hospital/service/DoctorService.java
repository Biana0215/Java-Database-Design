package hospital.service;

import hospital.dao.DeptDao;
import hospital.dao.DoctorDao;
import hospital.entity.Doctor;
import hospital.util.DBUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * 医生业务类：负责医生查询、新增、修改与在职状态维护，并校验职称、挂号费和所属科室。
 */
public class DoctorService {

    private final DoctorDao doctorDao = new DoctorDao();
    private final DeptDao deptDao = new DeptDao();

    /**
     * 查询全部医生。
     */
    public List<Doctor> listAll() {
        try (Connection conn = DBUtil.getConnection()) {
            return doctorDao.findAll(conn);
        } catch (SQLException e) {
            throw new BusinessException("查询医生失败：" + e.getMessage(), e);
        }
    }

    /**
     * 查询指定科室下在职的医生。
     */
    public List<Doctor> listByDept(int deptId) {
        if (deptId <= 0) {
            throw new BusinessException("科室编号非法：" + deptId);
        }
        try (Connection conn = DBUtil.getConnection()) {
            return doctorDao.findActiveByDept(conn, deptId);
        } catch (SQLException e) {
            throw new BusinessException("查询医生失败：" + e.getMessage(), e);
        }
    }

    /**
     * 按编号查询医生，不存在时抛出业务异常。
     */
    public Doctor get(int doctorId) {
        if (doctorId <= 0) {
            throw new BusinessException("医生编号非法：" + doctorId);
        }
        try (Connection conn = DBUtil.getConnection()) {
            Doctor doctor = doctorDao.findById(conn, doctorId);
            if (doctor == null) {
                throw new BusinessException("医生不存在");
            }
            return doctor;
        } catch (SQLException e) {
            throw new BusinessException("查询医生失败：" + e.getMessage(), e);
        }
    }

    /**
     * 新增医生，返回新医生编号；姓名、职称、挂号费和所属科室均在此校验。
     */
    public int create(Doctor d) {
        if (d == null) {
            throw new BusinessException("医生信息不能为空");
        }
        String name = requireText(d.getDoctorName(), "医生姓名不能为空");
        if (d.getDeptId() <= 0) {
            throw new BusinessException("所属科室不能为空");
        }
        String title = requireTitle(d.getTitle());
        BigDecimal regFee = requireRegFee(d.getRegFee());
        String status = d.getStatus() == null || d.getStatus().trim().isEmpty()
                ? "在职" : requireStatus(d.getStatus());

        d.setDoctorName(name);
        d.setTitle(title);
        d.setRegFee(regFee);
        d.setPhone(trimToNull(d.getPhone()));
        d.setStatus(status);

        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            if (deptDao.findById(conn, d.getDeptId()) == null) {
                throw new BusinessException("所属科室不存在");
            }
            int doctorId = doctorDao.insert(conn, d);
            if (doctorId <= 0) {
                throw new SQLException("未取回新增医生的编号");
            }

            conn.commit();
            d.setDoctorId(doctorId);
            return doctorId;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
            }
            if (isUniqueViolation(e)) {
                throw new BusinessException("该医生信息已存在", e);
            }
            throw new BusinessException("新增医生失败：" + e.getMessage(), e);
        } finally {
            DBUtil.close(conn);
        }
    }

    /**
     * 修改医生姓名、科室、职称、挂号费、电话和状态。
     */
    public void update(Doctor d) {
        if (d == null) {
            throw new BusinessException("医生信息不能为空");
        }
        if (d.getDoctorId() <= 0) {
            throw new BusinessException("医生编号非法：" + d.getDoctorId());
        }
        String name = requireText(d.getDoctorName(), "医生姓名不能为空");
        if (d.getDeptId() <= 0) {
            throw new BusinessException("所属科室不能为空");
        }
        String title = requireTitle(d.getTitle());
        BigDecimal regFee = requireRegFee(d.getRegFee());

        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            Doctor existing = doctorDao.findById(conn, d.getDoctorId());
            if (existing == null) {
                throw new BusinessException("医生不存在");
            }
            if (deptDao.findById(conn, d.getDeptId()) == null) {
                throw new BusinessException("所属科室不存在");
            }
            String status = d.getStatus() == null || d.getStatus().trim().isEmpty()
                    ? existing.getStatus() : requireStatus(d.getStatus());

            d.setDoctorName(name);
            d.setTitle(title);
            d.setRegFee(regFee);
            d.setPhone(trimToNull(d.getPhone()));
            d.setStatus(status);
            if (!doctorDao.update(conn, d)) {
                throw new BusinessException("医生信息修改失败，请刷新后重试");
            }

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
            }
            if (isUniqueViolation(e)) {
                throw new BusinessException("该医生信息已存在", e);
            }
            throw new BusinessException("修改医生失败：" + e.getMessage(), e);
        } finally {
            DBUtil.close(conn);
        }
    }

    /**
     * 修改医生在职状态，status 只允许“在职/离职”。
     */
    public void changeStatus(int doctorId, String status) {
        if (doctorId <= 0) {
            throw new BusinessException("医生编号非法：" + doctorId);
        }
        String value = requireStatus(status);

        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            if (doctorDao.findById(conn, doctorId) == null) {
                throw new BusinessException("医生不存在");
            }
            if (!doctorDao.updateStatus(conn, doctorId, value)) {
                throw new BusinessException("医生状态修改失败，请刷新后重试");
            }

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
            }
            throw new BusinessException("修改医生状态失败：" + e.getMessage(), e);
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
     * 校验职称只能是表结构规定的四种。
     */
    private String requireTitle(String title) {
        String value = title == null ? "" : title.trim();
        if (value.isEmpty()) {
            throw new BusinessException("职称不能为空");
        }
        if (!"主任医师".equals(value) && !"副主任医师".equals(value)
                && !"主治医师".equals(value) && !"住院医师".equals(value)) {
            throw new BusinessException("职称值非法：" + value);
        }
        return value;
    }

    /**
     * 校验挂号费必须大于 0，不依赖数据库 CHECK 约束报错。
     */
    private BigDecimal requireRegFee(BigDecimal regFee) {
        if (regFee == null) {
            throw new BusinessException("挂号费不能为空");
        }
        if (regFee.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("挂号费必须大于 0");
        }
        return regFee;
    }

    /**
     * 校验医生状态枚举值。
     */
    private String requireStatus(String status) {
        String value = status == null ? "" : status.trim();
        if (value.isEmpty()) {
            throw new BusinessException("状态值不能为空");
        }
        if (!"在职".equals(value) && !"离职".equals(value)) {
            throw new BusinessException("状态值非法：" + value);
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
