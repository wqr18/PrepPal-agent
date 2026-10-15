/**
 * 原型演示开关（仅用于演示与验收）
 * 由「我的 → 开发者调试」面板控制，数据层读取这些标志来模拟空状态 / 生成超时 / 资料不足。
 * 正式交付时把 config/index.js 的 DEBUG_PANEL 置为 false 即可从界面上隐藏。
 */
const flags = {
  /** 新用户空状态：课程列表返回空 */
  empty: false,
  /** 教案生成超时失败 */
  timeout: false,
  /** 生成结果未引用到上传资料 */
  noRefs: false
};

const LABELS = {
  empty: '新用户空状态（无课程）',
  timeout: '生成超时失败',
  noRefs: '资料不足（通用知识提示）'
};

module.exports = {
  flags,
  LABELS,
  get(key) {
    return !!flags[key];
  },
  set(key, value) {
    flags[key] = !!value;
  },
  reset() {
    flags.empty = false;
    flags.timeout = false;
    flags.noRefs = false;
  }
};
