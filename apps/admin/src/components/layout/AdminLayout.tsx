'use client';
import React, { useState } from 'react';
import { Layout, Menu, theme } from 'antd';
import { DashboardOutlined, UserOutlined, ApiOutlined, TeamOutlined, AppstoreOutlined, BellOutlined, SettingOutlined } from '@ant-design/icons';
import { useRouter, usePathname } from 'next/navigation';

const { Header, Sider, Content } = Layout;
const menuItems = [
  { key: '/dashboard', icon: <DashboardOutlined />, label: '仪表盘' },
  { key: '/users', icon: <UserOutlined />, label: '用户管理' },
  { key: '/sources', icon: <ApiOutlined />, label: '源管理' },
  { key: '/community', icon: <TeamOutlined />, label: '社区管理' },
  { key: '/versions', icon: <AppstoreOutlined />, label: '版本管理' },
  { key: '/announcements', icon: <BellOutlined />, label: '公告管理' },
  { key: '/config', icon: <SettingOutlined />, label: '系统配置' },
];

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const [collapsed, setCollapsed] = useState(false);
  const router = useRouter();
  const pathname = usePathname();
  const { token: { colorBgContainer } } = theme.useToken();

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider collapsible collapsed={collapsed} onCollapse={setCollapsed} style={{ background: '#1a1a2e' }}>
        <div style={{ height: 64, display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff', fontSize: collapsed ? 16 : 20, fontWeight: 'bold' }}>
          {collapsed ? '漫' : '漫界管理'}
        </div>
        <Menu theme="dark" mode="inline" selectedKeys={[pathname]}
          items={menuItems} onClick={({ key }) => router.push(key)}
          style={{ background: 'transparent' }} />
      </Sider>
      <Layout>
        <Header style={{ padding: '0 24px', background: colorBgContainer, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <h2 style={{ margin: 0 }}>{menuItems.find(m => m.key === pathname)?.label || '管理后台'}</h2>
          <div><span>管理员</span><button onClick={() => {}} style={{ marginLeft: 12, background: 'none', border: 'none', color: '#ff4d4f', cursor: 'pointer' }}>退出</button></div>
        </Header>
        <Content style={{ margin: 24, padding: 24, background: colorBgContainer, borderRadius: 8 }}>{children}</Content>
      </Layout>
    </Layout>
  );
}
