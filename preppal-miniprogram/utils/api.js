/**
 * 业务接口层
 *
 * 每个函数内部按 config.USE_MOCK 分流：mock 走本地契约模拟器，否则请求真实后端。
 * 两条路径的**入参与返回值形状完全一致**（均已通过 mapper 转成展示结构），
 * 因此页面代码与后端是否就绪无关 —— 后端落地后只需把 USE_MOCK 改成 false。
 *
 * 真实分支对应的后端接口见 AI-CONTEXT.md「核心接口设计（MVP）」。
 */
const config = require('../config/index');
const request = require('./request');
const mock = require('./mock/db');
const mapper = require('./mapper');

function delay(ms, factory) {
  return new Promise(function (resolve) {
    setTimeout(function () {
      resolve(factory ? factory() : undefined);
    }, ms);
  });
}

/* ---------------- 登录 ---------------- */

/** POST /api/auth/login */
function login(code) {
  if (config.USE_MOCK) {
    return delay(900, function () {
      return { token: 'mock-token-' + Date.now(), user: mock.getUser() };
    });
  }
  return request({ url: '/api/auth/login', method: 'POST', data: { code: code } });
}

/* ---------------- 课程 ---------------- */

/** GET /api/courses */
function listCourses() {
  if (config.USE_MOCK) {
    return delay(260, function () {
      return mock.listCourses().map(mapper.courseFromWire);
    });
  }
  return request({ url: '/api/courses' }).then(function (list) {
    return (list || []).map(mapper.courseFromWire);
  });
}

/** GET /api/courses/{id} */
function getCourse(id) {
  if (config.USE_MOCK) {
    return delay(200, function () {
      const course = mock.getCourse(id);
      return course ? mapper.courseFromWire(course) : null;
    });
  }
  return request({ url: '/api/courses/' + id }).then(function (data) {
    return data ? mapper.courseFromWire(data) : null;
  });
}

/** POST /api/courses */
function createCourse(payload) {
  if (config.USE_MOCK) {
    return delay(320, function () {
      return mapper.courseFromWire(mock.createCourse(payload));
    });
  }
  return request({ url: '/api/courses', method: 'POST', data: payload }).then(function (data) {
    return mapper.courseFromWire(data);
  });
}

/** PUT /api/courses/{id}（AI-CONTEXT 未列出，编辑课程信息需要，落地时请后端补齐） */
function updateCourse(id, payload) {
  if (config.USE_MOCK) {
    return delay(320, function () {
      const course = mock.updateCourse(id, payload);
      return course ? mapper.courseFromWire(course) : null;
    });
  }
  return request({ url: '/api/courses/' + id, method: 'PUT', data: payload }).then(function (data) {
    return data ? mapper.courseFromWire(data) : null;
  });
}

/** DELETE /api/courses/{id} */
function deleteCourse(id) {
  if (config.USE_MOCK) {
    return delay(280, function () {
      return mock.deleteCourse(id);
    });
  }
  return request({ url: '/api/courses/' + id, method: 'DELETE' });
}

/* ---------------- 教学资料 ---------------- */

/** GET /api/materials?courseId= */
function listMaterials(courseId) {
  if (config.USE_MOCK) {
    return delay(240, function () {
      return mock.listMaterials(courseId).map(mapper.materialFromWire);
    });
  }
  return request({ url: '/api/materials', data: { courseId: courseId } }).then(function (list) {
    return (list || []).map(mapper.materialFromWire);
  });
}

/**
 * POST /api/materials（multipart/form-data）
 * mock 模式下用 sampleKey 区分成功 / 编码失败样本；真实模式走 wx.uploadFile。
 */
function uploadMaterial(options) {
  const opts = options || {};
  if (config.USE_MOCK) {
    return mock.uploadMaterial(opts).then(function (res) {
      return {
        material: mapper.materialFromWire(res.material),
        duplicate: !!res.duplicate
      };
    });
  }
  return request
    .upload({
      url: '/api/materials',
      filePath: opts.filePath,
      name: 'file',
      formData: { courseId: opts.courseId },
      onProgress: opts.onProgress
    })
    .then(function (data) {
      return {
        material: mapper.materialFromWire(data),
        duplicate: !!(data && data.duplicate)
      };
    });
}

/** DELETE /api/materials/{id} */
function deleteMaterial(id) {
  if (config.USE_MOCK) {
    return delay(240, function () {
      return mock.deleteMaterial(id);
    });
  }
  return request({ url: '/api/materials/' + id, method: 'DELETE' });
}

