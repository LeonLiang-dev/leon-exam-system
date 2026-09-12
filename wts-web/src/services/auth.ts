import request from './request';

export interface LoginParams {
  loginName: string;
  password: string;
}

export interface LoginResult {
  accessToken: string;
  refreshToken: string;
  userId: string;
  loginName: string;
  name: string;
  userType: string;
  post?: string;
}

export interface UserInfo {
  userId: string;
  loginName: string;
  name: string;
  userType: string;
  post?: string;
  permissions: string[];
}

/** 登录 */
export async function login(params: LoginParams) {
  return request.post('/auth/login', params);
}

/** 刷新 Token */
export async function refreshToken(refreshToken: string) {
  return request.post('/auth/refresh', { refreshToken });
}

/** 获取当前用户信息 */
export async function getCurrentUser() {
  return request.get('/auth/me');
}

/** 获取当前用户菜单 */
export async function getCurrentMenus() {
  return request.get('/auth/menus');
}

/** 登出 */
export async function logout() {
  return request.post('/auth/logout');
}
