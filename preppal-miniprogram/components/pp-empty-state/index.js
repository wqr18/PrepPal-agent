/** 空状态：图标 + 标题 + 说明 + 可选操作按钮 */
Component({
  options: {
    addGlobalClass: true
  },
  properties: {
    icon: {
      type: String,
      value: 'empty-doc'
    },
    title: {
      type: String,
      value: ''
    },
    desc: {
      type: String,
      value: ''
    },
    actionText: {
      type: String,
      value: ''
    }
  },
  methods: {
    onAction() {
      this.triggerEvent('action');
    }
  }
});
