import request from './request'

// 會員常用的超商取貨門市
export function listCvsStores() {
  return request.get('/members/cvs-stores')
}

export function saveCvsStore(store) {
  return request.post('/members/cvs-stores', store)
}

export function deleteCvsStore(id) {
  return request.delete(`/members/cvs-stores/${id}`)
}
