import { useState } from 'react';
import { Bot, Send, X } from 'lucide-react';
import { api } from '../api.js';

export default function ChatAssistant() {
  const [open, setOpen] = useState(false);
  const [input, setInput] = useState('');
  const [busy, setBusy] = useState(false);
  const [messages, setMessages] = useState([
    { role: 'assistant', content: 'Ask about submitted reports, blockers, workload, or approvals.' }
  ]);

  async function send(event) {
    event.preventDefault();
    const text = input.trim();
    if (!text || busy) return;

    const nextMessages = [...messages, { role: 'user', content: text }];
    setMessages(nextMessages);
    setInput('');
    setBusy(true);
    try {
      const response = await api.assistantChat({ message: text, history: messages.slice(-8) });
      setMessages([...nextMessages, { role: 'assistant', content: response.answer }]);
    } catch (err) {
      setMessages([...nextMessages, { role: 'assistant', content: err.message }]);
    } finally {
      setBusy(false);
    }
  }

  if (!open) {
    return (
      <button className="chat-launcher" onClick={() => setOpen(true)} aria-label="Open AI assistant">
        <Bot size={22} />
      </button>
    );
  }

  return (
    <section className="chat-panel" aria-label="AI assistant">
      <header>
        <div><Bot size={18} /><strong>AI assistant</strong></div>
        <button onClick={() => setOpen(false)} aria-label="Close AI assistant"><X size={18} /></button>
      </header>
      <div className="chat-messages">
        {messages.map((message, index) => (
          <div className={`chat-message ${message.role}`} key={index}>{message.content}</div>
        ))}
        {busy && <div className="chat-message assistant">Thinking...</div>}
      </div>
      <form onSubmit={send} className="chat-input">
        <input value={input} onChange={(event) => setInput(event.target.value)} placeholder="Ask about team reports" />
        <button type="submit" aria-label="Send message"><Send size={18} /></button>
      </form>
    </section>
  );
}
