/**
 * 全局配置（唯一环境开关）
 * 当前 USE_MOCK 已为 false —— 直连真实后端；改回 true 可回退到 utils/mock/db.js 本地模拟器。
 * 上线前把 ENV 切到 prod，并补上真实的 baseUrl。
 */
const ENV = 'dev';

const ENV_CONFIG = {
  dev: { BASE_URL: 'http://localhost:8080' },
  prod: { BASE_URL: 'https://api.preppal.example.com' }
};

module.exports = {
  ENV,
  BASE_URL: ENV_CONFIG[ENV].BASE_URL,

  /** true = 使用本地 mock 数据库（默认）；false = 请求真实后端 */
  USE_MOCK: false,

  /**
   * 后端已接入真实微信 jscode2session。
   * - 联调接入真实 AI 链路时：保持 true，登录提交 code=mock，跳过真实微信授权流程。
   * - 最终验收：改 false，让小程序走真实 wx.login 换取 code。
   * 注意：真实登录必须在开发者工具使用真实 appid（非游客模式），否则 code 无效。
   */
  DEV_MOCK_LOGIN: true,

  /** true = 「我的」页展示开发者调试面板 */
  DEBUG_PANEL: true,

  /** 本地缓存 key */
  TOKEN_KEY: 'pp_token',
  USER_KEY: 'pp_user',

  /** 资料上传限制（与后端 /api/materials 约定保持一致） */
  ALLOWED_EXT: ['txt', 'md'],
  MAX_FILE_SIZE: 5 * 1024 * 1024,

  /** 新建课程表单限制 */
  NAME_MIN: 2,
  NAME_MAX: 24,
  INTRO_MAX: 120,

  /** 生成教案表单限制 */
  TOPIC_MAX: 40,
  AUDIENCE_MAX: 30,
  DURATION_MIN: 10,
  DURATION_MAX: 180,
  TEACHING_TYPES: ['理论', '实验', '研讨', '习题']
};
