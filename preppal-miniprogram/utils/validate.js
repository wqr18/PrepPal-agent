/** 表单校验：返回 { valid, errors, firstField }，firstField 用于滚动定位到首个错误字段 */
const config = require('../config/index');

function result(errors) {
  const keys = Object.keys(errors);
  return {
    valid: keys.length === 0,
    errors,
    firstField: keys.length ? keys[0] : ''
  };
}

/** 新建 / 编辑课程 */
function validateCourse(form) {
  const errors = {};
  const name = String((form && form.name) || '').trim();
  if (name.length < config.NAME_MIN || name.length > config.NAME_MAX) {
    errors.name = '请填写课程名称（' + config.NAME_MIN + '–' + config.NAME_MAX + ' 个字）';
  }
  const intro = String((form && form.description) || '').trim();
  if (intro.length > config.INTRO_MAX) {
    errors.description = '课程简介不超过 ' + config.INTRO_MAX + ' 个字';
  }
  return result(errors);
}

/** 生成教案表单 */
function validateGenerate(form) {
  const errors = {};
  const f = form || {};
  if (!String(f.topic || '').trim()) {
    errors.topic = '请填写教学主题';
  }
  if (!String(f.targetAudience || '').trim()) {
    errors.targetAudience = '请填写授课对象';
  }
  const raw = String(f.classDuration == null ? '' : f.classDuration).trim();
  const duration = Number(raw);
  if (!/^\d+$/.test(raw) || duration < config.DURATION_MIN || duration > config.DURATION_MAX) {
    errors.classDuration = '请输入 ' + config.DURATION_MIN + '–' + config.DURATION_MAX + ' 之间的正整数';
  }
  if (!String(f.teachingType || '').trim()) {
    errors.teachingType = '请选择教学类型';
  }
  return result(errors);
}

module.exports = {
  validateCourse,
  validateGenerate
};
