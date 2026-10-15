const config = require('../../config/index');
const api = require('../../utils/api');
const format = require('../../utils/format');

const SAMPLE_FILES = [
  { key: 'ok1', filename: '热学与热力学基础.md', ext: 'MD', size: 264 * 1024, note: '' },
  { key: 'ok2', filename: '实验讲义-验证牛顿第二定律.txt', ext: 'TXT', size: 128 * 1024, note: '' },
  { key: 'bad', filename: '旧版力学讲义.txt', ext: 'TXT', size: 1800 * 1024, note: '模拟解析失败（GBK 编码）' },
  { key: 'big', filename: '课程录音整理.txt', ext: 'TXT', size: 6.4 * 1024 * 1024, note: '超出 5MB 上限' }
];

Page({
  data: {
    courseId: 0,
    courseName: '',
    useMock: config.USE_MOCK,
    maxSizeText: format.fileSize(config.MAX_FILE_SIZE),
    samples: [],
    picked: null,
    pickedKey: '',
    /** pick | uploading | done | failed | error */
    stage: 'pick',
    progress: 0,
    progressText: '正在上传…',
    result: null,
    failReason: ''
  },

  onLoad(options) {
    const courseId = Number(options.courseId || 0);
    const self = this;
    this.setData({
      courseId: courseId,
      samples: SAMPLE_FILES.map(function (item) {
        return Object.assign({}, item, {
          tooBig: item.size > config.MAX_FILE_SIZE,
          sizeText: format.fileSize(item.size)
        });
      })
    });
    api
      .getCourse(courseId)
      .then(function (course) {
        if (course) {
          self.setData({ courseName: course.name });
          wx.setNavigationBarTitle({ title: '上传资料 · ' + course.name });
        }
      })
      .catch(function () {
        /* 课程名仅用于展示，失败不影响上传 */
      });
  },

  /** 点击虚线框：mock 模式选一个示例文件，真实模式调起聊天文件选择 */
  onTapDropzone() {
    if (this.data.useMock) {
      this.onPickSample({ currentTarget: { dataset: { key: 'ok1' } } });
      return;
    }
    this.chooseFile();
  },

  chooseFile() {
    const self = this;
    wx.chooseMessageFile({
      count: 1,
      type: 'file',
      extension: config.ALLOWED_EXT,
      success(res) {
        const file = (res.tempFiles || [])[0];
        if (!file) {
          return;
        }
        const ext = format.extOf(file.name);
        if (config.ALLOWED_EXT.indexOf(ext.toLowerCase()) < 0) {
          wx.showToast({ title: '仅支持 TXT、MD 文件', icon: 'none' });
          return;
        }
        const picked = {
          key: 'real',
          filename: file.name,
          ext: ext,
          size: file.size,
          sizeText: format.fileSize(file.size),
          filePath: file.path
        };
        self.setData({
          picked: picked,
          pickedKey: picked.key,
          stage: file.size > config.MAX_FILE_SIZE ? 'error' : 'pick',
          result: null,
          failReason: ''
        });
      }
    });
  },

  onPickSample(e) {
    const key = e.currentTarget.dataset.key;
    const sample = this.data.samples.filter(function (s) { return s.key === key; })[0];
    if (!sample) {
      return;
    }
    this.setData({
      picked: sample,
      pickedKey: sample.key,
      stage: sample.tooBig ? 'error' : 'pick',
      result: null,
      failReason: ''
    });
  },

  onClearPick() {
    this.setData({ picked: null, pickedKey: '', stage: 'pick', progress: 0, result: null, failReason: '' });
  },

  onUpload() {
    const picked = this.data.picked;
    if (!picked || this.data.stage === 'uploading') {
      return;
    }
    if (picked.size > config.MAX_FILE_SIZE) {
      this.setData({ stage: 'error' });
      return;
    }

    const self = this;
    this.setData({ stage: 'uploading', progress: 0, progressText: '正在上传…' });

    api
      .uploadMaterial({
        courseId: this.data.courseId,
        filename: picked.filename,
        fileSize: picked.size,
        sampleKey: picked.key,
        filePath: picked.filePath,
        onProgress(percent) {
          self.setData({
            progress: percent,
            progressText: percent >= 100 ? '上传完成，正在解析内容…' : '正在上传…'
          });
        }
      })
      .then(function (res) {
        const material = res.material;
        if (material.isFailed) {
          self.setData({
            stage: 'failed',
            progress: 100,
            result: material,
            failReason: material.errorMessage || '文件内容无法读取。'
          });
          return;
        }
        self.setData({ stage: 'done', progress: 100, result: material });
        wx.showToast({
          title: res.duplicate ? '该资料已存在，直接复用' : '资料已解析，可用于生成教案',
          icon: 'none',
          duration: 2000
        });
      })
      .catch(function (err) {
        self.setData({ stage: 'failed', progress: 0, failReason: err.message || '上传失败，请重试' });
      });
  },

  onGoMaterials() {
    wx.redirectTo({ url: '/pages/materials/materials?courseId=' + this.data.courseId });
  },

  onGoGenerate() {
    wx.redirectTo({ url: '/pages/plan-generate/plan-generate?courseId=' + this.data.courseId });
  }
});
