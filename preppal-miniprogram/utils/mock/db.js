/**
 * 本地 mock 数据库（契约模拟器）
 *
 * 重要：这里**按后端 wire 格式存储**（objectives 为单段文本、teachingProcess / sourceReferences 为 JSON 字符串），
 * 与 AI-CONTEXT.md 的 DTO 定义一致。展示格式由 utils/mapper.js 在 api.js 边界统一转换，
 * 因此 USE_MOCK 切换为 false 后页面代码与数据形状完全不变。
 */
const config = require('../../config/index');
const mapper = require('../mapper');
const debug = require('../debug');

const MINUTE = 60 * 1000;
const HOUR = 60 * MINUTE;
const DAY = 24 * HOUR;

function ago(ms) {
  return Date.now() - ms;
}

function hashOf(text) {
  let h = 0;
  const s = String(text || '');
  for (let i = 0; i < s.length; i += 1) {
    h = (h * 31 + s.charCodeAt(i)) % 2147483647;
  }
  return 'h' + h.toString(16);
}

function jsonOrEmpty(value) {
  return value ? JSON.stringify(value) : '';
}

/** 把「前端形态」的种子数据打包成后端 wire 形态，保持种子数据可读 */
function wirePlan(seed) {
  return {
    id: seed.id,
    courseId: seed.courseId,
    userId: 1,
    title: seed.title,
    topic: seed.topic,
    targetAudience: seed.targetAudience,
    classDuration: seed.classDuration,
    teachingType: seed.teachingType,
    objectives: mapper.objectivesToText(seed.objectives),
    keyPoints: seed.keyPoints,
    difficultPoints: seed.difficultPoints,
    teachingProcess: JSON.stringify(mapper.processToWire(seed.process)),
    blackboardDesign: seed.blackboardDesign,
    homework: seed.homework,
    sourceReferences: jsonOrEmpty(seed.refs),
    disclaimer: seed.refs && seed.refs.length ? '以下内容基于上传资料生成，引用位置已标注。' : '以下内容基于通用知识生成，未参考上传资料。',
    status: seed.status,
    inputSnapshot: jsonOrEmpty(seed.input),
    modelName: 'deepseek-ai/DeepSeek-V3',
    promptVersion: 'v1',
    createdAt: seed.createdAt,
    updatedAt: seed.updatedAt
  };
}

function seedCourses() {
  return [
    {
      id: 1,
      userId: 1,
      name: '大学物理',
      description: '面向理工科一年级，讲授力学与电磁学基础，强调用实验证据建立物理模型。',
      createdAt: ago(20 * DAY),
      updatedAt: ago(12 * MINUTE)
    },
    {
      id: 2,
      userId: 1,
      name: '数据结构',
      description: '面向计算机类二年级，讲授常用数据结构与基础算法分析。',
      createdAt: ago(15 * DAY),
      updatedAt: ago(20 * HOUR)
    },
    {
      id: 3,
      userId: 1,
      name: '高等数学',
      description: '面向工科一年级，微积分基础概念与计算方法训练。',
      createdAt: ago(10 * DAY),
      updatedAt: ago(6 * DAY)
    }
  ];
}

function seedMaterials() {
  return [
    { id: 1, userId: 1, courseId: 1, filename: '大学物理第一章.md', fileSize: 248 * 1024, mimeType: 'text/markdown', parseStatus: 'SUCCESS', errorMessage: '', createdAt: ago(2 * DAY) },
    { id: 2, userId: 1, courseId: 1, filename: '牛顿运动定律.txt', fileSize: 96 * 1024, mimeType: 'text/plain', parseStatus: 'SUCCESS', errorMessage: '', createdAt: ago(26 * HOUR) },
    { id: 3, userId: 1, courseId: 1, filename: '质点运动学讲义.md', fileSize: 512 * 1024, mimeType: 'text/markdown', parseStatus: 'PENDING', errorMessage: '', createdAt: ago(3 * DAY) },
    { id: 4, userId: 1, courseId: 2, filename: '二叉树的定义与性质.md', fileSize: 320 * 1024, mimeType: 'text/markdown', parseStatus: 'SUCCESS', errorMessage: '', createdAt: ago(2 * DAY + 3 * HOUR) },
    { id: 5, userId: 1, courseId: 2, filename: '排序算法整理.txt', fileSize: 1600 * 1024, mimeType: 'text/plain', parseStatus: 'FAILED', errorMessage: '文件编码为 GBK，解析在读到第 12 行时中断。请另存为 UTF-8 编码的 .txt 后重新上传。', createdAt: ago(4 * DAY) },
    { id: 6, userId: 1, courseId: 3, filename: '极限与连续.md', fileSize: 180 * 1024, mimeType: 'text/markdown', parseStatus: 'SUCCESS', errorMessage: '', createdAt: ago(6 * DAY) }
  ].map(function (m) {
    m.contentHash = hashOf(m.filename + m.fileSize);
    m.updatedAt = m.createdAt;
    return m;
  });
}

