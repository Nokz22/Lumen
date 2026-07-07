import { Client } from '@stomp/stompjs'
import { useEffect, useRef } from 'react'
import { API_BASE_URL } from '../../api/client'
import type { RecommendationNotification } from '../../types/recommendation'

const WS_URL = `${API_BASE_URL.replace(/^http/, 'ws')}/ws`

/**
 * Native WebSocket (no SockJS) — the access_token cookie is sent automatically on the
 * handshake because localhost:5173 and localhost:8099 share the same "site" for
 * SameSite cookie purposes, the same way the REST API's credentials: 'include' calls
 * already work cross-port in this project.
 */
export function useRecommendationSocket(
  onRecommendation: (notification: RecommendationNotification) => void,
) {
  const onRecommendationRef = useRef(onRecommendation)
  onRecommendationRef.current = onRecommendation

  useEffect(() => {
    const client = new Client({
      brokerURL: WS_URL,
      reconnectDelay: 5000,
    })

    client.onConnect = () => {
      client.subscribe('/user/queue/recommendations', (message) => {
        onRecommendationRef.current(JSON.parse(message.body) as RecommendationNotification)
      })
    }

    client.activate()

    return () => {
      client.deactivate()
    }
  }, [])
}
