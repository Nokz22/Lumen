export type MoodEmotion = 'HAPPY' | 'SAD' | 'ANGRY' | 'ANXIOUS' | 'FRUSTRATED' | 'NEUTRAL'

export const MOOD_EMOTIONS: MoodEmotion[] = [
  'HAPPY',
  'SAD',
  'ANGRY',
  'ANXIOUS',
  'FRUSTRATED',
  'NEUTRAL',
]

export interface MoodCheckInRequest {
  emotion: MoodEmotion
  energyLevel: number
  sleepHours: number
  sleepQuality: number
  note?: string
}

export interface MoodCheckInResponse {
  id: string
  emotion: MoodEmotion
  energyLevel: number
  sleepHours: number
  sleepQuality: number
  note: string | null
  checkInDate: string
  createdAt: string
}
