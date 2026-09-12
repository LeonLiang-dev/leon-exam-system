import { defineConfig } from '@umijs/max';

export default defineConfig({
  antd: {
    // 启用 antd App 容器，使 App.useApp() 能读取动态主题上下文
    appConfig: {},
  },
  access: {},
  model: {},
  initialState: {},
  request: {},
  layout: {
    title: 'Leon在线考试系统',
    locale: false,
  },
  proxy: {
    '/api': {
      // 容器内开发时通过 PROXY_TARGET 指向后端服务名（如 http://backend:8080）
      target: process.env.PROXY_TARGET || 'http://localhost:8080',
      changeOrigin: true,
    },
  },
  routes: [
    {
      path: '/login',
      layout: false,
      component: './Login',
    },
    {
      path: '/my-exams',
      name: '我的考试',
      component: './MyExams',
      icon: 'FileTextOutlined',
      access: 'isStudent',
    },
    {
      path: '/exam/card/:id',
      layout: false,
      component: './Exam/Card',
    },
    {
      path: '/exam/card/:id/result',
      layout: false,
      component: './Exam/Card/Result',
    },
    {
      path: '/exam/card/:id/judge',
      layout: false,
      component: './Exam/Card/Judge',
    },
    {
      path: '/exam/room/:roomId/cards',
      component: './Exam/Room/CardList',
      access: 'isStaff',
    },
    {
      path: '/',
      redirect: '/login',
    },
    {
      name: '首页',
      path: '/dashboard',
      component: './Dashboard',
      icon: 'DashboardOutlined',
      access: 'isStaff',
    },
    {
      name: '考试管理',
      path: '/exam',
      icon: 'FormOutlined',
      access: 'isStaff',
      routes: [
        {
          name: '题目分类',
          path: '/exam/subject-type',
          component: './Exam/SubjectType',
        },
        {
          name: '题目管理',
          path: '/exam/subject',
          component: './Exam/Subject',
        },
        {
          name: '试卷管理',
          path: '/exam/paper',
          component: './Exam/Paper',
        },
        {
          name: '答题室管理',
          path: '/exam/room',
          component: './Exam/Room',
        },
        {
          name: '随机组卷',
          path: '/exam/random',
          component: './Exam/Random',
        },
      ],
    },
    {
      name: '系统管理',
      path: '/system',
      icon: 'SettingOutlined',
      access: 'isStaff',
      routes: [
        {
          name: '用户管理',
          path: '/system/user',
          component: './System/User',
        },
        {
          name: '组织机构',
          path: '/system/organization',
          component: './System/Organization',
        },
      ],
    },
  ],
  npmClient: 'npm',
});
