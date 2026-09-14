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
