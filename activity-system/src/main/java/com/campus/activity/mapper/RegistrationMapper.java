package com.campus.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.activity.dto.RegistrationExportVO;
import com.campus.activity.dto.RegistrationVO;
import com.campus.activity.dto.WaitingQueueVO;
import com.campus.activity.entity.Registration;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 报名记录 Mapper
 *
 * <p>V2.0 变更:
 * <ul>
 *   <li>状态四态: ENROLLED/WAITING/CANCELLED/INVALID</li>
 *   <li>新增 existsActive: 学生是否存在 ENROLLED 或 WAITING 记录 (防重复)</li>
 *   <li>新增 countWaitingByActivity: 候补队列长度</li>
 *   <li>新增 nextWaitingRecord: 取队首 WAITING (按 wait_order ASC)</li>
 *   <li>新增 clearWaitOrder: 递补后清空 wait_order</li>
 *   <li>新增 pageWaitingQueue: 候补队列分页 (教师端)</li>
 *   <li>新增 listForExport: 报名名单导出 (教师端)</li>
 * </ul>
 *
 * @author 勇闯天涯组
 */
@Mapper
public interface RegistrationMapper extends BaseMapper<Registration> {

    /**
     * 统计某活动的已报人数 (status = ENROLLED)
     */
    @Select("""
            SELECT COUNT(*)
              FROM registration
             WHERE activity_id = #{activityId}
               AND status = 'ENROLLED'
            """)
    int countEnrolledByActivity(@Param("activityId") Long activityId);

    /**
     * 统计某活动候补人数 (status = WAITING)
     */
    @Select("""
            SELECT COUNT(*)
              FROM registration
             WHERE activity_id = #{activityId}
               AND status = 'WAITING'
            """)
    int countWaitingByActivity(@Param("activityId") Long activityId);

    /**
     * 当前学生是否对该活动存在 ENROLLED 记录
     */
    @Select("""
            SELECT COUNT(*)
              FROM registration
             WHERE student_account = #{account}
               AND activity_id = #{activityId}
               AND status = 'ENROLLED'
            """)
    int countEnrolled(@Param("account") String account, @Param("activityId") Long activityId);

    default boolean existsEnrolled(String account, Long activityId) {
        return countEnrolled(account, activityId) > 0;
    }

    /**
     * V2.0: 当前学生是否对该活动存在"有效"记录 (ENROLLED 或 WAITING)
     * 防重复报名: 同一学生对同一活动只能有一条 ENROLLED 或 WAITING
     */
    @Select("""
            SELECT COUNT(*)
              FROM registration
             WHERE student_account = #{account}
               AND activity_id = #{activityId}
               AND status IN ('ENROLLED','WAITING')
            """)
    int countActive(@Param("account") String account, @Param("activityId") Long activityId);

    default boolean existsActive(String account, Long activityId) {
        return countActive(account, activityId) > 0;
    }

    /**
     * V2.0: 取某活动队首候补记录 (按 wait_order ASC)
     * 用于退选后递补
     */
    @Select("""
            SELECT *
              FROM registration
             WHERE activity_id = #{activityId}
               AND status = 'WAITING'
             ORDER BY wait_order ASC
             LIMIT 1
            """)
    Registration nextWaitingRecord(@Param("activityId") Long activityId);

    /**
     * V2.0: 取某活动候补队列最大序号 (用于分配新候补序号)
     */
    @Select("""
            SELECT COALESCE(MAX(wait_order), 0)
              FROM registration
             WHERE activity_id = #{activityId}
               AND status = 'WAITING'
            """)
    int maxWaitOrder(@Param("activityId") Long activityId);

    /**
     * V2.0: 候补递补 - 将队首 WAITING 改为 ENROLLED 并清空 wait_order
     * 用乐观锁 (record_id + status=WAITING) 防并发
     */
    @Update("""
            UPDATE registration
               SET status = 'ENROLLED',
                   wait_order = NULL
             WHERE record_id = #{recordId}
               AND status = 'WAITING'
            """)
    int promoteToEnrolled(@Param("recordId") Long recordId);

    /**
     * 分页查询学生报名记录, 联表活动表 (XML)
     */
    IPage<RegistrationVO> pageMyRegistrations(IPage<RegistrationVO> page,
                                              @Param("account") String account,
                                              @Param("status") String status);

    /**
     * V2.0: 教师端 - 候补队列分页查询 (XML, 按 wait_order ASC)
     */
    IPage<WaitingQueueVO> pageWaitingQueue(IPage<WaitingQueueVO> page,
                                           @Param("activityId") Long activityId);

    /**
     * V2.0: 教师端 - 报名名单导出 (XML, 含学号/姓名/年级/报名时间/状态)
     * 不分页, 全量返回 (Excel 导出用)
     */
    List<RegistrationExportVO> listForExport(@Param("activityId") Long activityId);
}
