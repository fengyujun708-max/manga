'use client';
import React from 'react';
import { Card, Form, Switch, Input, Button, message, Divider } from 'antd';
import AdminLayout from '@/components/layout/AdminLayout';

export default function ConfigPage() {
  const [form] = Form.useForm();
  return (
    <AdminLayout>
      <h2>系统配置</h2>
      <Card style={{ maxWidth: 600, marginTop: 16 }}>
        <Form form={form} layout="vertical" initialValues={{
          registrationEnabled: true, communityEnabled: true,
          sourceRegistryUrl: 'https://source.manjie.xxx/registry/index.json',
          defaultReaderMode: 'webtoon', maxDownloads: 3, maintenanceMode: false,
        }}>
          <Form.Item name="registrationEnabled" label="开放注册" valuePropName="checked"><Switch /></Form.Item>
          <Form.Item name="communityEnabled" label="社区功能" valuePropName="checked"><Switch /></Form.Item>
          <Form.Item name="sourceRegistryUrl" label="源注册表地址"><Input /></Form.Item>
          <Form.Item name="defaultReaderMode" label="默认阅读模式"><Input /></Form.Item>
          <Form.Item name="maxDownloads" label="最大下载数"><Input type="number" /></Form.Item>
          <Divider />
          <Form.Item name="maintenanceMode" label="维护模式" valuePropName="checked"><Switch /></Form.Item>
          <Form.Item name="maintenanceMessage" label="维护提示"><Input.TextArea rows={2} /></Form.Item>
          <Button type="primary" onClick={() => { form.validateFields().then(() => message.success('已保存')); }}>保存配置</Button>
        </Form>
      </Card>
    </AdminLayout>
  );
}
