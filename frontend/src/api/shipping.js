import request from './request'

// { fee, freeThreshold }
export function getShippingPolicy() {
  return request.get('/shipping/policy')
}
