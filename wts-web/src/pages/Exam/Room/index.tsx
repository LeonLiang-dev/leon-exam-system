import React, { useRef, useState } from 'react';
import { history } from '@umijs/max';
import { ProTable, type ActionType, type ProColumns } from '@ant-design/pro-components';
import {
  Button, message, Modal, Form, Input, InputNumber, Select, Popconfirm, Space, Tag, DatePicker,
} from 'antd';
import dayjs from 'dayjs';
import { DeleteOutlined, PlusOutlined, ReloadOutlined, SendOutlined, StopOutlined } from '@ant-design/icons';
import { getUsers } from '@/services/system';
import { formatExamDateTime } from '@/utils/examTime';
import {
  getRooms,
  createRoom,
  updateRoom,
  deleteRoom,
  batchDeleteRooms,
  publishRoom,
  batchPublishRooms,
  closeRoom,
  batchCloseRooms,
  enterRoom,
  getRoomPapers,
  addRoomPaper,
  getPapers,
  getRoomUsers,
  assignRoomUsers,
} from '@/services/exam';

const PSTATE_MAP: Record<string, { text: string; color: string }> = {
  '11': { text: '草稿', color: 'default' },
  '21': { text: '已发布', color: 'green' },
  '31': { text: '已关闭', color: 'red' },
};

const PUBLIC_TYPE_MAP: Record<string, { text: string; color: string }> = {
  '1': { text: '公开', color: 'blue' },
  '2': { text: '指定人员', color: 'purple' },
};

const parseRoomDateTime = (value?: string) => {
  if (!value) return undefined;
  if (/^\d{14}$/.test(value)) {
    return dayjs(
      `${value.slice(0, 4)}-${value.slice(4, 6)}-${value.slice(6, 8)}T${value.slice(8, 10)}:${value.slice(10, 12)}:${value.slice(12, 14)}`,
    );
  }
  if (/^\d{12}$/.test(value)) {
    return dayjs(
      `${value.slice(0, 4)}-${value.slice(4, 6)}-${value.slice(6, 8)}T${value.slice(8, 10)}:${value.slice(10, 12)}:00`,
    );
  }
  const parsed = dayjs(value);
  return parsed.isValid() ? parsed : undefined;
};

const getPublishValidationMessage = (room: any) => {
  const start = parseRoomDateTime(room.starttime);
  const end = parseRoomDateTime(room.endtime);
  if (!start) return '请先设置开始时间';
  if (!end) return '请先设置结束时间';
  if (!start.isBefore(end)) return '开始时间必须早于结束时间';
  if (!end.isAfter(dayjs())) return '结束时间已过，无法发布';
  return '';
};

