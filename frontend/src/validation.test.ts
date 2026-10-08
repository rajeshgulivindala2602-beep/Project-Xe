import { describe, expect, it } from 'vitest'
import { validateThreshold } from './validation'

describe('validateThreshold', () => {
  it('accepts positive decimal values and surrounding whitespace', () => {
    expect(validateThreshold('1.84')).toBe(1.84)
    expect(validateThreshold(' 2 ')).toBe(2)
  })

  it.each(['', 'abc', '-1', '0', '1,84'])('rejects %j', (text) => {
    expect(validateThreshold(text)).toBeTypeOf('string')
  })
})
