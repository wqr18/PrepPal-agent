const api = require('../../utils/api');

const STEP_TEXTS = ['检索课程资料并定位相关段落', '组织教学目标与教学过程', '生成引用来源说明'];

function buildSteps(states) {
  return STEP_TEXTS.map(function (text, index) {
    return { text: text, state: states[index] || 'wait' };
  });
}

Page({
  data: {
    /** running | failed */
    stage: 'running',
    progress: 0,
    steps: buildSteps(['doing', 'wait', 'wait']),
    topic: '',
    refsText: '',
    failMessage: '',
    params: {}
  },

  onLoad(options) {
    const params = {
      courseId: Number(options.courseId || 0),
      topic: decodeURIComponent(options.topic || ''),
      targetAudience: decodeURIComponent(options.targetAudience || ''),
      classDuration: Number(options.classDuration || 0),
      teachingType: decodeURIComponent(options.teachingType || ''),
      customObjectives: decodeURIComponent(options.customObjectives || ''),
      idempotencyKey: options.idempotencyKey || ''
    };
    this.setData({ params: params, topic: params.topic });
    this.loadRefsPreview(params.courseId);
    this.start();
  },

  onUnload() {
    this.clearTimers();
  },

  clearTimers() {
    if (this.ticker) {
      clearInterval(this.ticker);
      this.ticker = null;
    }
    if (this.redirectTimer) {
      clearTimeout(this.redirectTimer);
      this.redirectTimer = null;
    }
  },

  loadRefsPreview(courseId) {
    const self = this;
    api
      .listMaterials(courseId)
      .then(function (materials) {
        const ok = (materials || []).filter(function (m) { return m.isOk; });
        self.setData({
          refsText: ok.length
            ? ok.slice(0, 2).map(function (m) { return m.filename; }).join('、')
            : '（未检索到可用资料）'
        });
      })
      .catch(function () {
        self.setData({ refsText: '（未检索到可用资料）' });
      });
  },

  start() {
    const self = this;
    this.clearTimers();
    this.setData({
      stage: 'running',
      progress: 0,
      steps: buildSteps(['doing', 'wait', 'wait']),
      failMessage: ''
    });

    this.ticker = setInterval(function () {
      const next = Math.min(92, self.data.progress + 3 + Math.random() * 5);
      if (next > 70) {
        self.setData({ progress: Math.round(next), steps: buildSteps(['done', 'done', 'doing']) });
        return;
      }
      if (next > 28) {
        self.setData({ progress: Math.round(next), steps: buildSteps(['done', 'doing', 'wait']) });
        return;
      }
      self.setData({ progress: Math.round(next) });
    }, 190);

    api
      .generatePlan(this.data.params)
      .then(function (plan) {
        self.clearTimers();
        self.setData({ progress: 100, steps: buildSteps(['done', 'done', 'done']) });
        if (!plan) {
          self.fail('生成失败，请重试');
          return;
        }
        self.redirectTimer = setTimeout(function () {
          wx.redirectTo({
            url: '/pages/plan-detail/plan-detail?id=' + plan.id,
            success() {
              wx.showToast({
                title: plan.refs && plan.refs.length ? '教案草稿已生成，引用来源已标注' : '教案草稿已生成（未引用到上传资料）',
                icon: 'none',
                duration: 2400
              });
            }
          });
        }, 340);
      })
      .catch(function (err) {
        self.fail(err.message || '生成失败，请重试');
      });
  },

  fail(message) {
    this.clearTimers();
    const isTimeout = message.indexOf('超时') > -1;
    this.setData({
      stage: 'failed',
      failMessage: isTimeout
        ? '已等待超过 2 分钟，可能是资料较多或网络较慢。已保留你填写的教学信息。'
        : message
    });
  },

  onRetry() {
    this.start();
  },

  onBack() {
    wx.navigateBack();
  }
});