/* ---------------- 教案 ---------------- */

/** GET /api/lesson-plans?courseId=&status= */
function listPlans(query) {
  const q = query || {};
  if (config.USE_MOCK) {
    return delay(240, function () {
      return mock.listPlans(q).map(mapper.planFromWire);
    });
  }
  // 仅当显式传入 courseId 才带该参数，避免 courseId=undefined 打挂后端 Long 绑定
  const data = { status: q.status || '' };
  if (q.courseId != null) {
    data.courseId = q.courseId;
  }
  return request({
    url: '/api/lesson-plans',
    data: data
  }).then(function (list) {
    return (list || []).map(mapper.planFromWire);
  });
}

/** GET /api/lesson-plans/{id} */
function getPlan(id) {
  if (config.USE_MOCK) {
    return delay(200, function () {
      const plan = mock.getPlan(id);
      return plan ? mapper.planFromWire(plan) : null;
    });
  }
  return request({ url: '/api/lesson-plans/' + id }).then(function (data) {
    return data ? mapper.planFromWire(data) : null;
  });
}

/** PUT /api/lesson-plans/{id}（保存草稿内容，plan 为展示结构） */
function updatePlan(id, plan) {
  const wire = mapper.planToWire(plan);
  if (config.USE_MOCK) {
    return delay(300, function () {
      const saved = mock.updatePlan(id, wire);
      return saved ? mapper.planFromWire(saved) : null;
    });
  }
  return request({ url: '/api/lesson-plans/' + id, method: 'PUT', data: wire }).then(function (data) {
    return data ? mapper.planFromWire(data) : null;
  });
}

/** POST /api/lesson-plans/{id}/finalize */
function finalizePlan(id) {
  if (config.USE_MOCK) {
    return delay(300, function () {
      const saved = mock.finalizePlan(id);
      return saved ? mapper.planFromWire(saved) : null;
    });
  }
  return request({ url: '/api/lesson-plans/' + id + '/finalize', method: 'POST' }).then(function (data) {
    return data ? mapper.planFromWire(data) : null;
  });
}

/** DELETE /api/lesson-plans/{id} */
function deletePlan(id) {
  if (config.USE_MOCK) {
    return delay(240, function () {
      return mock.deletePlan(id);
    });
  }
  return request({ url: '/api/lesson-plans/' + id, method: 'DELETE' });
}

/**
 * POST /api/lesson-plans/generate
 * 同步接口，后端超时阈值 60s；失败时抛错，由调用方展示「生成超时，可重试」。
 */
function generatePlan(payload) {
  const data = payload || {};
  if (config.USE_MOCK) {
    return mock.generatePlan(data).then(function (plan) {
      return mapper.planFromWire(plan);
    });
  }
  return request({
    url: '/api/lesson-plans/generate',
    method: 'POST',
    data: {
      courseId: data.courseId,
      topic: data.topic,
      targetAudience: data.targetAudience,
      classDuration: data.classDuration,
      teachingType: data.teachingType,
      customObjectives: data.customObjectives || '',
      idempotencyKey: data.idempotencyKey || ''
    }
  }).then(function (body) {
    const res = body || {};
    if (res.status === 'FAILED') {
      throw new Error(res.errorMessage || '生成失败，请重试');
    }
    return res.lessonPlan ? mapper.planFromWire(res.lessonPlan) : null;
  });
}

/* ---------------- 调试 ---------------- */

/** GET /api/hello — 返回纯文本，用于「后端连通性检测」 */
function checkBackend() {
  return request({ url: '/api/hello', rawText: true });
}

/** 重置 mock 数据库（仅调试面板使用） */
function resetMockDb() {
  mock.reset();
}

module.exports = {
  login: login,
  listCourses: listCourses,
  getCourse: getCourse,
  createCourse: createCourse,
  updateCourse: updateCourse,
  deleteCourse: deleteCourse,
  listMaterials: listMaterials,
  uploadMaterial: uploadMaterial,
  deleteMaterial: deleteMaterial,
  listPlans: listPlans,
  getPlan: getPlan,
  updatePlan: updatePlan,
  finalizePlan: finalizePlan,
  deletePlan: deletePlan,
  generatePlan: generatePlan,
  checkBackend: checkBackend,
  resetMockDb: resetMockDb
};
