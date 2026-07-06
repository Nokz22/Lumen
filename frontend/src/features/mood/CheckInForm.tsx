import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useSubmitMoodCheckIn } from './useMoodCheckIns'
import { MOOD_EMOTIONS, type MoodEmotion } from '../../types/mood'
import { DEMO_USER_ID } from '../../config/demoUser'

export function CheckInForm() {
  const { t } = useTranslation()
  const [emotion, setEmotion] = useState<MoodEmotion>('NEUTRAL')
  const [energyLevel, setEnergyLevel] = useState(3)
  const [sleepHours, setSleepHours] = useState(7)
  const [sleepQuality, setSleepQuality] = useState(3)
  const [note, setNote] = useState('')

  const submitCheckIn = useSubmitMoodCheckIn(DEMO_USER_ID)

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault()
    submitCheckIn.mutate({ emotion, energyLevel, sleepHours, sleepQuality, note: note || undefined })
  }

  return (
    <form
      onSubmit={handleSubmit}
      className="flex flex-col gap-5 rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] p-6"
    >
      <h2 className="text-lg font-medium">{t('checkin.title')}</h2>

      <fieldset className="flex flex-col gap-2">
        <legend className="mb-1 text-sm text-[var(--color-text-muted)]">
          {t('checkin.emotionLabel')}
        </legend>
        <div className="flex flex-wrap gap-2">
          {MOOD_EMOTIONS.map((option) => (
            <button
              key={option}
              type="button"
              onClick={() => setEmotion(option)}
              aria-pressed={emotion === option}
              className={`rounded-full border px-4 py-2 text-sm transition-colors ${
                emotion === option
                  ? 'border-[var(--color-accent)] bg-[var(--color-accent)] text-[var(--color-accent-contrast)]'
                  : 'border-[var(--color-border)]'
              }`}
            >
              {t(`checkin.emotion.${option}`)}
            </button>
          ))}
        </div>
      </fieldset>

      <label className="flex flex-col gap-1 text-sm">
        {t('checkin.energyLabel')}: {energyLevel}
        <input
          type="range"
          min={1}
          max={5}
          value={energyLevel}
          onChange={(event) => setEnergyLevel(Number(event.target.value))}
        />
      </label>

      <div className="flex gap-4">
        <label className="flex flex-1 flex-col gap-1 text-sm">
          {t('checkin.sleepHoursLabel')}
          <input
            type="number"
            min={0}
            max={24}
            step={0.5}
            value={sleepHours}
            onChange={(event) => setSleepHours(Number(event.target.value))}
            className="rounded-lg border border-[var(--color-border)] bg-transparent px-3 py-2"
          />
        </label>

        <label className="flex flex-1 flex-col gap-1 text-sm">
          {t('checkin.sleepQualityLabel')}: {sleepQuality}
          <input
            type="range"
            min={1}
            max={5}
            value={sleepQuality}
            onChange={(event) => setSleepQuality(Number(event.target.value))}
          />
        </label>
      </div>

      <label className="flex flex-col gap-1 text-sm">
        {t('checkin.noteLabel')}
        <textarea
          value={note}
          onChange={(event) => setNote(event.target.value)}
          placeholder={t('checkin.notePlaceholder')}
          maxLength={1000}
          rows={3}
          className="rounded-lg border border-[var(--color-border)] bg-transparent px-3 py-2"
        />
      </label>

      <button
        type="submit"
        disabled={submitCheckIn.isPending}
        className="rounded-full bg-[var(--color-accent)] px-5 py-2.5 text-[var(--color-accent-contrast)] disabled:opacity-60"
      >
        {t('checkin.submit')}
      </button>

      {submitCheckIn.isSuccess && <p role="status">{t('checkin.success')}</p>}
      {submitCheckIn.isError && (
        <p role="alert" className="text-red-500">
          {t('checkin.error')}
        </p>
      )}
    </form>
  )
}
