/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  async rewrites() {
    // Proxy HTTP requests during development to the local Micronaut backend.
    // Note: WebSockets are not proxied by Next rewrites; connect directly to ws://localhost:7090.
    return [
      {
        source: "/eidolon/:path*",
        destination: "http://localhost:7090/eidolon/:path*"
      }
    ];
  }
};

export default nextConfig;
