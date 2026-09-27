import request from '../request'

// params: { answered?: boolean, page, size }
export function listAdminQuestions(params) {
  return request.get('/admin/questions', { params })
}

export function answerQuestion(id, answer) {
  return request.put(`/admin/questions/${id}/answer`, { answer })
}

export function deleteQuestion(id) {
  return request.delete(`/admin/questions/${id}`)
}
