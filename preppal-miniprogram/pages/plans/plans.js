const api = require('../../utils/api');

const FILTERS = [
  { key: 'ALL', label: '全部' },
  { key: 'DRAFT', label: '草稿' },
  { key: 'FINALIZED', label: '已定稿' }
];

Page({
  data: {
    courseId: 0,
    courseName: '',
    filters: FILTERS,
    filter: 'ALL',
    filterItems: [],
    counts: { ALL: 0, DRAFT: 0, FINALIZED: 0 },
    plans: [],
    allPlans: [],
    loading: true
  },

  onLoad(options) {
    const status = options.status === 'DRAFT' || options.status === 'FINALIZED' ? options.status : 'ALL';
    this.setData({ courseId: Number(options.courseId || 0), filter: status });
  },

  onShow() {
    if (this.data.courseId) {
      this.load();
    }
  },

  onPullDownRefresh() {
    this.load(true);
  },

  load(fromPull) {
    const self = this;
    const courseId = this.data.courseId;
    Promise.all([api.getCourse(courseId), api.listPlans({ courseId: courseId })])
      .then(function (res) {
        const course = res[0];
        const plans = (res[1] || []).slice().sort(function (a, b) {
          return b.updatedAt - a.updatedAt;
        });
        const counts = {
          ALL: plans.length,
          DRAFT: plans.filter(function (p) { return p.status === 'DRAFT'; }).length,
          FINALIZED: plans.filter(function (p) { return p.status === 'FINALIZED'; }).length
        };
        self.setData({
          loading: false,
          courseName: course ? course.name : '',
          allPlans: plans,
          counts: counts,
          filterItems: FILTERS.map(function (item) {
            return { key: item.key, label: item.label, count: counts[item.key] };
          })
        });
        self.applyFilter(self.data.filter);
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

  applyFilter(filter) {
    const all = this.data.allPlans;
    this.setData({
      filter: filter,
      plans: all.filter(function (p) {
        return filter === 'ALL' || p.status === filter;
      })
    });
  },

  onFilterTap(e) {
    this.applyFilter(e.currentTarget.dataset.key);
  },

  onPlanTap(e) {
    wx.navigateTo({ url: '/pages/plan-detail/plan-detail?id=' + e.currentTarget.dataset.id });
  },

  onDelete(e) {
    const self = this;
    const id = e.currentTarget.dataset.id;
    const plan = this.data.allPlans.filter(function (p) { return p.id === id; })[0];
    if (!plan) {
      return;
    }
    wx.showModal({
      title: '删除这份教案？',
      content: '《' + plan.title + '》删除后无法恢复。',
      confirmText: '删除',
      confirmColor: '#e5484d',
      success(res) {
        if (!res.confirm) {
          return;
        }
        api
          .deletePlan(id)
          .then(function () {
            self.load();
            wx.showToast({ title: '教案已删除', icon: 'none' });
          })
          .catch(function (err) {
            wx.showToast({ title: err.message || '删除失败', icon: 'none' });
          });
      }
    });
  },

  onGenerate() {
    wx.navigateTo({ url: '/pages/plan-generate/plan-generate?courseId=' + this.data.courseId });
  }
});
