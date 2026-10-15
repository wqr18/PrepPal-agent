/**
 * 后端 wire 格式 ↔ 小程序展示格式 的映射层
 *
 * 这是前后端字段差异的唯一收敛点：
 *  - objectives：后端 TEXT 单段文本 ↔ 前端 { knowledge, ability, literacy } 三维目标
 *  - teachingProcess：后端 JSON 字符串 [{ stage, duration: '5min', content }] ↔ 前端 [{ stage, duration: 5, content }]
 *  - sourceReferences：后端 JSON 字符串 [{ materialId, materialName, chunkIndex, excerpt }] ↔ 前端展示结构
 *
 * 后端 DTO 真正落地后，只需要调整本文件。
 */
const format = require('./format');

const OBJECTIVE_KEYS = ['knowledge', 'ability', 'literacy'];
const OBJECTIVE_PREFIX = { knowledge: '知识', ability: '能力', literacy: '素养' };

function safeParse(text, fallback) {
  if (!text) {
    return fallback;
  }
  if (typeof text === 'object') {
    return text;
  }
  try {
    const parsed = JSON.parse(text);
    return parsed == null ? fallback : parsed;
  } catch (e) {
    return fallback;
  }
}

/** { knowledge, ability, literacy } → 「知识：…\n能力：…\n素养：…」 */
function objectivesToText(objectives) {
  if (!objectives) {
    return '';
  }
  if (typeof objectives === 'string') {
    return objectives;
  }
  return OBJECTIVE_KEYS
    .filter(function (key) { return String(objectives[key] || '').trim(); })
    .map(function (key) { return OBJECTIVE_PREFIX[key] + '：' + String(objectives[key]).trim(); })
    .join('\n');
}

/** 反向解析；无法识别前缀时整段落入 knowledge，保证内容不丢 */
function textToObjectives(text) {
  const out = { knowledge: '', ability: '', literacy: '' };
  const raw = String(text || '');
  if (!raw.trim()) {
    return out;
  }
  const lines = raw.split('\n');
  let matched = false;
  lines.forEach(function (line) {
    const trimmed = line.trim();
    if (!trimmed) {
      return;
    }
    let hit = null;
    OBJECTIVE_KEYS.forEach(function (key) {
      if (trimmed.indexOf(OBJECTIVE_PREFIX[key] + '：') === 0 || trimmed.indexOf(OBJECTIVE_PREFIX[key] + ':') === 0) {
        hit = key;
      }
    });
    if (hit) {
      matched = true;
      const value = trimmed.replace(/^[^：:]*[：:]/, '').trim();
      out[hit] = out[hit] ? out[hit] + ' ' + value : value;
    } else {
      out.knowledge = out.knowledge ? out.knowledge + '\n' + trimmed : trimmed;
    }
  });
  if (!matched) {
    return { knowledge: raw.trim(), ability: '', literacy: '' };
  }
  return out;
}

/** 前端过程项 → 后端 wire */
function processToWire(process) {
  return (process || []).map(function (item) {
    return {
      stage: item.stage,
      duration: Number(item.duration) + 'min',
      content: item.content
    };
  });
}

/** 后端 wire → 前端过程项（分钟数为数字，便于时间轴合计） */
function processFromWire(process) {
  const list = Array.isArray(process) ? process : [];
  return list.map(function (item) {
    return {
      stage: item.stage || '',
      duration: parseInt(item.duration, 10) || 0,
      content: item.content || ''
    };
  });
}

/** 前端教案 → 后端 wire 载荷 */
function planToWire(plan) {
  const p = plan || {};
  return {
    title: p.title,
    topic: p.topic,
    targetAudience: p.targetAudience,
    classDuration: Number(p.classDuration) || null,
    teachingType: p.teachingType,
    objectives: objectivesToText(p.objectives),
    keyPoints: p.keyPoints || '',
    difficultPoints: p.difficultPoints || '',
    teachingProcess: JSON.stringify(processToWire(p.process)),
    blackboardDesign: p.blackboardDesign || '',
    homework: p.homework || '',
    status: p.status
  };
}

/** 后端 wire 教案 → 前端展示结构 */
function planFromWire(wire) {
  const p = wire || {};
  const refs = safeParse(p.sourceReferences, []);
  const total = processFromWire(safeParse(p.teachingProcess, [])).reduce(function (sum, item) {
    return sum + (Number(item.duration) || 0);
  }, 0);
  const refList = (Array.isArray(refs) ? refs : []).map(function (ref) {
    return {
      materialId: ref.materialId,
      materialName: ref.materialName || '',
      chunkIndex: ref.chunkIndex,
      excerpt: ref.excerpt || ''
    };
  });
  return {
    id: p.id,
    courseId: p.courseId,
    courseName: p.courseName || '',
    title: p.title || '',
    topic: p.topic || '',
    targetAudience: p.targetAudience || '',
    classDuration: p.classDuration || 0,
    teachingType: p.teachingType || '',
    objectives: textToObjectives(p.objectives),
    keyPoints: p.keyPoints || '',
    difficultPoints: p.difficultPoints || '',
    process: processFromWire(safeParse(p.teachingProcess, [])),
    processTotal: total,
    blackboardDesign: p.blackboardDesign || '',
    homework: p.homework || '',
    refs: refList,
    hasRefs: refList.length > 0,
    disclaimer: p.disclaimer || '',
    status: p.status || 'DRAFT',
    updatedAt: p.updatedAt || 0,
    updatedText: format.relativeTime(p.updatedAt),
    isDraft: (p.status || 'DRAFT') === 'DRAFT'
  };
}

/** 后端 wire 资料 → 前端展示结构 */
function materialFromWire(wire) {
  const m = wire || {};
  return {
    id: m.id,
    courseId: m.courseId,
    filename: m.filename || '',
    ext: format.extOf(m.filename),
    fileSize: m.fileSize || 0,
    fileSizeText: format.fileSize(m.fileSize),
    parseStatus: m.parseStatus || 'PENDING',
    errorMessage: m.errorMessage || '',
    createdAt: m.createdAt || 0,
    createdText: format.relativeTime(m.createdAt),
    isOk: m.parseStatus === 'SUCCESS',
    isPending: m.parseStatus === 'PENDING',
    isFailed: m.parseStatus === 'FAILED',
    statusText: m.parseStatus === 'SUCCESS' ? '解析成功' : (m.parseStatus === 'PENDING' ? '解析中' : '解析失败')
  };
}

/** 后端 wire 课程 → 前端展示结构 */
function courseFromWire(wire) {
  const c = wire || {};
  return {
    id: c.id,
    name: c.name || '',
    description: c.description || '',
    materialCount: c.materialCount || 0,
    planCount: c.planCount || 0,
    updatedAt: c.updatedAt || 0,
    updatedText: format.relativeTime(c.updatedAt)
  };
}

module.exports = {
  objectivesToText,
  textToObjectives,
  processToWire,
  processFromWire,
  planToWire,
  planFromWire,
  materialFromWire,
  courseFromWire
};
