import Compressor from 'compressorjs'

export const compressImage = (file, options = {}) => {
  return new Promise((resolve, reject) => {
    new Compressor(file, {
      quality: 0.8,
      maxWidth: 1200,
      maxHeight: 1200,
      convertSize: 500000,
      ...options,
      success(result) {
        resolve(result)
      },
      error(err) {
        reject(err)
      }
    })
  })
}

export const compressImages = async (files, options = {}) => {
  const compressedFiles = []
  for (const file of files) {
    if (file.type.startsWith('image/')) {
      const compressed = await compressImage(file, options)
      compressedFiles.push(compressed)
    } else {
      compressedFiles.push(file)
    }
  }
  return compressedFiles
}
