// pequenos rabiscos svg desenhados a mao livre. servem so de enfeite, por isso
// ficam marcados como aria-hidden para nao atrapalhar leitores de tela.

export function CoffeeDoodle({ className }) {
  return (
    <svg className={className} viewBox="0 0 64 64" fill="none" aria-hidden="true">
      <path d="M12 26c-1 12 4 24 18 24s19-12 18-24" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" />
      <path d="M10 26h44" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" />
      <path d="M50 30c6-2 9 6 3 9-2 1-4 1-5 0" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" />
      <path d="M24 8c-2 4 2 6 0 10M34 6c-2 5 2 7 0 12" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
    </svg>
  )
}

export function Squiggle({ className }) {
  return (
    <svg className={className} viewBox="0 0 120 12" fill="none" aria-hidden="true" preserveAspectRatio="none">
      <path d="M2 6c10-7 20 7 30 0s20-7 30 0 20 7 28 0" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" />
    </svg>
  )
}

export function LeafDoodle({ className }) {
  return (
    <svg className={className} viewBox="0 0 40 40" fill="none" aria-hidden="true">
      <path d="M8 32C8 16 22 8 34 8c2 14-10 26-26 24Z" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
      <path d="M30 12C22 18 14 24 10 30" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
    </svg>
  )
}

// rabiscos das reacoes. cada um corresponde a um tipo que o microsservico de
// engajamento conhece (CORACAO, CAFE, IDEIA), no mesmo traco dos outros desenhos.

export function HeartDoodle({ className }) {
  return (
    <svg className={className} viewBox="0 0 32 32" fill="none" aria-hidden="true">
      <path
        d="M16 27C9 22 4 17.5 4 12.5A6.5 6.5 0 0 1 16 9a6.5 6.5 0 0 1 12 3.5C28 17.5 23 22 16 27Z"
        stroke="currentColor"
        strokeWidth="2.2"
        strokeLinecap="round"
      />
    </svg>
  )
}

export function CupDoodle({ className }) {
  return (
    <svg className={className} viewBox="0 0 32 32" fill="none" aria-hidden="true">
      <path d="M6 12c-.6 8 2 14 10 14s10.6-6 10-14" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" />
      <path d="M5 12h22" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" />
      <path d="M25 15c3.4-1 5 3.6 1.6 5.2" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
      <path d="M13 3c-1.2 2.4 1 3.6 0 6M18 2c-1.2 3 1.2 4.2 0 7" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
    </svg>
  )
}

export function BulbDoodle({ className }) {
  return (
    <svg className={className} viewBox="0 0 32 32" fill="none" aria-hidden="true">
      <path
        d="M16 4a8 8 0 0 0-4.6 14.6c.7.5 1.1 1.3 1.1 2.1V22h7v-1.3c0-.8.4-1.6 1.1-2.1A8 8 0 0 0 16 4Z"
        stroke="currentColor"
        strokeWidth="2.2"
        strokeLinecap="round"
      />
      <path d="M13 25h6M14 28h4" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
    </svg>
  )
}
