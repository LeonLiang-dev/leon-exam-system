import React, { useState, useEffect } from 'react';
import { Card, Tree, Button, Modal, Form, Input, InputNumber, message, Space, Popconfirm, Empty, Tag } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import {
  getSubjectTypeTree,
  createSubjectType,
  updateSubjectType,
  deleteSubjectType,
  batchDeleteSubjectTypes,
} from '@/services/exam';

const SubjectTypePage: React.FC = () => {
  const [treeData, setTreeData] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingType, setEditingType] = useState<any>(null);
  const [parentId, setParentId] = useState<string | null>(null);
  const [checkedTypeKeys, setCheckedTypeKeys] = useState<string[]>([]);
  const [batchDeleting, setBatchDeleting] = useState(false);
  const [form] = Form.useForm();

  const loadTree = async () => {
    try {
      setLoading(true);
      const res: any = await getSubjectTypeTree();
      setTreeData(res.data || []);
      setCheckedTypeKeys([]);
    } catch {
      message.error('加载分类失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadTree(); }, []);

  const convertToTreeData = (nodes: any[]): any[] =>
    nodes.map((t: any) => ({
      title: (
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <span>{t.name}</span>
          <Tag>{t.children?.length || 0} 子分类</Tag>
          <EditOutlined
            style={{ color: '#1890ff', fontSize: 12 }}
            onClick={(e) => {
              e.stopPropagation();
              setEditingType(t);
              setParentId(null);
              form.setFieldsValue({ name: t.name, comments: t.comments, sort: t.sort });
              setModalOpen(true);
            }}
          />
          <Popconfirm
            title="确定删除此分类？"
            onConfirm={async (e) => {
              e?.stopPropagation();
              await deleteSubjectType(t.id);
              message.success('已删除');
              loadTree();
            }}
          >
            <DeleteOutlined
              style={{ color: '#ff4d4f', fontSize: 12 }}
              onClick={(e) => e.stopPropagation()}
            />
          </Popconfirm>
        </div>
      ),
      key: t.id,
      children: t.children?.length ? convertToTreeData(t.children) : undefined,
    }));

  const handleOk = async () => {
    const values = await form.validateFields();
    try {
      if (editingType) {
        await updateSubjectType(editingType.id, values);
        message.success('更新成功');
      } else {
        await createSubjectType({ ...values, parentid: parentId || 'NONE' });
        message.success('创建成功');
      }
      setModalOpen(false);
      form.resetFields();
      setEditingType(null);
      setParentId(null);
      loadTree();
    } catch {
      message.error('操作失败');
    }
  };

  const handleBatchDelete = () => {
    if (checkedTypeKeys.length === 0) {
      message.warning('请先选择分类');
      return;
    }
    Modal.confirm({
      title: `确定删除选中的 ${checkedTypeKeys.length} 个分类？`,
      content: '删除分类会让该分类从题目分类树中隐藏。',
      okText: '删除',
      cancelText: '取消',
      okButtonProps: { danger: true },
      onOk: async () => {
        setBatchDeleting(true);
        try {
          await batchDeleteSubjectTypes(checkedTypeKeys);
          message.success('批量删除成功');
          loadTree();
        } finally {
          setBatchDeleting(false);
        }
      },
    });
  };

  return (
    <div>
      <Card
        title="题目分类管理"
        extra={
          <Space>
            <Button
              danger
              icon={<DeleteOutlined />}
              disabled={checkedTypeKeys.length === 0}
              loading={batchDeleting}
              onClick={handleBatchDelete}
            >
              批量删除
            </Button>
            <Button
              icon={<PlusOutlined />}
              type="primary"
              onClick={() => {
                setEditingType(null);
                setParentId(null);
                form.resetFields();
                setModalOpen(true);
              }}
            >
              新建根分类
            </Button>
          </Space>
        }
      >
        {treeData.length === 0 && !loading ? (
          <Empty description="暂无分类，点击上方按钮新建" />
        ) : (
          <Tree
            checkable
            showLine
            defaultExpandAll
            checkedKeys={checkedTypeKeys}
            onCheck={(keys) => {
              const nextKeys = Array.isArray(keys) ? keys : keys.checked;
              setCheckedTypeKeys(nextKeys.map(String));
            }}
            treeData={convertToTreeData(treeData)}
          />
        )}
      </Card>

      <Modal
        title={editingType ? '编辑分类' : '新建分类'}
        open={modalOpen}
        onOk={handleOk}
        onCancel={() => {
          setModalOpen(false);
          form.resetFields();
          setEditingType(null);
          setParentId(null);
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
