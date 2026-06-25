import React, { useRef, useState, useEffect } from 'react';
import { ProTable, type ActionType, type ProColumns } from '@ant-design/pro-components';
import { Button, message, Modal, Form, Input, InputNumber, Popconfirm, Space, Select, Tag } from 'antd';
import { DeleteOutlined, PlusOutlined, ReloadOutlined, MinusCircleOutlined } from '@ant-design/icons';
import {
  getPapers, createPaper, updatePaper, deletePaper, batchDeletePapers,
  getPaperSubjects, addPaperSubject, getSubjects,
} from '@/services/exam';

const TIPTYPE_LABELS: Record<string, string> = {
  '1': '填空', '2': '单选', '3': '多选', '4': '判断', '5': '问答', '6': '附件',
};

const PaperPage: React.FC = () => {
  const actionRef = useRef<ActionType>();
  const [modalOpen, setModalOpen] = useState(false);
  const [editingPaper, setEditingPaper] = useState<any>(null);
  const [form] = Form.useForm();
  const [subjectModalOpen, setSubjectModalOpen] = useState(false);
  const [managingPaper, setManagingPaper] = useState<any>(null);
  const [paperSubjects, setPaperSubjects] = useState<any[]>([]);
  const [allSubjects, setAllSubjects] = useState<any[]>([]);
  const [addingSubjectIds, setAddingSubjectIds] = useState<string[]>([]);
  const [addingPoint, setAddingPoint] = useState<number>(1);
  const [batchAdding, setBatchAdding] = useState(false);
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  const [batchDeleting, setBatchDeleting] = useState(false);

  // Auto-set point from first selected subject's default
  useEffect(() => {
    if (addingSubjectIds.length > 0) {
      const firstSubject = allSubjects.find((s: any) => s.id === addingSubjectIds[0]);
      if (firstSubject?.point) {
        setAddingPoint(firstSubject.point);
      }
    }
  }, [addingSubjectIds]);

  const columns: ProColumns[] = [
    {
      title: '试卷名称',
      dataIndex: 'name',
      width: 250,
    },
    {
      title: '题目数',
      dataIndex: 'subjectnum',
      width: 80,
      hideInSearch: true,
    },
    {
      title: '总分',
      dataIndex: 'pointnum',
      width: 80,
      hideInSearch: true,
    },
    {
      title: '建议时间(分)',
      dataIndex: 'advicetime',
      width: 110,
      hideInSearch: true,
    },
    {
      title: '答题人数',
      dataIndex: 'completetnum',
      width: 90,
      hideInSearch: true,
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
      title: '操作',
      valueType: 'option',
      width: 180,
      render: (_, record) => (
        <Space>
          <a
            onClick={() => {
              setEditingPaper(record);
              form.setFieldsValue(record);
              setModalOpen(true);
            }}
          >
            编辑
          </a>
          <a
            onClick={async () => {
              setManagingPaper(record);
              try {
                const [psRes, subRes]: any[] = await Promise.all([
                  getPaperSubjects(record.id),
                  getSubjects({ page: 1, size: 200 }),
                ]);
                setPaperSubjects(psRes.data || []);
                setAllSubjects(subRes.data?.records || []);
                setSubjectModalOpen(true);
              } catch {
                message.error('加载数据失败');
              }
            }}
          >
            管理题目
          </a>
          <Popconfirm
            title="确定删除此试卷？"
            onConfirm={async () => {
              try {
                await deletePaper(record.id);
                message.success('已删除');
                actionRef.current?.reload();
              } catch {
                message.error('删除失败，试卷可能正在使用中');
              }
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
    try {
      if (editingPaper) {
        await updatePaper(editingPaper.id, values);
        message.success('更新成功');
      } else {
        await createPaper(values);
        message.success('创建成功');
      }
      setModalOpen(false);
      form.resetFields();
      setEditingPaper(null);
      actionRef.current?.reload();
    } catch {
      message.error('操作失败');
    }
  };

  const handleBatchDelete = () => {
    const ids = selectedRowKeys.map(String);
    if (ids.length === 0) {
      message.warning('请先选择试卷');
      return;
    }
    Modal.confirm({
      title: `确定删除选中的 ${ids.length} 份试卷？`,
      content: '删除试卷会同时移除试卷下的章节和题目关联。',
      okText: '删除',
      cancelText: '取消',
      okButtonProps: { danger: true },
      onOk: async () => {
        setBatchDeleting(true);
        try {
          await batchDeletePapers(ids);
          message.success('批量删除成功');
          setSelectedRowKeys([]);
          actionRef.current?.reload();
        } catch {
          message.error('批量删除失败，部分试卷可能正在使用中');
        } finally {
          setBatchDeleting(false);
        }
      },
    });
  };

  return (
    <>
      <ProTable
        headerTitle="试卷管理"
        actionRef={actionRef}
        rowKey="id"
        search={{ labelWidth: 80 }}
        toolBarRender={() => [
          <Button
            key="add"
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => {
              setEditingPaper(null);
              form.resetFields();
              setModalOpen(true);
            }}
          >
            新建试卷
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
        ]}
        request={async (params) => {
          const res: any = await getPapers({
            page: params.current,
            size: params.pageSize,
            keyword: params.name,
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
        title={editingPaper ? '编辑试卷' : '新建试卷'}
        open={modalOpen}
        onOk={handleOk}
        onCancel={() => {
          setModalOpen(false);
          form.resetFields();
          setEditingPaper(null);
        }}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="name"
            label="试卷名称"
            rules={[{ required: true, message: '请输入试卷名称' }]}
          >
            <Input placeholder="请输入试卷名称" />
          </Form.Item>
          <Form.Item name="advicetime" label="建议答题时间(分钟)" initialValue={60}>
            <InputNumber min={1} max={600} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="papernote" label="试卷说明">
            <Input.TextArea rows={3} placeholder="可选" />
          </Form.Item>
        </Form>
      </Modal>

      {/* Subject management modal */}
      <Modal
        title={`管理题目 — ${managingPaper?.name || ''}`}
        open={subjectModalOpen}
        onCancel={() => setSubjectModalOpen(false)}
        footer={null}
        width={720}
      >
        <div style={{ marginBottom: 16 }}>
          <div style={{ display: 'flex', gap: 8, alignItems: 'center', marginBottom: 8 }}>
            <Select
              mode="multiple"
              showSearch
              placeholder="选择题目（可多选）"
              style={{ flex: 1 }}
              value={addingSubjectIds}
              onChange={setAddingSubjectIds}
              optionFilterProp="label"
              maxTagCount="responsive"
              options={allSubjects
                .filter((s: any) => !paperSubjects.some((ps: any) => ps.subjectid === s.id))
                .map((s: any) => ({
                  label: `[${TIPTYPE_LABELS[s.tiptype] || '?'}] ${s.introduction?.substring(0, 40) || s.id} (${s.point || 1}分)`,
                  value: s.id,
                }))}
            />
            <InputNumber min={1} max={100} value={addingPoint} onChange={(v) => setAddingPoint(v || 1)} placeholder="分值" style={{ width: 80 }} />
            <Button
              type="primary"
              loading={batchAdding}
              onClick={async () => {
                if (addingSubjectIds.length === 0) { message.warning('请选择题目'); return; }
                setBatchAdding(true);
                let successCount = 0;
                for (const sid of addingSubjectIds) {
                  try {
                    await addPaperSubject(managingPaper.id, { subjectId: sid, point: addingPoint });
                    successCount++;
                  } catch { /* continue with next */ }
                }
                setBatchAdding(false);
                if (successCount > 0) {
                  message.success(`成功添加 ${successCount} 道题目`);
                }
                if (successCount < addingSubjectIds.length) {
                  message.warning(`${addingSubjectIds.length - successCount} 道添加失败`);
                }
                const res: any = await getPaperSubjects(managingPaper.id);
                setPaperSubjects(res.data || []);
                setAddingSubjectIds([]);
                actionRef.current?.reload();
              }}
            >
              批量添加
            </Button>
          </div>
          {addingSubjectIds.length > 0 && (
            <div style={{ color: '#1890ff', fontSize: 13 }}>
              已选 {addingSubjectIds.length} 道题，每题 {addingPoint} 分，共 {addingSubjectIds.length * addingPoint} 分
            </div>
          )}
        </div>
        <div style={{ color: '#999', marginBottom: 8 }}>
          当前共 {paperSubjects.length} 题，总分 {paperSubjects.reduce((sum: number, ps: any) => sum + (ps.point || 0), 0)} 分
        </div>
        {paperSubjects.map((ps: any, idx: number) => (
          <div key={ps.id} style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 0', borderBottom: '1px solid #f0f0f0' }}>
            <span>
              <Tag>{idx + 1}</Tag>
              {allSubjects.find((s: any) => s.id === ps.subjectid)?.introduction?.substring(0, 60) || ps.subjectid}
            </span>
            <span>
              <Tag color="blue">{ps.point || 0}分</Tag>
            </span>
          </div>
        ))}
      </Modal>
    </>
  );
};

export default PaperPage;
