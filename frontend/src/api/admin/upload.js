import request from '../request'

export function uploadImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/admin/uploads/image', formData)
}
