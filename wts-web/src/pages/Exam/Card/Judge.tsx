import React, { useEffect, useState } from 'react';
import { useParams, history } from '@umijs/max';
import { Button, message, Spin, Card, Input, InputNumber, Tag, Space, Divider, Modal } from 'antd';
import { getCardPaperForReview, getCardResult, judgeCard } from '@/services/exam';
import AnswerValueView, { getCardAnswerDisplayValues } from './AnswerValueView';

const TIPTYPE_LABELS: Record<string, string> = {
  '1': '填空题', '2': '单选题', '3': '多选题',
  '4': '判断题', '5': '主观题',
};

const JudgePage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [loading, setLoading] = useState(true);
  const [paperData, setPaperData] = useState<any>(null);
  const [resultData, setResultData] = useState<any>(null);
  const [scores, setScores] = useState<Record<string, number>>({});
  const [answerScores, setAnswerScores] = useState<Record<string, number>>({});
  const [scoreComments, setScoreComments] = useState<Record<string, string>>({});
  const [answerComments, setAnswerComments] = useState<Record<string, string>>({});

  useEffect(() => {
    if (!id) return;
    Promise.all([getCardPaperForReview(id), getCardResult(id)])
      .then(([paperRes, resultRes]: any[]) => {
        setPaperData(paperRes.data);
        setResultData(resultRes.data);
        // Initialize scores from existing points (normalize key to camelCase)
        const existingScores: Record<string, number> = {};
        const existingScoreComments: Record<string, string> = {};
        for (const p of resultRes.data?.points || []) {
          const versionId = p.versionid || p.versionId;
          existingScores[versionId] = p.point || 0;
          existingScoreComments[versionId] = p.reviewComment || p.reviewcomment || '';
        }
        setScores(existingScores);
        setScoreComments(existingScoreComments);
        const existingAnswerScores: Record<string, number> = {};
        const existingAnswerComments: Record<string, string> = {};
        for (const a of resultRes.data?.answers || []) {
          const versionId = a.versionid || a.versionId;
          const answerId = a.answerid || a.answerId;
          const key = `${versionId}|${answerId}`;
          existingAnswerScores[key] = a.point || 0;
          existingAnswerComments[key] = a.reviewComment || a.reviewcomment || '';
        }
        setAnswerScores(existingAnswerScores);
        setAnswerComments(existingAnswerComments);
        setLoading(false);
      })
      .catch(() => setLoading(false));
  }, [id]);

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <Spin size="large" />
      </div>
    );
  }

  if (!paperData || !resultData) {
    return <div style={{ textAlign: 'center', padding: 100 }}>数据加载失败</div>;
  }

  // Build answer map (normalize key to camelCase)
  const answerMap: Record<string, any[]> = {};
  for (const a of resultData.answers || []) {
    const key = a.versionid || a.versionId;
    if (!answerMap[key]) answerMap[key] = [];
    answerMap[key].push(a);
  }

  // Flatten subjects
  const allSubjects: any[] = [];
  for (const ch of paperData.chapters || []) {
    for (const s of ch.subjects || []) {
      allSubjects.push(s);
    }
  }

  // Find point info map (normalize key to camelCase)
  const pointMap: Record<string, any> = {};
  for (const p of resultData.points || []) {
    const key = p.versionid || p.versionId;
    pointMap[key] = p;
  }

  const handleScoreChange = (versionId: string, value: number | null) => {
    setScores((prev) => ({ ...prev, [versionId]: value ?? 0 }));
  };

  const handleScoreCommentChange = (versionId: string, value: string) => {
    setScoreComments((prev) => ({ ...prev, [versionId]: value }));
  };

  const handleAnswerScoreChange = (versionId: string, answerId: string, value: number | null) => {
    setAnswerScores((prev) => ({ ...prev, [`${versionId}|${answerId}`]: value ?? 0 }));
  };

  const handleAnswerCommentChange = (versionId: string, answerId: string, value: string) => {
    setAnswerComments((prev) => ({ ...prev, [`${versionId}|${answerId}`]: value }));
  };

  // Collect manual-type subject versionIds
  const manualVersionIds = new Set<string>();
  for (const ch of paperData.chapters || []) {
    for (const s of ch.subjects || []) {
      if (s.tiptype === '5') {
        manualVersionIds.add(s.versionId);
      }
    }
  }

  const handleSubmit = async () => {
    const points = Object.entries(scores)
      .filter(([versionId]) => manualVersionIds.has(versionId))
      .map(([versionId, point]) => ({
        versionId,
        point,
        reviewComment: scoreComments[versionId] || '',
      }));
    const answerPoints: any[] = [];
    for (const subject of allSubjects) {
      const pointInfo = pointMap[subject.versionId];
      const reviewRequired = (pointInfo?.reviewRequired || pointInfo?.reviewrequired) === '1';
      if (subject.tiptype !== '1' || !reviewRequired) continue;
      for (const answer of answerMap[subject.versionId] || []) {
        const answerId = answer.answerid || answer.answerId;
        const key = `${subject.versionId}|${answerId}`;
        const answerReviewRequired = (answer.reviewRequired || answer.reviewrequired) === '1';
        if (answerReviewRequired) {
          answerPoints.push({
            versionId: subject.versionId,
            answerId,
            point: answerScores[key] || 0,
            reviewComment: answerComments[key] || '',
          });
        }
      }
    }

    if (points.length === 0 && answerPoints.length === 0) {
      message.warning('没有需要人工评分的题目');
      return;
    }

    const unscored = [...manualVersionIds].filter(
      (vid) => scores[vid] === undefined || scores[vid] === null
    );
    if (unscored.length > 0) {
      Modal.confirm({
        title: '确认提交',
        content: `还有 ${unscored.length} 道题未评分（将按0分计算），确定提交吗？`,
        onOk: async () => {
          try {
            await judgeCard(id!, { points, answerPoints });
            message.success('阅卷完成');
            history.push(`/exam/card/${id}/result`);
          } catch {
            message.error('阅卷失败');
          }
        },
      });
      return;
    }

    try {
      await judgeCard(id!, { points, answerPoints });
      message.success('阅卷完成');
      history.push(`/exam/card/${id}/result`);
    } catch {
      message.error('阅卷失败');
    }
  };

  return (
    <div className="wts-judge-container">
      <Card title="人工阅卷" className="wts-judge-card" style={{ marginBottom: 24 }}>
        <p>答卷ID: {id}</p>
        <p>用户: {resultData.card?.userid}</p>
        <p>当前状态: <Tag color={resultData.card?.pstate === '21' ? 'green' : 'orange'}>
          {resultData.card?.pstate === '21' ? '已阅卷' : '待阅卷'}
        </Tag></p>
      </Card>

      {allSubjects.map((subject, index) => {
        const userAnswers = answerMap[subject.versionId] || [];
        const displayAnswers = getCardAnswerDisplayValues(userAnswers, subject);
        const pointInfo = pointMap[subject.versionId];
        const reviewRequired = (pointInfo?.reviewRequired || pointInfo?.reviewrequired) === '1';
        const isSubjective = subject.tiptype === '5';
        const isFillReview = subject.tiptype === '1' && reviewRequired;

        return (
          <Card
            key={subject.versionId}
            size="small"
            className="wts-judge-card"
            style={{ marginBottom: 16 }}
            title={
              <Space>
                <span>第 {index + 1} 题</span>
                <Tag>{TIPTYPE_LABELS[subject.tiptype] || '未知'}</Tag>
                <span style={{ color: '#999', fontWeight: 400 }}>({subject.point}分)</span>
              </Space>
            }
          >
            <div style={{ marginBottom: 12 }}>
              <strong>题目：</strong>{subject.tipstr || subject.introduction}
            </div>

            {isSubjective ? (
              <>
                <div className="wts-judge-answer-box">
                  <strong>用户答案：</strong>
                  <div style={{ marginTop: 4, display: 'flex', flexDirection: 'column', gap: 8 }}>
                    {displayAnswers.length > 0
                      ? displayAnswers.map((value: string, answerIndex: number) => (
                        <AnswerValueView
                          key={`${subject.versionId}-${answerIndex}`}
                          value={value}
                          block
                        />
                      ))
                      : <span style={{ color: '#999' }}>未作答</span>}
                  </div>
                </div>
                <div className="wts-judge-score-row">
                  <span>评分：</span>
                  <InputNumber
                    min={0}
                    max={subject.point}
                    value={scores[subject.versionId]}
                    onChange={(val) => handleScoreChange(subject.versionId, val)}
                    style={{ width: 120 }}
                  />
                  <span style={{ color: '#999' }}>/ {subject.point}分</span>
                </div>
                <Input.TextArea
                  rows={3}
                  maxLength={512}
                  showCount
                  placeholder="批注（可选）"
                  value={scoreComments[subject.versionId] || ''}
                  onChange={(event) => handleScoreCommentChange(subject.versionId, event.target.value)}
                />
              </>
            ) : isFillReview ? (
              <>
                <div className="wts-judge-answer-box">
                  <strong>填空复核：</strong>
                  <div style={{ marginTop: 8, display: 'flex', flexDirection: 'column', gap: 12 }}>
                    {(subject.answers || []).map((standard: any, blankIndex: number) => {
                      const userAnswer = userAnswers.find((a: any) => (a.answerid || a.answerId) === standard.id);
                      const answerId = standard.id;
                      const key = `${subject.versionId}|${answerId}`;
                      const maxPoint = userAnswer?.mpoint ?? userAnswer?.mPoint ?? 0;
                      const answerReviewRequired = (userAnswer?.reviewRequired || userAnswer?.reviewrequired) === '1';
                      return (
                        <div key={answerId} style={{ padding: 10, border: '1px solid #f0f0f0', borderRadius: 4 }}>
                          <Space wrap align="center">
                            <Tag>第 {blankIndex + 1} 空</Tag>
                            <span>学生答案：</span>
                            <AnswerValueView value={userAnswer?.valstr} />
                            <span>标准答案：</span>
                            <Tag color="blue">{standard.answer}</Tag>
                            {answerReviewRequired ? <Tag color="orange">待复核</Tag> : <Tag color="green">已自动判分</Tag>}
                          </Space>
                          <div style={{ marginTop: 8 }}>
                            <span>该空评分：</span>
                            <InputNumber
                              min={0}
                              max={maxPoint}
                              value={answerScores[key] || 0}
                              disabled={!answerReviewRequired}
                              onChange={(val) => handleAnswerScoreChange(subject.versionId, answerId, val)}
                              style={{ width: 120, marginLeft: 8, marginRight: 8 }}
                            />
                            <span style={{ color: '#999' }}>/ {maxPoint}分</span>
                          </div>
                          <Input.TextArea
                            rows={2}
                            maxLength={512}
                            showCount
                            disabled={!answerReviewRequired}
                            placeholder="批注（可选）"
                            value={answerComments[key] || ''}
                            onChange={(event) => handleAnswerCommentChange(subject.versionId, answerId, event.target.value)}
                            style={{ marginTop: 8 }}
                          />
                        </div>
                      );
                    })}
                  </div>
                </div>
              </>
            ) : (
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span>
                  用户答案：
                  {displayAnswers.length > 0
                    ? displayAnswers.map((value: string, answerIndex: number) => (
                      <AnswerValueView
                        key={`${subject.versionId}-${answerIndex}`}
                        value={value}
                      />
                    ))
                    : <span style={{ color: '#999' }}>未作答</span>}
                </span>
                <span>
                  得分：<Tag color={pointInfo?.point > 0 ? 'green' : 'red'}>{pointInfo?.point || 0}</Tag> / {pointInfo?.mpoint || subject.point}分
                </span>
              </div>
            )}
          </Card>
        );
      })}

      <Divider />

      <div style={{ textAlign: 'center', padding: '24px 0' }}>
        <Space size="large">
          <Button size="large" onClick={() => history.push(`/exam/card/${id}/result`)}>查看成绩</Button>
          <Button size="large" type="primary" onClick={handleSubmit}>提交阅卷</Button>
        </Space>
      </div>
    </div>
  );
};

export default JudgePage;
