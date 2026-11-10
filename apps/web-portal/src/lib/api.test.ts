import { describe, expect, it } from 'vitest'
import { ApiError, describeError, identityHeaders } from './api'

describe('identityHeaders', () => {
  it('sends subject, roles and the data scope', () => {
    const h = identityHeaders({
      subject: 'abc',
      roles: ['SECURITY_GUARD', 'HR_STAFF'],
      factories: 'BD',
      gates: 'BD-GATE-01,BD-GATE-02',
    })
    expect(h['X-Subject']).toBe('abc')
    expect(h['X-Roles']).toBe('SECURITY_GUARD,HR_STAFF')
    expect(h['X-Scope-Factories']).toBe('BD')
    expect(h['X-Scope-Gates']).toBe('BD-GATE-01,BD-GATE-02')
  })

  it('omits empty scope headers instead of sending blanks', () => {
    const h = identityHeaders({ subject: 'abc', roles: ['EMPLOYEE'], factories: '', gates: '' })
    expect(h).not.toHaveProperty('X-Scope-Factories')
    expect(h).not.toHaveProperty('X-Scope-Gates')
  })
})

describe('describeError', () => {
  it('explains a 403 in plain language', () => {
    expect(describeError(new ApiError(403, null))).toContain('403')
  })

  it('does not leak server internals for other statuses', () => {
    expect(describeError(new ApiError(500, { trace: 'secret stack' }))).not.toContain('secret')
  })

  it('handles network failures', () => {
    expect(describeError(new TypeError('failed to fetch'))).toContain('kết nối')
  })
})
