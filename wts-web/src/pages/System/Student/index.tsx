import React, { useEffect, useRef, useState } from 'react';
import { ProTable, type ActionType, type ProColumns, type ProFormInstance } from '@ant-design/pro-components';
import { App, Button, Modal, Form, Input, Popconfirm, Space, Upload, TreeSelect, Tag } from 'antd';
import { DeleteOutlined, DownloadOutlined, PlusOutlined, ReloadOutlined, StopOutlined, UploadOutlined } from '@ant-design/icons';
import { useModel } from '@umijs/max';
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
  downloadStudentTemplate,
  getOrganizationTree,
} from '@/services/system';

const StudentPage: React.FC = () => {
  const { message } = App.useApp();
  const { initialState } = useModel('@@initialState');
  const currentUser = initialState?.currentUser;
  const isPlatformAdmin =
    currentUser?.post === 'platform_admin' || (currentUser?.type === '3' && !currentUser?.post);

  // 可管理用户（主任/副主任/平台管理员）：显示全部管理操作；仅可导入的教师只显示导入按钮
  const canManage =
    isPlatformAdmin || String(currentUser?.perms || '').split(',').includes('USER_MANAGE');

  const actionRef = useRef<ActionType>();
  const proFormRef = useRef<ProFormInstance>();
  const [modalOpen, setModalOpen] = useState(false);
  const [importModalOpen, setImportModalOpen] = useState(false);
  const [editingUser, setEditingUser] = useState<any>(null);
  const [importing, setImporting] = useState(false);
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  const [batchOperating, setBatchOperating] = useState(false);
  const [orgTree, setOrgTree] = useState<any[]>([]);
  const [form] = Form.useForm();

  useEffect(() => {
    getOrganizationTree()
      .then((res: any) => {
        setOrgTree((res.data || []).map(normalizeOrgNode));
      })
      .catch(() => {});
  }, []);

  // 组织页跳转联动：URL 携带 orgId 时自动带该组织筛选
  useEffect(() => {
    const orgId = new URLSearchParams(window.location.search).get('orgId');
    if (orgId) {
      proFormRef.current?.setFieldValue('orgId', orgId);
      proFormRef.current?.submit?.();
    }
  }, [orgTree]);

  const normalizeOrgNode = (node: any): any => ({
    title: node.name,
    value: node.id,
    key: node.id,
    children: node.children ? node.children.map(normalizeOrgNode) : undefined,
  });

  const selectedUserIds = selectedRowKeys.map(String);

  const clearSelection = () => setSelectedRowKeys([]);

  const batchDisableSelected = () => {
    if (selectedUserIds.length === 0) {
      message.warning('请先选择学生');
      return;
    }
    Modal.confirm({
      title: `确定禁用选中的 ${selectedUserIds.length} 个学生？`,
      content: '禁用后学生不能登录，但账号数据仍会保留。',
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
      message.warning('请先选择学生');
      return;
    }
    Modal.confirm({
      title: `确定永久删除选中的 ${selectedUserIds.length} 个学生？`,
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

  const openEdit = (record?: any) => {
    setEditingUser(record || null);
    if (record) {
      form.setFieldsValue({
        name: record.name,
        loginname: record.loginname,
        orgId: record.orgId,
        className: record.className,
        comments: record.comments,
      });
    } else {
      form.setFieldsValue({
        name: undefined,
        loginname: undefined,
        className: undefined,
        orgId: undefined,
        comments: undefined,
      });
    }
    setModalOpen(true);
  };

  const columns: ProColumns[] = [
    {
      title: '教研室',
      dataIndex: 'orgName',
      width: 150,
      hideInSearch: true,
      render: (_, record) => record.orgName || '-',
    },
    {
      title: '组织',
      dataIndex: 'orgId',
      width: 180,
      hideInTable: true,
      valueType: 'treeSelect',
      fieldProps: { treeData: orgTree, treeDefaultExpandAll: true, allowClear: true, placeholder: '请选择组织' },
    },
    {
      title: '姓名',
      dataIndex: 'name',
      width: 120,
      hideInSearch: false,
    },
    {
      title: '登录名',
      dataIndex: 'loginname',
      width: 130,
      hideInSearch: true,
    },
    {
      title: '班级',
      dataIndex: 'className',
      width: 120,
      hideInSearch: false,
      render: (_, record) => record.className || '-',
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
      hideInTable: !canManage,
      render: (_, record) => (
        <Space>
          <a onClick={() => openEdit(record)}>编辑</a>
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
            title="确定禁用此学生？"
            onConfirm={async () => {
              await deleteUser(record.id);
              message.success('已禁用');
              actionRef.current?.reload();
            }}
          >
            <a style={{ color: '#fa8c16' }}>禁用</a>
          </Popconfirm>
          <Popconfirm
            title="确定永久删除此学生？"
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
    try {
      const values = await form.validateFields();
      // 学生固定职位为 student、无功能权限
      const payload = {
        name: values.name,
        loginname: values.loginname,
        post: 'student',
        perms: '',
        orgId: values.orgId,
        className: values.className,
        comments: values.comments,
      };
      if (editingUser) {
        await updateUser(editingUser.id, payload);
        message.success('更新成功');
      } else {
        await createUser(payload as any);
        message.success('创建成功，初始密码为 123456');
      }
      setModalOpen(false);
      form.resetFields();
      setEditingUser(null);
      actionRef.current?.reload();
    } catch (error: any) {
      message.error(error?.data?.message || error?.message || '操作失败');
    }
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
      setImportModalOpen(false);
    } catch (error: any) {
      message.error(error?.data?.message || error?.message || '导入学生帐号失败');
    } finally {
      setImporting(false);
    }
  };

  const handleDownloadTemplate = async () => {
    try {
      const res: any = await downloadStudentTemplate();
      const url = window.URL.createObjectURL(res);
      const a = document.createElement('a');
      a.href = url;
      a.download = '学生导入模板.xlsx';
      a.click();
      window.URL.revokeObjectURL(url);
      message.success('模板已下载');
    } catch {
      message.error('下载模板失败');
    }
  };

  return (
    <>
      <ProTable
        headerTitle="学生管理"
        actionRef={actionRef}
        rowKey="id"
        search={{ labelWidth: 80 }}
        toolBarRender={() => [
          ...(canManage
            ? [
                <Button
                  key="add"
                  type="primary"
                  icon={<PlusOutlined />}
                  onClick={() => openEdit()}
                >
                  新建学生
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
              ]
            : []),
          <Button
            key="import-students"
            icon={<UploadOutlined />}
            onClick={() => setImportModalOpen(true)}
          >
            导入学生
          </Button>,
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
            keyword: params.name,
            state: params.state,
            post: 'student',
            className: params.className,
            orgId: params.orgId,
          });
          return {
            data: res.data?.records || [],
            total: res.data?.total || 0,
            success: true,
          };
        }}
        formRef={proFormRef}
        rowSelection={
          canManage
            ? {
                selectedRowKeys,
                onChange: setSelectedRowKeys,
              }
            : false
        }
        columns={columns}
      />

      <Modal
        title={editingUser ? '编辑学生' : '新建学生'}
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
            <Input placeholder="请输入登录名（学号）" disabled={!!editingUser} />
          </Form.Item>
          <Form.Item name="orgId" label="组织归属" tooltip="学生可不选，直接填写班级即可">
            <TreeSelect
              treeData={orgTree}
              treeDefaultExpandAll
              allowClear
              placeholder="选择组织节点"
            />
          </Form.Item>
          <Form.Item name="className" label="班级">
            <Input placeholder="请输入班级" />
          </Form.Item>
          <Form.Item name="comments" label="备注">
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="批量导入学生"
        open={importModalOpen}
        onCancel={() => setImportModalOpen(false)}
        footer={null}
        width={480}
      >
        <div style={{ marginBottom: 16 }}>
          <p style={{ marginBottom: 8 }}>上传Excel文件（.xlsx / .xls）：</p>
          <p style={{ color: '#999', fontSize: 12 }}>
            每行格式：学号 | 姓名 | 班级。初始密码为 123123，重复学号自动更新班级。
          </p>
          <Button
            size="small"
            icon={<DownloadOutlined />}
            onClick={handleDownloadTemplate}
          >
            下载模板
          </Button>
        </div>
        <Upload
          accept=".xlsx,.xls"
          showUploadList={false}
          beforeUpload={(file) => {
            handleImportStudents(file);
            return false;
          }}
        >
          <Button icon={<UploadOutlined />} type="primary" block loading={importing}>
            选择文件并导入
          </Button>
        </Upload>
      </Modal>
    </>
  );
};

export default StudentPage;
