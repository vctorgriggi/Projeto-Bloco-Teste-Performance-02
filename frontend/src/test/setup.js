// matchers de dom para o expect (toBeInTheDocument, toHaveTextContent, ...) e limpeza
// do dom entre um teste e outro
import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

afterEach(() => {
  cleanup()
  localStorage.clear()
})
