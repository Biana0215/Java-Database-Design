package hospital.service;

import hospital.dao.DeptDao;
import hospital.entity.Dept;
import hospital.util.DBUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * 科室业务类：负责科室查询、新增、修改与启停用，写操作均在同一事务内完成。
 */
public class DeptService {

    private final DeptDao deptDao = new DeptDao();

    /**
     * 查询全部科室。
     */
    public List<Dept> listAll() {
        try (Connection conn = DBUtil.getConnection()) {
            return deptDao.findAll(conn);
        } catch (SQLException e) {
            throw new BusinessException("查询科室失败：" + e.getMessage(), e);
        }
    }

    /**
     * 查询启用的科室。
     */
    public List<Dept> listEnabled() {
        try (Connection conn = DBUtil.getConnection()) {
            return deptDao.findEnabled(conn);
        } catch (SQLException e) {
            throw new BusinessException("查询科室失败：" + e.getMessage(), e);
        }
    }

    /**
     * 按编号查询科室，不存在时抛出业务异常。
     */
    public Dept get(int deptId) {
        if (deptId <= 0) {
            throw new BusinessException("科室编号非法：" + deptId);
        }
        try (Connection conn = DBUtil.getConnection()) {
            Dept dept = deptDao.findById(conn, deptId);
            if (dept == null) {
                throw new BusinessException("科室不存在");
            }
            return dept;
        } catch (SQLException e) {
            throw new BusinessException("查询科室失败：" + e.getMessage(), e);
        }
    }

    /**
     * 新增科室，返回新科室编号；名称重复时给出业务提示。
     */
    public int create(Dept dept) {
        if (dept == null) {
            throw new BusinessException("科室信息不能为空");
        }
        String name = requireName(dept.getDeptName());
        String status = dept.getStatus() == null || dept.getStatus().trim().isEmpty()
                ? "启用" : requireStatus(dept.getStatus());
        dept.setDeptName(name);
        dept.setDeptPhone(trimToNull(dept.getDeptPhone()));
        dept.setLocation(trimToNull(dept.getLocation()));
        dept.setStatus(status);

        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            if (deptDao.findByName(conn, name) != null) {
                throw new BusinessException("该科室名称已存在");
            }
            int deptId = deptDao.insert(conn, dept);
            if (deptId <= 0) {
                throw new SQLException("未取回新增科室的编号");
            }

            conn.commit();
            dept.setDeptId(deptId);
            return deptId;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
            }
            if (isUniqueViolation(e)) {
                throw new BusinessException("该科室名称已存在", e);
            }
            throw new BusinessException("新增科室失败：" + e.getMessage(), e);
        } finally {
            DBUtil.close(conn);
        }
    }

    /**
     * 修改科室名称、电话和位置，科室状态仍由 changeStatus 单独维护。
     */
    public void update(Dept dept) {
        if (dept == null) {
            throw new BusinessException("科室信息不能为空");
        }
        if (dept.getDeptId() <= 0) {
            throw new BusinessException("科室编号非法：" + dept.getDeptId());
        }
        String name = requireName(dept.getDeptName());

        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            Dept existing = deptDao.findById(conn, dept.getDeptId());
            if (existing == null) {
                throw new BusinessException("科室不存在");
            }
            Dept sameName = deptDao.findByName(conn, name);
            if (sameName != null && sameName.getDeptId() != dept.getDeptId()) {
                throw new BusinessException("该科室名称已存在");
            }

            dept.setDeptName(name);
            dept.setDeptPhone(trimToNull(dept.getDeptPhone()));
            dept.setLocation(trimToNull(dept.getLocation()));
            dept.setStatus(existing.getStatus());
            if (!deptDao.update(conn, dept)) {
                throw new BusinessException("科室信息修改失败，请刷新后重试");
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
                throw new BusinessException("该科室名称已存在", e);
            }
            throw new BusinessException("修改科室失败：" + e.getMessage(), e);
        } finally {
            DBUtil.close(conn);
        }
    }

    /**
     * 启用或停用科室，status 只允许“启用/停用”。
     */
    public void changeStatus(int deptId, String status) {
        if (deptId <= 0) {
            throw new BusinessException("科室编号非法：" + deptId);
        }
        String value = requireStatus(status);

        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            if (deptDao.findById(conn, deptId) == null) {
                throw new BusinessException("科室不存在");
            }
            if (!deptDao.updateStatus(conn, deptId, value)) {
                throw new BusinessException("科室状态修改失败，请刷新后重试");
            }

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
            }
            throw new BusinessException("修改科室状态失败：" + e.getMessage(), e);
        } finally {
            DBUtil.close(conn);
        }
    }

    /**
     * 校验科室名称必填并去除首尾空白。
     */
    private String requireName(String name) {
        String value = name == null ? "" : name.trim();
        if (value.isEmpty()) {
            throw new BusinessException("科室名称不能为空");
        }
        return value;
    }

    /**
     * 校验科室状态枚举值。
     */
    private String requireStatus(String status) {
        String value = status == null ? "" : status.trim();
        if (value.isEmpty()) {
            throw new BusinessException("状态值不能为空");
        }
        if (!"启用".equals(value) && !"停用".equals(value)) {
            throw new BusinessException("状态值非法：" + value);
        }
        return value;
    }

    /**
     * 可空文本去空白，空串转 null，避免写入无意义空字符串。
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
