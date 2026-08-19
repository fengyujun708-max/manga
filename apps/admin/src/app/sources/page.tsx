'use client';
import React, { useState } from 'react';
import { Card, Table, Tag, Button, Space, Switch, Modal, message } from 'antd';
import { CheckCircleOutlined, CloseCircleOutlined } from '@ant-design/icons';
import AdminLayout from '@/components/layout/AdminLayout';

export default function SourcesPage() {
  const [testVisible, setTestVisible] = useState(false);
  const data = [
    { key: '1', name: '哔咔漫画', version: '2.1.0', status: 'active', downloads: 15230, lastTest: '通过' },
    { key: '2', name: '禁漫天堂', version: '3.0.5', status: 'active', downloads: 12890, lastTest: '通过' },
    { key: '3', name: 'E-Hentai', version: '1.5.0', status: 'inactive', downloads: 8760, lastTest: '失败' },
  ];

  return (
    <AdminLayout>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2>源管理</h2>
        <Space>
          <Button type="primary" onClick={() => setTestVisible(true)}>测试全部源</Button>
          <Button>同步上游</Button>
        </Space>
      </div>
      <Table dataSource={data} columns={[
        { title: '源名称', dataIndex: 'name', key: 'name' },
        { title: '版本', dataIndex: 'version', key: 'version' },
        { title: '状态', dataIndex: 'status', key: 'status', render: (s: string) => {
          const m: Record<string, any> = { active: { color: 'green', text: '活跃' }, inactive: { color: 'red', text: '已禁用' } };
          return <Tag color={m[s]?.color}>{m[s]?.text || s}</Tag>;
        }},
        { title: '下载量', dataIndex: 'downloads', key: 'downloads' },
        { title: '上次测试', dataIndex: 'lastTest', key: 'lastTest', render: (t: string) => <Tag icon={t === '通过' ? <CheckCircleOutlined /> : <CloseCircleOutlined />} color={t === '通过' ? 'success' : 'error'}>{t}</Tag> },
        { title: '操作', key: 'action', render: () => <Space><Button size="small">测试</Button><Switch size="small" defaultChecked /></Space> },
      ]} pagination={false} size="small" />
      <Modal title="测试结果" open={testVisible} onCancel={() => setTestVisible(false)} footer={null}>
        <div>连接测试: <Tag color="success">✓</Tag></div>
        <div>搜索测试: <Tag color="success">✓</Tag></div>
        <div>详情测试: <Tag color="success">✓</Tag></div>
      </Modal>
    </AdminLayout>
  );
}
