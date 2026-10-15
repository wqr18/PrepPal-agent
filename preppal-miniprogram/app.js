const config = require('./config/index');
const api = require('./utils/api');
const debug = require('./utils/debug');

App({
  globalData: {
    userInfo: null,
    statusBarHeight: 20
  },

  onLaunch() {
    this.initSystemInfo();
    const user = wx.getStorageSync(config.USER_KEY);
    if (user) {
      this.globalData.userInfo = user;
    }
  },

  initSystemInfo() {
    try {
      const info = wx.getWindowInfo ? wx.getWindowInfo() : wx.getSystemInfoSync();
      this.globalData.statusBarHeight = info.statusBarHeight || 20;
    } catch (e) {
      this.globalData.statusBarHeight = 20;
    }
  },

  /** 是否已登录（本地存在 token） */
  isLoggedIn() {
    return !!wx.getStorageSync(config.TOKEN_KEY);
  },

  /** 未登录时跳转登录页，返回是否已登录 */
  ensureLogin() {
    if (this.isLoggedIn()) {
      return true;
    }
    wx.reLaunch({ url: '/pages/login/login' });
    return false;
  },

  setSession(session) {
    wx.setStorageSync(config.TOKEN_KEY, session.token);
    wx.setStorageSync(config.USER_KEY, session.user);
    this.globalData.userInfo = session.user;
  },

  clearSession() {
    wx.removeStorageSync(config.TOKEN_KEY);
    wx.removeStorageSync(config.USER_KEY);
    this.globalData.userInfo = null;
  },

  /** 微信登录：wx.login 换取 code → 后端换 openid（mock 模式返回固定测试用户） */
  login(code) {
    const self = this;
    return api.login(code).then(function (session) {
      self.setSession(session);
      return session;
    });
  },

  /** 演示开关（仅调试面板使用） */
  debugFlag: debug.get,
  setDebugFlag: debug.set,
  resetDebug: debug.reset
});
