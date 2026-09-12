import React, { useEffect, useState, useRef, useCallback } from 'react';
import { useParams, history } from '@umijs/max';
import { App, Button, Modal, Spin, Row, Col, Card as AntCard, Drawer } from 'antd';
import { ClockCircleOutlined, UnorderedListOutlined } from '@ant-design/icons';
import { getCardPaper, saveCardAnswers, submitCard } from '@/services/exam';
import { getRequestErrorMessage, parseExamDateTime } from '@/utils/examTime';
import QuestionItem from './QuestionItem';
import AnswerSheet from './AnswerSheet';

const isMobile = () => window.innerWidth < 768;

const ExamCardPage: React.FC = () => {
  const { message } = App.useApp();
  const { id } = useParams<{ id: string }>();
  const [loading, setLoading] = useState(true);
  const [paperData, setPaperData] = useState<any>(null);
  const [answers, setAnswers] = useState<Record<string, any>>({});
  const [currentIndex, setCurrentIndex] = useState(0);
  const [remaining, setRemaining] = useState(0);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [mobile, setMobile] = useState(isMobile());
  const [submitted, setSubmitted] = useState(false);
  const timerRef = useRef<ReturnType<typeof setInterval>>();
  const autoSaveTickRef = useRef(0);

  // Use refs to avoid stale closures in setInterval
  const answersRef = useRef(answers);
  answersRef.current = answers;
  const paperDataRef = useRef(paperData);
  paperDataRef.current = paperData;
  const submittedRef = useRef(submitted);
  submittedRef.current = submitted;

  useEffect(() => {
    const onResize = () => setMobile(isMobile());
    window.addEventListener('resize', onResize);
    return () => window.removeEventListener('resize', onResize);
  }, []);

  // Warn before closing/navigating away during exam
  useEffect(() => {
    const handler = (e: BeforeUnloadEvent) => {
      if (!submittedRef.current) {
        e.preventDefault();
        e.returnValue = '';
      }
    };
    window.addEventListener('beforeunload', handler);
    return () => window.removeEventListener('beforeunload', handler);
  }, []);

  // Final save on unmount
  useEffect(() => {
    return () => {
      if (!submittedRef.current) {
        doSaveRef.current?.(true);
      }
    };
  }, []);

  // Flatten subjects from chapters (memoize from paperData)
  const allSubjects: { subject: any; chapterName: string }[] = [];
  if (paperData?.chapters) {
    for (const ch of paperData.chapters) {
      for (const s of ch.subjects || []) {
        allSubjects.push({ subject: s, chapterName: ch.name || '默认章节' });
      }
    }
  }

  const buildSubmitData = useCallback(() => {
    const pd = paperDataRef.current;
    if (!pd?.chapters) return { answers: [] };
    const subjects: { subject: any }[] = [];
    for (const ch of pd.chapters) {
      for (const s of ch.subjects || []) {
        subjects.push({ subject: s });
      }
    }
    const ans = answersRef.current;
    const submitAnswers: any[] = [];
    for (const { subject } of subjects) {
      const val = ans[subject.versionId];
      if (val === undefined || val === null) continue;
      switch (subject.tiptype) {
        case '2':
        case '4':
          for (const answer of subject.answers || []) {
            submitAnswers.push({
              answerid: answer.id,
              versionid: subject.versionId,
              valstr: answer.id === val ? 'true' : 'false',
            });
          }
          break;
        case '3':
          {
            const selected = Array.isArray(val) ? val : [];
            for (const answer of subject.answers || []) {
              submitAnswers.push({
                answerid: answer.id,
                versionid: subject.versionId,
                valstr: selected.includes(answer.id) ? 'true' : 'false',
              });
            }
          }
          break;
        case '1':
          if (typeof val === 'object') {
            for (const answer of subject.answers || []) {
              submitAnswers.push({
                answerid: answer.id,
                versionid: subject.versionId,
                valstr: (val as Record<string, string>)[answer.id] || '',
              });
            }
          }
          break;
        case '5':
          if (subject.answers?.[0]) {
            const normalized = val && typeof val === 'object'
              ? { text: val.text || '', images: Array.isArray(val.images) ? val.images : [] }
              : { text: typeof val === 'string' ? val : '', images: [] };
            submitAnswers.push({
              answerid: subject.answers[0].id,
              versionid: subject.versionId,
              valstr: JSON.stringify(normalized),
            });
          }
          break;
      }
    }
    return { answers: submitAnswers };
  }, []);

  const doSubmitRef = useRef<() => Promise<void>>();
  doSubmitRef.current = async () => {
    if (submittedRef.current) return;
    try {
      await submitCard(id!, buildSubmitData());
      setSubmitted(true);
      message.success('交卷成功');
      history.replace('/my-exams');
    } catch (err: any) {
      const msg = getRequestErrorMessage(err, '');
      if (msg.includes('已提交')) {
        setSubmitted(true);
        history.replace('/my-exams');
        return;
      }
      message.error(msg || '交卷失败');
    }
  };

  const doSaveRef = useRef<(silent?: boolean) => Promise<void>>();
  doSaveRef.current = async (silent = true) => {
    if (submittedRef.current) return;
    try {
      await saveCardAnswers(id!, buildSubmitData());
      if (!silent) message.success('已暂存');
    } catch (err: any) {
      const msg = getRequestErrorMessage(err, '');
      if (msg.includes('已提交')) {
        message.warning('答卷已提交');
        setSubmitted(true);
        clearInterval(timerRef.current);
        return;
      }
      if (!silent) message.error('暂存失败');
    }
  };

  // Load paper data
  useEffect(() => {
    if (!id) return;
    getCardPaper(id)
      .then((res: any) => {
        setPaperData(res.data);
        // Calculate remaining time from card starttime
        const timelen = res.data?.timelen || 60;
        const card = res.data?.card;
        let seconds = timelen * 60;
        const start = parseExamDateTime(card?.starttime);
        if (start) {
          const elapsed = Math.floor((Date.now() - start.getTime()) / 1000);
          seconds = Math.max(0, timelen * 60 - elapsed);
        }
        const end = parseExamDateTime(card?.endtime);
        if (end) {
          const roomWindowRemaining = Math.floor((end.getTime() - Date.now()) / 1000);
          seconds = Math.max(0, Math.min(seconds, roomWindowRemaining));
        }
        setRemaining(seconds);

        // Restore saved answers
        const savedAnswers = res.data?.savedAnswers || [];
        if (savedAnswers.length > 0) {
          const subjectMap: Record<string, any> = {};
          for (const ch of res.data?.chapters || []) {
            for (const s of ch.subjects || []) {
              subjectMap[s.versionId] = s;
            }
          }
          const grouped: Record<string, any[]> = {};
          for (const a of savedAnswers) {
            if (!grouped[a.versionid]) grouped[a.versionid] = [];
            grouped[a.versionid].push(a);
          }
          const restored: Record<string, any> = {};
          for (const [versionId, ans] of Object.entries(grouped)) {
            const subject = subjectMap[versionId];
            const tiptype = subject?.tiptype;
            if (tiptype === '2' || tiptype === '4') {
              const selected = ans.find((a: any) => a.valstr === 'true');
              if (selected) restored[versionId] = selected.answerid;
            } else if (tiptype === '3') {
              const selected = ans.filter((a: any) => a.valstr === 'true').map((a: any) => a.answerid);
              if (selected.length > 0) restored[versionId] = selected;
            } else if (tiptype === '1') {
              const obj: Record<string, string> = {};
              for (const a of ans) {
                if (a.valstr) obj[a.answerid] = a.valstr;
              }
              if (Object.keys(obj).length > 0) restored[versionId] = obj;
            } else if (tiptype === '5') {
              const text = ans[0]?.valstr;
              if (text) {
                try {
                  const parsed = JSON.parse(text);
                  restored[versionId] = {
                    text: parsed?.text || '',
                    images: Array.isArray(parsed?.images) ? parsed.images : [],
                  };
                } catch {
                  restored[versionId] = { text, images: [] };
                }
              }
            }
          }
          setAnswers(restored);
        }
        setLoading(false);
      })
      .catch((err: any) => {
        const msg = getRequestErrorMessage(err, '');
        if (msg.includes('已提交') || msg.includes('无法再次作答') || msg.includes('未开始') || msg.includes('已结束')) {
          if (msg) message.warning(msg);
          history.replace('/my-exams');
          return;
        }
        message.error(msg || '加载试卷失败');
        setLoading(false);
      });
  }, [id]);

  // Timer countdown — single source of truth
  useEffect(() => {
    if (loading || submitted || remaining <= 0) return;
    timerRef.current = setInterval(() => {
      setRemaining((prev) => {
        if (prev <= 1) {
          clearInterval(timerRef.current);
          return 0;
        }
        return prev - 1;
      });
      // Auto-save every 60 ticks
      autoSaveTickRef.current++;
      if (autoSaveTickRef.current % 60 === 0) {
        doSaveRef.current?.(true);
      }
    }, 1000);
    return () => clearInterval(timerRef.current);
  }, [loading, submitted]);

  // Auto-submit when timer reaches 0 (separate effect, no stale closure)
  useEffect(() => {
    if (!loading && remaining === 0 && !submitted && paperData) {
      doSubmitRef.current?.();
    }
  }, [remaining, loading, submitted, paperData]);

  const handleAnswerChange = (versionId: string, value: any) => {
    setAnswers((prev) => ({ ...prev, [versionId]: value }));
  };

  // Count answered questions
  const answeredSet = new Set<string>();
  for (const { subject } of allSubjects) {
    const val = answers[subject.versionId];
    if (val !== undefined && val !== null) {
      if (typeof val === 'string' && val.length > 0) answeredSet.add(subject.versionId);
      else if (Array.isArray(val) && val.length > 0) answeredSet.add(subject.versionId);
      else if (typeof val === 'object' && !Array.isArray(val) && Object.keys(val).length > 0) {
        const hasTextValue = Object.values(val).some((item: any) => {
          if (typeof item === 'string') return item.trim().length > 0;
          if (Array.isArray(item)) return item.length > 0;
          return Boolean(item);
        });
        if (hasTextValue) answeredSet.add(subject.versionId);
      }
    }
  }

  const formatTime = (seconds: number) => {
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    const s = seconds % 60;
    return `${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  };

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <Spin size="large" tip="加载试卷中..." />
      </div>
    );
  }

  if (!paperData) {
    return <div style={{ textAlign: 'center', padding: 100 }}>试卷数据加载失败</div>;
  }

  const currentSubject = allSubjects[currentIndex]?.subject;

  return (
    <div style={{ minHeight: '100vh', background: '#f8fafc' }}>
      {/* Top bar */}
      <div className="wts-exam-topbar">
        <div className="wts-exam-topbar-title">
          {paperData.paperName}
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 16, flexWrap: 'wrap' }}>
          <span className={`wts-exam-timer${remaining < 300 ? ' wts-exam-timer-warning' : ''}`}>
            <ClockCircleOutlined />
            {formatTime(remaining)}
          </span>
          <span className="wts-exam-progress">
            已答 {answeredSet.size}/{allSubjects.length} 题
          </span>
        </div>
      </div>

      {/* Content */}
      <div style={{ padding: mobile ? '12px 8px' : '16px 24px' }}>
        <Row gutter={mobile ? 0 : 16}>
          {/* Questions */}
          <Col xs={24} md={17}>
            <AntCard size="small" style={{ marginBottom: 16 }}>
              <div style={{ fontSize: mobile ? 14 : 16, fontWeight: 600, marginBottom: 8 }}>
                {currentSubject?.tipstr || currentSubject?.introduction || '题目'}
              </div>
            </AntCard>

            {allSubjects.map(({ subject }, index) => (
              <QuestionItem
                key={subject.versionId}
                index={index}
                subject={subject}
                value={answers[subject.versionId]}
                onChange={(val) => handleAnswerChange(subject.versionId, val)}
                mobile={mobile}
              />
            ))}

            {/* Bottom action bar */}
            <div className="wts-bottom-bar">
              <Button size="large" block={mobile} disabled={submitted} onClick={() => doSaveRef.current?.(false)}>
                暂存
              </Button>
              <Button size="large" block={mobile} type="primary" danger disabled={submitted}
                onClick={() => {
                  Modal.confirm({
                    title: '确认交卷',
                    content: '交卷后将无法修改答案，确定要交卷吗？',
                    onOk: () => doSubmitRef.current?.(),
                  });
                }}>
                交卷
              </Button>
            </div>
          </Col>

          {/* Answer sheet - desktop */}
          <Col xs={0} md={7}>
            <AnswerSheet
              subjects={allSubjects}
              answeredSet={answeredSet}
              currentIndex={currentIndex}
              onJump={(idx) => {
                setCurrentIndex(idx);
                const el = document.getElementById(`question-${idx}`);
                el?.scrollIntoView({ behavior: 'smooth', block: 'start' });
              }}
            />
          </Col>
        </Row>
      </div>

      {/* Mobile floating answer sheet button */}
      {mobile && (
        <div className="wts-float-btn" onClick={() => setDrawerOpen(true)}>
          <UnorderedListOutlined style={{ fontSize: 22 }} />
          <span className="wts-float-badge">
            {allSubjects.length - answeredSet.size}
          </span>
        </div>
      )}

      {/* Mobile answer sheet drawer */}
      <Drawer
        title="答题卡"
        placement="bottom"
        height="60vh"
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        styles={{ body: { padding: '12px 16px' } }}
      >
        <AnswerSheet
          subjects={allSubjects}
          answeredSet={answeredSet}
          currentIndex={currentIndex}
          compact
          onJump={(idx) => {
            setCurrentIndex(idx);
            setDrawerOpen(false);
            const el = document.getElementById(`question-${idx}`);
            el?.scrollIntoView({ behavior: 'smooth', block: 'start' });
          }}
        />
      </Drawer>
    </div>
  );
};

export default ExamCardPage;
