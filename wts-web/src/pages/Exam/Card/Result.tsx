import React, { useEffect, useState } from 'react';
import { useParams } from '@umijs/max';
import { Card, Tag, Spin, Statistic, Row, Col, Divider, Button, Space, Result, Image } from 'antd';
import { CheckCircleOutlined, CloseCircleOutlined, MinusCircleOutlined } from '@ant-design/icons';
import { getCardResult } from '@/services/exam';
import { getRequestErrorMessage } from '@/utils/examTime';
import AnswerValueView, { getCardAnswerDisplayValues } from './AnswerValueView';

const TIPTYPE_LABELS: Record<string, string> = {
  '1': '填空题', '2': '单选题', '3': '多选题',
  '4': '判断题', '5': '主观题',
};

const SELECTION_TIPTYPES = new Set(['2', '3', '4']);

const optionLabel = (index: number) =>
  index >= 0 && index < 26 ? `${String.fromCharCode(65 + index)}. ` : '';

/** 客观题：将选项 id 集合渲染为 "A. xxx / B. xxx" 文本 */
const optionsToText = (options: any[], optionIds: string[]): string =>
  options
    .map((opt, index) => (optionIds.includes(opt.id) ? `${optionLabel(index)}${opt.answer || opt.pcontent || opt.id}` : null))
    .filter(Boolean)
    .join('；');

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
          const displayAnswers = getCardAnswerDisplayValues(cardAns, subject);
          const earnedPoint = pointInfo.point || 0;
          const maxPoint = pointInfo.mpoint || 0;
          const isCorrect = earnedPoint > 0 && maxPoint > 0;
          const reviewRequired = (pointInfo.reviewRequired || pointInfo.reviewrequired) === '1';
          const isPending = card?.pstate === '16' && (reviewRequired || pointInfo.complete === '1');
          const isWrong = !isCorrect && !isPending && pointInfo.complete === '1' && earnedPoint === 0;
          const isUnanswered = pointInfo.complete !== '1';
          const pointComment = pointInfo.reviewComment || pointInfo.reviewcomment || '';
          const answerComments = cardAns
            .map((answer: any) => answer.reviewComment || answer.reviewcomment)
            .filter((comment: string | undefined) => Boolean(comment));

          const tiptype = subject?.tiptype != null ? String(subject.tiptype) : '';
          const isSelection = SELECTION_TIPTYPES.has(tiptype);
          const subjectAnswers: any[] = subject?.answers || [];
          const selectedOptionIds = (cardAns || [])
            .filter((answer: any) => answer.valstr === 'true')
            .map((answer: any) => answer.answerid || answer.answerId)
            .filter(Boolean);
          const correctOptionIds = subjectAnswers
            .filter((opt: any) => opt.rightanswer === '1')
            .map((opt: any) => opt.id);
          const correctText = isSelection ? optionsToText(subjectAnswers, correctOptionIds) : '';
          const stem = subject?.tipstr || subject?.introduction || '';
          const explanation = subject?.tipnote || '';

          return (
            <div
              key={pointInfoKey}
              className={`wts-result-question ${isCorrect ? 'wts-result-question-correct' : isWrong ? 'wts-result-question-wrong' : isPending ? 'wts-result-question-pending' : ''}`}
            >
              <div style={{ marginBottom: 8, display: 'flex', justifyContent: 'space-between' }}>
                <Space>
                  <span style={{ fontWeight: 600 }}>第 {index + 1} 题</span>
                  {subject?.tiptype != null && (
                    <Tag>{TIPTYPE_LABELS[tiptype] || `题型${tiptype}`}</Tag>
                  )}
                  {isCorrect && <Tag color="green">正确 +{earnedPoint}分</Tag>}
                  {isPending && <Tag color="orange">待阅卷</Tag>}
                  {isWrong && <Tag color="red">错误 0分</Tag>}
                  {isUnanswered && <Tag>未作答</Tag>}
                </Space>
                <span style={{ color: '#999' }}>{earnedPoint}/{maxPoint}分</span>
              </div>

              {/* 题干 */}
              {stem && (
                <div style={{ marginBottom: 8, color: '#333', whiteSpace: 'pre-wrap' }}>{stem}</div>
              )}
              {subject?.pcontent && (
                <div style={{ marginBottom: 8 }}>
                  <Image
                    src={subject.pcontent}
                    alt="题干配图"
                    style={{ maxWidth: 320, borderRadius: 6, border: '1px solid #e5e7eb' }}
                  />
                </div>
              )}

              {/* 全部选项（客观题） */}
              {isSelection && subjectAnswers.length > 0 && (
                <div style={{ marginBottom: 8 }}>
                  {subjectAnswers.map((opt: any, optionIndex: number) => {
                    const isSelected = selectedOptionIds.includes(opt.id);
                    const isCorrectOption = correctOptionIds.includes(opt.id);
                    const stateClass = isSelected && isCorrectOption
                      ? 'wts-option-selected-correct'
                      : isSelected
                        ? 'wts-option-selected-wrong'
                        : isCorrectOption
                          ? 'wts-option-correct'
                          : '';
                    return (
                      <div
                        key={opt.id || optionIndex}
                        className={`wts-option ${stateClass}`}
                        style={{
                          padding: '6px 10px',
                          marginBottom: 4,
                          borderRadius: 6,
                          border: '1px solid #e5e7eb',
                          background: isSelected && isCorrectOption ? '#ecfdf5' : isSelected ? '#fef2f2' : isCorrectOption ? '#f0fdf4' : '#fff',
                          borderColor: isSelected && isCorrectOption ? '#34d399' : isSelected ? '#f87171' : isCorrectOption ? '#4ade80' : '#e5e7eb',
                          color: '#333',
                        }}
                      >
                        {optionLabel(optionIndex)}
                        {opt.answer || opt.pcontent || opt.id}
                        {isSelected && <Tag color={isCorrectOption ? 'green' : 'red'} style={{ marginLeft: 8 }}>我的选择</Tag>}
                        {isCorrectOption && <Tag color="green" style={{ marginLeft: 4 }}>正确答案</Tag>}
                      </div>
                    );
                  })}
                </div>
              )}

              {/* 正确答案（客观题） */}
              {isSelection && correctText && (
                <div style={{ marginBottom: 8, color: '#15803d' }}>
                  <span style={{ color: '#666' }}>正确答案：</span>
                  <span style={{ fontWeight: 500 }}>{correctText}</span>
                </div>
              )}

              {/* 你的答案 */}
              <div style={{ marginBottom: 8, color: '#333' }}>
                {displayAnswers.length > 0 ? (
                  <div>
                    <span style={{ color: '#666' }}>你的答案：</span>
                    <Space wrap align="start" style={{ marginLeft: 4 }}>
                      {displayAnswers.map((value: string, answerIndex: number) => (
                        <AnswerValueView
                          key={`${pointInfoKey}-${answerIndex}`}
                          value={value}
                        />
                      ))}
                    </Space>
                  </div>
                ) : (
                  <span style={{ color: '#999' }}>未作答</span>
                )}
              </div>

              {/* 解析 */}
              {explanation && (
                <div style={{ marginTop: 8, color: '#666' }}>
                  <span style={{ color: '#999' }}>解析：</span>{explanation}
                </div>
              )}
              {pointComment && (
                <div style={{ marginTop: 8, color: '#666' }}>
                  阅卷批注：{pointComment}
                </div>
              )}
              {answerComments.length > 0 && (
                <div style={{ marginTop: 8, color: '#666' }}>
                  复核批注：{answerComments.join('；')}
                </div>
              )}
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
