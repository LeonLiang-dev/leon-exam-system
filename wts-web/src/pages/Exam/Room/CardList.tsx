import React, { useRef, useState } from 'react';
import { useParams, history } from '@umijs/max';
import { ProTable, type ActionType, type ProColumns } from '@ant-design/pro-components';
import { Button, message, Modal, Tag, Space } from 'antd';
import { CheckCircleOutlined, ReloadOutlined, ArrowLeftOutlined } from '@ant-design/icons';
import { getRoomCards, batchJudgeCards } from '@/services/exam';

const PSTATE_MAP: Record<string, { text: string; color: string }> = {
  '11': { text: '答题中', color: 'blue' },
  '16': { text: '已提交', color: 'orange' },
  '21': { text: '已阅卷', color: 'green' },
};

const CardListPage: React.FC = () => {
  const { roomId } = useParams<{ roomId: string }>();
  const actionRef = useRef<ActionType>();
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  const [selectedRows, setSelectedRows] = useState<any[]>([]);
  const [batchJudging, setBatchJudging] = useState(false);

  const handleBatchJudge = () => {
    if (selectedRows.length === 0) {
      message.warning('请先选择答卷');
      return;
    }
    const invalidRows = selectedRows.filter((card) => card.pstate !== '16');
    if (invalidRows.length > 0) {
      message.warning('只能批量阅卷已提交的答卷');
      return;
    }
    const ids = selectedRows.map((card) => card.id);
    Modal.confirm({
      title: `确定批量阅卷选中的 ${ids.length} 份答卷？`,
      content: '批量阅卷会按当前自动得分结果标记为已阅卷。',
      okText: '阅卷',
      cancelText: '取消',
      onOk: async () => {
        setBatchJudging(true);
        try {
          await batchJudgeCards(ids);
          message.success('批量阅卷成功');
          setSelectedRowKeys([]);
          setSelectedRows([]);
          actionRef.current?.reload();
        } finally {
          setBatchJudging(false);
        }
      },
    });
  };

  const columns: ProColumns[] = [
    {
      title: '用户ID',
      dataIndex: 'userid',
      width: 200,
      ellipsis: true,
    },
    {
      title: '得分',
      dataIndex: 'point',
      width: 80,
      hideInSearch: true,
      render: (_, record) => (
        <span style={{ fontWeight: 600, color: record.point >= 60 ? '#52c41a' : '#ff4d4f' }}>
          {record.point || 0}
        </span>
      ),
    },
    {
      title: '答题数',
      width: 100,
      hideInSearch: true,
      render: (_, record) => `${record.completenum || 0}/${record.allnum || 0}`,
    },
    {
      title: '状态',
      dataIndex: 'pstate',
      width: 100,
      valueEnum: {
        '11': { text: '答题中' },
        '16': { text: '已提交' },
        '21': { text: '已阅卷' },
      },
      render: (_, record) => {
        const s = PSTATE_MAP[record.pstate] || { text: '未知', color: 'default' };
        return <Tag color={s.color}>{s.text}</Tag>;
      },
    },
    {
      title: '提交时间',
      dataIndex: 'submittime',
      width: 160,
      hideInSearch: true,
    },
    {
      title: '操作',
      valueType: 'option',
      width: 200,
      render: (_, record) => (
        <Space>
          <a onClick={() => history.push(`/exam/card/${record.id}/result`)}>查看成绩</a>
          {record.pstate === '16' && (
            <a
              style={{ color: '#faad14' }}
              onClick={() => history.push(`/exam/card/${record.id}/judge`)}
            >
              阅卷
            </a>
          )}
        </Space>
      ),
    },
  ];

  return (
    <>
      <div style={{ marginBottom: 16 }}>
        <Button icon={<ArrowLeftOutlined />} onClick={() => history.push('/exam/room')}>
          返回答题室列表
        </Button>
      </div>
      <ProTable
        headerTitle="答卷列表"
        actionRef={actionRef}
        rowKey="id"
        search={false}
        toolBarRender={() => [
          <Button
            key="batch-judge"
            icon={<CheckCircleOutlined />}
            disabled={selectedRowKeys.length === 0}
            loading={batchJudging}
            onClick={handleBatchJudge}
          >
            批量阅卷
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
          const res: any = await getRoomCards(roomId!, {
            page: params.current,
            size: params.pageSize,
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
    </>
  );
};

export default CardListPage;
