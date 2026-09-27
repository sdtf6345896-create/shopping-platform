import request from './request'

// 會員上傳圖片(評論照片),回傳 { url }
export function uploadMemberImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/uploads/image', formData, { timeout: 30000 })
}
