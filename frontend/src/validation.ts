export function validateThreshold(text: string): number | string {
  const value = text.trim()
  if (!/^\d+(?:\.\d+)?$/.test(value)) {
    return 'Enter a positive number, such as 1.84.'
  }

  const number = Number(value)
  if (!Number.isFinite(number) || number <= 0) {
    return 'Enter a positive number, such as 1.84.'
  }
  return number
}
