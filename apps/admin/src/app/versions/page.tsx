'use client';
import React, { useState } from 'react';
import { Card, Table, Tag, Button, Space, Modal, Form, Input, Switch, message } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import AdminLayout from '@/components/layout/AdminLayout';

export default function VersionsPage() {
  const [visible, setVisible] = useState(false);
  const [form] = Form.useForm();
  const data = [
    { key: '1', version: '2.0.0', buildNumber: 10, platform: 'android', status: 'latest', forceUpdate: true, createdAt: '2025-01-15' },
    { key: '2', version: '1.9.0', buildNumber: 9, platform: 'android', status: 'optional', forceUpdate: false, createdAt: '2025-01-01' },
  ];

  return (
    <AdminLayout>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2>版本管理</h2>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setVisible(true)}>添加版本</Button>
      </div>
      <Table dataSource={data} columns={[
        { title: '版本号', dataIndex: 'version', key: 'version' },
        { title: '构建号', dataIndex: 'buildNumber', key: 'buildNumber' },
        { title: '平台', dataIndex: 'platform', key: 'platform', render: (p: string) => <Tag>{p}</Tag> },
        { title: '状态', dataIndex: 'status', key: 'status', render: (s: string) => <Tag color={s === 'latest' ? 'green' : 'blue'}>{s === 'latest' ? '最新' : '可选'}</Tag> },
        { title: '强制更新', dataIndex: 'forceUpdate', key: 'forceUpdate', render: (f: boolean) => f ? <Tag color="red">强制</Tag> : <Tag>可选</Tag> },
        { title: '发布时间', dataIndex: 'createdAt', key: 'createdAt' },
      ]} pagination={false} size="small" />
      <Modal title="添加版本" open={visible} onCancel={() => setVisible(false)} onOk={() => { form.validateFields().then(() => { message.success('已添加'); setVisible(false); }); }}>
        <Form form={form} layout="vertical">
          <Form.Item name="version" label="版本号" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="platform" label="平台" rules={[{ required: true }]}><Input placeholder="android" /></Form.Item>
          <Form.Item name="forceUpdate" label="强制更新" valuePropName="checked"><Switch /></Form.Item>
          <Form.Item name="changelog" label="更新日志"><Input.TextArea rows={3} /></Form.Item>
        </Form>
      </Modal>
    </AdminLayout>
  );
}
