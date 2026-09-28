// 比較表的規格列:所有商品規格項目的聯集(依第一次出現的順序),沒有該項目的商品顯示「-」
export function buildSpecRows(products) {
  const names = []
  for (const product of products) {
    for (const spec of product.specs || []) {
      if (!names.includes(spec.name)) names.push(spec.name)
    }
  }
  return names.map((name) => ({
    name,
    values: products.map((p) => (p.specs || []).find((s) => s.name === name)?.value ?? '-'),
    // 各商品內容都相同的列,畫面上可以淡化
    same: new Set(products.map((p) => (p.specs || []).find((s) => s.name === name)?.value ?? '-')).size === 1,
  }))
}
