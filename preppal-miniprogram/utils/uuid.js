/** 生成幂等键，用于防止重复提交生成教案请求（对应后端 idempotency_key） */
function uuid() {
  const chars = '0123456789abcdef';
  let out = '';
  for (let i = 0; i < 32; i += 1) {
    if (i === 8 || i === 12 || i === 16 || i === 20) {
      out += '-';
    }
    out += chars[Math.floor(Math.random() * 16)];
  }
  return out;
}

module.exports = { uuid };