const RoomPage: React.FC = () => {
  const actionRef = useRef<ActionType>();
  const [modalOpen, setModalOpen] = useState(false);
  const [editingRoom, setEditingRoom] = useState<any>(null);
  const [form] = Form.useForm();
  const [paperModalOpen, setPaperModalOpen] = useState(false);
  const [managingRoom, setManagingRoom] = useState<any>(null);
  const [roomPapers, setRoomPapers] = useState<any[]>([]);
  const [allPapers, setAllPapers] = useState<any[]>([]);
  const [addingPaperId, setAddingPaperId] = useState<string>('');
  const [userModalOpen, setUserModalOpen] = useState(false);
  const [managingUserRoom, setManagingUserRoom] = useState<any>(null);
  const [allUsers, setAllUsers] = useState<any[]>([]);
  const [selectedUserIds, setSelectedUserIds] = useState<string[]>([]);
  const [userModalLoading, setUserModalLoading] = useState(false);
  const [savingUsers, setSavingUsers] = useState(false);
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  const [selectedRows, setSelectedRows] = useState<any[]>([]);
  const [batchOperating, setBatchOperating] = useState(false);

  const clearSelection = () => {
    setSelectedRowKeys([]);
    setSelectedRows([]);
  };

  const getSelectedRoomIdsByState = (state: string, actionName: string) => {
    if (selectedRows.length === 0) {
      message.warning('请先选择答题室');
      return null;
    }
    const invalidRows = selectedRows.filter((room) => room.pstate !== state);
    if (invalidRows.length > 0) {
      message.warning(`只能${actionName}${PSTATE_MAP[state]?.text || '指定状态'}的答题室`);
      return null;
    }
    return selectedRows.map((room) => room.id);
  };

  const handleBatchPublish = () => {
    const ids = getSelectedRoomIdsByState('11', '发布');
    if (!ids) return;
    const invalidRoom = selectedRows.find((room) => getPublishValidationMessage(room));
    if (invalidRoom) {
      message.warning(`${invalidRoom.name || '答题室'}：${getPublishValidationMessage(invalidRoom)}`);
      return;
    }
    Modal.confirm({
      title: `确定发布选中的 ${ids.length} 个答题室？`,
      content: '发布后学生将可以看到符合参与范围的答题室。',
      okText: '发布',
      cancelText: '取消',
      onOk: async () => {
        setBatchOperating(true);
        try {
          await batchPublishRooms(ids);
          message.success('批量发布成功');
          clearSelection();
          actionRef.current?.reload();
        } finally {
          setBatchOperating(false);
        }
      },
    });
  };

  const handleBatchClose = () => {
    const ids = getSelectedRoomIdsByState('21', '关闭');
    if (!ids) return;
    Modal.confirm({
      title: `确定关闭选中的 ${ids.length} 个答题室？`,
      content: '关闭后学生不能继续进入这些答题室。',
      okText: '关闭',
      cancelText: '取消',
      okButtonProps: { danger: true },
      onOk: async () => {
        setBatchOperating(true);
        try {
          await batchCloseRooms(ids);
          message.success('批量关闭成功');
          clearSelection();
          actionRef.current?.reload();
        } finally {
          setBatchOperating(false);
        }
      },
    });
  };

  const handleBatchDelete = () => {
    const ids = getSelectedRoomIdsByState('11', '删除');
    if (!ids) return;
    Modal.confirm({
      title: `确定删除选中的 ${ids.length} 个答题室？`,
      content: '删除答题室会同时移除试卷和参与人员关联。',
      okText: '删除',
      cancelText: '取消',
      okButtonProps: { danger: true },
      onOk: async () => {
        setBatchOperating(true);
        try {
          await batchDeleteRooms(ids);
          message.success('批量删除成功');
          clearSelection();
          actionRef.current?.reload();
        } finally {
          setBatchOperating(false);
        }
      },
    });
  };

  const openUserModal = async (room: any) => {
    setManagingUserRoom(room);
    setUserModalOpen(true);
    setUserModalLoading(true);
    try {
      const [usersRes, roomUsersRes]: any[] = await Promise.all([
        getUsers({ page: 1, size: 500, state: '1' }),
        getRoomUsers(room.id),
      ]);
      setAllUsers(usersRes.data?.records || []);
      setSelectedUserIds((roomUsersRes.data || [])
        .map((item: any) => item.userid)
        .filter(Boolean));
    } catch {
      message.error('加载人员失败');
      setUserModalOpen(false);
    } finally {
      setUserModalLoading(false);
    }
  };

  const columns: ProColumns[] = [
    {
      title: '答题室名称',
      dataIndex: 'name',
      width: 250,
    },
    {
      title: '状态',
      dataIndex: 'pstate',
      width: 100,
      valueType: 'select',
      valueEnum: {
        '11': { text: '草稿' },
        '21': { text: '已发布' },
        '31': { text: '已关闭' },
      },
      render: (_, record) => {
        const s = PSTATE_MAP[record.pstate] || { text: '未知', color: 'default' };
        return <Tag color={s.color}>{s.text}</Tag>;
      },
    },
    {
      title: '答题模式',
      dataIndex: 'pshowtype',
      width: 100,
      hideInSearch: true,
      valueEnum: {
        '1': { text: '标准答题' },
        '2': { text: '抽卷答题' },
        '3': { text: '练习' },
        '4': { text: '学习' },
        '5': { text: '问卷' },
      },
    },
    {
      title: '参与范围',
      dataIndex: 'publictype',
      width: 100,
      hideInSearch: true,
      render: (_, record) => {
        const s = PUBLIC_TYPE_MAP[record.publictype || '1'] || { text: '未知', color: 'default' };
        return <Tag color={s.color}>{s.text}</Tag>;
      },
    },
    {
      title: '时长(分)',
      dataIndex: 'timelen',
      width: 80,
      hideInSearch: true,
    },
    {
      title: '开始时间',
      dataIndex: 'starttime',
      width: 150,
      hideInSearch: true,
      render: (_, record) => formatExamDateTime(record.starttime),
    },
    {
      title: '结束时间',
      dataIndex: 'endtime',
      width: 150,
      hideInSearch: true,
      render: (_, record) => formatExamDateTime(record.endtime),
    },
    {
      title: '操作',
      valueType: 'option',
      width: 320,
      render: (_, record) => (
        <Space>
          <a
            onClick={() => {
              setEditingRoom(record);
              form.setFieldsValue({
                ...record,
                publictype: record.publictype || '1',
                pstate: record.pstate || '11',
                starttime: parseRoomDateTime(record.starttime),
                endtime: parseRoomDateTime(record.endtime),
              });
              setModalOpen(true);
            }}
          >
            编辑
          </a>
          {record.pstate === '21' && (
            <a
              style={{ color: '#1890ff' }}
              onClick={async () => {
                try {
                  const res: any = await enterRoom(record.id);
                  const cardId = res.data?.id;
                  if (cardId) {
                    history.push(`/exam/card/${cardId}`);
                  }
                } catch {
                  message.error('进入答题室失败');
                }
              }}
            >
              进入答题
            </a>
          )}
          {record.pstate === '11' && (
            <Popconfirm
              title="确定发布此答题室？"
              onConfirm={async () => {
                const validationMessage = getPublishValidationMessage(record);
                if (validationMessage) {
                  message.warning(validationMessage);
                  return;
                }
                try {
                  await publishRoom(record.id);
                  message.success('已发布');
                  actionRef.current?.reload();
                } catch (error: any) {
                  message.error(error?.message || '发布失败');
                }
              }}
            >
              <a style={{ color: '#52c41a' }}>发布</a>
            </Popconfirm>
          )}
          {record.pstate === '21' && (
            <Popconfirm
              title="确定关闭此答题室？"
              onConfirm={async () => {
                try {
                  await closeRoom(record.id);
                  message.success('已关闭');
                  actionRef.current?.reload();
                } catch {
                  message.error('关闭失败');
                }
              }}
            >
              <a style={{ color: '#faad14' }}>关闭</a>
            </Popconfirm>
          )}
          {record.pstate === '11' && (
            <Popconfirm
              title="确定删除此答题室？"
              onConfirm={async () => {
                try {
                  await deleteRoom(record.id);
                  message.success('已删除');
                  actionRef.current?.reload();
                } catch {
                  message.error('删除失败');
                }
              }}
            >
              <a style={{ color: '#ff4d4f' }}>删除</a>
            </Popconfirm>
          )}
          <a onClick={() => history.push(`/exam/room/${record.id}/cards`)}>答卷</a>
          {record.pstate === '11' && record.publictype === '2' && (
            <a onClick={() => openUserModal(record)}>管理人员</a>
          )}
          {record.pstate === '11' && (
            <a
              onClick={async () => {
                setManagingRoom(record);
                try {
                  const [rpRes, pRes]: any[] = await Promise.all([
                    getRoomPapers(record.id),
                    getPapers({ page: 1, size: 200 }),
                  ]);
                  setRoomPapers(rpRes.data || []);
                  setAllPapers(pRes.data?.records || []);
                  setPaperModalOpen(true);
                } catch {
                  message.error('加载数据失败');
                }
              }}
            >
              管理试卷
            </a>
          )}
        </Space>
      ),
    },
  ];

  const handleOk = async () => {
    const values = await form.validateFields();
    try {
      if (editingRoom) {
        await updateRoom(editingRoom.id, values);
        message.success('更新成功');
      } else {
        await createRoom(values);
        message.success('创建成功');
      }
      setModalOpen(false);
      form.resetFields();
      setEditingRoom(null);
      actionRef.current?.reload();
    } catch (error: any) {
      message.error(error?.message || '操作失败');
    }
  };

  const handleSaveUsers = async () => {
    if (!managingUserRoom) return;
    setSavingUsers(true);
    try {
      await assignRoomUsers(managingUserRoom.id, selectedUserIds);
      message.success('参与人员已保存');
      setUserModalOpen(false);
      setManagingUserRoom(null);
      setSelectedUserIds([]);
    } catch {
      message.error('保存参与人员失败');
    } finally {
      setSavingUsers(false);
    }
  };

  const userOptions = allUsers.map((user) => {
    const labelName = user.name || user.loginname || user.id;
    const label = user.loginname && user.loginname !== labelName
      ? `${labelName} (${user.loginname})`
      : labelName;
    return { label, value: user.id };
  });

  return (
    <>
      <ProTable
        headerTitle="答题室管理"
        actionRef={actionRef}
        rowKey="id"
        search={{ labelWidth: 80 }}
        toolBarRender={() => [
          <Button
            key="add"
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => {
              setEditingRoom(null);
              form.resetFields();
              setModalOpen(true);
            }}
          >
            新建答题室
          </Button>,
          <Button
            key="reload"
            icon={<ReloadOutlined />}
            onClick={() => actionRef.current?.reload()}
          >
            刷新
          </Button>,
          <Button
            key="batch-publish"
            icon={<SendOutlined />}
            disabled={selectedRowKeys.length === 0}
            loading={batchOperating}
            onClick={handleBatchPublish}
          >
            批量发布
          </Button>,
          <Button
            key="batch-close"
            icon={<StopOutlined />}
            disabled={selectedRowKeys.length === 0}
            loading={batchOperating}
            onClick={handleBatchClose}
          >
            批量关闭
          </Button>,
          <Button
            key="batch-delete"
            danger
            icon={<DeleteOutlined />}
            disabled={selectedRowKeys.length === 0}
            loading={batchOperating}
            onClick={handleBatchDelete}
          >
            批量删除
          </Button>,
        ]}
        request={async (params) => {
          const res: any = await getRooms({
            page: params.current,
            size: params.pageSize,
            keyword: params.name,
            pstate: params.pstate,
          });
          return {
            data: res.data?.records || [],
            total: res.data?.total || 0,
            success: true,
          };
        }}
        rowSelection={{
          selectedRowKeys,
          onChange: (keys, rows) => {
            setSelectedRowKeys(keys);
            setSelectedRows(rows);
          },
        }}
        columns={columns}
      />

      <Modal
        title={editingRoom ? '编辑答题室' : '新建答题室'}
        open={modalOpen}
        onOk={handleOk}
        onCancel={() => {
          setModalOpen(false);
          form.resetFields();
          setEditingRoom(null);
        }}
        width={640}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="name"
            label="答题室名称"
            rules={[{ required: true, message: '请输入答题室名称' }]}
          >
            <Input placeholder="请输入答题室名称" />
          </Form.Item>
          <Form.Item name="pshowtype" label="答题模式" initialValue="1">
            <Select>
              <Select.Option value="1">标准答题</Select.Option>
              <Select.Option value="2">抽卷答题</Select.Option>
              <Select.Option value="3">练习</Select.Option>
              <Select.Option value="4">学习</Select.Option>
              <Select.Option value="5">问卷</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item name="publictype" label="参与范围" initialValue="1">
            <Select>
              <Select.Option value="1">公开</Select.Option>
              <Select.Option value="2">指定人员</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item name="pstate" label="答题室状态" initialValue="11">
            <Select>
              <Select.Option value="11">草稿</Select.Option>
              <Select.Option value="21">已发布</Select.Option>
              <Select.Option value="31">已关闭</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item
            name="starttime"
            label="开始时间"
            rules={[{ required: true, message: '请选择开始时间' }]}
          >
            <DatePicker
              showTime={{ format: 'HH:mm' }}
              format="YYYY-MM-DD HH:mm"
              placeholder="选择开始时间"
              style={{ width: '100%' }}
            />
          </Form.Item>
          <Form.Item
            name="endtime"
            label="结束时间"
            dependencies={['starttime']}
            rules={[
              { required: true, message: '请选择结束时间' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  const start = getFieldValue('starttime');
                  if (!value || !start || start.isBefore(value)) {
                    return Promise.resolve();
                  }
                  return Promise.reject(new Error('结束时间必须晚于开始时间'));
                },
              }),
            ]}
          >
            <DatePicker
              showTime={{ format: 'HH:mm' }}
              format="YYYY-MM-DD HH:mm"
              placeholder="选择结束时间"
              style={{ width: '100%' }}
            />
          </Form.Item>
          <Form.Item name="timelen" label="答题时长(分钟)" initialValue={60}>
            <InputNumber min={1} max={600} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="restarttype" label="重考策略" initialValue="99">
            <Select>
              <Select.Option value="1">允许重考</Select.Option>
              <Select.Option value="99">不允许重考</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item name="resultstype" label="成绩展示" initialValue="1">
            <Select>
              <Select.Option value="1">交卷后显示</Select.Option>
              <Select.Option value="2">阅卷后显示</Select.Option>
              <Select.Option value="3">不显示</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item name="roomnote" label="答题室说明">
            <Input.TextArea rows={3} placeholder="可选" />
          </Form.Item>
        </Form>
      </Modal>

      {/* User assignment modal */}
      <Modal
        title={`管理人员 — ${managingUserRoom?.name || ''}`}
        open={userModalOpen}
        onOk={handleSaveUsers}
        confirmLoading={savingUsers}
        onCancel={() => {
          setUserModalOpen(false);
          setManagingUserRoom(null);
          setSelectedUserIds([]);
        }}
        width={680}
      >
        <Select
          mode="multiple"
          allowClear
          showSearch
          loading={userModalLoading}
          placeholder="选择可参加此答题室的人员"
          value={selectedUserIds}
          onChange={(values) => setSelectedUserIds(values)}
          optionFilterProp="label"
          options={userOptions}
          maxTagCount="responsive"
          style={{ width: '100%' }}
        />
        <div style={{ marginTop: 12, color: '#999' }}>
          已选择 {selectedUserIds.length} 人
        </div>
      </Modal>

      {/* Paper management modal */}
      <Modal
        title={`管理试卷 — ${managingRoom?.name || ''}`}
        open={paperModalOpen}
        onCancel={() => setPaperModalOpen(false)}
        footer={null}
        width={600}
      >
        <div style={{ marginBottom: 16, display: 'flex', gap: 8, alignItems: 'center' }}>
          <Select
            showSearch
            placeholder="选择试卷"
            style={{ flex: 1 }}
            value={addingPaperId || undefined}
            onChange={setAddingPaperId}
            optionFilterProp="label"
            options={allPapers
              .filter((p: any) => !roomPapers.some((rp: any) => rp.paperid === p.id))
              .map((p: any) => ({ label: p.name, value: p.id }))}
          />
          <Button
            type="primary"
            onClick={async () => {
              if (!addingPaperId) { message.warning('请选择试卷'); return; }
              try {
                await addRoomPaper(managingRoom.id, { paperId: addingPaperId });
                message.success('已添加');
                const res: any = await getRoomPapers(managingRoom.id);
                setRoomPapers(res.data || []);
                setAddingPaperId('');
              } catch {
                message.error('添加失败');
              }
            }}
          >
            添加
          </Button>
        </div>
        <div style={{ color: '#999', marginBottom: 8 }}>
          当前共 {roomPapers.length} 份试卷
        </div>
        {roomPapers.map((rp: any) => (
          <div key={rp.id} style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 0', borderBottom: '1px solid #f0f0f0' }}>
            <span>{rp.name || rp.paperid}</span>
            <Tag color="blue">及格分: {rp.passpoint || 60}</Tag>
          </div>
        ))}
      </Modal>
    </>
  );
};

export default RoomPage;
