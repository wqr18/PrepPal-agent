/**
 * 语义图标 → Vant 图标字体名 的唯一映射表
 * 原型中的内联 SVG 全部收敛到这里；若某个图标渲染为空，只需修改本文件的映射值。
 * 传入未登记的 name 时会原样交给 van-icon（便于直接使用 Vant 图标名）。
 */
const ICONS = {
  back: 'arrow-left',
  chev: 'arrow',
  'chev-down': 'arrow-down',
  dots: 'ellipsis',
  plus: 'plus',
  trash: 'delete-o',
  doc: 'records',
  book: 'notes-o',
  spark: 'fire-o',
  upload: 'upgrade',
  target: 'flag-o',
  warn: 'warning-o',
  info: 'info-o',
  check: 'checked',
  clock: 'clock-o',
  layer: 'apps-o',
  board: 'desktop-o',
  plan: 'todo-list-o',
  edit: 'edit',
  help: 'question-o',
  shield: 'shield-o',
  user: 'user-o',
  home: 'wap-home-o',
  grid: 'apps-o',
  wechat: 'wechat',
  'empty-course': 'notes-o',
  'empty-doc': 'records',
  'empty-plan': 'todo-list-o'
};

Component({
  options: {
    addGlobalClass: true
  },
  properties: {
    name: {
      type: String,
      value: ''
    },
    size: {
      type: String,
      value: '32rpx'
    },
    color: {
      type: String,
      value: ''
    }
  },
  data: {
    vantName: ''
  },
  lifetimes: {
    attached() {
      this.resolve();
    }
  },
  observers: {
    name() {
      this.resolve();
    }
  },
  methods: {
    resolve() {
      const raw = this.data.name || '';
      this.setData({ vantName: ICONS[raw] || raw });
    }
  }
});
