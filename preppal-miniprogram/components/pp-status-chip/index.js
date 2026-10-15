/**
 * 状态标签：草稿 / 已定稿 / 解析中 / 解析成功 / 解析失败 / 中性
 * 样式自带（不依赖 app.wxss），但沿用 page 上定义的 CSS 变量令牌
 */
Component({
  options: {
    addGlobalClass: true
  },
  properties: {
    type: {
      type: String,
      value: 'neutral'
    },
    /** 是否显示前置圆点（草稿 / 已定稿 使用） */
    dot: {
      type: Boolean,
      value: false
    }
  }
});
