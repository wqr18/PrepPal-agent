/**
 * 自定义 tabBar（app.json 中 tabBar.custom = true）
 * 用图标字体绘制，因此不需要任何 PNG 图标资源。
 * 各 tab 页在 onShow 中调用 this.getTabBar().setData({ selected }) 同步选中态。
 */
Component({
  options: {
    addGlobalClass: true
  },
  data: {
    selected: 0,
    list: [
      { pagePath: '/pages/home/home', text: '首页', icon: 'home' },
      { pagePath: '/pages/courses/courses', text: '课程', icon: 'grid' },
      { pagePath: '/pages/me/me', text: '我的', icon: 'user' }
    ]
  },
  methods: {
    onTap(e) {
      const index = Number(e.currentTarget.dataset.index);
      const item = this.data.list[index];
      if (!item || index === this.data.selected) {
        return;
      }
      wx.switchTab({ url: item.pagePath });
    }
  }
});
