import React, { useEffect, useState } from 'react';
import { useParams } from '@umijs/max';
import { Card, Tag, Spin, Statistic, Row, Col, Divider, Button, Space, Result } from 'antd';
import { CheckCircleOutlined, CloseCircleOutlined, MinusCircleOutlined } from '@ant-design/icons';
import { getCardResult } from '@/services/exam';
import { getRequestErrorMessage } from '@/utils/examTime';
import AnswerValueView, { getCardAnswerDisplayValue } from './AnswerValueView';

const TIPTYPE_LABELS: Record<string, string> = {
  '1': '填空题', '2': '单选题', '3': '多选题',
  '4': '判断题', '5': '问答题', '6': '附件题',
};

const PSTATE_MAP: Record<string, { text: string; color: string }> = {
  '11': { text: '答题中', color: 'blue' },
  '16': { text: '已提交', color: 'orange' },
  '21': { text: '已阅卷', color: 'green' },
};

const ExamResultPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [loading, setLoading] = useState(true);
  const [data, setData] = useState<any>(null);
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    if (!id) return;
    getCardResult(id)
      .then((res: any) => {
        setData(res.data);
        setLoading(false);
      })
      .catch((error: any) => {
        setErrorMessage(getRequestErrorMessage(error, '加载成绩数据失败'));
        setLoading(false);
      });
  }, [id]);

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <Spin size="large" />
      </div>
    );
  }

  if (errorMessage) {
    return (
      <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 24 }}>
        <Result
          status="info"
          title={errorMessage}
          extra={<Button type="primary" onClick={() => window.history.back()}>返回</Button>}
        />
      </div>
    );
  }

  if (!data) {
    return <div style={{ textAlign: 'center', padding: 100 }}>数据加载失败</div>;
  }

  const { card, answers: cardAnswers, points, paper } = data;
  const pstateInfo = PSTATE_MAP[card?.pstate] || { text: '未知', color: 'default' };

  // Build answer map: versionId -> list of card answers (normalize casing)
  const answerMap: Record<string, any[]> = {};
  for (const a of cardAnswers || []) {
    const key = a.versionid || a.versionId;
    if (!answerMap[key]) answerMap[key] = [];
    answerMap[key].push(a);
  }

  // Build point map: versionId -> card point (normalize casing)
  const pointMap: Record<string, any> = {};
  for (const p of points || []) {
    const key = p.versionid || p.versionId;
    pointMap[key] = p;
  }

  const subjectMap: Record<string, any> = {};
  for (const chapter of paper?.chapters || []) {
    for (const subject of chapter.subjects || []) {
      subjectMap[subject.versionId] = subject;
    }
  }

  return (
    <div className="wts-result-container">
      {/* Score header */}
      <Card className="wts-result-header" style={{ marginBottom: 24 }}>
        <Row gutter={24} align="middle">
          <Col flex="auto">
            {card?.pstate === '16' ? (
              <Statistic
                title="成绩"
                value="待阅卷"
                valueStyle={{ fontSize: 48, color: '#f59e0b' }}
              />
            ) : (
              <Statistic
                title="总分"
                value={card?.point || 0}
                suffix="分"
                valueStyle={{ fontSize: 48, color: '#4f46e5' }}
              />
            )}
          </Col>
          <Col>
            <Tag color={pstateInfo.color} style={{ fontSize: 14, padding: '4px 12px' }}>
              {pstateInfo.text}
            </Tag>
          </Col>
        </Row>
        <Divider />
        <Row gutter={24}>
          <Col>
            <Statistic title="答对" value={points?.filter((p: any) => p.point > 0).length || 0} suffix="题"
              valueStyle={{ color: '#10b981' }} prefix={<CheckCircleOutlined />} />
          </Col>
          <Col>
            <Statistic
              title={card?.pstate === '16' ? '待阅卷/答错' : '答错'}
              value={points?.filter((p: any) => p.point === 0 && p.complete === '1').length || 0}
              suffix="题"
              valueStyle={{ color: card?.pstate === '16' ? '#f59e0b' : '#ef4444' }}
              prefix={card?.pstate === '16' ? <MinusCircleOutlined /> : <CloseCircleOutlined />}
            />
          </Col>
          <Col>
            <Statistic title="未答" value={(card?.allnum || 0) - (card?.completenum || 0)} suffix="题"
              valueStyle={{ color: '#999' }} prefix={<MinusCircleOutlined />} />
          </Col>
          <Col>
            <Statistic title="答题数" value={card?.completenum || 0} suffix={`/ ${card?.allnum || 0}`} />
          </Col>
        </Row>
      </Card>

      {/* Per-question results */}
      <Card title="答题详情">
        {(points || []).map((pointInfo: any, index: number) => {
          const pointInfoKey = pointInfo.versionid || pointInfo.versionId;
          const subject = subjectMap[pointInfoKey];
          const cardAns = answerMap[pointInfoKey] || [];
          const earnedPoint = pointInfo.point || 0;
          const maxPoint = pointInfo.mpoint || 0;
          const isCorrect = earnedPoint > 0 && maxPoint > 0;
          // Answered but scored 0 while card not yet judged → might be pending manual grading
          const isPending = card?.pstate === '16' && earnedPoint === 0 && pointInfo.complete === '1';
          const isWrong = !isCorrect && !isPending && pointInfo.complete === '1' && earnedPoint === 0;
          const isUnanswered = pointInfo.complete !== '1';

          return (
            <div
              key={pointInfoKey}
              className={`wts-result-question ${isCorrect ? 'wts-result-question-correct' : isWrong ? 'wts-result-question-wrong' : isPending ? 'wts-result-question-pending' : ''}`}
            >
              <div style={{ marginBottom: 8, display: 'flex', justifyContent: 'space-between' }}>
                <Space>
                  <span style={{ fontWeight: 600 }}>第 {index + 1} 题</span>
                  {isCorrect && <Tag color="green">正确 +{earnedPoint}分</Tag>}
                  {isPending && <Tag color="orange">待阅卷</Tag>}
                  {isWrong && <Tag color="red">错误 0分</Tag>}
                  {isUnanswered && <Tag>未作答</Tag>}
                </Space>
                <span style={{ color: '#999' }}>{earnedPoint}/{maxPoint}分</span>
              </div>

              <div style={{ marginBottom: 8, color: '#333' }}>
                {cardAns.length > 0 ? (
                  <div>
                    <span style={{ color: '#666' }}>你的答案：</span>
                    <Space wrap align="start" style={{ marginLeft: 4 }}>
                      {cardAns.map((a: any) => (
                        <AnswerValueView
                          key={a.id}
                          value={getCardAnswerDisplayValue(a, subject)}
                        />
                      ))}
                    </Space>
                  </div>
                ) : (
                  <span style={{ color: '#999' }}>未作答</span>
                )}
              </div>
            </div>
          );
        })}
      </Card>

      <div style={{ textAlign: 'center', marginTop: 24 }}>
        <Button type="primary" size="large" onClick={() => window.history.back()}>返回</Button>
      </div>
    </div>
  );
};

export default ExamResultPage;
