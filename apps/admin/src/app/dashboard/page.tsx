'use client';
import React from 'react';
import { Card, Row, Col, Statistic, Table, Tag } from 'antd';
import { UserOutlined, ApiOutlined, WarningOutlined, CheckCircleOutlined } from '@ant-design/icons';
import AdminLayout from '@/components/layout/AdminLayout';

const stats = [
  { title: '总用户', value: 1280, icon: <UserOutlined />, color: '#6C5CE7' },
  { title: '活跃源', value: 8, icon: <ApiOutlined />, color: '#00D2D3' },
  { title: '待处理举报', value: 3, icon: <WarningOutlined />, color: '#ff4d4f' },
  { title: '今日活跃', value: 256, icon: <CheckCircleOutlined />, color: '#52c41a' },
];

export default function DashboardPage() {
  return (
    <AdminLayout>
      <h2 style={{ marginBottom: 24 }}>仪表盘</h2>
      <Row gutter={[16, 16]}>
        {stats.map((s, i) => (
          <Col xs={24} sm={12} lg={6} key={i}>
            <Card hoverable><Statistic title={s.title} value={s.value} prefix={<span style={{ color: s.color }}>{s.icon}</span>} valueStyle={{ color: s.color }} /></Card>
          </Col>
        ))}
      </Row>
      <Card title="最近注册用户" style={{ marginTop: 24 }}>
        <Table dataSource={[
          { key: '1', nickname: '用户A', phone: '138****0001', status: '正常', createdAt: '2025-01-15' },
          { key: '2', nickname: '用户B', phone: '138****0002', status: '已封禁', createdAt: '2025-01-14' },
        ]} columns={[
          { title: '昵称', dataIndex: 'nickname', key: 'nickname' },
          { title: '手机号', dataIndex: 'phone', key: 'phone' },
          { title: '状态', dataIndex: 'status', key: 'status', render: (s: string) => <Tag color={s === '正常' ? 'green' : 'red'}>{s}</Tag> },
          { title: '注册时间', dataIndex: 'createdAt', key: 'createdAt' },
        ]} pagination={false} size="small" />
      </Card>
    </AdminLayout>
  );
}
