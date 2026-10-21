import { ref, onMounted, onBeforeUnmount } from 'vue'
import { ElNotification } from 'element-plus'
import { unreadCount, pageNotifications } from '@/api/student'

/**
 * V3.0 通知主动触达 composable
 *
 * 数据依据: 模拟运行数据中消息页访问量为 0, 用户反馈"候补转成功隔两天才发现"。
 * 解决: 定时轮询未读数, 状态变化时弹浏览器桌面通知 + 站内通知卡片。
 *
 * @returns {{ unread: Ref<number>, startPolling: Function, stopPolling: Function }}
 */
export function useNotify() {
  const unread = ref(0)
  let timer = null
  let lastUnread = 0

  async function refresh() {
    try {
      const n = await unreadCount()
      // 未读数增加 → 有新通知, 主动提示
      if (n > lastUnread) {
        try {
          const data = await pageNotifications({ current: 1, size: n, isRead: 0 })
          const latest = (data.records || [])[0]
          if (latest) {
            // 浏览器桌面通知 (需用户授权)
            if ('Notification' in window && Notification.permission === 'granted') {
              new Notification('校园活动系统', { body: latest.content || '您的报名状态有更新' })
            }
            // 站内通知卡片 (始终显示)
            ElNotification({
              title: '状态更新',
              message: latest.content || '您的报名状态有更新',
              type: 'info',
              duration: 5000
            })
          }
        } catch (e) {
          // ignore
        }
      }
      lastUnread = n
      unread.value = n
    } catch (e) {
      // ignore
    }
  }

  function startPolling(intervalMs = 30000) {
    refresh()
    timer = setInterval(refresh, intervalMs)
  }

  function stopPolling() {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }

  // 请求桌面通知权限 (用户首次登录时)
  function requestPermission() {
    if ('Notification' in window && Notification.permission === 'default') {
      Notification.requestPermission()
    }
  }

  onMounted(() => {
    requestPermission()
  })

  onBeforeUnmount(() => {
    stopPolling()
  })

  return { unread, startPolling, stopPolling, refresh }
}
