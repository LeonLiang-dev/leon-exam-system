import React, { useRef, useState, useEffect } from 'react';
import { ProTable, type ActionType, type ProColumns } from '@ant-design/pro-components';
import {
  App, Button, Modal, Form, Input, Select, Popconfirm, Space, InputNumber, Card, TreeSelect, Upload, Image,
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
  { value: '5', label: '主观题' },
];

const FILL_BLANK_TIPTYPE = '1';
const SINGLE_CHOICE_TIPTYPE = '2';
const MULTIPLE_CHOICE_TIPTYPE = '3';
const TRUE_FALSE_TIPTYPE = '4';
const SUBJECTIVE_TIPTYPE = '5';
const CHOICE_TIPTYPES = new Set([SINGLE_CHOICE_TIPTYPE, MULTIPLE_CHOICE_TIPTYPE]);

const compressImageToDataUrl = (file: File): Promise<string> => new Promise((resolve, reject) => {
  if (!/^image\/(jpeg|jpg|png|webp)$/i.test(file.type)) {
    reject(new Error('仅支持 JPG、JPEG、PNG、WEBP 图片'));
    return;
  }
  const reader = new FileReader();
  reader.onerror = () => reject(new Error('读取图片失败'));
  reader.onload = () => {
    const img = new window.Image();
    img.onerror = () => reject(new Error('图片解析失败'));
    img.onload = () => {
      const scale = Math.min(1, 1400 / img.width, 1400 / img.height);
      const canvas = document.createElement('canvas');
      canvas.width = Math.max(1, Math.round(img.width * scale));
      canvas.height = Math.max(1, Math.round(img.height * scale));
      const ctx = canvas.getContext('2d');
      if (!ctx) {
        reject(new Error('浏览器不支持图片处理'));
        return;
      }
      ctx.fillStyle = '#fff';
      ctx.fillRect(0, 0, canvas.width, canvas.height);
      ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
      resolve(canvas.toDataURL('image/jpeg', 0.82));
    };
    img.src = String(reader.result || '');
  };
  reader.readAsDataURL(file);
});

const defaultAnswersForType = (tiptype: string) => {
  if (tiptype === TRUE_FALSE_TIPTYPE) {
    return [
      { answer: '正确', rightanswer: '1' },
      { answer: '错误', rightanswer: '0' },
    ];
  }
  if (tiptype === FILL_BLANK_TIPTYPE) {
    return [{ answer: '', rightanswer: '1', pointweight: 100 }];
  }
  if (CHOICE_TIPTYPES.has(tiptype)) {
    return [
      { answer: '', rightanswer: '1' },
      { answer: '', rightanswer: '0' },
    ];
  }
  return [];
};


const countCorrectAnswers = (answers: any[] = []) =>
  answers.filter((answer) => answer?.rightanswer === '1').length;

const normalizeSingleChoiceAnswers = (answers: any[] = [], correctIndex?: number) => {
  if (answers.length === 0) {
    return answers;
  }
  const selectedIndex = typeof correctIndex === 'number'
    ? correctIndex
    : answers.findIndex((answer) => answer?.rightanswer === '1');
  if (selectedIndex < 0) {
    return answers;
  }
  return answers.map((answer, index) => ({
    ...answer,
    rightanswer: index === selectedIndex ? '1' : '0',
  }));
};

