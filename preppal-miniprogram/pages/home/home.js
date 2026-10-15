const config = require('../../config/index');
const api = require('../../utils/api');

Page({
  data: {
    loading: true,
    greeting: '上午好',
    userName: '老师',
    avatarText: '师',
    empty: false,
    stats: { courses: 0, drafts: 0, finals: 0 },
    recentCourse: null,
    recentPlans: [],
    steps: ['建立课程并上传教学资料', '填写教学主题、对象与课时', 'AI 生成草稿，核对来源后定稿']
  },

  onShow() {
    this.syncTabBar();
    const app = getApp();
    const user = app.globalData.userInfo || wx.getStorageSync(config.USER_KEY) || {};
    const name = user.nickname || user.realName || '老师';
    this.setData({
      userName: name,
      avatarText: name.slice(0, 1),
      greeting: this.greetingText()
    });
    this.load();
  },

  onPullDownRefresh() {
    this.load(true);
  },

  syncTabBar() {
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ selected: 0 });
    }
  },

  greetingText() {
    const hour = new Date().getHours();
    if (hour < 6) {
      return '凌晨好';
    }
    if (hour < 12) {
      return '上午好';
    }
    if (hour < 18) {
      return '下午好';
    }
    return '晚上好';
  },

  load(fromPull) {
    const self = this;
    Promise.all([api.listCourses(), api.listPlans({})])
      .then(function (res) {
        const courses = (res[0] || []).slice().sort(function (a, b) {
          return b.updatedAt - a.updatedAt;
        });
        const plans = (res[1] || []).slice().sort(function (a, b) {
          return b.updatedAt - a.updatedAt;
        });
        self.setData({
          loading: false,
          empty: courses.length === 0,
          stats: {
            courses: courses.length,
            drafts: plans.filter(function (p) { return p.status === 'DRAFT'; }).length,
            finals: plans.filter(function (p) { return p.status === 'FINALIZED'; }).length
          },
          recentCourse: courses[0] || null,
          recentPlans: plans.slice(0, 3)
        });
        if (fromPull) {
          wx.stopPullDownRefresh();
        }
      })
      .catch(function (err) {
        self.setData({ loading: false });
        if (fromPull) {
          wx.stopPullDownRefresh();
        }
        wx.showToast({ title: err.message || '加载失败', icon: 'none' });
      });
  },

  /** 首页 CTA：有课程就直接进入生成页，没有课程先引导建课 */
  onStartGenerate() {
    const course = this.data.recentCourse;
    if (!course) {
      wx.showToast({ title: '先创建一门课程', icon: 'none' });
      wx.navigateTo({ url: '/pages/course-edit/course-edit' });
      return;
    }
    wx.navigateTo({ url: '/pages/plan-generate/plan-generate?courseId=' + course.id });
  },

  onCourseTap(e) {
    wx.navigateTo({ url: '/pages/course-detail/course-detail?id=' + e.currentTarget.dataset.id });
  },

  onPlanTap(e) {
    wx.navigateTo({ url: '/pages/plan-detail/plan-detail?id=' + e.currentTarget.dataset.id });
  },

  onGoCourses() {
    wx.switchTab({ url: '/pages/courses/courses' });
  },

  onGoMe() {
    wx.switchTab({ url: '/pages/me/me' });
  },

  onGoCreateCourse() {
    wx.navigateTo({ url: '/pages/course-edit/course-edit' });
  },

  onGoCoursePlans() {
    const course = this.data.recentCourse;
    if (!course) {
      return;
    }
    wx.navigateTo({ url: '/pages/plans/plans?courseId=' + course.id });
  }
});
