import React, { useRef, useState } from 'react';
import { ProTable, type ActionType, type ProColumns } from '@ant-design/pro-components';
import { Button, message, Modal, Form, Input, Select, Popconfirm, Space, Upload } from 'antd';
import { DeleteOutlined, PlusOutlined, ReloadOutlined, StopOutlined, UploadOutlined } from '@ant-design/icons';
import {
  getUsers,
  createUser,
  updateUser,
  deleteUser,
  disableUsers,
  hardDeleteUser,
  hardDeleteUsers,
  resetPassword,
  importStudentUsers,
} from '@/services/system';

const UserPage: React.FC = () => {
  const actionRef = useRef<ActionType>();
  const [modalOpen, setModalOpen] = useState(false);
  const [editingUser, setEditingUser] = useState<any>(null);
  const [importing, setImporting] = useState(false);
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  const [batchOperating, setBatchOperating] = useState(false);
  const [form] = Form.useForm();

  const selectedUserIds = selectedRowKeys.map(String);

  const clearSelection = () => setSelectedRowKeys([]);

  const batchDisableSelected = () => {
    if (selectedUserIds.length === 0) {
      message.warning('请先选择用户');
      return;
    }
    Modal.confirm({
      title: `确定禁用选中的 ${selectedUserIds.length} 个用户？`,
      content: '禁用后用户不能登录，但账号数据仍会保留。',
      okText: '禁用',
      cancelText: '取消',
      okButtonProps: { danger: true },
      onOk: async () => {
        setBatchOperating(true);
        try {
          await disableUsers(selectedUserIds);
          message.success('批量禁用成功');
          clearSelection();
          actionRef.current?.reload();
        } finally {
          setBatchOperating(false);
        }
      },
    });
  };

  const batchHardDeleteSelected = () => {
    if (selectedUserIds.length === 0) {
      message.warning('请先选择用户');
      return;
    }
    Modal.confirm({
      title: `确定永久删除选中的 ${selectedUserIds.length} 个用户？`,
      content: '永久删除会移除账号及其组织关联，操作不可恢复。',
      okText: '永久删除',
      cancelText: '取消',
      okButtonProps: { danger: true },
      onOk: async () => {
        setBatchOperating(true);
        try {
          await hardDeleteUsers(selectedUserIds);
          message.success('批量删除成功');
          clearSelection();
          actionRef.current?.reload();
        } finally {
          setBatchOperating(false);
        }
      },
    });
  };

  const columns: ProColumns[] = [
    {
      title: '姓名',
      dataIndex: 'name',
      width: 120,
    },
    {
      title: '登录名',
      dataIndex: 'loginname',
      width: 120,
    },
    {
      title: '用户类型',
      dataIndex: 'type',
      width: 100,
      valueEnum: {
        '1': { text: '系统用户' },
        '2': { text: '其他' },
        '3': { text: '超级管理员' },
      },
    },
    {
      title: '状态',
      dataIndex: 'state',
      width: 80,
      valueEnum: {
        '1': { text: '正常', status: 'Success' },
        '0': { text: '禁用', status: 'Error' },
      },
    },
    {
      title: '最后登录',
      dataIndex: 'logintime',
      width: 160,
      hideInSearch: true,
      render: (_, record) => {
        const t = record.logintime;
        if (!t || t.length !== 14) return '-';
        return `${t.slice(0, 4)}-${t.slice(4, 6)}-${t.slice(6, 8)} ${t.slice(8, 10)}:${t.slice(10, 12)}:${t.slice(12, 14)}`;
      },
    },
    {
      title: '操作',
      valueType: 'option',
      width: 280,
      render: (_, record) => (
        <Space>
          <a
            onClick={() => {
              setEditingUser(record);
              form.setFieldsValue(record);
              setModalOpen(true);
            }}
          >
            编辑
          </a>
          <Popconfirm
            title="确定重置密码为 123456？"
            onConfirm={async () => {
              await resetPassword(record.id);
              message.success('密码已重置为 123456');
            }}
          >
            <a style={{ color: '#faad14' }}>重置密码</a>
          </Popconfirm>
          <Popconfirm
            title="确定禁用此用户？"
            onConfirm={async () => {
              await deleteUser(record.id);
              message.success('已禁用');
              actionRef.current?.reload();
            }}
          >
            <a style={{ color: '#fa8c16' }}>禁用</a>
          </Popconfirm>
          <Popconfirm
            title="确定永久删除此用户？"
            description="删除后账号及组织关联不可恢复。"
            okText="永久删除"
            cancelText="取消"
            okButtonProps={{ danger: true }}
            onConfirm={async () => {
              await hardDeleteUser(record.id);
              message.success('已删除');
              actionRef.current?.reload();
            }}
          >
            <a style={{ color: '#ff4d4f' }}>删除</a>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const handleOk = async () => {
    const values = await form.validateFields();
    if (editingUser) {
      await updateUser(editingUser.id, values);
      message.success('更新成功');
    } else {
      await createUser(values);
      message.success('创建成功，初始密码为 123456');
    }
    setModalOpen(false);
    form.resetFields();
    setEditingUser(null);
    actionRef.current?.reload();
  };

  const handleImportStudents = async (file: File) => {
    setImporting(true);
    try {
      const res: any = await importStudentUsers(file);
      const data = res.data || {};
      const errors = data.errors || [];
      Modal.info({
        title: '导入完成',
        content: (
          <div>
            <p>处理 {data.total || 0} 行，新增 {data.created || 0} 个，更新 {data.updated || 0} 个，失败 {data.failed || 0} 个。</p>
            {errors.length > 0 && (
              <div style={{ maxHeight: 180, overflow: 'auto', color: '#ff4d4f' }}>
                {errors.slice(0, 20).map((error: string) => (
                  <div key={error}>{error}</div>
                ))}
                {errors.length > 20 && <div>仅显示前 20 条错误</div>}
              </div>
            )}
          </div>
        ),
      });
      actionRef.current?.reload();
    } catch (error: any) {
      message.error(error?.data?.message || error?.message || '导入学生帐号失败');
    } finally {
      setImporting(false);
    }
  };

  return (
    <>
      <ProTable
        headerTitle="用户管理"
        actionRef={actionRef}
        rowKey="id"
        search={{ labelWidth: 80 }}
        toolBarRender={() => [
          <Button
            key="add"
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => {
              setEditingUser(null);
              form.resetFields();
              setModalOpen(true);
            }}
          >
            新建用户
          </Button>,
          <Button
            key="batch-disable"
            icon={<StopOutlined />}
            disabled={selectedRowKeys.length === 0}
            loading={batchOperating}
            onClick={batchDisableSelected}
          >
            批量禁用
          </Button>,
          <Button
            key="batch-delete"
            danger
            icon={<DeleteOutlined />}
            disabled={selectedRowKeys.length === 0}
            loading={batchOperating}
            onClick={batchHardDeleteSelected}
          >
            批量删除
          </Button>,
          <Upload
            key="import-students"
            accept=".xlsx,.xls"
            showUploadList={false}
            beforeUpload={(file) => {
              handleImportStudents(file);
              return false;
            }}
          >
            <Button icon={<UploadOutlined />} loading={importing}>
              导入学生
            </Button>
          </Upload>,
          <Button
            key="reload"
            icon={<ReloadOutlined />}
            onClick={() => actionRef.current?.reload()}
          >
            刷新
          </Button>,
        ]}
        request={async (params) => {
          const res: any = await getUsers({
            page: params.current,
            size: params.pageSize,
            keyword: params.name || params.loginname,
            state: params.state,
          });
          return {
            data: res.data?.records || [],
            total: res.data?.total || 0,
            success: true,
          };
        }}
        rowSelection={{
          selectedRowKeys,
          onChange: setSelectedRowKeys,
        }}
        columns={columns}
      />

      <Modal
        title={editingUser ? '编辑用户' : '新建用户'}
        open={modalOpen}
        onOk={handleOk}
        onCancel={() => {
          setModalOpen(false);
          form.resetFields();
          setEditingUser(null);
        }}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="name"
            label="姓名"
            rules={[{ required: true, message: '请输入姓名' }]}
          >
            <Input placeholder="请输入姓名" />
          </Form.Item>
          <Form.Item
            name="loginname"
            label="登录名"
            rules={[{ required: true, message: '请输入登录名' }]}
          >
            <Input placeholder="请输入登录名" disabled={!!editingUser} />
          </Form.Item>
          <Form.Item name="type" label="用户类型" initialValue="1">
            <Select>
              <Select.Option value="1">系统用户</Select.Option>
              <Select.Option value="2">其他</Select.Option>
              <Select.Option value="3">超级管理员</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item name="comments" label="备注">
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
};

export default UserPage;
