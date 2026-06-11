import { FormEvent, KeyboardEvent, useEffect, useRef, useState } from "react";
import { aiApi, extractError } from "../../api";
import { ChatMessage } from "../../types";
import { useToast } from "../../context/ToastContext";

export function ChatPage() {
  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      role: "ASSISTANT",
      content:
        "Hi! I'm your HealthAssure AI assistant. I can help you understand your coverage, check claim status, or pick a plan. What would you like to know?",
      createdAt: new Date().toISOString(),
    },
  ]);
  const [conversationId, setConversationId] = useState<string | undefined>();
  const [input, setInput] = useState("");
  const [busy, setBusy] = useState(false);
  const toast = useToast();
  const streamRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (streamRef.current) {
      streamRef.current.scrollTop = streamRef.current.scrollHeight;
    }
  }, [messages]);

  async function send(e: FormEvent) {
    e.preventDefault();
    const text = input.trim();
    if (!text || busy) return;
    setInput("");
    setMessages((m) => [
      ...m,
      { role: "USER", content: text, createdAt: new Date().toISOString() },
    ]);
    setBusy(true);
    try {
      const res = await aiApi.sendMessage(text, conversationId);
      setConversationId(res.conversationId);
      setMessages((m) => [...m, res.message]);
    } catch (err) {
      const msg = extractError(err);
      toast.error(msg);
      setMessages((m) => [
        ...m,
        {
          role: "ASSISTANT",
          content:
            "Sorry — I couldn't reach the AI service. Please try again in a moment.",
          createdAt: new Date().toISOString(),
        },
      ]);
    } finally {
      setBusy(false);
    }
  }

  function onKey(e: KeyboardEvent<HTMLTextAreaElement>) {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      send(e as unknown as FormEvent);
    }
  }

  return (
    <>
      <h1 className="page-title">AI assistant</h1>
      <p className="page-sub">
        Ask anything about your coverage, claims, or the right plan for you.
      </p>

      <div className="chat-shell">
        <div className="chat-stream" ref={streamRef}>
          {messages.map((m, i) => (
            <div
              key={i}
              className={m.role === "USER" ? "bubble bubble-user" : "bubble bubble-ai"}
            >
              {m.content}
            </div>
          ))}
          {busy && (
            <div className="bubble bubble-ai">
              <span className="spinner" />
            </div>
          )}
        </div>

        <form className="chat-composer" onSubmit={send}>
          <textarea
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={onKey}
            placeholder="Type your question…"
            rows={1}
            disabled={busy}
          />
          <button type="submit" className="btn btn-primary" disabled={busy || !input.trim()}>
            Send
          </button>
        </form>
      </div>
    </>
  );
}