/** 演示用教案正文模板（牛顿第二定律），新生成的教案以它为基础 */
const TEMPLATE_BODY = {
  objectives: {
    knowledge: '理解牛顿第二定律的内容与矢量性，掌握 F = ma 的适用条件与单位关系。',
    ability: '能对简单力学情境建立受力模型，用正交分解列出方程并求解加速度。',
    literacy: '形成“以实验证据支持结论”的科学态度，体会物理量之间可定量描述的关系。'
  },
  keyPoints: '牛顿第二定律的内容与数学表达式；合力、质量与加速度三者的矢量对应关系。',
  difficultPoints: '合力的分析与正交分解的建立过程；对“瞬时性与同时性”的理解。',
  process: [
    { stage: '导入', duration: 5, content: '用“推购物车”和“电梯加速上升”两段生活情境提问：同样是用力，为什么运动状态的变化不一样？引出“力如何决定运动状态的变化”。' },
    { stage: '新授', duration: 22, content: '回顾演示实验的数据表格，引导学生归纳 a 与 F 成正比、a 与 m 成反比；给出 F = ma 的表达式并强调矢量性与同体性；板演水平面上受拉力的小车例题。' },
    { stage: '探究', duration: 10, content: '小组任务：分析斜面上物体所受的合力，正交分解后求出加速度，教师巡回并提示常见错误。' },
    { stage: '总结', duration: 5, content: '与学生共同整理解题步骤：选对象 → 受力分析 → 正交分解 → 列方程 → 检验单位与方向。' },
    { stage: '作业', duration: 3, content: '布置教材第 2 章习题 2、5、7，说明下节课将用该定律处理连接体问题。' }
  ],
  blackboardDesign: '左侧列出主板书：F = ma 的三要素（大小、方向、单位）与适用条件；右侧留出例题演算区，用不同颜色区分矢量与标量，底部固定“解题五步”提示条。',
  homework: '必做：教材第 2 章习题 2、5、7。选做：拍摄一段生活中的“力与加速度”现象，画出受力分析简图并估算加速度大小。'
};

