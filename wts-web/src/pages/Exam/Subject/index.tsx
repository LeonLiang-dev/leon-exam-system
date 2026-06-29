import React, { useRef, useState, useEffect } from 'react';
import { ProTable, type ActionType, type ProColumns } from '@ant-design/pro-components';
import {
  Button, message, Modal, Form, Input, Select, Popconfirm, Space, InputNumber, Card, TreeSelect, Upload,
} from 'antd';
import {
  DeleteOutlined,
  PlusOutlined,
  ReloadOutlined,
  MinusCircleOutlined,
  UploadOutlined,
  DownloadOutlined,
} from '@ant-design/icons';
import {
  getSubjects,
  getSubject,
  createSubject,
  updateSubject,
  deleteSubject,
  batchDeleteSubjects,
  getSubjectTypeTree,
  importSubjects,
  exportSubjects,
} from '@/services/exam';

const TIPTYPE_OPTIONS = [
  { value: '1', label: '填空题' },
  { value: '2', label: '单选题' },
  { value: '3', label: '多选题' },
  { value: '4', label: '判断题' },
  { value: '5', label: '问答题' },
  { value: '6', label: '附件题' },
];

const SubjectPage: React.FC = () => {
  const actionRef = useRef<ActionType>();
  const [modalOpen, setModalOpen] = useState(false);
  const [editingSubject, setEditingSubject] = useState<any>(null);
  const [form] = Form.useForm();
  const [typeTree, setTypeTree] = useState<any[]>([]);
  const [selectedTiptype, setSelectedTiptype] = useState<string>('2');
  const [importModalOpen, setImportModalOpen] = useState(false);
  const [importTypeid, setImportTypeid] = useState<string>('');
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  const [batchDeleting, setBatchDeleting] = useState(false);

  useEffect(() => {
    getSubjectTypeTree().then((res: any) => {
      setTypeTree(res.data || []);
    });
  }, []);

  // Convert tree data for TreeSelect
  const buildTreeData = (nodes: any[]): any[] =>
    nodes.map((t: any) => ({
      title: t.name,
      value: t.id,
      children: t.children?.length ? buildTreeData(t.children) : undefined,
    }));

  const columns: ProColumns[] = [
    {
      title: '题目内容',
      dataIndex: 'introduction',
      ellipsis: true,
      width: 300,
    },
    {
      title: '状态',
      dataIndex: 'pstate',
      width: 80,
      valueEnum: {
        '1': { text: '正常', status: 'Success' },
        '0': { text: '已删除', status: 'Error' },
      },
    },
    {
      title: '难度',
      dataIndex: 'level',
      width: 80,
      hideInSearch: true,
      valueEnum: {
        '1': { text: '简单' },
        '2': { text: '中等' },
        '3': { text: '困难' },
      },
    },
    {
      title: '使用次数',
      dataIndex: 'donum',
      width: 90,
      hideInSearch: true,
    },
    {
      title: '默认分值',
      dataIndex: 'point',
      width: 90,
      hideInSearch: true,
      render: (_, record) => record.point || 1,
    },
    {
      title: '操作',
      valueType: 'option',
      width: 180,
      render: (_, record) => (
        <Space>
          <a
            onClick={async () => {
              try {
                const res: any = await getSubject(record.id);
                const { subject, version, answers } = res.data;
                setEditingSubject(record);
                setSelectedTiptype(version?.tiptype || '2');
                form.setFieldsValue({
                  typeid: subject.typeid,
                  tipstr: version?.tipstr || subject.introduction,
                  tipnote: version?.tipnote,
                  level: subject.level,
                  point: subject.point || 1,
                  answers: (answers || []).map((a: any) => ({
                    answer: a.answer,
                    rightanswer: a.rightanswer || '0',
                    pointweight: a.pointweight,
                    answernote: a.answernote,
                  })),
                });
                setModalOpen(true);
              } catch {
                message.error('加载题目详情失败');
              }
            }}
          >
            编辑
          </a>
          <Popconfirm
            title="确定删除此题目？"
            onConfirm={async () => {
              await deleteSubject(record.id);
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
    const dto = {
      typeid: values.typeid,
      tiptype: selectedTiptype,
      tipstr: values.tipstr,
      tipnote: values.tipnote,
      pcontent: values.pcontent,
      level: values.level,
      point: values.point,
      answers: (values.answers || []).map((a: any, idx: number) => ({
        answer: a.answer,
        answernote: a.answernote,
        rightanswer: a.rightanswer || '0',
        sort: idx + 1,
        pointweight: a.pointweight,
      })),
    };
    try {
      if (editingSubject) {
        await updateSubject(editingSubject.id, dto);
        message.success('更新成功');
      } else {
        await createSubject(dto);
        message.success('创建成功');
      }
      setModalOpen(false);
      form.resetFields();
      setEditingSubject(null);
      actionRef.current?.reload();
    } catch {
      message.error('操作失败');
    }
  };

  const handleBatchDelete = () => {
    const ids = selectedRowKeys.map(String);
    if (ids.length === 0) {
      message.warning('请先选择题目');
      return;
    }
    Modal.confirm({
      title: `确定删除选中的 ${ids.length} 道题目？`,
      content: '删除后题目会从题库中移除。',
      okText: '删除',
      cancelText: '取消',
      okButtonProps: { danger: true },
      onOk: async () => {
        setBatchDeleting(true);
        try {
          await batchDeleteSubjects(ids);
          message.success('批量删除成功');
          setSelectedRowKeys([]);
          actionRef.current?.reload();
        } finally {
          setBatchDeleting(false);
        }
      },
    });
  };

  const showAnswers = ['2', '3', '4', '1'].includes(selectedTiptype);

  return (
    <>
      <ProTable
        headerTitle="题目管理"
        actionRef={actionRef}
        rowKey="id"
        search={{ labelWidth: 80 }}
        toolBarRender={() => [
          <Button
            key="add"
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => {
              setEditingSubject(null);
              form.resetFields();
              setSelectedTiptype('2');
              setModalOpen(true);
            }}
          >
            新建题目
          </Button>,
          <Button
            key="reload"
            icon={<ReloadOutlined />}
            onClick={() => actionRef.current?.reload()}
          >
            刷新
          </Button>,
          <Button
            key="batch-delete"
            danger
            icon={<DeleteOutlined />}
            disabled={selectedRowKeys.length === 0}
            loading={batchDeleting}
            onClick={handleBatchDelete}
          >
            批量删除
          </Button>,
          <Button
            key="import"
            icon={<UploadOutlined />}
            onClick={() => setImportModalOpen(true)}
          >
            导入题目
          </Button>,
          <Button
            key="export"
            icon={<DownloadOutlined />}
            onClick={async () => {
              try {
                const res: any = await exportSubjects();
                const url = window.URL.createObjectURL(res);
                const a = document.createElement('a');
                a.href = url;
                a.download = '题目导出.xlsx';
                a.click();
                window.URL.revokeObjectURL(url);
                message.success('导出成功');
              } catch {
                message.error('导出失败');
              }
            }}
          >
            导出题目
          </Button>,
        ]}
        request={async (params) => {
          const res: any = await getSubjects({
            page: params.current,
            size: params.pageSize,
            keyword: params.introduction,
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
          onChange: setSelectedRowKeys,
        }}
        columns={columns}
      />

      <Modal
        title={editingSubject ? '编辑题目' : '新建题目'}
        open={modalOpen}
        onOk={handleOk}
        onCancel={() => {
          setModalOpen(false);
          form.resetFields();
          setEditingSubject(null);
        }}
        width={720}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="typeid" label="题目分类">
            <TreeSelect
              placeholder="请选择分类"
              treeData={buildTreeData(typeTree)}
              allowClear
            />
          </Form.Item>
          <Form.Item label="题型" required>
            <Select value={selectedTiptype} onChange={setSelectedTiptype}>
              {TIPTYPE_OPTIONS.map((o) => (
                <Select.Option key={o.value} value={o.value}>{o.label}</Select.Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item
            name="tipstr"
            label="题目内容"
            rules={[{ required: true, message: '请输入题目内容' }]}
          >
            <Input.TextArea rows={3} placeholder="请输入题目内容" />
          </Form.Item>
          <Form.Item name="tipnote" label="题目说明">
            <Input.TextArea rows={2} placeholder="可选" />
          </Form.Item>
          <Form.Item name="level" label="难度" initialValue={1}>
            <Select>
              <Select.Option value={1}>简单</Select.Option>
              <Select.Option value={2}>中等</Select.Option>
              <Select.Option value={3}>困难</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item name="point" label="默认分值" initialValue={1}>
            <InputNumber min={1} max={100} style={{ width: '100%' }} />
          </Form.Item>

          {showAnswers && (
            <Card title="答案选项" size="small" style={{ marginBottom: 16 }}>
              <Form.List name="answers">
                {(fields, { add, remove }) => (
                  <>
                    {fields.map(({ key, name, ...restField }) => (
                      <Space key={key} style={{ display: 'flex', marginBottom: 8 }} align="baseline">
                        <Form.Item
                          {...restField}
                          name={[name, 'answer']}
                          rules={[{ required: true, message: '选项内容' }]}
                        >
                          <Input placeholder="选项内容" style={{ width: 280 }} />
                        </Form.Item>
                        <Form.Item
                          {...restField}
                          name={[name, 'rightanswer']}
                          initialValue="0"
                        >
                          <Select style={{ width: 100 }}>
                            <Select.Option value="0">错误</Select.Option>
                            <Select.Option value="1">正确</Select.Option>
                          </Select>
                        </Form.Item>
                        <MinusCircleOutlined onClick={() => remove(name)} />
                      </Space>
                    ))}
                    <Button type="dashed" onClick={() => add()} block icon={<PlusOutlined />}>
                      添加选项
                    </Button>
                  </>
                )}
              </Form.List>
            </Card>
          )}
        </Form>
      </Modal>

      <Modal
        title="批量导入题目"
        open={importModalOpen}
        onCancel={() => setImportModalOpen(false)}
        footer={null}
        width={480}
      >
        <div style={{ marginBottom: 16 }}>
          <p style={{ marginBottom: 8 }}>选择题目分类：</p>
          <TreeSelect
            style={{ width: '100%' }}
            placeholder="请选择分类"
            value={importTypeid || undefined}
            onChange={(val) => setImportTypeid(val)}
            treeData={buildTreeData(typeTree)}
            allowClear
          />
        </div>
        <div style={{ marginBottom: 16 }}>
          <p style={{ marginBottom: 8 }}>上传Excel文件（.xlsx）：</p>
          <p style={{ color: '#999', fontSize: 12 }}>
            Excel模板包含5个Sheet：选择题、判断题、填空题、问答题、附件题。
            每行格式：TYPE | TEXT | RIGHT1~6 | RIGHT | ...
          </p>
        </div>
        <Upload
          accept=".xlsx,.xls"
          showUploadList={false}
          customRequest={async ({ file }) => {
            if (!importTypeid) {
              message.warning('请先选择题目分类');
              return;
            }
            try {
              const res: any = await importSubjects(file as File, importTypeid);
              const data = res.data;
              message.success(`导入完成：共${data.total}题，成功${data.success}题`);
              if (data.errors?.length > 0) {
                Modal.warning({
                  title: '导入警告',
                  content: data.errors.join('\n'),
                  width: 600,
                });
              }
              setImportModalOpen(false);
              actionRef.current?.reload();
            } catch {
              message.error('导入失败');
            }
          }}
        >
          <Button icon={<UploadOutlined />} type="primary" block>选择文件并导入</Button>
        </Upload>
      </Modal>
    </>
  );
};

export default SubjectPage;
