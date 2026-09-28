export function flattenCategories(tree) {
  const result = []
  function walk(list, depth) {
    for (const item of list) {
      result.push({ id: item.id, name: item.name, depth })
      if (item.children?.length) walk(item.children, depth + 1)
    }
  }
  walk(tree, 0)
  return result
}

// 從分類樹找出某分類與其所有上層分類的 id(找不到回傳空陣列)
export function categoryPath(tree, id) {
  for (const node of tree || []) {
    if (node.id === id) return [node.id]
    const sub = categoryPath(node.children, id)
    if (sub.length) return [node.id, ...sub]
  }
  return []
}
