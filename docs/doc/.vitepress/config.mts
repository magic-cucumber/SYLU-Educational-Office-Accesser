import { defineConfig } from 'vitepress'

export default defineConfig({
  title: "EOA - Website",
  description: "沈阳理工大学神秘小软件",
  themeConfig: {
    nav: [
      { text: '首页', link: '/' },
      { text: '文档', link: '/welcome' }
    ],

    sidebar: [
      {
        text: '文档',
        items: [
          { text: '欢迎', link: '/welcome' },
          { text: '架构特性', link: '/architecture' },
          { text: '登录', link: '/login' },
          { text: '课表页面', link: '/course-timetable' },
          { text: '课表总览', link: '/course-overview' },
          { text: '考试', link: '/exam' },
          { text: 'GPA', link: '/gpa' },
          { text: '第二课堂', link: '/second-class' },
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