const SubjectPage: React.FC = () => {
  const { message } = App.useApp();
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
  const questionImage = Form.useWatch('pcontent', form);

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
                const tiptype = version?.tiptype || SINGLE_CHOICE_TIPTYPE;
                const answerValues = (answers || []).map((a: any) => ({
                  answer: a.answer,
                  rightanswer: a.rightanswer || '0',
                  pointweight: a.pointweight,
                  answernote: a.answernote,
                  pcontent: a.pcontent,
                  groupno: a.groupno,
                }));
                const judgeCorrect = answerValues.find((a: any) => a.rightanswer === '1')?.answer === '错误'
                  ? 'false'
                  : 'true';
                setEditingSubject(record);
                setSelectedTiptype(tiptype);
                form.setFieldsValue({
                  typeid: subject.typeid,
                  tipstr: version?.tipstr || subject.introduction,
                  tipnote: version?.tipnote,
                  pcontent: version?.pcontent,
                  level: subject.level,
                  point: subject.point || 1,
                  judgeAnswer: judgeCorrect,
                  answers: tiptype === SINGLE_CHOICE_TIPTYPE
                    ? normalizeSingleChoiceAnswers(answerValues)
                    : (answerValues.length > 0 ? answerValues : defaultAnswersForType(tiptype)),
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

  const handleTiptypeChange = (value: string) => {
    setSelectedTiptype(value);
    if (value === TRUE_FALSE_TIPTYPE) {
      form.setFieldsValue({ answers: defaultAnswersForType(value), judgeAnswer: 'true' });
      return;
    }
    if (value === SUBJECTIVE_TIPTYPE) {
      form.setFieldsValue({ answers: [] });
      return;
    }
    if (value === FILL_BLANK_TIPTYPE) {
      form.setFieldsValue({ answers: defaultAnswersForType(value) });
      return;
    }
    if (CHOICE_TIPTYPES.has(value)) {
      const answers = form.getFieldValue('answers') || [];
      const nextAnswers = answers.length >= 2 ? answers : defaultAnswersForType(value);
      form.setFieldsValue({
        answers: value === SINGLE_CHOICE_TIPTYPE
          ? normalizeSingleChoiceAnswers(nextAnswers)
          : nextAnswers,
      });
    }
  };

  const markSingleChoiceAnswer = (answerIndex: number) => {
    const answers = form.getFieldValue('answers') || [];
    form.setFieldsValue({
      answers: normalizeSingleChoiceAnswers(answers, answerIndex),
    });
  };

  const handleOk = async () => {
    const values = await form.validateFields();
    let answers: any[] = [];
    if (CHOICE_TIPTYPES.has(selectedTiptype)) {
      answers = values.answers || [];
      if (answers.length < 2 || answers.length > 10) {
        message.error('选择题必须设置2到10个选项');
        return;
      }
      if (answers.some((answer) => !answer?.answer?.trim())) {
        message.error('每个选项内容不能为空');
        return;
      }
      const correctCount = countCorrectAnswers(answers);
      if (selectedTiptype === SINGLE_CHOICE_TIPTYPE && correctCount !== 1) {
        message.error('单选题必须且只能设置一个正确答案');
        return;
      }
      if (selectedTiptype === MULTIPLE_CHOICE_TIPTYPE && correctCount < 2) {
        message.error('多选题必须设置至少两个正确答案');
        return;
      }
      if (selectedTiptype === MULTIPLE_CHOICE_TIPTYPE && correctCount === answers.length) {
        message.error('多选题至少需要一个错误选项');
        return;
      }
    } else if (selectedTiptype === TRUE_FALSE_TIPTYPE) {
      answers = [
        { answer: '正确', rightanswer: values.judgeAnswer === 'true' ? '1' : '0' },
        { answer: '错误', rightanswer: values.judgeAnswer === 'false' ? '1' : '0' },
      ];
    } else if (selectedTiptype === FILL_BLANK_TIPTYPE) {
      answers = values.answers || [];
      if (answers.length === 0 || answers.some((answer) => !answer?.answer?.trim())) {
        message.error('填空题每个空都必须设置标准答案');
        return;
      }
    }

    const dto = {
      typeid: values.typeid,
      tiptype: selectedTiptype,
      tipstr: values.tipstr,
      tipnote: values.tipnote,
      pcontent: values.pcontent,
      level: values.level,
      point: values.point,
      answers: answers.map((a: any, idx: number) => ({
        answer: a.answer || '',
        answernote: a.answernote,
        rightanswer: selectedTiptype === FILL_BLANK_TIPTYPE ? '1' : (a.rightanswer || '0'),
        sort: idx + 1,
        pointweight: a.pointweight,
        groupno: selectedTiptype === FILL_BLANK_TIPTYPE ? idx + 1 : a.groupno,
        pcontent: a.pcontent,
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

  const showAnswers = CHOICE_TIPTYPES.has(selectedTiptype) || selectedTiptype === FILL_BLANK_TIPTYPE;

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
              setSelectedTiptype(SINGLE_CHOICE_TIPTYPE);
              form.setFieldsValue({ point: 1, level: 1, answers: defaultAnswersForType(SINGLE_CHOICE_TIPTYPE) });
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
            <Select value={selectedTiptype} onChange={handleTiptypeChange}>
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
          <Form.Item name="pcontent" hidden>
            <Input />
          </Form.Item>
          <Form.Item label="题干配图">
            <Space direction="vertical" style={{ width: '100%' }}>
              {questionImage && (
                <Image
                  src={questionImage}
                  alt="题干配图"
                  style={{ maxWidth: 240, maxHeight: 160, objectFit: 'contain', borderRadius: 4 }}
                />
              )}
              <Space>
                <Upload
                  accept=".jpg,.jpeg,.png,.webp,image/jpeg,image/png,image/webp"
                  showUploadList={false}
                  beforeUpload={async (file) => {
                    try {
                      const dataUrl = await compressImageToDataUrl(file as File);
                      form.setFieldValue('pcontent', dataUrl);
                      message.success('题干配图已添加');
                    } catch (error: any) {
                      message.error(error?.message || '图片处理失败');
                    }
                    return false;
                  }}
                >
                  <Button icon={<UploadOutlined />}>上传图片</Button>
                </Upload>
                {questionImage && (
                  <Button danger onClick={() => form.setFieldValue('pcontent', undefined)}>
                    删除图片
                  </Button>
                )}
              </Space>
            </Space>
          </Form.Item>
          <Form.Item
            name="tipnote"
            label={selectedTiptype === SUBJECTIVE_TIPTYPE ? '评分标准 / 示例答案' : '答案解析'}
          >
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

          {selectedTiptype === TRUE_FALSE_TIPTYPE && (
            <Card title="判断题答案" size="small" style={{ marginBottom: 16 }}>
              <Form.Item name="judgeAnswer" initialValue="true" rules={[{ required: true, message: '请选择正确答案' }]}>
                <Select>
                  <Select.Option value="true">正确</Select.Option>
                  <Select.Option value="false">错误</Select.Option>
                </Select>
              </Form.Item>
            </Card>
          )}

          {showAnswers && (
            <Card title={selectedTiptype === FILL_BLANK_TIPTYPE ? '填空标准答案' : '答案选项'} size="small" style={{ marginBottom: 16 }}>
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
                          <Input
                            placeholder={selectedTiptype === FILL_BLANK_TIPTYPE ? '标准答案，多个答案用 | 分隔' : '选项内容'}
                            style={{ width: selectedTiptype === FILL_BLANK_TIPTYPE ? 360 : 280 }}
                          />
                        </Form.Item>
                        {selectedTiptype === FILL_BLANK_TIPTYPE ? (
                          <Form.Item
                            {...restField}
                            name={[name, 'pointweight']}
                            initialValue={100}
                          >
                            <InputNumber min={1} max={1000} placeholder="权重" style={{ width: 90 }} />
                          </Form.Item>
                        ) : (
                          <Form.Item
                            {...restField}
                            name={[name, 'rightanswer']}
                            initialValue="0"
                          >
                            <Select
                              style={{ width: 100 }}
                              onChange={(value) => {
                                if (selectedTiptype === SINGLE_CHOICE_TIPTYPE && value === '1') {
                                  markSingleChoiceAnswer(name);
                                }
                              }}
                            >
                              <Select.Option value="0">错误</Select.Option>
                              <Select.Option value="1">正确</Select.Option>
                            </Select>
                          </Form.Item>
                        )}
                        <MinusCircleOutlined onClick={() => remove(name)} />
                      </Space>
                    ))}
                    <Button
                      type="dashed"
                      onClick={() => add(selectedTiptype === FILL_BLANK_TIPTYPE
                        ? { answer: '', rightanswer: '1', pointweight: 100 }
                        : { answer: '', rightanswer: '0' })}
                      block
                      icon={<PlusOutlined />}
                      disabled={selectedTiptype !== FILL_BLANK_TIPTYPE && fields.length >= 10}
                    >
                      {selectedTiptype === FILL_BLANK_TIPTYPE ? '添加空' : '添加选项'}
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
            Excel模板包含4个Sheet：选择题、判断题、填空题、主观题。
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
