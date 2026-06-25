import { history } from '@umijs/max';
import type { RequestConfig, RunTimeLayoutConfig } from '@umijs/max';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { getCurrentUser } from '@/services/auth';
import './global.less';

// 管理端路径前缀（用 / 结尾来精确匹配路径段）
const ADMIN_PATHS = ['/dashboard', '/exam/subject', '/exam/paper', '/exam/room', '/exam/random', '/system'];

// 运行时配置
export const getInitialState = async (): Promise<{
  currentUser?: any;
}> => {
  // 如果在登录页，不获取用户信息
  if (history.location.pathname === '/login') {
    return {};
  }

  try {
    const res: any = await getCurrentUser();
    const user = res?.data;
    return { currentUser: user };
  } catch {
    history.push('/login');
    return {};
  }
};

// 布局配置
export const layout: RunTimeLayoutConfig = ({ initialState }) => {
  return {
    title: 'Leon 在线考试',
    logo: <div className="wts-brand-logo">L</div>,
    menu: { locale: false },
    fixedHeader: true,
    fixSiderbar: true,
    contentStyle: {
      minHeight: 'calc(100vh - 56px)',
      padding: 24,
      background: '#f8fafc',
    },
    token: {
      header: {
        colorBgHeader: '#ffffff',
        colorTextMenu: '#475569',
        colorTextMenuSelected: '#4f46e5',
        colorBgMenuItemSelected: 'rgba(79, 70, 229, 0.08)',
      },
      sider: {
        colorMenuBackground: '#ffffff',
        colorTextMenu: '#475569',
        colorTextMenuSelected: '#4f46e5',
        colorBgMenuItemSelected: 'rgba(79, 70, 229, 0.08)',
      },
    },
    logout: () => {
      localStorage.removeItem('access_token');
      localStorage.removeItem('refresh_token');
      history.push('/login');
    },
    onPageChange: () => {
      const token = localStorage.getItem('access_token');
      const pathname = history.location.pathname;

      if (!token && pathname !== '/login') {
        history.push('/login');
        return;
      }

      // Recompute isAdmin on every navigation to avoid stale closure
      const userType = initialState?.currentUser?.type;
      const isAdmin = userType === '3' || userType === '1';

      // Students accessing admin pages → redirect
      if (token && userType && !isAdmin) {
        const isAdminPath = ADMIN_PATHS.some((p) => pathname === p || pathname.startsWith(p + '/'));
        if (isAdminPath) {
          history.push('/my-exams');
        }
      }
    },
  };
};

export const rootContainer = (container: JSX.Element) => (
  <ConfigProvider
    locale={zhCN}
    theme={{
      token: {
        colorPrimary: '#4f46e5',
        colorInfo: '#4f46e5',
        colorSuccess: '#10b981',
        colorWarning: '#f59e0b',
        colorError: '#ef4444',
        colorTextBase: '#0f172a',
        colorBgLayout: '#f8fafc',
        colorBgContainer: '#ffffff',
        colorBorderSecondary: '#e2e8f0',
        borderRadius: 10,
        borderRadiusLG: 12,
        borderRadiusSM: 8,
        boxShadowSecondary: '0 12px 40px rgba(15, 23, 42, 0.08)',
        controlHeight: 38,
        fontFamily:
          '"Inter", -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif',
      },
      components: {
        Button: {
          fontWeight: 600,
        },
        Card: {
          paddingLG: 24,
        },
      },
    }}
  >
    {container}
  </ConfigProvider>
);

// 请求配置
export const request: RequestConfig = {
  timeout: 30000,
  errorConfig: {
    errorHandler: (error: any) => {
      console.error(error);
    },
    errorThrower: () => {},
  },
  requestInterceptors: [
    (config: any) => {
      const token = localStorage.getItem('access_token');
      if (token) {
        config.headers = {
          ...config.headers,
          Authorization: `Bearer ${token}`,
        };
      }
      return config;
    },
  ],
  responseInterceptors: [
    (response: any) => {
      const data = response.data;
      // Backend returns { code, message, data } — reject on non-200 code
      if (data && data.code !== undefined && data.code !== 200) {
        return Promise.reject({ response, data, message: data.message || '请求失败' });
      }
      return response;
    },
  ],
};
