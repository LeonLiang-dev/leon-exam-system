import React, { useEffect, useState } from 'react';
import { useParams, history } from '@umijs/max';
import { Button, message, Spin, Card, InputNumber, Tag, Space, Divider, Modal } from 'antd';
import { getCardPaperForReview, getCardResult, judgeCard } from '@/services/exam';
import AnswerValueView, { getCardAnswerDisplayValue } from './AnswerValueView';

const TIPTYPE_LABELS: Record<string, string> = {
  '1': '填空题', '2': '单选题', '3': '多选题',
  '4': '判断题', '5': '问答题', '6': '附件题',
};

const JudgePage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [loading, setLoading] = useState(true);
  const [paperData, setPaperData] = useState<any>(null);
  const [resultData, setResultData] = useState<any>(null);
  const [scores, setScores] = useState<Record<string, number>>({});

  useEffect(() => {
    if (!id) return;
    Promise.all([getCardPaperForReview(id), getCardResult(id)])
      .then(([paperRes, resultRes]: any[]) => {
        setPaperData(paperRes.data);
        setResultData(resultRes.data);
        // Initialize scores from existing points (normalize key to camelCase)
        const existingScores: Record<string, number> = {};
        for (const p of resultRes.data?.points || []) {
          existingScores[p.versionid || p.versionId] = p.point || 0;
        }
        setScores(existingScores);
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

  // Collect manual-type subject versionIds
  const manualVersionIds = new Set<string>();
  for (const ch of paperData.chapters || []) {
    for (const s of ch.subjects || []) {
      if (s.tiptype === '5' || s.tiptype === '6') {
        manualVersionIds.add(s.versionId);
      }
    }
  }

  const handleSubmit = async () => {
    // Only send scores for manual-type questions
    const points = Object.entries(scores)
      .filter(([versionId]) => manualVersionIds.has(versionId))
      .map(([versionId, point]) => ({ versionId, point }));

    if (points.length === 0) {
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
            await judgeCard(id!, { points });
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
      await judgeCard(id!, { points });
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
        const pointInfo = pointMap[subject.versionId];
        const isManualType = subject.tiptype === '5' || subject.tiptype === '6';

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

            {isManualType ? (
              <>
                <div className="wts-judge-answer-box">
                  <strong>用户答案：</strong>
                  <div style={{ marginTop: 4, display: 'flex', flexDirection: 'column', gap: 8 }}>
                    {userAnswers.length > 0
                      ? userAnswers.map((a: any) => (
                        <AnswerValueView
                          key={a.id}
                          value={getCardAnswerDisplayValue(a, subject)}
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
              </>
            ) : (
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span>
                  用户答案：
                  {userAnswers.length > 0
                    ? userAnswers.map((a: any) => (
                      <AnswerValueView
                        key={a.id}
                        value={getCardAnswerDisplayValue(a, subject)}
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
