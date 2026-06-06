const CATEGORY_MAP = {
  1: '数码家电',
  2: '图书文具',
  3: '家居用品',
  4: '母婴儿童',
  5: '运动户外',
  6: '服饰鞋包'
}

const isGarbled = (str) => {
  if (!str || typeof str !== 'string') return true
  for (let i = 0; i < str.length; i++) {
    const code = str.charCodeAt(i)
    if ((code >= 0xC0 && code <= 0xFF) || (code >= 0x00C0 && code <= 0x00FF)) {
      if (str.includes('Ã') || str.includes('Â') || str.includes('æ') || str.includes('ç')) {
        return true
      }
    }
  }
  return false
}

export const getCategoryName = (item) => {
  if (!item) return '其他'
  if (item.categoryId && CATEGORY_MAP[item.categoryId]) {
    return CATEGORY_MAP[item.categoryId]
  }
  if (item.categoryName && !isGarbled(item.categoryName)) {
    return item.categoryName
  }
  return '其他'
}

export const getCategoryNameById = (categoryId) => {
  return CATEGORY_MAP[categoryId] || '其他'
}

export default {
  getCategoryName,
  getCategoryNameById
}
