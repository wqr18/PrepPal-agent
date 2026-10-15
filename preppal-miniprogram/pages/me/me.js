const config = require('../../config/index');
const api = require('../../utils/api');
const debug = require('../../utils/debug');

Page({
  data: {
    user: {},
    avatarText: '师',
    stats: { courses: 0, drafts: 0, finals: 0 },
    menus: [],
    debugPanel: config.DEBUG_PANEL,
    isMock: config.USE_MOCK,
    debug: { empty: false, timeout: false, noRefs: false },
    debugLabels: debug.LABELS,
    checking: false,
    courses: []
  },

  onShow() {
    this.syncTabBar();
    this.load();
  },

  onPullDownRefresh() {
    this.load(true);
  },

  syncTabBar() {
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ selected: 2 });
    }
  },

  load(fromPull) {
    const self = this;
    const app = getApp();
    const user = app.globalData.userInfo || wx.getStorageSync(config.USER_KEY) || {};
    const displayName = user.realName || user.nickname || '老师';

    Promise.all([api.listCourses(), api.listPlans({})])
      .then(function (res) {
        const courses = res[0] || [];
        const plans = res[1] || [];
        const drafts = plans.filter(function (p) { return p.status === 'DRAFT'; }).length;
        const finals = plans.filter(function (p) { return p.status === 'FINALIZED'; }).length;
        self.setData({
          user: user,
          avatarText: displayName.slice(0, 1),
          courses: courses,
          stats: { courses: courses.length, drafts: drafts, finals: finals },
          menus: [
            { key: 'courses', icon: 'book', title: '我的课程', desc: '共 ' + courses.length + ' 门课程' },
            { key: 'drafts', icon: 'plan', title: '我的草稿', desc: drafts + ' 份待定稿' },
            { key: 'help', icon: 'help', title: '帮助与反馈', desc: '使用问题与建议反馈' },
            { key: 'about', icon: 'shield', title: '关于 PrepPal', desc: '版本 1.0.0 · 隐私说明' }
          ],
          debug: {
            empty: debug.get('empty'),
            timeout: debug.get('timeout'),
            noRefs: debug.get('noRefs')
          }
        });
        if (fromPull) {
          wx.stopPullDownRefresh();
        }
      })
      .catch(function (err) {
        if (fromPull) {
          wx.stopPullDownRefresh();
        }
        wx.showToast({ title: err.message || '加载失败', icon: 'none' });
      });
  },

  onMenuTap(e) {
    const key = e.currentTarget.dataset.key;
    if (key === 'courses') {
      wx.switchTab({ url: '/pages/courses/courses' });
      return;
    }
    if (key === 'drafts') {
      const withDraft = this.data.courses.filter(function (c) { return c.planCount > 0; })[0] || this.data.courses[0];
      if (!withDraft) {
        wx.showToast({ title: '还没有课程，先去创建一门', icon: 'none' });
        wx.navigateTo({ url: '/pages/course-edit/course-edit' });
        return;
      }
      wx.navigateTo({ url: '/pages/plans/plans?courseId=' + withDraft.id + '&status=DRAFT' });
      return;
    }
    if (key === 'help') {
      wx.showModal({
        title: '帮助与反馈',
        content:
          '常见问题：\n1. 支持哪些资料格式？目前仅支持 .txt 与 .md，单文件 ≤ 5MB。\n2. 为什么提示“未参考到上传资料”？说明课程资料与该主题关联度低，可补充相关讲义后重新生成。\n3. 定稿后还能改吗？可以，定稿后仍可再次编辑。\n\n反馈邮箱：support@preppal.edu',
        showCancel: false,
        confirmText: '我知道了'
      });
      return;
    }
    if (key === 'about') {
      wx.showModal({
        title: '关于 PrepPal',
        content:
          'PrepPal 智能备课 v1.0.0\n\n面向高校教师的 AI 备课助手：基于你的课程资料生成教案草稿，并保留可追溯的引用来源。\n\n教学资料仅用于你自己的备课，不会用于其他用途。',
        showCancel: false,
        confirmText: '关闭'
      });
    }
  },

  onLogout() {
    const app = getApp();
    wx.showModal({
      title: '退出登录？',
      content: '退出后需要重新使用微信一键登录。',
      confirmText: '退出登录',
      confirmColor: '#e5484d',
      success(res) {
        if (!res.confirm) {
          return;
        }
        app.clearSession();
        wx.reLaunch({ url: '/pages/login/login' });
      }
    });
  },

  /* ---------------- 开发者调试（仅 config.DEBUG_PANEL 为真时展示） ---------------- */

  onToggleDebug(e) {
    const key = e.currentTarget.dataset.key;
    const value = !!e.detail;
    const app = getApp();
    app.setDebugFlag(key, value);
    const patch = {};
    patch['debug.' + key] = value;
    this.setData(patch);
    if (key === 'empty') {
      this.load();
    }
    wx.showToast({
      title: (value ? '已开启：' : '已关闭：') + debug.LABELS[key],
      icon: 'none',
      duration: 1600
    });
  },

  onResetDemo() {
    const app = getApp();
    api.resetMockDb();
    app.resetDebug();
    this.setData({ debug: { empty: false, timeout: false, noRefs: false } });
    this.load();
    wx.showToast({ title: '演示数据已重置', icon: 'none' });
  },

  onCheckBackend() {
    const self = this;
    if (this.data.checking) {
      return;
    }
    this.setData({ checking: true });
    api
      .checkBackend()
      .then(function (text) {
        self.setData({ checking: false });
        wx.showModal({
          title: '后端连通成功',
          content: config.BASE_URL + '/api/hello 返回：\n' + String(text).slice(0, 120),
          showCancel: false,
          confirmText: '关闭'
        });
      })
      .catch(function (err) {
        self.setData({ checking: false });
        wx.showModal({
          title: '后端未连通',
          content:
            (err.message || '请求失败') +
            '\n\n请确认后端已启动：\ncd PrepPal && mvnw.cmd spring-boot:run\n当前地址：' +
            config.BASE_URL,
          showCancel: false,
          confirmText: '关闭'
        });
      });
  }
});
