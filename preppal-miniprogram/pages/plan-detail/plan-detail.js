const api = require('../../utils/api');

const SECTION_KEYS = ['goal', 'key', 'diff', 'process', 'board', 'homework'];

Page({
  data: {
    id: 0,
    plan: null,
    editing: false,
    draft: null,
    open: {},
    processTotal: 0,
    saving: false,
    popupShow: false,
    refPopup: { materialName: '', sec: '', excerpt: '' },
    dialogShow: false,
    dialogTitle: '',
    dialogMessage: '',
    dialogOkText: '确定',
    dialogDanger: false,
    sheetShow: false,
    sheetActions: []
  },

  onLoad(options) {
    const open = {};
    SECTION_KEYS.forEach(function (key) {
      open[key] = true;
    });
    this.setData({ id: Number(options.id || 0), open: open });
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
    api
      .getPlan(this.data.id)
      .then(function (plan) {
        if (fromPull) {
          wx.stopPullDownRefresh();
        }
        if (!plan) {
          wx.showToast({ title: '教案不存在或已删除', icon: 'none' });
          setTimeout(function () {
            wx.navigateBack();
          }, 900);
          return;
        }
        self.applyPlan(plan);
      })
      .catch(function (err) {
        if (fromPull) {
          wx.stopPullDownRefresh();
        }
        wx.showToast({ title: err.message || '加载失败', icon: 'none' });
      });
  },

  applyPlan(plan) {
    const refs = (plan.refs || []).map(function (ref) {
      return Object.assign({}, ref, {
        sec: '# 第 ' + (ref.chunkIndex || 0) + ' 段'
      });
    });
    const enriched = Object.assign({}, plan, {
      refs: refs,
      hasRefs: refs.length > 0
    });
    this.setData({
      plan: enriched,
      processTotal: plan.processTotal || 0,
      sheetActions: [
        { key: 'goCourse', name: '查看所属课程', subname: plan.courseName },
        { key: 'goPlans', name: '查看该课程全部教案' },
        { key: 'delete', name: '删除教案', subname: '删除后无法恢复', color: '#e5484d' }
      ]
    });
  },

  onToggle(e) {
    const key = e.currentTarget.dataset.key;
    const patch = {};
    patch['open.' + key] = !this.data.open[key];
    this.setData(patch);
  },

  /* ---------------- 编辑模式 ---------------- */

  onStartEdit() {
    const plan = this.data.plan;
    this.setData({
      editing: true,
      draft: {
        objectives: Object.assign({ knowledge: '', ability: '', literacy: '' }, plan.objectives),
        keyPoints: plan.keyPoints,
        difficultPoints: plan.difficultPoints,
        process: (plan.process || []).map(function (item) { return Object.assign({}, item); }),
        blackboardDesign: plan.blackboardDesign,
        homework: plan.homework
      }
    });
    wx.showToast({ title: '已进入编辑模式，修改后请保存', icon: 'none', duration: 1800 });
  },

  onCancelEdit() {
    this.setData({ editing: false, draft: null });
    wx.showToast({ title: '已取消编辑', icon: 'none' });
  },

  onDraftInput(e) {
    const field = e.currentTarget.dataset.field;
    const patch = {};
    patch['draft.' + field] = e.detail.value;
    this.setData(patch);
  },

  onProcessInput(e) {
    const index = Number(e.currentTarget.dataset.index);
    const field = e.currentTarget.dataset.field;
    const patch = {};
    patch['draft.process[' + index + '].' + field] = e.detail.value;
    this.setData(patch);
    if (field === 'duration') {
      this.recalcTotal();
    }
  },

  recalcTotal() {
    const draft = this.data.draft;
    const total = ((draft && draft.process) || []).reduce(function (sum, item) {
      return sum + (Number(item.duration) || 0);
    }, 0);
    this.setData({ processTotal: total });
  },

  /**
   * 保存教案
   * targetStatus 为 DRAFT 时：先保存内容再读回最新数据
   * targetStatus 为 FINALIZED 时：先保存内容（若在编辑），再调 POST /finalize
   */
  savePlan(targetStatus, successMessage) {
    const self = this;
    const planId = this.data.plan.id;
    this.setData({ saving: true });

    const step = this.data.editing
      ? api.updatePlan(planId, Object.assign({}, this.data.plan, this.data.draft, { status: 'DRAFT' }))
      : Promise.resolve(null);

    return step
      .then(function () {
        return targetStatus === 'FINALIZED' ? api.finalizePlan(planId) : api.getPlan(planId);
      })
      .then(function (plan) {
        self.setData({ saving: false, editing: false, draft: null });
        if (plan) {
          self.applyPlan(plan);
        }
        wx.showToast({ title: successMessage, icon: 'none', duration: 1800 });
      })
      .catch(function (err) {
        self.setData({ saving: false });
        wx.showToast({ title: err.message || '保存失败', icon: 'none' });
      });
  },

  onSaveDraft() {
    if (this.data.saving) {
      return;
    }
    this.savePlan('DRAFT', '草稿已保存');
  },

  onFinalize() {
    if (this.data.saving) {
      return;
    }
    const self = this;
    this.setData({
      dialogShow: true,
      dialogTitle: '标记为定稿？',
      dialogMessage: '定稿后仍可再次编辑，确定标记为定稿吗？',
      dialogOkText: '标记为定稿',
      dialogDanger: false
    });
    this.dialogAction = function () {
      self.savePlan('FINALIZED', '已标记为定稿');
    };
  },

  onDialogCancel() {
    this.setData({ dialogShow: false });
    this.dialogAction = null;
  },

  onDialogConfirm() {
    const action = this.dialogAction;
    this.setData({ dialogShow: false });
    this.dialogAction = null;
    if (action) {
      action();
    } else {
      this.doDelete();
    }
  },

  /* ---------------- 参考资料 ---------------- */

  onRefTap(e) {
    const index = Number(e.currentTarget.dataset.index);
    const ref = this.data.plan.refs[index];
    if (!ref) {
      return;
    }
    this.setData({
      popupShow: true,
      refPopup: {
        materialName: ref.materialName || '（未知资料）',
        sec: ref.sec,
        excerpt: ref.excerpt || '该段落原文摘要暂不可用，请在资料原文中核对。'
      }
    });
  },

  onRefPopupClose() {
    this.setData({ popupShow: false });
  },

  /* ---------------- 更多操作 ---------------- */

  onMore() {
    this.setData({ sheetShow: true });
  },

  onSheetClose() {
    this.setData({ sheetShow: false });
  },

  onSheetSelect(e) {
    const key = e.detail.key;
    const plan = this.data.plan;
    this.setData({ sheetShow: false });
    if (key === 'goCourse') {
      wx.navigateTo({ url: '/pages/course-detail/course-detail?id=' + plan.courseId });
      return;
    }
    if (key === 'goPlans') {
      wx.navigateTo({ url: '/pages/plans/plans?courseId=' + plan.courseId });
      return;
    }
    if (key === 'delete') {
      this.setData({
        dialogShow: true,
        dialogTitle: '删除这份教案？',
        dialogMessage: '《' + plan.title + '》删除后无法恢复。',
        dialogOkText: '删除',
        dialogDanger: true
      });
      this.dialogAction = null;
    }
  },

  doDelete() {
    const plan = this.data.plan;
    api
      .deletePlan(plan.id)
      .then(function () {
        wx.redirectTo({
          url: '/pages/plans/plans?courseId=' + plan.courseId,
          success() {
            wx.showToast({ title: '教案已删除', icon: 'none' });
          }
        });
      })
      .catch(function (err) {
        wx.showToast({ title: err.message || '删除失败', icon: 'none' });
      });
  },

  onGoUpload() {
    wx.navigateTo({ url: '/pages/upload/upload?courseId=' + this.data.plan.courseId });
  }
});
