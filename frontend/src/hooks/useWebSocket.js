import { useEffect, useRef, useCallback } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

/**
 * WebSocket hook for subscribing to live queue updates.
 *
 * @param {string} topic     - STOMP topic e.g. '/topic/queue/5'
 * @param {function} onMessage - Callback receiving the parsed message
 */
export function useWebSocket(topic, onMessage) {
  const clientRef   = useRef(null)
  const callbackRef = useRef(onMessage)

  // Keep callback ref current without resubscribing
  useEffect(() => { callbackRef.current = onMessage }, [onMessage])

  const connect = useCallback(() => {
    if (clientRef.current?.active) return

    const token = localStorage.getItem('accessToken')

    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe(topic, (frame) => {
          try {
            const msg = JSON.parse(frame.body)
            callbackRef.current?.(msg)
          } catch (e) {
            console.warn('WS parse error', e)
          }
        })
      },
      onStompError: (frame) => {
        console.error('STOMP error', frame)
      },
    })

    client.activate()
    clientRef.current = client
  }, [topic])

  useEffect(() => {
    if (!topic) return
    connect()
    return () => {
      clientRef.current?.deactivate()
      clientRef.current = null
    }
  }, [topic, connect])
}
