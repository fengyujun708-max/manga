'use client';
import React from 'react';
import { Card, Form, Input, Button, message } from 'antd';
import { LockOutlined, UserOutlined } from '@ant-design/icons';
import { useRouter } from 'next/navigation';

export default function LoginPage() {
  const router = useRouter();
  return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh', background: '#0a0a1a' }}>
      <Card style={{ width: 400, padding: 24 }}>
        <h2 style={{ textAlign: 'center', marginBottom: 24, color: '#6C5CE7' }}>漫界管理后台</h2>
        <Form layout="vertical" onFinish={() => { message.success('登录成功'); router.push('/dashboard'); }}>
          <Form.Item name="username" rules={[{ required: true }]}>
            <Input prefix={<UserOutlined />} placeholder="管理员账号" size="large" />
          </Form.Item>
          <Form.Item name="password" rules={[{ required: true }]}>
            <Input.Password prefix={<LockOutlined />} placeholder="密码" size="large" />
          </Form.Item>
          <Button type="primary" htmlType="submit" block size="large" style={{ background: '#6C5CE7', borderColor: '#6C5CE7' }}>登录</Button>
        </Form>
      </Card>
    </div>
  );
}
