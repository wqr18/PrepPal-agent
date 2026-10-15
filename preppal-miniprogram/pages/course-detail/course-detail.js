const api = require('../../utils/api');

Page({
  data: {
    id: 0,
    course: null,
    materials: [],
    plans: [],
    okCount: 0,
    sheetShow: false,
    sheetActions: [
      { key: 'edit', name: '编辑课程信息', subname: '修改课程名称与简介' },
      { key: 'delete', name: '删除课程', subname: '课程下的资料与教案会一并删除', color: '#e5484d' }
    ],
    dialogShow: false,
    dialogTitle: '',
    dialogMessage: '',
    dialogOkText: '确定',
    dialogDanger: false
  },

  onLoad(options) {
    this.setData({ id: Number(options.id || 0) });
  },

  onShow() {
    if (this.data.id) {
      this.load();
    }
  },

  onPullDownRefresh() {
    this.load(true);
  },

  load(fromPull) {
    const self = this;
    const id = this.data.id;
    Promise.all([api.getCourse(id), api.listMaterials(id), api.listPlans({ courseId: id })])
      .then(function (res) {
        const course = res[0];
        if (fromPull) {
          wx.stopPullDownRefresh();
        }
        if (!course) {
          wx.showToast({ title: '课程不存在或已删除', icon: 'none' });
          setTimeout(function () {
            wx.navigateBack();
          }, 900);
          return;
        }
        const materials = res[1] || [];
        const plans = res[2] || [];
        self.setData({
          course: course,
          materials: materials.slice(0, 3),
          plans: plans.slice(0, 3),
          okCount: materials.filter(function (m) { return m.isOk; }).length
        });
      })
      .catch(function (err) {
        if (fromPull) {
          wx.stopPullDownRefresh();
        }
        wx.showToast({ title: err.message || '加载失败', icon: 'none' });
      });
  },

  onUpload() {
    wx.navigateTo({ url: '/pages/upload/upload?courseId=' + this.data.id });
  },

  onGenerate() {
    wx.navigateTo({ url: '/pages/plan-generate/plan-generate?courseId=' + this.data.id });
  },

  onGoMaterials() {
    wx.navigateTo({ url: '/pages/materials/materials?courseId=' + this.data.id });
  },

  onGoPlans() {
    wx.navigateTo({ url: '/pages/plans/plans?courseId=' + this.data.id });
  },

  onPlanTap(e) {
    wx.navigateTo({ url: '/pages/plan-detail/plan-detail?id=' + e.currentTarget.dataset.id });
  },

  onMore() {
    this.setData({ sheetShow: true });
  },

  onSheetClose() {
    this.setData({ sheetShow: false });
  },

  onSheetSelect(e) {
    const key = e.detail.key;
    this.setData({ sheetShow: false });
    if (key === 'edit') {
      wx.navigateTo({ url: '/pages/course-edit/course-edit?id=' + this.data.id });
      return;
    }
    if (key === 'delete') {
      const course = this.data.course;
      this.setData({
        dialogShow: true,
        dialogTitle: '删除《' + course.name + '》？',
        dialogMessage:
          '该课程下的 ' +
          course.materialCount +
          ' 份资料与 ' +
          course.planCount +
          ' 份教案会一并删除，且无法恢复。',
        dialogOkText: '删除课程',
        dialogDanger: true
      });
    }
  },

  onDialogCancel() {
    this.setData({ dialogShow: false });
  },

  onDialogConfirm() {
    const self = this;
    this.setData({ dialogShow: false });
    api
      .deleteCourse(this.data.id)
      .then(function () {
        wx.switchTab({
          url: '/pages/courses/courses',
          success() {
            wx.showToast({ title: '课程已删除', icon: 'none' });
          }
        });
      })
      .catch(function (err) {
        wx.showToast({ title: err.message || '删除失败', icon: 'none' });
      });
  }
});
