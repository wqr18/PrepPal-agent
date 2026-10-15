const api = require('../../utils/api');

Page({
  data: {
    loading: true,
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
      this.getTabBar().setData({ selected: 1 });
    }
  },

  load(fromPull) {
    const self = this;
    api
      .listCourses()
      .then(function (courses) {
        self.setData({ loading: false, courses: courses || [] });
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

  onCourseTap(e) {
    wx.navigateTo({ url: '/pages/course-detail/course-detail?id=' + e.currentTarget.dataset.id });
  },

  onNewCourse() {
    wx.navigateTo({ url: '/pages/course-edit/course-edit' });
  }
});
