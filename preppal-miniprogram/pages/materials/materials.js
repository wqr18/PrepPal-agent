const api = require('../../utils/api');

Page({
  data: {
    courseId: 0,
    courseName: '',
    materials: [],
    loading: true
  },

  onLoad(options) {
    this.setData({ courseId: Number(options.courseId || 0) });
  },

  onShow() {
    if (this.data.courseId) {
      this.load();
    }
  },

  onUnload() {
    this.clearPendingTimer();
  },

  onPullDownRefresh() {
    this.load(true);
  },

  clearPendingTimer() {
    if (this.pendingTimer) {
      clearTimeout(this.pendingTimer);
      this.pendingTimer = null;
    }
  },

  load(fromPull) {
    const self = this;
    const courseId = this.data.courseId;
    this.clearPendingTimer();
    Promise.all([api.getCourse(courseId), api.listMaterials(courseId)])
      .then(function (res) {
        const course = res[0];
        const materials = res[1] || [];
        self.setData({
          loading: false,
          courseName: course ? course.name : '',
          materials: materials
        });
        if (fromPull) {
          wx.stopPullDownRefresh();
        }
        self.schedulePendingResolve();
      })
      .catch(function (err) {
        self.setData({ loading: false });
        if (fromPull) {
          wx.stopPullDownRefresh();
        }
        wx.showToast({ title: err.message || '加载失败', icon: 'none' });
      });
  },

  /** 「解析中」的资料会在约 2 秒后自动转为解析成功，这里做一次刷新演示状态流转 */
  schedulePendingResolve() {
    const self = this;
    const pending = this.data.materials.filter(function (m) { return m.isPending; });
    if (!pending.length) {
      return;
    }
    this.pendingTimer = setTimeout(function () {
      self.pendingTimer = null;
      api
        .listMaterials(self.data.courseId)
        .then(function (materials) {
          self.setData({ materials: materials || [] });
          wx.showToast({ title: '《' + pending[0].filename + '》解析成功，可用于生成教案', icon: 'none', duration: 2400 });
        })
        .catch(function () {
          /* 静默失败，下次进入页面会重新加载 */
        });
    }, 2400);
  },

  onUpload() {
    wx.navigateTo({ url: '/pages/upload/upload?courseId=' + this.data.courseId });
  },

  onDelete(e) {
    const self = this;
    const id = e.currentTarget.dataset.id;
    const material = this.data.materials.filter(function (m) { return m.id === id; })[0];
    if (!material) {
      return;
    }
    wx.showModal({
      title: '删除这份资料？',
      content: '《' + material.filename + '》删除后，已生成教案中的引用来源标注不会自动更新。',
      confirmText: '删除',
      confirmColor: '#e5484d',
      success(res) {
        if (!res.confirm) {
          return;
        }
        api
          .deleteMaterial(id)
          .then(function () {
            self.load();
            wx.showToast({ title: '资料已删除', icon: 'none' });
          })
          .catch(function (err) {
            wx.showToast({ title: err.message || '删除失败', icon: 'none' });
          });
      }
    });
  },

  onWhy(e) {
    const self = this;
    const id = e.currentTarget.dataset.id;
    const material = this.data.materials.filter(function (m) { return m.id === id; })[0];
    if (!material) {
      return;
    }
    wx.showModal({
      title: '解析失败原因',
      content: '《' + material.filename + '》\n\n' + (material.errorMessage || '文件内容无法读取，请检查编码后重试。'),
      confirmText: '重新上传',
      cancelText: '知道了',
      success(res) {
        if (res.confirm) {
          self.onUpload();
        }
      }
    });
  }
});