function seedPlans() {
  return [
    wirePlan({
      id: 1,
      courseId: 1,
      title: '牛顿第二定律教案',
      topic: '牛顿第二定律',
      targetAudience: '大学一年级理工科',
      classDuration: 45,
      teachingType: '理论',
      objectives: TEMPLATE_BODY.objectives,
      keyPoints: TEMPLATE_BODY.keyPoints,
      difficultPoints: TEMPLATE_BODY.difficultPoints,
      process: TEMPLATE_BODY.process,
      blackboardDesign: TEMPLATE_BODY.blackboardDesign,
      homework: TEMPLATE_BODY.homework,
      refs: [
        { materialId: 1, materialName: '大学物理第一章.md', chunkIndex: 3, excerpt: '当物体所受合外力不为零时，其加速度方向与合外力方向相同，大小与合外力成正比、与质量成反比。' },
        { materialId: 2, materialName: '牛顿运动定律.txt', chunkIndex: 8, excerpt: '实验表明，在质量不变时，加速度与合外力成正比；在合外力不变时，加速度与质量成反比。' }
      ],
      status: 'DRAFT',
      input: { courseId: 1, topic: '牛顿第二定律', targetAudience: '大学一年级理工科', classDuration: 45, teachingType: '理论', customObjectives: '' },
      createdAt: ago(10 * MINUTE),
      updatedAt: ago(10 * MINUTE)
    }),
    wirePlan({
      id: 2,
      courseId: 1,
      title: '牛顿第一定律教案',
      topic: '牛顿第一定律（惯性定律）',
      targetAudience: '大学一年级理工科',
      classDuration: 45,
      teachingType: '理论',
      objectives: {
        knowledge: '理解惯性的概念，掌握牛顿第一定律的内容与适用条件。',
        ability: '能举例说明惯性现象，并用惯性解释生活中的问题。',
        literacy: '体会从亚里士多德到伽利略再到牛顿的科学认识发展过程。'
      },
      keyPoints: '牛顿第一定律的内容；惯性与质量的关系。',
      difficultPoints: '“力不是维持运动的原因”这一观念的转变。',
      process: [
        { stage: '导入', duration: 5, content: '演示小车突然启动与刹车的录像，请学生描述身体的前倾后仰感受，引出惯性。' },
        { stage: '新授', duration: 25, content: '回顾伽利略斜面理想实验，说明力与运动的关系；给出牛顿第一定律的表述并强调“一切物体”“不受外力”的严格含义。' },
        { stage: '讨论', duration: 12, content: '分组讨论：为什么说质量是惯性大小的唯一量度？列举三个生活中的惯性现象。' },
        { stage: '作业', duration: 3, content: '布置教材第 2 章习题 1、3。' }
      ],
      blackboardDesign: '以“力与运动关系”的时间轴板书为主轴，列出亚里士多德 → 伽利略 → 牛顿三个节点及关键结论。',
      homework: '必做：教材第 2 章习题 1、3。选做：用一句话解释“安全带为什么能保护乘客”。',
      refs: [
        { materialId: 2, materialName: '牛顿运动定律.txt', chunkIndex: 2, excerpt: '一切物体总保持匀速直线运动状态或静止状态，直到有外力迫使它改变这种状态为止。' }
      ],
      status: 'FINALIZED',
      input: { courseId: 1, topic: '牛顿第一定律（惯性定律）', targetAudience: '大学一年级理工科', classDuration: 45, teachingType: '理论', customObjectives: '' },
      createdAt: ago(26 * HOUR),
      updatedAt: ago(20 * HOUR)
    }),
    wirePlan({
      id: 3,
      courseId: 2,
      title: '二叉树的遍历教案',
      topic: '二叉树的三种遍历',
      targetAudience: '计算机类二年级',
      classDuration: 45,
      teachingType: '理论',
      objectives: {
        knowledge: '理解二叉树的递归定义，掌握先序、中序、后序三种遍历的访问次序。',
        ability: '能对给定二叉树写出三种遍历序列，并用递归代码实现遍历。',
        literacy: '体会“递归结构对应递归算法”的思维方式。'
      },
      keyPoints: '三种遍历的访问次序与递归实现。',
      difficultPoints: '中序遍历与表达式还原之间的联系。',
      process: [
        { stage: '导入', duration: 5, content: '用文件夹目录结构类比二叉树，说明层次结构在计算机中的普遍性。' },
        { stage: '新授', duration: 20, content: '给出二叉树的递归定义，逐个演示三种遍历的访问次序，边讲边在板书上标注访问编号。' },
        { stage: '练习', duration: 15, content: '给出三棵不同的树，学生独立写出遍历序列并互相校对。' },
        { stage: '总结', duration: 5, content: '归纳“先序定根、中序分左右”的结论。' }
      ],
      blackboardDesign: '画出示例树的递归访问编号图，并以三种颜色区分先序、中序、后序遍历路径。',
      homework: '完成教材第 6 章习题 3、4，并尝试用递归写出三种遍历的伪代码。',
      refs: [],
      status: 'DRAFT',
      input: { courseId: 2, topic: '二叉树的三种遍历', targetAudience: '计算机类二年级', classDuration: 45, teachingType: '理论', customObjectives: '' },
      createdAt: ago(21 * HOUR),
      updatedAt: ago(20 * HOUR)
    }),
    wirePlan({
      id: 4,
      courseId: 2,
      title: '快速排序教案',
      topic: '快速排序',
      targetAudience: '计算机类二年级',
      classDuration: 45,
      teachingType: '习题',
      objectives: {
        knowledge: '掌握快速排序的分治思想与基准选取方法。',
        ability: '能手写一趟划分过程并分析时间复杂度。',
        literacy: '体会“以空间换时间”“分而治之”的算法设计策略。'
      },
      keyPoints: '一趟划分的过程与递归划分结构。',
      difficultPoints: '最坏时间复杂度 O(n²) 的成因与规避。',
      process: [
        { stage: '导入', duration: 5, content: '用成绩排序的实际问题说明比较排序的必要性，回顾冒泡排序的效率瓶颈。' },
        { stage: '新授', duration: 20, content: '讲解基准选取与双指针划分过程，逐步演示一趟划分后数组的变化。' },
        { stage: '练习', duration: 15, content: '给定数组手写完整排序过程，比较不同基准选取下的递归深度。' },
        { stage: '总结', duration: 5, content: '归纳平均 O(n log n)、最坏 O(n²)，以及随机化基准的作用。' }
      ],
      blackboardDesign: '左侧板书双指针划分示意，右侧对比冒泡与快排的复杂度曲线。',
      homework: '教材第 7 章习题 2、6。',
      refs: [
        { materialId: 4, materialName: '二叉树的定义与性质.md', chunkIndex: 5, excerpt: '递归结构天然适合用递归算法处理，分治策略的实质是把问题规模不断对半缩减。' }
      ],
      status: 'FINALIZED',
      input: { courseId: 2, topic: '快速排序', targetAudience: '计算机类二年级', classDuration: 45, teachingType: '习题', customObjectives: '' },
      createdAt: ago(7 * DAY),
      updatedAt: ago(6 * DAY)
    })
  ];
}

