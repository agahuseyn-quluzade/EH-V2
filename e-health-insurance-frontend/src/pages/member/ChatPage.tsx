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
          content: "Bağışlayın, cavab almaq mümkün olmadı. Yenidən cəhd edin.",
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
          <h1>AI Köməkçi</h1>
          <p>Sığorta ilə bağlı suallarınızı verin</p>
        </div>
        {sessionId && (
          <button className="btn btn-secondary btn-sm" onClick={startNew}>
            + Yeni söhbət
          </button>
        )}
      </div>

      <div className="chat-shell" style={{ display: "flex", flexDirection: "column", height: "60vh" }}>
        <div className="chat-main" style={{ flex: 1, display: "flex", flexDirection: "column" }}>
          <div className="chat-messages" style={{ flex: 1, overflowY: "auto", padding: "16px" }}>
            {messages.length === 0 && (
              <div className="empty-state">
                <div className="empty-icon">💬</div>
                <h3>Sual verin</h3>
                <p>
                  Məsələn: «Sığortam hansı xərclər üçün keçərlidir?» və ya «İddiam
                  niyə rədd edildi?»
                </p>
              </div>
            )}
            {messages.map((m, i) => (
              <div
                key={i}
                className={`chat-bubble ${m.role === "user" ? "chat-user" : "chat-assistant"}`}
              >
                {m.content}
                {m.createdAt && (
                  <div className="muted" style={{ fontSize: "0.72rem", marginTop: 4 }}>
                    {formatDateTime(m.createdAt)}
                  </div>
                )}
              </div>
            ))}
            {sending && (
              <div className="chat-bubble chat-assistant muted">Yazır...</div>
            )}
            <div ref={bottomRef} />
          </div>
          <form className="chat-input-row" onSubmit={onSend}>
            <input
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder="Mesajınızı yazın..."
              disabled={sending}
            />
            <button className="btn btn-primary" disabled={sending || !input.trim()}>
              Göndər
            </button>
          </form>
        </div>
      </div>
    </>
  );
}
