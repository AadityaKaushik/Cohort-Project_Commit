# ADR-013: Local-first AI provider

Status: Accepted  
Date: Sprint 8, P89  

## Context

Several Sprint 8 features send text to a language model: the coach, the job posting parser and the debrief extraction. That text is personal (applications, interview notes, salary hopes). Sending it to a hosted service by default would undo the privacy position the federated learning work builds.

## Decision

1. One interface, two implementations. `AiProvider` has `complete` and `stream`. `OllamaProvider` talks to a local Ollama server and is the default. `GeminiProvider` calls the Gemini REST API. Feature code only sees `AiService`, so changing provider touches no feature.
2. Gemini is opt-in. It is used only when `AI_PROVIDER=gemini` and `GEMINI_API_KEY` is set. With the key missing the app stays on Ollama rather than falling back to a hosted service.
3. Disclosure. `GET /api/v1/ai/status` returns the provider and whether it is hosted. The frontend shows a notice on every AI screen when it is.
4. Failure is a normal state. Connect and read timeouts are configured. Any transport error or non-2xx answer becomes `AiUnavailableException`, returned as 503 `AI_UNAVAILABLE`. Pages keep working and show an "AI unavailable" message.
5. Rate limit per user. A sliding window (default 30 calls per hour) held in memory, returned as 429 `AI_RATE_LIMITED`.
6. Usage log without content. `ai_usage_log` stores user, feature, provider, success flag, prompt and reply sizes and duration. Prompt and reply text are never written to the database or the logs.
7. Grounding. Prompts are built on the server from the requesting user's own rows. The client sends only its own question text.

## Consequences

- Tests use a stubbed provider and need no network or model.
- The rate limiter is per instance. A second API instance would need a shared store.
- Small local models give uneven structured output, so every JSON answer is validated before use and rejected when malformed.
