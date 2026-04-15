const trimTrailingSlash = (value = '') => value.replace(/\/+$/, '')

const API_BASE = trimTrailingSlash(import.meta.env.VITE_APP_BASE_API || 'http://localhost:8080')
const IMAGE_BASE = trimTrailingSlash(
  import.meta.env.VITE_APP_IMAGE_BASE_URL || API_BASE.replace(/\/api$/, '')
)

export const uploadActionUrl = `${API_BASE}/product/upload`

export const resolveImageUrl = (url) => {
  if (!url) return ''
  if (/^https?:\/\//i.test(url)) {
    try {
      const parsed = new URL(url)
      if (parsed.hostname === 'localhost' || parsed.hostname === '127.0.0.1') {
        return `${IMAGE_BASE}${parsed.pathname}${parsed.search}`
      }
    } catch (_) {
      return url
    }
    return url
  }
  return `${IMAGE_BASE}${url.startsWith('/') ? url : `/${url}`}`
}
