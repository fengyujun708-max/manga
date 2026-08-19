'use client';
import React, { useState } from 'react';
import { Card, Table, Tag, Button, Space, Modal, Form, Input, Switch, message } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import AdminLayout from '@/components/layout/AdminLayout';

export default function AnnouncementsPage() {
  const [visible, setVisible] = useState(false);
  const [form] = Form.useForm();
  const data = [
    { key: '1', title: '新版本上线通知', priority: 'important', status: 'active', startAt: '2025-01-15', endAt: '2025-02-15' },
    { key: '2', title: '系统维护公告', priority: 'maintenance', status: 'active', startAt: '2025-01-10', endAt: '2025-01-11' },
  ];

  return (
    <AdminLayout>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2>公告管理</h2>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setVisible(true)}>发布公告</Button>
      </div>
      <Table dataSource={data} columns={[
        { title: '标题', dataIndex: 'title', key: 'title' },
        { title: '优先级', dataIndex: 'priority', key: 'priority', render: (p: string) => <Tag color={p === 'important' ? 'red' : 'orange'}>{p === 'important' ? '重要' : '维护'}</Tag> },
        { title: '状态', dataIndex: 'status', key: 'status', render: (s: string) => <Tag color={s === 'active' ? 'green' : 'default'}>{s === 'active' ? '已发布' : '已下架'}</Tag> },
        { title: '开始时间', dataIndex: 'startAt', key: 'startAt' },
        { title: '结束时间', dataIndex: 'endAt', key: 'endAt' },
        { title: '操作', key: 'action', render: () => <Button size="small" type="link" danger>下架</Button> },
      ]} pagination={false} size="small" />
      <Modal title="发布公告" open={visible} onCancel={() => setVisible(false)} onOk={() => { form.validateFields().then(() => { message.success('已发布'); setVisible(false); }); }}>
        <Form form={form} layout="vertical">
          <Form.Item name="title" label="标题" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="content" label="内容" rules={[{ required: true }]}><Input.TextArea rows={4} /></Form.Item>
          <Form.Item name="priority" label="优先级"><Input placeholder="normal / important / maintenance" /></Form.Item>
          <Form.Item name="isForceRead" label="强制阅读" valuePropName="checked"><Switch /></Form.Item>
        </Form>
      </Modal>
    </AdminLayout>
  );
}
