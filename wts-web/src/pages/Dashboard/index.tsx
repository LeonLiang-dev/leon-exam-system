import React, { useEffect, useState } from 'react';
import { Button, Card, Col, Result, Row, Space, Spin, Statistic } from 'antd';
import { history } from '@umijs/max';
import {
  ArrowRightOutlined,
  FileTextOutlined,
  FormOutlined,
  TeamOutlined,
  HomeOutlined,
  CheckCircleOutlined,
  DatabaseOutlined,
  SendOutlined,
} from '@ant-design/icons';
import { getDashboardStats } from '@/services/exam';



const DashboardPage: React.FC = () => {
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState<any>({});
  const [error, setError] = useState(false);

  useEffect(() => {
    getDashboardStats()
      .then((res: any) => {
        setStats(res.data || {});
      })
      .catch(() => setError(true))
      .finally(() => setLoading(false));
  }, []);

  const statCards = [
    {
      title: '题目总数',
      value: stats.subjectCount ?? 0,
      icon: <FileTextOutlined />,
      iconClass: 'wts-stat-icon-blue',
    },
    {
      title: '试卷总数',
      value: stats.paperCount ?? 0,
      icon: <FormOutlined />,
      iconClass: 'wts-stat-icon-green',
    },
    {
      title: '已发布答题室',
      value: stats.roomCount ?? 0,
      icon: <HomeOutlined />,
      iconClass: 'wts-stat-icon-amber',
    },
    {
      title: '答卷数量',
      value: stats.cardCount ?? 0,
      icon: <CheckCircleOutlined />,
      iconClass: 'wts-stat-icon-purple',
    },
    {
      title: '用户数量',
      value: stats.userCount ?? 0,
      icon: <TeamOutlined />,
      iconClass: 'wts-stat-icon-rose',
    },
  ];

  const classStats: any[] = Array.isArray(stats.classStats) ? stats.classStats : [];

  return (
    <div className="wts-page">
      <div className="wts-page-header">
        <div>
          <div className="wts-page-eyebrow">管理工作台</div>
          <h1 className="wts-page-title">考试运营概览</h1>
          <div className="wts-page-desc">查看题库、试卷、答题室、答卷和用户规模，快速进入核心管理流程。</div>
        </div>
        <Space wrap>
          <Button icon={<DatabaseOutlined />} onClick={() => history.push('/exam/subject')}>
            题库管理
          </Button>
          <Button type="primary" icon={<SendOutlined />} onClick={() => history.push('/exam/room')}>
            发布考试
          </Button>
        </Space>
      </div>

      {error ? (
        <Card variant="outlined">
          <Result status="warning" title="统计数据加载失败" subTitle="请检查后端服务是否正常运行" />
        </Card>
      ) : (
        <Spin spinning={loading}>
          <Row gutter={[16, 16]}>
            {statCards.map((item, index) => (
              <Col xs={24} sm={12} lg={8} xl={6} xxl={4} key={item.title} style={{ animation: 'wts-fadeIn 0.4s ease-out both', animationDelay: `${index * 0.06}s` }}>
                <Card className="wts-stat-card" hoverable variant="outlined">
                  <div className="wts-stat-card-body">
                    <span className={`wts-stat-icon ${item.iconClass}`}>{item.icon}</span>
                    <Statistic title={item.title} value={item.value} />
                  </div>
                </Card>
              </Col>
            ))}
          </Row>

          <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
            <Col xs={24} lg={12} style={{ animation: 'wts-fadeIn 0.4s ease-out both', animationDelay: '0.25s' }}>
              <Card className="wts-stat-card" variant="outlined" title="班级答卷分布" extra={<span style={{ color: '#94a3b8', fontSize: 12 }}>已提交/已阅卷答卷按班级统计</span>}>
                {classStats.length === 0 ? (
                  <div style={{ padding: '24px 0', textAlign: 'center', color: '#94a3b8' }}>暂无班级统计数据（学生导入时填写班级后显示）</div>
                ) : (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                    {classStats.map((item) => (
                      <div key={item.className} style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                        <span style={{ width: 140, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', fontWeight: 600 }}>{item.className}</span>
                        <div style={{ flex: 1, height: 8, borderRadius: 4, background: '#e2e8f0', overflow: 'hidden' }}>
                          <div style={{ height: '100%', borderRadius: 4, background: 'linear-gradient(90deg,#4f46e5,#818cf8)', width: `${Math.min(100, (item.cardCount / Math.max(classStats[0]?.cardCount, 1)) * 100)}%` }} />
                        </div>
                        <span style={{ width: 60, textAlign: 'right', color: '#475569' }}>{item.cardCount} 份</span>
                        <span style={{ width: 56, textAlign: 'right', color: '#94a3b8' }}>均分 {item.avgPoint ?? '-'}</span>
                      </div>
                    ))}
                  </div>
                )}
              </Card>
            </Col>
          </Row>

          <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
            <Col xs={24} lg={8} style={{ animation: 'wts-fadeIn 0.4s ease-out both', animationDelay: '0.3s' }}>
              <Card className="wts-workflow-card" variant="outlined">
                <div style={{ display: 'flex', alignItems: 'flex-start', gap: 16 }}>
                  <span style={{ display: 'inline-flex', alignItems: 'center', justifyContent: 'center', width: 36, height: 36, borderRadius: '50%', background: 'linear-gradient(135deg, #4f46e5 0%, #818cf8 100%)', color: '#fff', fontWeight: 700, fontSize: 16, flexShrink: 0 }}>1</span>
                  <div>
                    <div className="wts-workflow-title">维护题库</div>
                    <div className="wts-workflow-desc">按题目分类维护选择题、判断题和主观题，为试卷配置准备基础数据。</div>
                  </div>
                </div>
              </Card>
            </Col>
            <Col xs={24} lg={8} style={{ animation: 'wts-fadeIn 0.4s ease-out both', animationDelay: '0.35s' }}>
              <Card className="wts-workflow-card" variant="outlined">
                <div style={{ display: 'flex', alignItems: 'flex-start', gap: 16 }}>
                  <span style={{ display: 'inline-flex', alignItems: 'center', justifyContent: 'center', width: 36, height: 36, borderRadius: '50%', background: 'linear-gradient(135deg, #4f46e5 0%, #818cf8 100%)', color: '#fff', fontWeight: 700, fontSize: 16, flexShrink: 0 }}>2</span>
                  <div>
                    <div className="wts-workflow-title">配置试卷</div>
                    <div className="wts-workflow-desc">使用固定试卷或随机组卷，完成题目结构、分值和答题配置。</div>
                  </div>
                </div>
              </Card>
            </Col>
            <Col xs={24} lg={8} style={{ animation: 'wts-fadeIn 0.4s ease-out both', animationDelay: '0.4s' }}>
              <Card
                className="wts-workflow-card"
                variant="outlined"
                extra={
                  <Button type="link" icon={<ArrowRightOutlined />} onClick={() => history.push('/exam/room')}>
                    去处理
                  </Button>
                }
              >
                <div style={{ display: 'flex', alignItems: 'flex-start', gap: 16 }}>
                  <span style={{ display: 'inline-flex', alignItems: 'center', justifyContent: 'center', width: 36, height: 36, borderRadius: '50%', background: 'linear-gradient(135deg, #4f46e5 0%, #818cf8 100%)', color: '#fff', fontWeight: 700, fontSize: 16, flexShrink: 0 }}>3</span>
                  <div>
                    <div className="wts-workflow-title">发布答题室</div>
                    <div className="wts-workflow-desc">设置考试时长、开放范围和参与人员，发布后学生即可在“我的考试”进入。</div>
                  </div>
                </div>
              </Card>
            </Col>
          </Row>
        </Spin>
      )}
    </div>
  );
};

export default DashboardPage;
