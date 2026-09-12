import React, { useEffect, useState } from 'react';
import { App, Card, Button, Tag, Empty, Spin, Row, Col, Typography, Tooltip } from 'antd';
import { history } from '@umijs/max';
import { ClockCircleOutlined, EyeOutlined, FileTextOutlined, PlayCircleOutlined, ScheduleOutlined } from '@ant-design/icons';
import { enterRoom, getMyRooms } from '@/services/exam';
import { formatExamDateTime, getRequestErrorMessage, parseExamDateTime } from '@/utils/examTime';

const { Title, Text } = Typography;

const ROOM_STATE_MAP: Record<string, { text: string; color: string }> = {
  open: { text: '进行中', color: 'green' },
  pending: { text: '未开始', color: 'gold' },
  ended: { text: '已结束', color: 'default' },
  closed: { text: '已关闭', color: 'red' },
  submitted: { text: '已提交', color: 'blue' },
};

interface Room {
  id: string;
  name: string;
  timelen: number;
  pstate: string;
  starttime?: string;
  endtime?: string;
  myCardId?: string;
  myCardPstate?: string;
  resultAvailable?: boolean;
  resultUnavailableReason?: string;
  paperNames?: string;
}

const getRoomWindow = (room: Room) => {
  if (room.myCardId) return { state: 'submitted', reason: room.resultUnavailableReason || '成绩暂不可查看' };
  if (room.pstate === '31') return { state: 'closed', reason: '答题室已关闭' };

  const now = Date.now();
  const start = parseExamDateTime(room.starttime);
  const end = parseExamDateTime(room.endtime);
  if (start && now < start.getTime()) return { state: 'pending', reason: '答题室未开始' };
  if (end && now > end.getTime()) return { state: 'ended', reason: '答题室已结束' };
  return { state: 'open', reason: '' };
};

const MyExamsPage: React.FC = () => {
  const { message } = App.useApp();
  const [loading, setLoading] = useState(true);
  const [rooms, setRooms] = useState<Room[]>([]);
  const [entering, setEntering] = useState<string | null>(null);

  useEffect(() => {
    loadRooms();
  }, []);

  const loadRooms = async () => {
    try {
      const res: any = await getMyRooms({ page: 1, size: 50, pstate: '21,31' });
      setRooms(res.data?.records || []);
    } catch {
      message.error('加载考试列表失败');
    } finally {
      setLoading(false);
    }
  };

  const handleEnter = async (room: Room) => {
    const windowInfo = getRoomWindow(room);
    if (windowInfo.state !== 'open') {
      message.warning(windowInfo.reason || '当前不可进入考试');
      return;
    }

    setEntering(room.id);
    try {
      const res: any = await enterRoom(room.id);
      const cardId = res.data?.id;
      if (cardId) {
        history.push(`/exam/card/${cardId}`);
      }
    } catch (error: any) {
      message.error(getRequestErrorMessage(error, '进入考试失败'));
    } finally {
      setEntering(null);
    }
  };

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '80vh' }}>
        <Spin size="large" tip="加载中..." />
      </div>
    );
  }

  return (
    <div className="wts-page wts-page-narrow">
      <div className="wts-page-header">
        <div>
          <div className="wts-page-eyebrow">学生考试入口</div>
          <Title level={3} className="wts-page-title">我的考试</Title>
          <div className="wts-page-desc">展示与你相关的考试，未到开始时间不能进入，成绩按结束时间和展示策略开放。</div>
        </div>
        <Tag color="blue" bordered={false}>
          相关考试 {rooms.length} 场
        </Tag>
      </div>

      {rooms.length === 0 ? (
        <div className="wts-empty-card" style={{ animation: 'fadeInUp 0.5s ease-out both' }}>
          <Empty description="暂无相关考试" />
        </div>
      ) : (
        <Row gutter={[16, 16]} className="wts-exam-grid">
          {rooms.map((room, index) => {
            const windowInfo = getRoomWindow(room);
            const stateInfo = ROOM_STATE_MAP[windowInfo.state] || ROOM_STATE_MAP.open;
            const hasResult = Boolean(room.myCardId);
            const canViewResult = hasResult && room.resultAvailable;
            const canEnter = !hasResult && windowInfo.state === 'open';
            const disabledReason = hasResult
              ? room.resultUnavailableReason || '成绩暂不可查看'
              : windowInfo.reason || '当前不可进入考试';
            return (
              <Col xs={24} md={12} key={room.id} style={{ animation: `fadeInUp 0.4s ease-out ${index * 0.08}s both` }}>
                <Card className="wts-exam-card" hoverable variant="outlined">
                  <div className="wts-exam-card-main">
                    <div>
                      <div className="wts-exam-title-row">
                        <div className="wts-exam-title">{room.name}</div>
                        <Tag color={stateInfo.color} bordered={false}>
                          {stateInfo.text}
                        </Tag>
                      </div>
                      <div className="wts-exam-meta">
                        <div className="wts-exam-meta-item">
                          <ClockCircleOutlined />
                          <Text type="secondary">考试时长：{room.timelen || 0} 分钟</Text>
                        </div>
                        <div className="wts-exam-meta-item">
                          <ScheduleOutlined />
                          <Text type="secondary">开始时间：{formatExamDateTime(room.starttime)}</Text>
                        </div>
                        <div className="wts-exam-meta-item">
                          <ScheduleOutlined />
                          <Text type="secondary">结束时间：{formatExamDateTime(room.endtime)}</Text>
                        </div>
                        {room.paperNames && (
                          <div className="wts-exam-meta-item">
                            <FileTextOutlined />
                            <Text type="secondary" ellipsis>
                              试卷：{room.paperNames}
                            </Text>
                          </div>
                        )}
                      </div>
                    </div>
                    {hasResult ? (
                      <Tooltip title={canViewResult ? '' : disabledReason}>
                        <span>
                          <Button
                            type="primary"
                            icon={<EyeOutlined />}
                            disabled={!canViewResult}
                            onClick={() => history.push(`/exam/card/${room.myCardId}/result`)}
                            block
                            style={{ height: 42, fontWeight: 600 }}
                          >
                            查看成绩
                          </Button>
                        </span>
                      </Tooltip>
                    ) : (
                      <Tooltip title={canEnter ? '' : disabledReason}>
                        <span>
                          <Button
                            type="primary"
                            icon={<PlayCircleOutlined />}
                            loading={entering === room.id}
                            disabled={!canEnter}
                            onClick={() => handleEnter(room)}
                            block
                            style={{ height: 42, fontWeight: 600 }}
                          >
                            进入考试
                          </Button>
                        </span>
                      </Tooltip>
                    )}
                  </div>
                </Card>
              </Col>
            );
          })}
        </Row>
      )}
    </div>
  );
};

export default MyExamsPage;
