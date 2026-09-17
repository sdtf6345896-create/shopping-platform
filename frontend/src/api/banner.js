import request from './request'

export function getBanners() {
  return request.get('/banners')
}
