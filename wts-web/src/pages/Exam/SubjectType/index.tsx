import React, { useState, useEffect } from 'react';
import { App, Card, Button, Modal, Form, Input, InputNumber, Space, Popconfirm, Table, Tag } from 'antd';
import { PlusOutlined, DeleteOutlined, ReloadOutlined } from '@ant-design/icons';
import {
  getSubjectTypeTree,
  createSubjectType,
  updateSubjectType,
  deleteSubjectType,
  batchDeleteSubjectTypes,
} from '@/services/exam';

const SubjectTypePage: React.FC = () => {
  const { message } = App.useApp();
  const [typeList, setTypeList] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingType, setEditingType] = useState<any>(null);
  const [selectedRowKeys, setSelectedRowKeys] = useState<string[]>([]);
  const [batchDeleting, setBatchDeleting] = useState(false);
  const [form] = Form.useForm();

  const loadTypes = async () => {
    try {
      setLoading(true);
      const res: any = await getSubjectTypeTree();
      // 分类为按课程展开的一层结构，直接平铺展示
      setTypeList(res.data || []);
      setSelectedRowKeys([]);
    } catch {
      message.error('加载分类失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadTypes(); }, []);

  const openCreate = () => {
    setEditingType(null);
    form.resetFields();
    form.setFieldsValue({ sort: 1 });
    setModalOpen(true);
  };

  const openEdit = (record: any) => {
    setEditingType(record);
    form.setFieldsValue({ name: record.name, comments: record.comments, sort: record.sort });
    setModalOpen(true);
  };

  const handleOk = async () => {
    const values = await form.validateFields();
    try {
      if (editingType) {
        await updateSubjectType(editingType.id, values);
        message.success('更新成功');
      } else {
        // 分类不做主从分支，全部为一级课程分类
        await createSubjectType({ ...values, parentid: 'NONE' });
        message.success('创建成功');
      }
      setModalOpen(false);
      form.resetFields();
      setEditingType(null);
      loadTypes();
    } catch {
      message.error('操作失败');
    }
  };

  const handleBatchDelete = () => {
    if (selectedRowKeys.length === 0) {
      message.warning('请先选择分类');
      return;
    }
    Modal.confirm({
      title: `确定删除选中的 ${selectedRowKeys.length} 个分类？`,
      content: '删除分类会让该分类从题目分类列表中隐藏。',
      okText: '删除',
      cancelText: '取消',
      okButtonProps: { danger: true },
      onOk: async () => {
        setBatchDeleting(true);
        try {
          await batchDeleteSubjectTypes(selectedRowKeys);
          message.success('批量删除成功');
          loadTypes();
        } finally {
          setBatchDeleting(false);
        }
      },
    });
  };

  const columns = [
    {
      title: '分类名称',
      dataIndex: 'name',
      key: 'name',
      render: (name: string, record: any) => (
        <Space>
          <span>{name}</span>
          {record.orgName && <Tag color="purple">{record.orgName}</Tag>}
        </Space>
      ),
    },
    {
      title: '排序',
      dataIndex: 'sort',
      key: 'sort',
      width: 100,
    },
    {
      title: '说明',
      dataIndex: 'comments',
      key: 'comments',
      ellipsis: true,
    },
    {
      title: '操作',
      key: 'actions',
      width: 160,
      render: (_: any, record: any) => (
        <Space>
          <a onClick={() => openEdit(record)}>编辑</a>
          <Popconfirm
            title="确定删除此分类？"
            onConfirm={async () => {
              await deleteSubjectType(record.id);
              message.success('已删除');
              loadTypes();
            }}
          >
            <a style={{ color: '#ff4d4f' }}>删除</a>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <Card
        title="题目分类管理"
        extra={
          <Space>
            <Button
              danger
              icon={<DeleteOutlined />}
              disabled={selectedRowKeys.length === 0}
              loading={batchDeleting}
              onClick={handleBatchDelete}
            >
              批量删除
            </Button>
            <Button
              icon={<ReloadOutlined />}
              onClick={loadTypes}
            >
              刷新
            </Button>
            <Button icon={<PlusOutlined />} type="primary" onClick={openCreate}>
              新建分类
            </Button>
          </Space>
        }
      >
        <Table
          rowKey="id"
          loading={loading}
          dataSource={typeList}
          columns={columns}
          rowSelection={{
            selectedRowKeys,
            onChange: (keys) => setSelectedRowKeys(keys.map(String)),
          }}
          pagination={typeList.length > 10 ? { pageSize: 10, showSizeChanger: true } : false}
        />
      </Card>

      <Modal
        title={editingType ? '编辑分类' : '新建分类'}
        open={modalOpen}
        onOk={handleOk}
        onCancel={() => {
          setModalOpen(false);
          form.resetFields();
          setEditingType(null);
        }}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="分类名称" rules={[{ required: true, message: '请输入分类名称' }]}>
            <Input placeholder="请输入分类名称" />
          </Form.Item>
          <Form.Item name="comments" label="说明">
            <Input.TextArea rows={2} placeholder="可选" />
          </Form.Item>
          <Form.Item name="sort" label="排序" initialValue={1}>
            <InputNumber min={1} max={999} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default SubjectTypePage;