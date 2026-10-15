/**
 * 统一请求封装
 * - baseURL 来自 config，不硬编码
 * - 自动附带 JWT（Authorization: Bearer <token>）
 * - 解包统一响应 { code, message, data }，code === 0 视为成功
 * - 401 清除登录态并跳转登录页
 * - rawText: true 用于 /api/hello 这类返回纯文本、非 JSON 包装的接口
 */
const config = require('../config/index');

function getToken() {
  return wx.getStorageSync(config.TOKEN_KEY) || '';
}

function showError(error) {
  wx.showToast({
    title: (error && error.message) || '请求失败',
    icon: 'none',
    duration: 2200
  });
}

function handleUnauthorized() {
  wx.removeStorageSync(config.TOKEN_KEY);
  wx.removeStorageSync(config.USER_KEY);
  const pages = getCurrentPages();
  const current = pages.length ? pages[pages.length - 1].route : '';
  if (current === 'pages/login/login') {
    return;
  }
  wx.reLaunch({ url: '/pages/login/login' });
}

function buildHeader(extra) {
  const header = Object.assign({ 'content-type': 'application/json' }, extra || {});
  const token = getToken();
  if (token) {
    header.Authorization = 'Bearer ' + token;
  }
  return header;
}

/**
 * 上传专用 header：只附鉴权 token。
 * 不设 content-type，交由 wx.uploadFile 自动生成带 boundary 的 multipart/form-data。
 */
function buildUploadHeader() {
  const header = {};
  const token = getToken();
  if (token) {
    header.Authorization = 'Bearer ' + token;
  }
  return header;
}

function request(options) {
  const opts = options || {};
  return new Promise(function (resolve, reject) {
    wx.request({
      url: config.BASE_URL + opts.url,
      method: opts.method || 'GET',
      data: opts.data || {},
      header: buildHeader(opts.header),
      timeout: opts.timeout || 65000,
      success: function (res) {
        const status = res.statusCode;
        const body = res.data;
        if (status === 401) {
          handleUnauthorized();
          reject(new Error('登录状态已失效，请重新登录'));
          return;
        }
        if (status < 200 || status >= 300) {
          // 非 2xx 时优先透出后端返回的 message，避免只显示「服务异常（500）」
          reject(new Error(body && body.message ? body.message : '服务异常（' + status + '）'));
          return;
        }
        if (opts.rawText) {
          resolve(body);
          return;
        }
        if (!body || typeof body !== 'object') {
          reject(new Error('响应格式异常，请确认后端已按 Result 统一包装'));
          return;
        }
        if (typeof body.code === 'undefined' || body.code === 0) {
          resolve(body.data);
          return;
        }
        reject(new Error(body.message || '请求失败'));
      },
      fail: function () {
        reject(new Error('网络异常，请确认后端服务已启动'));
      }
    });
  });
}

/**
 * 文件上传（multipart/form-data）
 * onProgress(percent) 驱动上传页进度条
 */
function upload(options) {
  const opts = options || {};
  return new Promise(function (resolve, reject) {
    const task = wx.uploadFile({
      url: config.BASE_URL + opts.url,
      filePath: opts.filePath,
      name: opts.name || 'file',
      formData: opts.formData || {},
      // 不手动设 content-type：wx.uploadFile 会自动生成带 boundary 的 multipart/form-data，
      // 手动指定会导致缺失 boundary，Spring 无法解析 multipart 抛 415/400
      header: buildUploadHeader(),
      timeout: opts.timeout || 65000,
      success: function (res) {
        if (res.statusCode === 401) {
          handleUnauthorized();
          reject(new Error('登录状态已失效，请重新登录'));
          return;
        }
        let body = res.data;
        try {
          body = JSON.parse(res.data);
        } catch (e) {
          reject(new Error('响应格式异常'));
          return;
        }
        if (res.statusCode < 200 || res.statusCode >= 300 || body.code !== 0) {
          reject(new Error(body.message || '上传失败'));
          return;
        }
        resolve(body.data);
      },
      fail: function () {
        reject(new Error('上传失败，请检查网络后重试'));
      }
    });
    if (typeof opts.onProgress === 'function' && task && task.onProgressUpdate) {
      task.onProgressUpdate(function (res) {
        opts.onProgress(res.progress);
      });
    }
  });
}

// 导出「可直接调用的函数」并附带具名属性，兼容两种用法：
//   const request = require('./request');  request({ url })      ← api.js 用的这种
//   const request = require('./request');  request.upload({...}) ← 具名方法
//   const { upload } = require('./request');                     ← 解构
module.exports = request;
module.exports.request = request;
module.exports.upload = upload;
module.exports.getToken = getToken;
module.exports.showError = showError;
