import { defineConfig } from 'vitepress'

export default defineConfig({
  title: "EOA - Website",
  description: "沈阳理工大学神秘小软件",
  head: [
    ['link', { rel: 'icon', type: 'image/png', href: '/icon.png' }]
  ],
  themeConfig: {
    logo: '/icon.png',

    nav: [
      { text: '首页', link: '/' },
      { text: '文档', link: '/welcome' },
      { text: '下载', link: 'https://gitee.com/kagg886/sylu-educational-office-accesser/releases/download/latest/app-release.apk'}
    ],

    sidebar: [
      {
        text: '文档',
        items: [
          { text: '欢迎', link: '/welcome' },
          { text: '安装', link: '/install' },
          { text: '登录', link: '/login' },
          { text: '课表页面', link: '/course-timetable' },
          { text: '课表总览', link: '/course-overview' },
          { text: '考试', link: '/exam' },
          { text: 'GPA', link: '/gpa' },
          { text: '第二课堂(自4.4.3起)', link: '/second-class' },
          { text: '小组件', link: '/widget' },
          { text: '图片标记', link: '/image-markup' },
          { text: '教务通知', link: '/academic-notice' },
          { text: '设置', link: '/settings' },
          { text: '更新日志', link: '/update' }
        ]
      }
    ],

    socialLinks: [
      { icon: 'github', link: 'https://gitee.com/kagg886/SYLU-Educational-Office-Accesser' }
    ]
  }
})
