import { useState, useRef, useEffect, useCallback } from 'react'
import { Bot, X, Send, MessageSquare, Loader2 } from 'lucide-react'
import { aiApi } from '../../api/api'
import toast from 'react-hot-toast'

// Helper for generating unique IDs safely
const generateId = () => {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID()
  }
  return Date.now().toString(36) + Math.random().toString(36).substring(2)
}

export default function AiAssistant() {
  const [isOpen, setIsOpen] = useState(false)
  const [messages, setMessages] = useState([
    { id: generateId(), role: 'assistant', content: 'Hi there! I am the DocQueue AI Assistant. How can I help you today?' }
  ])
  const [input, setInput] = useState('')
  const [isTyping, setIsTyping] = useState(false)
  const messagesEndRef = useRef(null)
  const abortControllerRef = useRef(null)

  // Auto-scroll to bottom of chat
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, isTyping])

  // Cleanup pending requests on unmount
  useEffect(() => {
    return () => {
      if (abortControllerRef.current) {
        abortControllerRef.current.abort()
      }
    }
  }, [])

  const handleSend = async (e) => {
    e.preventDefault()
    const trimmedInput = input.trim()
    if (!trimmedInput || isTyping) return

    const userMessage = { id: generateId(), role: 'user', content: trimmedInput }
    setMessages(prev => [...prev, userMessage])
    setInput('')
    setIsTyping(true)

    // Cancel previous request if any
    if (abortControllerRef.current) {
      abortControllerRef.current.abort()
    }
    abortControllerRef.current = new AbortController()

    try {
      // Pass the signal to axios
      const res = await aiApi.chat(userMessage.content, { signal: abortControllerRef.current.signal })
      const botReply = res?.data?.data?.reply || "I'm sorry, I couldn't process that response."
      setMessages(prev => [...prev, { id: generateId(), role: 'assistant', content: botReply }])
    } catch (err) {
      // Ignore abort errors
      if (err.name === 'CanceledError' || err.message === 'canceled') return

      if (err.response?.status !== 429) {
        toast.error('AI Assistant is currently unavailable. Please try again later.')
      }
    } finally {
      setIsTyping(false)
    }
  }

  if (!isOpen) {
    return (
      <button
        onClick={() => setIsOpen(true)}
        className="fixed bottom-6 right-6 w-14 h-14 bg-gradient-to-tr from-primary-600 to-primary-400 rounded-full flex items-center justify-center shadow-xl shadow-primary-500/30 hover:scale-110 transition-all duration-300 z-50 group"
      >
        <MessageSquare className="w-6 h-6 text-white group-hover:animate-pulse" />
      </button>
    )
  }

  return (
    <div className="fixed bottom-6 right-6 w-[380px] max-w-[calc(100vw-3rem)] h-[550px] max-h-[calc(100vh-3rem)] flex flex-col glass rounded-2xl shadow-2xl overflow-hidden z-50 animate-in fade-in slide-in-from-bottom-8 duration-300 border border-slate-700/50">
      
      {/* Header */}
      <div className="flex items-center justify-between p-4 bg-gradient-to-r from-slate-800 to-slate-900 border-b border-slate-700/50">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-full bg-primary-500/20 flex items-center justify-center">
            <Bot className="w-5 h-5 text-primary-400" />
          </div>
          <div>
            <h3 className="font-semibold text-white text-sm">DocQueue Assistant</h3>
            <div className="flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
              <p className="text-xs text-slate-400">Online</p>
            </div>
          </div>
        </div>
        <button 
          onClick={() => setIsOpen(false)}
          className="p-2 hover:bg-slate-700/50 rounded-lg text-slate-400 hover:text-white transition-colors"
        >
          <X className="w-5 h-5" />
        </button>
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto p-4 space-y-4 bg-slate-900/50 scrollbar-thin scrollbar-thumb-slate-700">
        {messages.map((msg) => (
          <div key={msg.id} className={`flex ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}>
            <div className={`max-w-[80%] rounded-2xl px-4 py-2.5 text-sm ${
              msg.role === 'user' 
                ? 'bg-primary-600 text-white rounded-br-none shadow-lg shadow-primary-900/20' 
                : 'bg-slate-800 border border-slate-700 text-slate-200 rounded-bl-none shadow-md shadow-black/20'
            }`}>
              {msg.content}
            </div>
          </div>
        ))}
        
        {isTyping && (
          <div className="flex justify-start">
            <div className="bg-slate-800 border border-slate-700 rounded-2xl rounded-bl-none px-4 py-3 flex items-center gap-1.5 shadow-md">
              <span className="w-2 h-2 bg-slate-500 rounded-full animate-bounce" style={{ animationDelay: '0ms' }} />
              <span className="w-2 h-2 bg-slate-500 rounded-full animate-bounce" style={{ animationDelay: '150ms' }} />
              <span className="w-2 h-2 bg-slate-500 rounded-full animate-bounce" style={{ animationDelay: '300ms' }} />
            </div>
          </div>
        )}
        <div ref={messagesEndRef} />
      </div>

      {/* Input */}
      <div className="p-4 bg-slate-900 border-t border-slate-700/50">
        <form onSubmit={handleSend} className="flex gap-2">
          <input
            type="text"
            value={input}
            maxLength={500}
            onChange={(e) => setInput(e.target.value)}
            placeholder="Type your message..."
            className="flex-1 bg-slate-800 border border-slate-700 rounded-xl px-4 py-2.5 text-sm text-white placeholder:text-slate-500 focus:outline-none focus:ring-2 focus:ring-primary-500/50 transition-shadow"
            disabled={isTyping}
          />
          <button 
            type="submit"
            disabled={!input.trim() || isTyping}
            className="w-10 h-10 rounded-xl bg-primary-600 hover:bg-primary-500 disabled:opacity-50 disabled:hover:bg-primary-600 flex items-center justify-center text-white transition-colors"
          >
            {isTyping ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4 ml-0.5" />}
          </button>
        </form>
      </div>
    </div>
  )
}
