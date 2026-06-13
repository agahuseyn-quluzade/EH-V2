import { FormEvent, useEffect, useRef, useState } from "react";
import { aiApi } from "../../api/ai";
import { extractError } from "../../api/client";
import { useToast } from "../../context/ToastContext";
import { formatDateTime } from "../../utils/format";

interface LocalMessage {
  role: "user" | "assistant";
  content: string;
  createdAt?: string;
}

function cleanText(text: string) {
  return text
    .replace(/\*\*([^*]+)\*\*/g, "$1")
    .replace(/\*([^*]+)\*/g, "$1");
}

export function ChatPage() {
  const toast = useToast();

  const [sessionId, setSessionId] = useState<string | null>(null);
  const [messages, setMessages] = useState<LocalMessage[]>([]);
  const [input, setInput] = useState("");
  const [sending, setSending] = useState(false);
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  const startNew = () => {
    setSessionId(null);
    setMessages([]);
  };

  const onSend = async (e: FormEvent) => {
    e.preventDefault();
    const content = input.trim();
    if (!content || sending) return;
    setInput("");
    setMessages((prev) => [...prev, { role: "user", content }]);
    setSending(true);
    try {
      const res = await aiApi.chat({ message: content, sessionId });
      setSessionId(res.sessionId);
      setMessages((prev) => [
        ...prev,
        { role: "assistant", content: res.reply, createdAt: res.timestamp },
      ]);
    } catch (err) {
      toast.error(extractError(err));
      setMessages((prev) => [
        ...prev,
        {
          role: "assistant",
          content: "Sorry, the response could not be retrieved. Please try again.",
        },
      ]);
    } finally {
      setSending(false);
    }
  };

  return (
    <>
      <div className="page-header">
        <div>
          <h1>AI Assistant</h1>
        </div>
        {sessionId && (
          <button className="btn btn-secondary btn-sm" onClick={startNew}>
            + New chat
          </button>
        )}
      </div>

      <div className="chat-shell" style={{ display: "flex", flexDirection: "column", height: "60vh" }}>
        <div className="chat-main" style={{ flex: 1, display: "flex", flexDirection: "column" }}>
          <div className="chat-messages" style={{ flex: 1, overflowY: "auto", padding: "16px" }}>
            {messages.length === 0 && (
              <div className="empty-state">
                <div className="empty-icon">💬</div>
                <h3>Ask a question</h3>
                <p>
                  For example: "What expenses does my policy cover?" or "Why was
                  my claim rejected?"
                </p>
              </div>
            )}
            {messages.map((m, i) => (
              <div
                key={i}
                className={`chat-bubble ${m.role === "user" ? "chat-user" : "chat-assistant"}`}
              >
                {m.role === "assistant" ? cleanText(m.content) : m.content}
                {m.createdAt && (
                  <div className="muted" style={{ fontSize: "0.72rem", marginTop: 4 }}>
                    {formatDateTime(m.createdAt)}
                  </div>
                )}
              </div>
            ))}
            {sending && (
              <div className="chat-bubble chat-assistant muted">Typing...</div>
            )}
            <div ref={bottomRef} />
          </div>
          <form className="chat-input-row" onSubmit={onSend}>
            <input
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder="Type your message..."
              disabled={sending}
            />
            <button className="btn btn-primary" disabled={sending || !input.trim()}>
              Send
            </button>
          </form>
        </div>
      </div>
    </>
  );
}
