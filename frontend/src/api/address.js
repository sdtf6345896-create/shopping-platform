import request from './request'

export function listAddresses() {
  return request.get('/members/addresses')
}

export function createAddress(data) {
  return request.post('/members/addresses', data)
}

export function updateAddress(id, data) {
  return request.put(`/members/addresses/${id}`, data)
}

export function deleteAddress(id) {
  return request.delete(`/members/addresses/${id}`)
}
