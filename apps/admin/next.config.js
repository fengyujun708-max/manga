/** @type {import('next').NextConfig} */
const nextConfig = {
  output: 'standalone',
  async rewrites() {
    return [{ source: '/api/:path*', destination: 'http://localhost:3000/v1/:path*' }];
  },
};
module.exports = nextConfig;
