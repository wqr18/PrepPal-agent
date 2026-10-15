/** 展示层格式化工具 */

function pad2(n) {
  return n < 10 ? '0' + n : '' + n;
}

function isSameDay(a, b) {
  return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
}

/**
 * 时间戳 → 相对时间文案：刚刚 / 12 分钟前 / 今天 09:20 / 昨天 16:20 / 10月8日
 */
function relativeTime(ts) {
  const t = Number(ts) || 0;
  if (!t) {
    return '';
  }
  const now = Date.now();
  const diff = now - t;
  if (diff < 60 * 1000) {
    return '刚刚';
  }
  if (diff < 60 * 60 * 1000) {
    return Math.floor(diff / 60000) + ' 分钟前';
  }
  const d = new Date(t);
  const nowDate = new Date(now);
  const hm = pad2(d.getHours()) + ':' + pad2(d.getMinutes());
  if (isSameDay(d, nowDate)) {
    return '今天 ' + hm;
  }
  if (isSameDay(d, new Date(now - 86400000))) {
    return '昨天 ' + hm;
  }
  if (d.getFullYear() === nowDate.getFullYear()) {
    return d.getMonth() + 1 + '月' + d.getDate() + '日';
  }
  return d.getFullYear() + '年' + (d.getMonth() + 1) + '月' + d.getDate() + '日';
}

/** 字节 → 248 KB / 1.6 MB */
function fileSize(bytes) {
  const n = Number(bytes) || 0;
  if (n < 1024) {
    return n + ' B';
  }
  if (n < 1024 * 1024) {
    return Math.round(n / 1024) + ' KB';
  }
  return (n / 1024 / 1024).toFixed(1).replace(/\.0$/, '') + ' MB';
}

/** 文件名 → 大写扩展名（无扩展名返回 FILE） */
function extOf(filename) {
  const name = String(filename || '');
  const dot = name.lastIndexOf('.');
  if (dot < 0 || dot === name.length - 1) {
    return 'FILE';
  }
  return name.slice(dot + 1).toUpperCase();
}

/** 分钟数 → 45 分钟；缺失时返回占位 */
function minutes(n) {
  const v = Number(n);
  return v > 0 ? v + ' 分钟' : '—';
}

module.exports = {
  pad2,
  relativeTime,
  fileSize,
  extOf,
  minutes
};
