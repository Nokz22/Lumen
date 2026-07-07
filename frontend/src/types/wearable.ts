export type WearableReadingType = 'HEART_RATE' | 'HRV' | 'SLEEP_DURATION' | 'STEPS'

export type WearableSourceType = 'SIMULATOR'

export interface WearableReading {
  id: string
  type: WearableReadingType
  value: number
  recordedAt: string
  source: WearableSourceType
}

/** description is always an observation ("on days with X, you tended to..."), never a verdict. */
export interface CorrelationInsight {
  metric: WearableReadingType
  correlationCoefficient: number
  description: string
}
