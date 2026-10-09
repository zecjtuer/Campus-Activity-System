package com.campus.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.activity.entity.Activity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 活动 Mapper
 *
 * <p>V2.0: countEnrolledByTeacher 同时返回候补人数, 供教师端列表展示</p>
 *
 * @author 勇闯天涯组
 */
@Mapper
public interface ActivityMapper extends BaseMapper<Activity> {

    /**
     * 按 teacher_account 统计其发布的所有活动的报名/候补人数
     * 用于教师端列表展示
     *
     * @param teacherAccount 教师账号
     * @return 每个活动的 enrolledCount / waitingCount
     */
    @Select("""
            SELECT activity_id AS activityId,
                   COUNT(CASE WHEN status = 'ENROLLED' THEN 1 END) AS enrolledCount,
                   COUNT(CASE WHEN status = 'WAITING'  THEN 1 END) AS waitingCount
              FROM registration
             WHERE activity_id IN (
                    SELECT activity_id FROM activity WHERE teacher_account = #{teacherAccount}
             )
             GROUP BY activity_id
            """)
    List<Map<String, Object>> countEnrolledByTeacher(@Param("teacherAccount") String teacherAccount);
}
