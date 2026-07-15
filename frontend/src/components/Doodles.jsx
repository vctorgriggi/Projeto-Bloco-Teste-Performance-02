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
