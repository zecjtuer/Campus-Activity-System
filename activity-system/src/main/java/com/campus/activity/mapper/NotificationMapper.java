package com.campus.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.activity.dto.NotificationVO;
import com.campus.activity.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 站内通知 Mapper (V2.0 新增)
 *
 * <p>复杂联表查询写在 NotificationMapper.xml</p>
 *
 * @author 勇闯天涯组
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    /**
     * 统计未读通知数
     */
    @Select("""
            SELECT COUNT(*)
              FROM notifications
             WHERE receiver_account = #{account}
               AND is_read = 0
            """)
    int countUnread(@Param("account") String account);

    /**
     * 分页查询某用户的通知列表 (XML, 联表活动标题)
     *
     * @param page       分页对象
     * @param account    接收人账号
     * @param onlyUnread true 只查未读; false/null 查全部
     */
    IPage<NotificationVO> pageByReceiver(IPage<NotificationVO> page,
                                          @Param("account") String account,
                                          @Param("onlyUnread") Boolean onlyUnread);

    /**
     * 全部标记已读 (某用户)
     */
    @Update("""
            UPDATE notifications
               SET is_read = 1
             WHERE receiver_account = #{account}
               AND is_read = 0
            """)
    int markAllRead(@Param("account") String account);

    /**
     * 单条标记已读 (防越权: 必须本人)
     */
    @Update("""
            UPDATE notifications
               SET is_read = 1
             WHERE id = #{id}
               AND receiver_account = #{account}
            """)
    int markRead(@Param("id") Long id, @Param("account") String account);
}
