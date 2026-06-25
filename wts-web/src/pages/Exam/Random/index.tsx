import React, { useRef, useState, useEffect } from 'react';
import { ProTable, type ActionType, type ProColumns } from '@ant-design/pro-components';
import {
  Button, message, Modal, Form, Input, InputNumber, Select, Popconfirm, Space, Card, Row, Col, Table,
} from 'antd';
import { PlusOutlined, ReloadOutlined, ThunderboltOutlined, DeleteOutlined } from '@ant-design/icons';
import {
  getRandomItems, createRandomItem, deleteRandomItem, batchDeleteRandomItems,
  getRandomSteps, addRandomStep, deleteRandomStep, batchDeleteRandomSteps, generatePapers,
} from '@/services/exam';

const TIPTYPE_OPTIONS = [
  { value: '1', label: '填空题' },
  { value: '2', label: '单选题' },
  { value: '3', label: '多选题' },
  { value: '4', label: '判断题' },
  { value: '5', label: '问答题' },
  { value: '6', label: '附件题' },
];

const RandomPage: React.FC = () => {
  const actionRef = useRef<ActionType>();
  const [modalOpen, setModalOpen] = useState(false);
  const [stepModalOpen, setStepModalOpen] = useState(false);
  const [generateModalOpen, setGenerateModalOpen] = useState(false);
  const [form] = Form.useForm();
  const [stepForm] = Form.useForm();
  const [selectedItem, setSelectedItem] = useState<any>(null);
  const [steps, setSteps] = useState<any[]>([]);
  const [generateCount, setGenerateCount] = useState(1);
  const [selectedItemKeys, setSelectedItemKeys] = useState<React.Key[]>([]);
  const [selectedStepKeys, setSelectedStepKeys] = useState<React.Key[]>([]);
  const [batchDeletingItems, setBatchDeletingItems] = useState(false);
  const [batchDeletingSteps, setBatchDeletingSteps] = useState(false);

  const loadSteps = async (itemId: string) => {
    try {
      const res: any = await getRandomSteps(itemId);
      setSteps(res.data || []);
      setSelectedStepKeys([]);
    } catch {
      setSteps([]);
    }
  };

  useEffect(() => {
    if (selectedItem) {
      loadSteps(selectedItem.id);
    }
  }, [selectedItem]);

  const columns: ProColumns[] = [
    {
      title: '规则名称',
      dataIndex: 'name',
      width: 250,
    },
    {
      title: '操作',
      valueType: 'option',
      width: 250,
      render: (_, record) => (
        <Space>
          <a onClick={() => { setSelectedItem(record); setSelectedStepKeys([]); }}>查看步骤</a>
          <a
            style={{ color: '#52c41a' }}
            onClick={() => { setSelectedItem(record); setGenerateModalOpen(true); }}
          >
            生成试卷
          </a>
          <Popconfirm
            title="确定删除此规则？"
            onConfirm={async () => {
              await deleteRandomItem(record.id);
              message.success('已删除');
              actionRef.current?.reload();
              if (selectedItem?.id === record.id) {
                setSelectedItem(null);
                setSteps([]);
              }
            }}
          >
            <a style={{ color: '#ff4d4f' }}>删除</a>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const stepColumns = [
    { title: '步骤名称', dataIndex: 'name', width: 120 },
    { title: '题型', dataIndex: 'tiptype', width: 80,
      render: (val: string) => TIPTYPE_OPTIONS.find(o => o.value === val)?.label || val },
    { title: '题目数', dataIndex: 'subnum', width: 80 },
    { title: '每题分值', dataIndex: 'subpoint', width: 80 },
    { title: '分类ID', dataIndex: 'typeid', width: 120, ellipsis: true },
    {
      title: '操作', width: 80,
      render: (_: any, record: any) => (
        <Popconfirm
          title="确定删除？"
          onConfirm={async () => {
            await deleteRandomStep(record.id);
            message.success('已删除');
            loadSteps(selectedItem.id);
          }}
        >
          <a style={{ color: '#ff4d4f' }}><DeleteOutlined /></a>
        </Popconfirm>
      ),
    },
  ];

  const handleCreateItem = async () => {
    try {
      const values = await form.validateFields();
      await createRandomItem(values);
      message.success('创建成功');
      setModalOpen(false);
      form.resetFields();
      actionRef.current?.reload();
    } catch {
      message.error('创建失败');
    }
  };

  const handleAddStep = async () => {
    if (!selectedItem) return;
    try {
      const values = await stepForm.validateFields();
      await addRandomStep(selectedItem.id, values);
      message.success('添加成功');
      setStepModalOpen(false);
      stepForm.resetFields();
      loadSteps(selectedItem.id);
    } catch {
      message.error('添加失败');
    }
  };

  const handleGenerate = async () => {
    try {
      const res: any = await generatePapers(selectedItem.id, generateCount);
      message.success(`成功生成 ${res.data?.length || 0} 份试卷`);
      setGenerateModalOpen(false);
    } catch {
      message.error('生成失败');
    }
  };

  const handleBatchDeleteItems = () => {
    const ids = selectedItemKeys.map(String);
    if (ids.length === 0) {
      message.warning('请先选择随机规则');
      return;
    }
    Modal.confirm({
      title: `确定删除选中的 ${ids.length} 个随机规则？`,
      content: '删除随机规则会同时删除其规则步骤。',
      okText: '删除',
      cancelText: '取消',
      okButtonProps: { danger: true },
      onOk: async () => {
        setBatchDeletingItems(true);
        try {
          await batchDeleteRandomItems(ids);
          message.success('批量删除成功');
          setSelectedItemKeys([]);
          if (selectedItem && ids.includes(selectedItem.id)) {
            setSelectedItem(null);
            setSteps([]);
          }
          actionRef.current?.reload();
        } finally {
          setBatchDeletingItems(false);
        }
      },
    });
  };

  const handleBatchDeleteSteps = () => {
    const ids = selectedStepKeys.map(String);
    if (!selectedItem) {
      message.warning('请先选择随机规则');
      return;
    }
    if (ids.length === 0) {
      message.warning('请先选择规则步骤');
      return;
    }
    Modal.confirm({
      title: `确定删除选中的 ${ids.length} 个规则步骤？`,
      content: '删除后该随机规则生成试卷时不会再使用这些步骤。',
      okText: '删除',
      cancelText: '取消',
      okButtonProps: { danger: true },
      onOk: async () => {
        setBatchDeletingSteps(true);
        try {
          await batchDeleteRandomSteps(ids);
          message.success('批量删除成功');
          loadSteps(selectedItem.id);
        } finally {
          setBatchDeletingSteps(false);
        }
      },
    });
  };

  return (
    <Row gutter={16}>
      <Col xs={24} md={10}>
        <Card title="随机规则" size="small">
          <ProTable
            actionRef={actionRef}
            rowKey="id"
            search={false}
            toolBarRender={() => [
              <Button
                key="batch-delete"
                danger
                icon={<DeleteOutlined />}
                disabled={selectedItemKeys.length === 0}
                loading={batchDeletingItems}
                onClick={handleBatchDeleteItems}
              >
                批量删除
              </Button>,
              <Button
                key="add"
                type="primary"
                icon={<PlusOutlined />}
                onClick={() => setModalOpen(true)}
              >
                新建规则
              </Button>,
            ]}
            request={async () => {
              const res: any = await getRandomItems();
              return { data: res.data || [], success: true };
            }}
            columns={columns}
            size="small"
            pagination={false}
            rowSelection={{
              selectedRowKeys: selectedItemKeys,
              onChange: setSelectedItemKeys,
            }}
            rowClassName={(record) => record.id === selectedItem?.id ? 'ant-table-row-selected' : ''}
          />
        </Card>
      </Col>
      <Col xs={24} md={14}>
        <Card
          title={selectedItem ? `${selectedItem.name} — 规则步骤` : '请从左侧选择规则'}
          size="small"
          extra={selectedItem && (
            <Space>
              <Button
                danger
                icon={<DeleteOutlined />}
                size="small"
                disabled={selectedStepKeys.length === 0}
                loading={batchDeletingSteps}
                onClick={handleBatchDeleteSteps}
              >
                批量删除
              </Button>
              <Button
                type="primary"
                icon={<PlusOutlined />}
                size="small"
                onClick={() => {
                  stepForm.resetFields();
                  stepForm.setFieldsValue({ sort: steps.length + 1, subnum: 5, subpoint: 2 });
                  setStepModalOpen(true);
                }}
              >
                添加步骤
              </Button>
            </Space>
          )}
        >
          <Table
            dataSource={steps}
            columns={stepColumns}
            rowKey="id"
            size="small"
            pagination={false}
            rowSelection={{
              selectedRowKeys: selectedStepKeys,
              onChange: setSelectedStepKeys,
            }}
          />
        </Card>
      </Col>

      {/* Create item modal */}
      <Modal
        title="新建随机规则"
        open={modalOpen}
        onOk={handleCreateItem}
        onCancel={() => { setModalOpen(false); form.resetFields(); }}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="规则名称" rules={[{ required: true }]}>
            <Input placeholder="如：期中考试A卷" />
          </Form.Item>
        </Form>
      </Modal>

      {/* Add step modal */}
      <Modal
        title="添加规则步骤"
        open={stepModalOpen}
        onOk={handleAddStep}
        onCancel={() => { setStepModalOpen(false); stepForm.resetFields(); }}
      >
        <Form form={stepForm} layout="vertical">
          <Form.Item name="name" label="步骤名称" rules={[{ required: true }]}>
            <Input placeholder="如：单选题部分" />
          </Form.Item>
          <Form.Item name="tiptype" label="题型" rules={[{ required: true }]}>
            <Select options={TIPTYPE_OPTIONS} />
          </Form.Item>
          <Form.Item name="subnum" label="题目数量" rules={[{ required: true }]}>
            <InputNumber min={1} max={100} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="subpoint" label="每题分值" rules={[{ required: true }]}>
            <InputNumber min={1} max={100} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="typeid" label="限定分类ID（可选）">
            <Input placeholder="留空表示不限分类" />
          </Form.Item>
          <Form.Item name="sort" label="排序">
            <InputNumber min={1} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>

      {/* Generate modal */}
      <Modal
        title="生成随机试卷"
        open={generateModalOpen}
        onOk={handleGenerate}
        onCancel={() => setGenerateModalOpen(false)}
      >
        <p>规则：<strong>{selectedItem?.name}</strong></p>
        <p>步骤数：{steps.length}，总题数：{steps.reduce((sum, s) => sum + (s.subnum || 0), 0)}</p>
        <Form.Item label="生成份数" style={{ marginTop: 16 }}>
          <InputNumber min={1} max={50} value={generateCount} onChange={(v) => setGenerateCount(v || 1)} style={{ width: '100%' }} />
        </Form.Item>
      </Modal>
    </Row>
  );
};

export default RandomPage;
