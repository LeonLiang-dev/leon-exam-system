import request from './request';

// ==================== 仪表盘 ====================

/** 获取首页统计数据 */
export async function getDashboardStats() {
  return request.get('/dashboard/stats');
}

// ==================== 题目分类 ====================

/** 题目分类树 */
export async function getSubjectTypeTree() {
  return request.get('/subject-types/tree');
}

/** 创建题目分类 */
export async function createSubjectType(data: any) {
  return request.post('/subject-types', data);
}

/** 更新题目分类 */
export async function updateSubjectType(id: string, data: any) {
  return request.put(`/subject-types/${id}`, data);
}

/** 删除题目分类 */
export async function deleteSubjectType(id: string) {
  return request.delete(`/subject-types/${id}`);
}

/** 批量删除题目分类 */
export async function batchDeleteSubjectTypes(ids: string[]) {
  return request.post('/subject-types/batch-delete', { ids });
}

// ==================== 题目 ====================

/** 题目列表 */
export async function getSubjects(params?: {
  page?: number;
  size?: number;
  keyword?: string;
  typeid?: string;
  tiptype?: string;
  pstate?: string;
}) {
  return request.get('/subjects', { params });
}

/** 题目详情 */
export async function getSubject(id: string) {
  return request.get(`/subjects/${id}`);
}

/** 创建题目 */
export async function createSubject(data: any) {
  return request.post('/subjects', data);
}

/** 更新题目 */
export async function updateSubject(id: string, data: any) {
  return request.put(`/subjects/${id}`, data);
}

/** 删除题目 */
export async function deleteSubject(id: string) {
  return request.delete(`/subjects/${id}`);
}

/** 批量删除题目 */
export async function batchDeleteSubjects(ids: string[]) {
  return request.post('/subjects/batch-delete', { ids });
}

