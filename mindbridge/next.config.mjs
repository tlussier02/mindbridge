/** @type {import('next').NextConfig} */
const nextConfig = {
<<<<<<< HEAD
  typescript: {
    ignoreBuildErrors: true,
  },
  images: {
    unoptimized: true,
=======
  typescript: { ignoreBuildErrors: true },
  images: { unoptimized: true },

  async rewrites() {
    const target =
      process.env.API_PROXY_TARGET || "http://backend:8080"

    return [
      {
        source: "/auth/:path*",
        destination: `${target}/auth/:path*`,
      },
      {
        source: "/sessions/:path*",
        destination: `${target}/sessions/:path*`,
      },
      {
        source: "/diary/:path*",
        destination: `${target}/diary/:path*`,
      },
      {
        source: "/progress/:path*",
        destination: `${target}/progress/:path*`,
      },
      {
        source: "/crisis/:path*",
        destination: `${target}/crisis/:path*`,
      },
      {
        source: "/actuator/:path*",
        destination: `${target}/actuator/:path*`,
      },
    ]
>>>>>>> 2bb2ef62b9902fd4c36412ff39432e6f45bb2bf3
  },
}

export default nextConfig