const db = {
  user: {
    id: 1,
    openid: 'mock-openid',
    nickname: '王老师',
    realName: '王明远',
    org: '示例大学 · 物理学院'
  },
  courses: [],
  materials: [],
  plans: [],
  seq: { course: 0, material: 0, plan: 0 }
};

function reset() {
  db.courses = seedCourses();
  db.materials = seedMaterials();
  db.plans = seedPlans();
  db.seq.course = 10;
  db.seq.material = 10;
  db.seq.plan = 10;
}

function nextId(kind) {
  db.seq[kind] += 1;
  return db.seq[kind];
}

reset();

/** 演示开关：空状态时对外表现为「没有任何课程」 */
function visibleCourses() {
  return debug.get('empty') ? [] : db.courses;
}

/** 「解析中」的资料 2 秒后自动转为解析成功，演示状态流转 */
function resolvePending() {
  const now = Date.now();
  db.materials.forEach(function (m) {
    if (m.parseStatus === 'PENDING' && now - m.createdAt > 2000) {
      m.parseStatus = 'SUCCESS';
      m.updatedAt = now;
    }
  });
}

function materialsOf(courseId) {
  return db.materials.filter(function (m) {
    return m.userId === 1 && (courseId == null || m.courseId === Number(courseId));
  });
}

function plansOf(courseId) {
  return db.plans.filter(function (p) {
    return p.userId === 1 && (courseId == null || p.courseId === Number(courseId));
  });
}

function courseName(id) {
  const course = db.courses.find(function (c) { return c.id === Number(id); });
  return course ? course.name : '';
}

function touchCourse(courseId, ts) {
  const course = db.courses.find(function (c) { return c.id === Number(courseId); });
  if (course) {
    course.updatedAt = ts || Date.now();
  }
}

