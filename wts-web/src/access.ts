/**
 * access 插件 — 根据职位(post)与权限点(perms)控制页面访问和菜单可见性
 *
 * post: student(学生) / teacher(教师) / director(主任) / deputy(副主任) / platform_admin(平台管理员)
 * perms: 逗号分隔的权限点; platform_admin 的 perms 为空 = 全部权限
 */
const parsePerms = (perms?: string | null): string[] =>
  perms ? perms.split(',').map((p) => p.trim()).filter(Boolean) : [];

/** 由 currentUser 推导职位（兼容旧数据：无 post 时按 type 推导） */
export const resolvePost = (user?: any): string | undefined => {
  if (user?.post) return user.post;
  if (user?.type === '2') return 'student';
  if (user?.type === '3') return 'platform_admin';
  if (user?.type === '1') return 'teacher';
  return undefined;
};

export default function access(initialState: { currentUser?: any }) {
  const user = initialState?.currentUser;
  const post = resolvePost(user);
  const perms = parsePerms(user?.perms);
  const isPlatformAdmin = post === 'platform_admin';

  // 教职工（教师及以上），区别于学生
  const isStaff = post !== undefined && post !== 'student';
  // 兼容旧代码对 type 1/3 的"管理员"语义（旧页面按钮仍可用）
  const isAdmin = user?.type === '3' || user?.type === '1';
  const isStudent = !isStaff;
  // 用户管理权限（主任/副主任/平台管理员）
  const canManage = isPlatformAdmin || perms.includes('USER_MANAGE');
  // 班级导入权限（所有教师默认具备），可进入用户页但仅限导入
  const canImport = isPlatformAdmin || perms.includes('CLASS_IMPORT');

  return {
    isAdmin,
    isStaff,
    isStudent,
    canManage,
    canImport,
    hasPerm: (p: string) => isPlatformAdmin || perms.includes(p),
  };
}