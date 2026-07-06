import React, { useEffect, useState } from 'react';
import { Card, Tree, Button, Modal, Form, Input, InputNumber, Select, message, Space, Spin, Popconfirm } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import {
  getOrganizationTree,
  createOrganization,
  updateOrganization,
  deleteOrganization,
} from '@/services/system';

interface OrgNode {
  id: string;
  name: string;
  type?: string;
  sort?: number;
  children?: OrgNode[];
}

const OrganizationPage: React.FC = () => {
  const [treeData, setTreeData] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedNode, setSelectedNode] = useState<OrgNode | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingOrg, setEditingOrg] = useState<OrgNode | null>(null);
  const [form] = Form.useForm();

  const loadTree = async () => {
    setLoading(true);
    try {
      const res: any = await getOrganizationTree();
      setTreeData(convertToTreeData(res.data || []));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadTree();
  }, []);

  const convertToTreeData = (nodes: OrgNode[]): any[] =>
    nodes.map((node) => ({
      key: node.id,
      title: node.name,
      data: node,
      children: node.children ? convertToTreeData(node.children) : [],
    }));

  const handleAdd = (parentId?: string) => {
    setEditingOrg(null);
    form.resetFields();
    form.setFieldsValue({ parentid: parentId || 'NONE', type: '0', sort: 1 });
    setModalOpen(true);
  };

  const handleEdit = () => {
    if (!selectedNode) return;
    setEditingOrg(selectedNode);
    form.setFieldsValue(selectedNode);
    setModalOpen(true);
  };

  const handleDelete = async () => {
    if (!selectedNode) return;
    try {
      await deleteOrganization(selectedNode.id);
      message.success('已删除');
      setSelectedNode(null);
      loadTree();
    } catch {
      message.error('删除失败');
    }
  };

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      if (editingOrg) {
        await updateOrganization(editingOrg.id, values);
        message.success('更新成功');
      } else {
        await createOrganization(values);
        message.success('创建成功');
      }
      setModalOpen(false);
      form.resetFields();
      setEditingOrg(null);
      loadTree();
    } catch {
      message.error('操作失败');
    }
  };

  return (
    <div style={{ display: 'flex', gap: 16 }}>
      <Card
        title="组织机构"
        style={{ width: 400, minHeight: 600 }}
        extra={
          <Button
            type="primary"
            size="small"
            icon={<PlusOutlined />}
            onClick={() => handleAdd()}
          >
            新建根组织
          </Button>
        }
      >
        <Spin spinning={loading}>
          {treeData.length > 0 ? (
            <Tree
              treeData={treeData}
              onSelect={(_, info) => {
                setSelectedNode(info.node.data);
              }}
              selectedKeys={selectedNode ? [selectedNode.id] : []}
            />
          ) : (
            <div style={{ textAlign: 'center', color: '#999', padding: 40 }}>
              暂无组织数据，请先创建
            </div>
          )}
        </Spin>
      </Card>

      <Card title="组织详情" style={{ flex: 1, minHeight: 600 }}>
        {selectedNode ? (
          <div>
            <p>
              <strong>名称：</strong>
              {selectedNode.name}
            </p>
            <p>
              <strong>类型：</strong>
              {selectedNode.type === '1'
                ? '科室'
                : selectedNode.type === '2'
                  ? '班组'
                  : selectedNode.type === '3'
                    ? '队组'
                    : '其他'}
            </p>
            <p>
              <strong>排序：</strong>
              {selectedNode.sort ?? '-'}
            </p>
            <Space style={{ marginTop: 16 }}>
              <Button
                icon={<PlusOutlined />}
                onClick={() => handleAdd(selectedNode.id)}
              >
                添加子组织
              </Button>
              <Button icon={<EditOutlined />} onClick={handleEdit}>
                编辑
              </Button>
              <Popconfirm title="确定删除此组织？" onConfirm={handleDelete}>
                <Button danger icon={<DeleteOutlined />}>
                  删除
                </Button>
              </Popconfirm>
            </Space>
          </div>
        ) : (
          <div style={{ textAlign: 'center', color: '#999', padding: 40 }}>
            请在左侧选择一个组织查看详情
          </div>
        )}
      </Card>

      <Modal
        title={editingOrg ? '编辑组织' : '新建组织'}
        open={modalOpen}
        onOk={handleOk}
        onCancel={() => {
          setModalOpen(false);
          form.resetFields();
          setEditingOrg(null);
        }}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="parentid" hidden>
            <Input />
          </Form.Item>
          <Form.Item
            name="name"
            label="组织名称"
            rules={[{ required: true, message: '请输入组织名称' }]}
          >
            <Input placeholder="请输入组织名称" />
          </Form.Item>
          <Form.Item name="type" label="类型" initialValue="0">
            <Select>
              <Select.Option value="0">其他</Select.Option>
              <Select.Option value="1">科室</Select.Option>
              <Select.Option value="2">班组</Select.Option>
              <Select.Option value="3">队组</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item name="sort" label="排序" initialValue={1}>
            <InputNumber min={1} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="comments" label="备注">
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default OrganizationPage;
