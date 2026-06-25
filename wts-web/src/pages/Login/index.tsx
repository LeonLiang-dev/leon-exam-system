import React, { useState } from 'react';
import { Form, Input, Button, Card, message } from 'antd';
import { LockOutlined, LoginOutlined, UserOutlined } from '@ant-design/icons';
import { login, LoginParams } from '@/services/auth';

const LoginPage: React.FC = () => {
  const [loading, setLoading] = useState(false);

  const onFinish = async (values: LoginParams) => {
    setLoading(true);
    try {
      const res: any = await login(values);
      const { accessToken, refreshToken, userType } = res.data;
      localStorage.setItem('access_token', accessToken);
      localStorage.setItem('refresh_token', refreshToken);
      message.success('登录成功');
      // Full page reload ensures getInitialState loads user before routing decisions
      const target = (userType === '3' || userType === '1') ? '/dashboard' : '/my-exams';
      window.location.href = target;
    } catch (error: any) {
      message.error(error?.data?.message || error?.message || '登录失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="wts-login-page">
      <section className="wts-login-identity">
        <div>
          <div className="wts-login-brand">L</div>
          <div className="wts-login-copy">
            <h1>Leon 在线考试系统</h1>
            <p>面向教师、管理员和学生的考试组织平台，支持题库维护、试卷配置、答题室发布和在线答题。</p>
          </div>
          <div className="wts-login-points">
            <div className="wts-login-point">
              <strong>题库</strong>
              <span>分类维护题目，支撑试卷和随机组卷。</span>
            </div>
            <div className="wts-login-point">
              <strong>考试</strong>
              <span>发布答题室，按公开或指定人员参与。</span>
            </div>
            <div className="wts-login-point">
              <strong>帐号</strong>
              <span>教师可批量导入学生帐号信息。</span>
            </div>
          </div>
        </div>
        <div className="wts-login-footer">学生帐号使用学号登录，初始密码由教师统一配置。</div>
      </section>

      <main className="wts-login-panel">
        <Card className="wts-login-card" variant="outlined">
          <div className="wts-login-card-title">
            <h2>欢迎回来</h2>
            <p>请输入帐号和密码登录系统。</p>
          </div>
        <Form
          name="login"
          initialValues={{ remember: true }}
          onFinish={onFinish}
          size="large"
          layout="vertical"
          requiredMark={false}
        >
          <Form.Item
            name="loginName"
            label="帐号"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input
              prefix={<UserOutlined />}
              placeholder="用户名或学号"
              autoComplete="username"
              allowClear
            />
          </Form.Item>

          <Form.Item
            name="password"
            label="密码"
            rules={[{ required: true, message: '请输入密码' }]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="密码"
              autoComplete="current-password"
            />
          </Form.Item>

          <Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              icon={<LoginOutlined />}
              loading={loading}
              block
              style={{ height: 44 }}
            >
              登录
            </Button>
          </Form.Item>
        </Form>
          <div style={{ textAlign: 'center', marginTop: 16, color: '#94a3b8', fontSize: 12 }}>Leon v2.0 · 安全登录</div>
      </Card>
      </main>
    </div>
  );
};

export default LoginPage;
