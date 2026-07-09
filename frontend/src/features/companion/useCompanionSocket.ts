import { Client } from '@stomp/stompjs'
import { useEffect, useRef } from 'react'
import { API_BASE_URL } from '../../api/client'
import type { CompanionStreamMessage } from '../../types/companion'

const WS_URL = `${API_BASE_URL.replace(/^http/, 'ws')}/ws`

/** Same connection pattern as useRecommendationSocket (Fase 4) — one native STOMP
 * client per mount, subscribed to this user's private companion queue. */
export function useCompanionSocket(onMessage: (message: CompanionStreamMessage) => void) {
  const onMessageRef = useRef(onMessage)
  onMessageRef.current = onMessage

  useEffect(() => {
    const client = new Client({
      brokerURL: WS_URL,
      reconnectDelay: 5000,
    })

    client.onConnect = () => {
      client.subscribe('/user/queue/companion', (message) => {
        onMessageRef.current(JSON.parse(message.body) as CompanionStreamMessage)
      })
    }

    client.activate()

    return () => {
      client.deactivate()
    }
  }, [])
}