/** 批量导入题目（Excel） */
export async function importSubjects(file: File, typeid: string) {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('typeid', typeid);
  return request.post('/subjects/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
}

/** 导出题目为 Excel */
export async function exportSubjects() {
  return request.get('/subjects/export', { responseType: 'blob' });
}

// ==================== 试卷 ====================

/** 试卷列表 */
export async function getPapers(params?: {
  page?: number;
  size?: number;
  keyword?: string;
}) {
  return request.get('/papers', { params });
}

/** 试卷详情 */
export async function getPaper(id: string) {
  return request.get(`/papers/${id}`);
}

/** 创建试卷 */
export async function createPaper(data: any) {
  return request.post('/papers', data);
}

/** 更新试卷 */
export async function updatePaper(id: string, data: any) {
  return request.put(`/papers/${id}`, data);
}

/** 删除试卷 */
export async function deletePaper(id: string) {
  return request.delete(`/papers/${id}`);
}

/** 批量删除试卷 */
export async function batchDeletePapers(ids: string[]) {
  return request.post('/papers/batch-delete', { ids });
}

/** 获取试卷章节 */
export async function getPaperChapters(paperId: string) {
  return request.get(`/papers/${paperId}/chapters`);
}

/** 获取试卷题目 */
export async function getPaperSubjects(paperId: string) {
  return request.get(`/papers/${paperId}/paper-subjects`);
}

/** 添加题目到试卷 */
export async function addPaperSubject(paperId: string, params: {
  subjectId: string;
  versionId?: string;
  chapterId?: string;
  sort?: number;
  point?: number;
}) {
  return request.post(`/papers/${paperId}/subjects`, null, { params });
}

// ==================== 答题室 ====================

const formatRoomDateTime = (value: any) => {
  if (!value) return undefined;
  if (typeof value?.format === 'function') {
    return value.format('YYYYMMDDHHmmss');
  }
  if (typeof value === 'string') {
    const trimmed = value.trim();
    if (/^\d{14}$/.test(trimmed)) return trimmed;
    if (/^\d{12}$/.test(trimmed)) return `${trimmed}00`;
    return trimmed.replace(/[-:\sT]/g, '').slice(0, 14);
  }
  return value;
};

const normalizeRoomPayload = (data: any) => ({
  ...data,
  starttime: formatRoomDateTime(data?.starttime),
  endtime: formatRoomDateTime(data?.endtime),
});

/** 答题室列表 */
export async function getRooms(params?: {
  page?: number;
  size?: number;
  keyword?: string;
  pstate?: string;
}) {
  return request.get('/rooms', { params });
}

/** 当前用户可参加的答题室 */
export async function getMyRooms(params?: {
  page?: number;
  size?: number;
  keyword?: string;
  pstate?: string;
}) {
  return request.get('/rooms/my', { params });
}

/** 答题室详情 */
export async function getRoom(id: string) {
  return request.get(`/rooms/${id}`);
}

/** 创建答题室 */
export async function createRoom(data: any) {
  return request.post('/rooms', normalizeRoomPayload(data));
}

/** 更新答题室 */
export async function updateRoom(id: string, data: any) {
  return request.put(`/rooms/${id}`, normalizeRoomPayload(data));
}

/** 删除答题室 */
export async function deleteRoom(id: string) {
  return request.delete(`/rooms/${id}`);
}

/** 批量删除答题室 */
export async function batchDeleteRooms(ids: string[]) {
  return request.post('/rooms/batch-delete', { ids });
}

/** 发布答题室 */
export async function publishRoom(id: string) {
  return request.post(`/rooms/${id}/publish`);
}

/** 批量发布答题室 */
export async function batchPublishRooms(ids: string[]) {
  return request.post('/rooms/batch-publish', { ids });
}

/** 关闭答题室 */
export async function closeRoom(id: string) {
  return request.post(`/rooms/${id}/close`);
}

/** 批量关闭答题室 */
export async function batchCloseRooms(ids: string[]) {
  return request.post('/rooms/batch-close', { ids });
}

/** 添加试卷到答题室 */
export async function addRoomPaper(roomId: string, params: {
  paperId: string;
  name?: string;
  passPoint?: number;
}) {
  return request.post(`/rooms/${roomId}/papers`, null, { params });
}

/** 获取答题室试卷 */
export async function getRoomPapers(roomId: string) {
  return request.get(`/rooms/${roomId}/papers`);
}

/** 获取答题室指定参与人员 */
export async function getRoomUsers(roomId: string) {
  return request.get(`/rooms/${roomId}/users`);
}

/** 保存答题室指定参与人员 */
export async function assignRoomUsers(roomId: string, userIds: string[]) {
  return request.post(`/rooms/${roomId}/users`, userIds);
}

// ==================== 随机组卷 ====================

/** 随机规则列表 */
export async function getRandomItems() {
  return request.get('/random-items');
}

/** 创建随机规则 */
export async function createRandomItem(data: any) {
  return request.post('/random-items', data);
}

/** 更新随机规则 */
export async function updateRandomItem(id: string, data: any) {
  return request.put(`/random-items/${id}`, data);
}

/** 删除随机规则 */
export async function deleteRandomItem(id: string) {
  return request.delete(`/random-items/${id}`);
}

/** 批量删除随机规则 */
export async function batchDeleteRandomItems(ids: string[]) {
  return request.post('/random-items/batch-delete', { ids });
}

/** 获取规则步骤 */
export async function getRandomSteps(itemId: string) {
  return request.get(`/random-items/${itemId}/steps`);
}

/** 添加规则步骤 */
export async function addRandomStep(itemId: string, data: any) {
  return request.post(`/random-items/${itemId}/steps`, data);
}

/** 更新规则步骤 */
export async function updateRandomStep(id: string, data: any) {
  return request.put(`/random-steps/${id}`, data);
}

/** 删除规则步骤 */
export async function deleteRandomStep(id: string) {
  return request.delete(`/random-steps/${id}`);
}

/** 批量删除规则步骤 */
export async function batchDeleteRandomSteps(ids: string[]) {
  return request.post('/random-steps/batch-delete', { ids });
}

/** 生成随机试卷 */
export async function generatePapers(itemId: string, count: number) {
  return request.post(`/random-items/${itemId}/generate`, null, { params: { count } });
}

// ==================== 答卷 ====================

/** 进入答题室 */
export async function enterRoom(roomId: string) {
  return request.post('/cards/enter', null, { params: { roomId } });
}

/** 获取答卷对应的试卷内容（答题用，不含正确答案） */
export async function getCardPaper(cardId: string) {
  return request.get(`/cards/${cardId}/paper`);
}

/** 获取答卷对应的试卷内容（阅卷用，不含正确答案） */
export async function getCardPaperForReview(cardId: string) {
  return request.get(`/cards/${cardId}/paper-review`);
}

/** 暂存答案 */
export async function saveCardAnswers(cardId: string, data: any) {
  return request.post(`/cards/${cardId}/save`, data);
}

/** 提交答卷 */
export async function submitCard(cardId: string, data: any) {
  return request.post(`/cards/${cardId}/submit`, data);
}

/** 获取答卷结果（含正确答案和得分） */
export async function getCardResult(cardId: string) {
  return request.get(`/cards/${cardId}`);
}

/** 获取答题室的答卷列表 */
export async function getRoomCards(roomId: string, params?: { page?: number; size?: number }) {
  return request.get(`/cards/rooms/${roomId}/cards`, { params });
}

/** 人工阅卷 */
export async function judgeCard(cardId: string, data?: any) {
  return request.post(`/cards/${cardId}/judge`, data);
}

/** 批量阅卷 */
export async function batchJudgeCards(ids: string[]) {
  return request.post('/cards/batch-judge', { ids });
}
