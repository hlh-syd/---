import { createRouter, createWebHistory } from 'vue-router';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/login',
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: {
        label: '登录',
      },
    },
    {
      path: '/app',
      component: () => import('@/layouts/AppShell.vue'),
      redirect: '/app/research',
      children: [
        {
          path: 'research',
          name: 'research',
          component: () => import('@/views/ResearchView.vue'),
          meta: {
            label: '学术调研',
            description: '论文搜索、思维导图、快速总结与历史搜索',
          },
        },
        {
          path: 'knowledge',
          name: 'knowledge',
          component: () => import('@/views/KnowledgeView.vue'),
          meta: {
            label: '知识库',
            description: '文件夹、文件管理、知识库问答与总结',
          },
        },
        {
          path: 'web-search',
          name: 'web-search',
          component: () => import('@/views/WebSearchView.vue'),
          meta: {
            label: '网页搜索',
            description: '抖音、小红书与网页搜索结果整理',
          },
        },
        {
          path: 'ai-services',
          name: 'ai-services',
          component: () => import('@/views/AiServicesView.vue'),
          meta: {
            label: 'AI服务',
            description: '工具列表、提示词优化与 Bio 助手',
          },
        },
        {
          path: 'research-plan',
          name: 'research-plan',
          component: () => import('@/views/ResearchPlanView.vue'),
          meta: {
            label: '研究计划',
            description: '番茄钟、日历、工作计划与打卡',
          },
        },
        {
          path: 'relax',
          name: 'relax',
          component: () => import('@/views/RelaxView.vue'),
          meta: {
            label: '放松一下',
            description: '八字与吃什么推荐',
          },
        },
        {
          path: 'profile',
          name: 'profile',
          component: () => import('@/views/ProfileView.vue'),
          meta: {
            label: '个人主页',
            description: '编辑资料、第三方关联与退出登录',
          },
        },
      ],
    },
  ],
});

export default router;