module.exports = {
  reset: reset,
  delay: function (ms) {
    return new Promise(function (resolve) {
      setTimeout(resolve, ms);
    });
  },

  /* ---------------- 用户 ---------------- */
  getUser: function () {
    return Object.assign({}, db.user);
  },

  /* ---------------- 课程 ---------------- */
  listCourses: function () {
    resolvePending();
    return visibleCourses().map(function (c) {
      return Object.assign({}, c, {
        materialCount: materialsOf(c.id).length,
        planCount: plansOf(c.id).length
      });
    });
  },
  getCourse: function (id) {
    resolvePending();
    const course = visibleCourses().find(function (c) { return c.id === Number(id); });
    if (!course) {
      return null;
    }
    return Object.assign({}, course, {
      materialCount: materialsOf(course.id).length,
      planCount: plansOf(course.id).length
    });
  },
  createCourse: function (data) {
    const now = Date.now();
    const course = {
      id: nextId('course'),
      userId: 1,
      name: String(data.name || '').trim(),
      description: String(data.description || '').trim(),
      createdAt: now,
      updatedAt: now
    };
    db.courses.unshift(course);
    return Object.assign({}, course, { materialCount: 0, planCount: 0 });
  },
  updateCourse: function (id, data) {
    const course = db.courses.find(function (c) { return c.id === Number(id); });
    if (!course) {
      return null;
    }
    if (typeof data.name !== 'undefined') {
      course.name = String(data.name).trim();
    }
    if (typeof data.description !== 'undefined') {
      course.description = String(data.description).trim();
    }
    course.updatedAt = Date.now();
    return Object.assign({}, course, {
      materialCount: materialsOf(course.id).length,
      planCount: plansOf(course.id).length
    });
  },
  deleteCourse: function (id) {
    const courseId = Number(id);
    db.courses = db.courses.filter(function (c) { return c.id !== courseId; });
    db.materials = db.materials.filter(function (m) { return m.courseId !== courseId; });
    db.plans = db.plans.filter(function (p) { return p.courseId !== courseId; });
    return true;
  },

  /* ---------------- 教学资料 ---------------- */
  listMaterials: function (courseId) {
    resolvePending();
    return materialsOf(courseId).slice().sort(function (a, b) {
      return b.createdAt - a.createdAt;
    });
  },
  getMaterial: function (id) {
    resolvePending();
    return db.materials.find(function (m) { return m.id === Number(id); }) || null;
  },
  deleteMaterial: function (id) {
    db.materials = db.materials.filter(function (m) { return m.id !== Number(id); });
    return true;
  },
  /**
   * 上传并解析资料
   * 进度通过 onProgress(percent) 回调驱动，与 wx.uploadFile 的 onProgressUpdate 行为一致
   */
  uploadMaterial: function (options) {
    const opts = options || {};
    const sampleKey = opts.sampleKey || '';
    const filename = opts.filename || '未命名.txt';
    const fileSize = Number(opts.fileSize) || 0;
    const courseId = Number(opts.courseId);

    return new Promise(function (resolve, reject) {
      let progress = 0;
      const timer = setInterval(function () {
        progress = Math.min(100, progress + 7 + Math.random() * 9);
        if (typeof opts.onProgress === 'function') {
          opts.onProgress(Math.floor(progress));
        }
        if (progress >= 100) {
          clearInterval(timer);
          setTimeout(function () {
            if (fileSize > config.MAX_FILE_SIZE) {
              reject(new Error('单个文件不超过 5MB，请压缩内容或拆分为多份后再上传'));
              return;
            }
            const contentHash = hashOf(filename + fileSize);
            const duplicate = db.materials.find(function (m) {
              return m.courseId === courseId && m.contentHash === contentHash;
            });
            if (duplicate) {
              resolve({ material: duplicate, duplicate: true });
              return;
            }
            const failed = sampleKey === 'bad';
            const now = Date.now();
            const material = {
              id: nextId('material'),
              userId: 1,
              courseId: courseId,
              filename: filename,
              fileSize: fileSize,
              mimeType: filename.toLowerCase().indexOf('.md') > -1 ? 'text/markdown' : 'text/plain',
              contentHash: contentHash,
              parseStatus: failed ? 'FAILED' : 'SUCCESS',
              errorMessage: failed
                ? '文件编码为 GBK，解析在读到第 41 行时中断。请另存为 UTF-8 编码的 .txt 后重新上传。'
                : '',
              createdAt: now,
              updatedAt: now
            };
            db.materials.unshift(material);
            touchCourse(courseId, now);
            resolve({ material: material, duplicate: false });
          }, 420);
        }
      }, 210);
    });
  },

  /* ---------------- 教案 ---------------- */
  listPlans: function (query) {
    const q = query || {};
    return plansOf(q.courseId)
      .filter(function (p) {
        return !q.status || q.status === 'ALL' || p.status === q.status;
      })
      .slice()
      .sort(function (a, b) {
        return b.updatedAt - a.updatedAt;
      })
      .map(function (p) {
        return Object.assign({}, p, { courseName: courseName(p.courseId) });
      });
  },
  getPlan: function (id) {
    const plan = db.plans.find(function (p) { return p.id === Number(id); });
    if (!plan) {
      return null;
    }
    return Object.assign({}, plan, { courseName: courseName(plan.courseId) });
  },
  updatePlan: function (id, patch) {
    const plan = db.plans.find(function (p) { return p.id === Number(id); });
    if (!plan) {
      return null;
    }
    Object.keys(patch || {}).forEach(function (key) {
      plan[key] = patch[key];
    });
    plan.updatedAt = Date.now();
    touchCourse(plan.courseId, plan.updatedAt);
    return Object.assign({}, plan, { courseName: courseName(plan.courseId) });
  },
  finalizePlan: function (id) {
    return module.exports.updatePlan(id, { status: 'FINALIZED' });
  },
  deletePlan: function (id) {
    db.plans = db.plans.filter(function (p) { return p.id !== Number(id); });
    return true;
  },

  /**
   * 生成教案草稿（对应 POST /api/lesson-plans/generate）
   * 成功 2.9s 返回；debug.timeout 打开时模拟超时失败
   */
  generatePlan: function (payload) {
    const data = payload || {};
    const courseId = Number(data.courseId);
    return new Promise(function (resolve, reject) {
      setTimeout(function () {
        if (debug.get('timeout')) {
          reject(new Error('生成超时，请重试'));
          return;
        }
        const okMaterials = materialsOf(courseId).filter(function (m) { return m.parseStatus === 'SUCCESS'; });
        const noRefs = debug.get('noRefs') || okMaterials.length === 0;
        const topic = String(data.topic || '').trim();
        const objectives = Object.assign({}, TEMPLATE_BODY.objectives);
        if (String(data.customObjectives || '').trim()) {
          objectives.knowledge = String(data.customObjectives).trim();
        }
        if (noRefs) {
          objectives.knowledge = '掌握' + topic + '的基本内容与常见应用场景。';
        }
        const refs = noRefs
          ? []
          : okMaterials.slice(0, 2).map(function (m, index) {
            return {
              materialId: m.id,
              materialName: m.filename,
              chunkIndex: 3 + index * 5,
              excerpt: '（原文摘要）' + topic + '相关的教材表述，用于支撑本教案的知识目标与教学过程设计。'
            };
          });
        const now = Date.now();
        const plan = wirePlan({
          id: nextId('plan'),
          courseId: courseId,
          title: topic + '教案',
          topic: topic,
          targetAudience: data.targetAudience,
          classDuration: data.classDuration,
          teachingType: data.teachingType,
          objectives: objectives,
          keyPoints: TEMPLATE_BODY.keyPoints,
          difficultPoints: TEMPLATE_BODY.difficultPoints,
          process: TEMPLATE_BODY.process,
          blackboardDesign: TEMPLATE_BODY.blackboardDesign,
          homework: TEMPLATE_BODY.homework,
          refs: refs,
          status: 'DRAFT',
          input: {
            courseId: courseId,
            topic: topic,
            targetAudience: data.targetAudience,
            classDuration: data.classDuration,
            teachingType: data.teachingType,
            customObjectives: data.customObjectives || ''
          },
          createdAt: now,
          updatedAt: now
        });
        plan.idempotencyKey = data.idempotencyKey || '';
        db.plans.unshift(plan);
        touchCourse(courseId, now);
        resolve(Object.assign({}, plan, { courseName: courseName(courseId) }));
      }, 2900);
    });
  }
};

module.exports.TEMPLATE_BODY = TEMPLATE_BODY;
