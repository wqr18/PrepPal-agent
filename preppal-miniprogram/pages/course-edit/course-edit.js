const config = require('../../config/index');
const api = require('../../utils/api');
const validate = require('../../utils/validate');

Page({
  data: {
    id: 0,
    isEdit: false,
    form: { name: '', description: '' },
    errors: {},
    submitting: false,
    nameMax: config.NAME_MAX,
    introMax: config.INTRO_MAX
  },

  onLoad(options) {
    const id = Number(options.id || 0);
    this.setData({ id: id, isEdit: !!id });
    wx.setNavigationBarTitle({ title: id ? '编辑课程信息' : '新建课程' });
    if (id) {
      this.loadCourse(id);
    }
  },

  loadCourse(id) {
    const self = this;
    api
      .getCourse(id)
      .then(function (course) {
        if (!course) {
          wx.showToast({ title: '课程不存在', icon: 'none' });
          return;
        }
        self.setData({
          form: { name: course.name, description: course.description }
        });
      })
      .catch(function (err) {
        wx.showToast({ title: err.message || '加载失败', icon: 'none' });
      });
  },

  onInput(e) {
    const field = e.currentTarget.dataset.field;
    const patch = {};
    patch['form.' + field] = e.detail.value;
    patch['errors.' + field] = '';
    this.setData(patch);
  },

  onSubmit() {
    if (this.data.submitting) {
      return;
    }
    const form = this.data.form;
    const check = validate.validateCourse(form);
    if (!check.valid) {
      this.setData({ errors: check.errors });
      wx.showToast({ title: '还有信息未填完整', icon: 'none' });
      return;
    }

    const self = this;
    const payload = { name: form.name.trim(), description: form.description.trim() };
    this.setData({ submitting: true, errors: {} });

    const task = this.data.isEdit ? api.updateCourse(this.data.id, payload) : api.createCourse(payload);

    task
      .then(function (course) {
        self.setData({ submitting: false });
        if (!course) {
          wx.showToast({ title: '保存失败，请重试', icon: 'none' });
          return;
        }
        if (self.data.isEdit) {
          wx.navigateBack({
            success() {
              wx.showToast({ title: '课程信息已更新', icon: 'none' });
            }
          });
          return;
        }
        // 新建：直接进入该课程详情，返回时回到课程列表
        wx.redirectTo({
          url: '/pages/course-detail/course-detail?id=' + course.id,
          success() {
            wx.showToast({ title: '课程已创建，先上传教学资料吧', icon: 'none', duration: 2000 });
          }
        });
      })
      .catch(function (err) {
        self.setData({ submitting: false });
        wx.showToast({ title: err.message || '保存失败', icon: 'none' });
      });
  }
});
