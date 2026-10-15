const config = require('../../config/index');

Page({
  data: {
    loading: false,
    statusBarHeight: 20,
    points: [
      { title: '基于课程资料生成', desc: '优先引用你上传的 txt / md 教学资料，而不是泛泛而谈。' },
      { title: '引用可追溯', desc: '每条内容都能对回资料的具体段落，方便课前核对。' },
      { title: '草稿与定稿可管理', desc: '先存草稿随时修改，确认后标记定稿归档到课程下。' }
    ]
  },

  onLoad() {
    const app = getApp();
    this.setData({ statusBarHeight: app.globalData.statusBarHeight || 20 });
    if (app.isLoggedIn()) {
      wx.switchTab({ url: '/pages/home/home' });
    }
  },

  onLogin() {
    if (this.data.loading) {
      return;
    }
    const self = this;
    this.setData({ loading: true });

    // 后端目前只实现了 mock 登录通道（code=mock）：
    // DEV_MOCK_LOGIN 打开时跳过 wx.login 直接提交 mock code，便于本地联调。
    if (config.DEV_MOCK_LOGIN) {
      this.doLogin('mock');
      return;
    }

    wx.login({
      success(res) {
        self.doLogin(res.code);
      },
      fail() {
        self.setData({ loading: false });
        wx.showToast({ title: '微信登录失败，请重试', icon: 'none' });
      }
    });
  },

  doLogin(code) {
    const app = getApp();
    const self = this;
    app
      .login(code)
      .then(function (session) {
        self.setData({ loading: false });
        wx.switchTab({
          url: '/pages/home/home',
          success() {
            const name = (session.user && (session.user.nickname || session.user.realName)) || '老师';
            wx.showToast({ title: '登录成功，欢迎回来，' + name, icon: 'none', duration: 2000 });
          }
        });
      })
      .catch(function (err) {
        self.setData({ loading: false });
        wx.showToast({ title: err.message || '登录失败，请重试', icon: 'none' });
      });
  },

  onAgreement(e) {
    const type = e.currentTarget.dataset.type;
    wx.showModal({
      title: type,
      content:
        '原型演示内容：PrepPal 仅用于创建你的个人备课空间，不会获取通讯录与聊天内容；上传的教学资料仅用于你自己的备课。',
      showCancel: false,
      confirmText: '我知道了'
    });
  }
});
