import request from './request';

/** 用户列表 */
export async function getUsers(params?: {
  page?: number;
  size?: number;
  keyword?: string;
  state?: string;
}) {
  return request.get('/users', { params });
}

/** 创建用户 */
export async function createUser(data: {
  name: string;
  loginname: string;
  type?: string;
  comments?: string;
}) {
  return request.post('/users', data);
}

/** 批量导入学生帐号 */
export async function importStudentUsers(file: File) {
  const formData = new FormData();
  formData.append('file', file);
  return request.post('/users/import-students', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
}

/** 更新用户 */
export async function updateUser(
  id: string,
  data: { name?: string; type?: string; state?: string; comments?: string },
) {
  return request.put(`/users/${id}`, data);
}

/** 删除用户 */
export async function deleteUser(id: string) {
  return request.delete(`/users/${id}`);
}

/** 批量禁用用户 */
export async function disableUsers(ids: string[]) {
  return request.post('/users/batch-disable', { ids });
}

/** 永久删除用户 */
export async function hardDeleteUser(id: string) {
  return request.delete(`/users/${id}/hard-delete`);
}

/** 批量永久删除用户 */
export async function hardDeleteUsers(ids: string[]) {
  return request.post('/users/batch-hard-delete', { ids });
}

/** 重置密码 */
export async function resetPassword(id: string) {
  return request.post(`/users/${id}/reset-password`);
}

/** 修改密码 */
export async function changePassword(data: {
  oldPassword: string;
  newPassword: string;
}) {
  return request.post('/users/change-password', data);
}

/** 组织树 */
export async function getOrganizationTree() {
  return request.get('/organizations/tree');
}

/** 创建组织 */
export async function createOrganization(data: {
  name: string;
  parentid?: string;
  type?: string;
  sort?: number;
  comments?: string;
}) {
  return request.post('/organizations', data);
}

/** 更新组织 */
export async function updateOrganization(
  id: string,
  data: { name?: string; type?: string; sort?: number; comments?: string },
) {
  return request.put(`/organizations/${id}`, data);
}

/** 删除组织 */
export async function deleteOrganization(id: string) {
  return request.delete(`/organizations/${id}`);
}
