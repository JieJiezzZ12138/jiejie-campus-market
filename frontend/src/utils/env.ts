const trimTrailingSlash = (value = ''): string => value.replace(/\/+$/, '')

const API_BASE = trimTrailingSlash(import.meta.env.VITE_APP_BASE_API || 'http://localhost:8080')
const IMAGE_BASE = trimTrailingSlash(
  import.meta.env.VITE_APP_IMAGE_BASE_URL || API_BASE.replace(/\/api$/, '')
)

export const uploadActionUrl = `${API_BASE}/product/upload`

export const resolveImageUrl = (url: string): string => {
  if (!url) return ''
  let clean = String(url).trim()
  while ((clean.startsWith('"') && clean.endsWith('"')) || (clean.startsWith("'") && clean.endsWith("'"))) {
    if (clean.length < 2) break
    clean = clean.slice(1, -1).trim()
  }
  if (!clean) return ''
  url = clean
  if (/^https?:\/\//i.test(url)) {
    try {
      const parsed = new URL(url)
      if (parsed.hostname === 'localhost' || parsed.hostname === '127.0.0.1') {
        return `${IMAGE_BASE}${parsed.pathname}${parsed.search}`
      }
    } catch (_e) {
      return url
    }
    return url
  }
  const normalized = url.startsWith('/') ? url : `/${url}`
  if (normalized.startsWith('/images/')) {
    return `${API_BASE}${normalized}`
  }
  return `${IMAGE_BASE}${normalized}`
}
