const config = require('../../config/index');
const api = require('../../utils/api');
const validate = require('../../utils/validate');
const uuid = require('../../utils/uuid');

Page({
  data: {
    courseId: 0,
    course: null,
    courses: [],
    sheetShow: false,
    courseActions: [],
    types: config.TEACHING_TYPES,
    form: {
      topic: '',
      targetAudience: '',
      classDuration: '',
      teachingType: config.TEACHING_TYPES[0],
      customObjectives: ''
    },
    errors: {},
    okMaterials: [],
    submitting: false
  },

  onLoad(options) {
    const courseId = Number(options.courseId || 0);
    const self = this;
    api
      .listCourses()
      .then(function (courses) {
        const list = courses || [];
        if (!list.length) {
          wx.showModal({
            title: '还没有课程',
            content: '生成教案前需要先创建一门课程，并在课程下上传教学资料。',
            confirmText: '新建课程',
            showCancel: false,
            success() {
              wx.redirectTo({ url: '/pages/course-edit/course-edit' });
            }
          });
          return;
        }
        const current = list.filter(function (c) { return c.id === courseId; })[0] || list[0];
        self.setData({
          courses: list,
          course: current,
          courseId: current.id,
          courseActions: list.map(function (c) {
            return {
              key: String(c.id),
              name: c.name,
              subname: c.materialCount + ' 份资料 · ' + c.planCount + ' 份教案'
            };
          })
        });
        self.loadMaterials(current.id);
      })
      .catch(function (err) {
        wx.showToast({ title: err.message || '加载失败', icon: 'none' });
      });
  },

  loadMaterials(courseId) {
    const self = this;
    api
      .listMaterials(courseId)
      .then(function (materials) {
        self.setData({
          okMaterials: (materials || []).filter(function (m) { return m.isOk; })
        });
      })
      .catch(function () {
        self.setData({ okMaterials: [] });
      });
  },

  onInput(e) {
    const field = e.currentTarget.dataset.field;
    const patch = {};
    patch['form.' + field] = e.detail.value;
    patch['errors.' + field] = '';
    this.setData(patch);
  },

  onPickType(e) {
    this.setData({ 'form.teachingType': e.currentTarget.dataset.value, 'errors.teachingType': '' });
  },

  onOpenCourseSheet() {
    this.setData({ sheetShow: true });
  },

  onCourseSheetClose() {
    this.setData({ sheetShow: false });
  },

  onCourseSelect(e) {
    const id = Number(e.detail.key);
    const self = this;
    this.setData({ sheetShow: false });
    const course = this.data.courses.filter(function (c) { return c.id === id; })[0];
    if (!course) {
      return;
    }
    this.setData({ course: course, courseId: course.id });
    this.loadMaterials(course.id);
    wx.showToast({ title: '已切换到《' + course.name + '》', icon: 'none' });
  },

  onSubmit() {
    if (this.data.submitting) {
      return;
    }
    const check = validate.validateGenerate(this.data.form);
    if (!check.valid) {
      this.setData({ errors: check.errors });
      wx.showToast({ title: '还有必填信息未填完', icon: 'none' });
      if (check.firstField) {
        wx.pageScrollTo({ selector: '#field-' + check.firstField, duration: 250 });
      }
      return;
    }

    const form = this.data.form;
    this.setData({ submitting: true, errors: {} });

    const params = [
      'courseId=' + this.data.courseId,
      'topic=' + encodeURIComponent(form.topic.trim()),
      'targetAudience=' + encodeURIComponent(form.targetAudience.trim()),
      'classDuration=' + Number(form.classDuration),
      'teachingType=' + encodeURIComponent(form.teachingType),
      'customObjectives=' + encodeURIComponent((form.customObjectives || '').trim()),
      'idempotencyKey=' + uuid.uuid()
    ].join('&');

    wx.navigateTo({
      url: '/pages/plan-generating/plan-generating?' + params,
      complete: () => {
        this.setData({ submitting: false });
      }
    });
  }
});
