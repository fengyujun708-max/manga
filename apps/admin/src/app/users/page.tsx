'use client';
import React from 'react';
import { Card, Table, Tag, Button, Space, Input, Select, Modal, message } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
import AdminLayout from '@/components/layout/AdminLayout';

export default function UsersPage() {
  const data = Array.from({ length: 20 }, (_, i) => ({
    key: i.toString(), nickname: `用户${i + 1}`, phone: `138****${String(1000 + i).slice(1)}`,
    status: ['正常', '封禁', '正常', '正常'][i % 4], role: ['user', 'user', 'admin', 'user'][i % 4],
    createdAt: `2025-01-${String(15 - (i % 15)).padStart(2, '0')}`, lastLogin: `${i + 1}天前`,
  }));

  return (
    <AdminLayout>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2>用户管理</h2>
        <Space>
          <Input prefix={<SearchOutlined />} placeholder="搜索用户" style={{ width: 200 }} />
          <Select defaultValue="all" style={{ width: 120 }}
            options={[{ value: 'all', label: '全部' }, { value: 'normal', label: '正常' }, { value: 'banned', label: '已封禁' }]} />
        </Space>
      </div>
      <Table dataSource={data} columns={[
        { title: '昵称', dataIndex: 'nickname', key: 'nickname' },
        { title: '手机号', dataIndex: 'phone', key: 'phone' },
        { title: '角色', dataIndex: 'role', key: 'role', render: (r: string) => <Tag color={r === 'admin' ? 'gold' : 'default'}>{r}</Tag> },
        { title: '状态', dataIndex: 'status', key: 'status', render: (s: string) => <Tag color={s === '正常' ? 'green' : 'red'}>{s}</Tag> },
        { title: '注册时间', dataIndex: 'createdAt', key: 'createdAt' },
        { title: '操作', key: 'action', render: () => <Button size="small" type="link" danger onClick={() => Modal.confirm({ title: '确认封禁', content: '确定要封禁该用户吗？', onOk: () => message.success('已封禁') })}>封禁</Button> },
      ]} pagination={{ pageSize: 10 }} size="small" />
    </AdminLayout>
  );
}
