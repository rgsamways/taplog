# Module 33 — Context File
> For: `/opsx:propose "module-33-ai-copilot"`
> Written: 2026-05-31
> Depends on: Module 32 (ChecklistCard — co-pilot is the connected complement to the offline checklist)

---

## What this module is

A floating AI help button on `InspectionFormScreen` that opens a bottom sheet chat assistant. The assistant is grounded in the specific vertical, asset type, and checklist items the inspector is currently working with. It answers questions like "what does UV degradation look like on webbing?" or "is a sticky snap hook gate a fail or a requires-attention?"

This is the **Level 2 inspection guidance layer** — connected, contextual, conversational. The checklist (Module 32) is Level 1 — offline, static, always available. The co-pilot requires connectivity and is supplementary.

---

## Why the vertical engine changes this

The original Module 33 spec pre-dated `VerticalConfig`. The co-pilot would have required hardcoded OFC knowledge in the system prompt. With the vertical engine, the system prompt is **generated at runtime from `VerticalConfig`** — meaning the co-pilot automatically knows the regulatory framework, asset types, checklist items, result options, and result actions for any current or future vertical. Anchor and Hatch will work without any code changes once their configs are seeded.

---

## Scope

**In scope:**
- Floating help FAB on `InspectionFormScreen` (e.g. `?` icon, bottom-right)
- Tap → `ModalBottomSheet` with a simple chat UI
- Multi-turn conversation (conversation history held in memory for session duration)
- System prompt generated from `VerticalConfig` + current `VerticalAssetType` at runtime
- Calls Anthropic API directly from the app (`claude-sonnet-4-20250514`)
- Loading indicator while response streams / awaits
- "Clear conversation" button in bottom sheet header
- Conversation resets when inspector navigates away from `InspectionFormScreen`

**Out of scope:**
- Offline mode — co-pilot requires connectivity; show "No connection — co-pilot unavailable" if offline
- Conversation persistence — session-scoped only, not saved to Room
- Backend proxy — direct API call in v1
- Voice input
- No changes to any screen other than `InspectionFormScreen`
- No Room changes, no backend changes

---

## System prompt (generated at runtime)

```
You are a field inspection assistant for ${verticalConfig.displayName} inspectors using TapLog.

Regulatory framework: ${verticalConfig.regulatoryFramework}

The inspector is currently inspecting:
Asset type: ${assetType.label}
Description: ${assetType.description}

Pre-inspection checklist for this asset type:
${assetType.checklistItems.joinToString("\n") { "- $it" }}

Result options for this vertical:
${verticalConfig.formProfile.resultOptions.joinToString("\n") { "- ${it.label}: ${it.action.name}" }}

Your role:
- Answer questions about what to look for during inspection of this specific asset type
- Clarify what constitutes a pass vs. a fail vs. requires-attention finding
- Explain regulatory requirements in plain language
- Help the inspector understand unfamiliar deficiency types
- Be concise — inspectors are in the field, not at a desk

Do not:
- Make up regulatory requirements not grounded in the framework above
- Tell the inspector what result to record — that is always their professional judgment
- Discuss assets or verticals not relevant to this inspection
```

---

## API call

**Model:** `claude-sonnet-4-20250514`
**Max tokens:** 400 — field answers should be brief
**API key:** `BuildConfig.ANTHROPIC_API_KEY` (same key introduced in Module 31; if Module 31 ships first, reuse `AiRepository`)

**Request shape:**
```kotlin
// Multi-turn: send full conversation history each call
messages = conversationHistory + listOf(Message(role = "user", content = userMessage))
```

**Response:** Plain text extracted from `content[0].text` — no structured output needed.

---

## AiRepository changes

If Module 31 has shipped, `AiRepository` already exists. Add a second method:

```kotlin
suspend fun chat(
    systemPrompt: String,
    conversationHistory: List<ChatMessage>,
    userMessage: String
): String?  // returns assistant response text, or null on failure
```

If Module 31 has not shipped yet, create `AiRepository` with both methods (`identifyAsset` + `chat`).

`ChatMessage` data class:
```kotlin
data class ChatMessage(
    val role: String,   // "user" or "assistant"
    val content: String
)
```

---

## Bottom sheet UI

```
┌──────────────────────────────────┐
│  🤖 Inspection Assistant   [✕]  │  ← header, X clears conversation
├──────────────────────────────────┤
│                                  │
│  [Assistant message bubble]      │
│                                  │
│         [User message bubble]    │
│                                  │
│  [Assistant message bubble]      │
│                                  │
├──────────────────────────────────┤
│  [Type a question…]    [Send ↑] │  ← input row
└──────────────────────────────────┘
```

- Assistant bubbles: left-aligned, `TapLogNavy700` background
- User bubbles: right-aligned, `TapLogTeal600` background
- Loading: assistant bubble shows a pulsing `...` while awaiting response
- Error: assistant bubble shows "Couldn't reach the assistant — check your connection"
- Offline: bottom sheet opens but shows "Co-pilot requires an internet connection" banner; input disabled

---

## ViewModel changes

`EmberViewModel` additions:

```kotlin
// Co-pilot state — scoped to current inspection session
val copilotMessages: StateFlow<List<ChatMessage>>
val copilotLoading: StateFlow<Boolean>

fun sendCopilotMessage(userMessage: String, verticalConfig: VerticalConfig, assetType: VerticalAssetType)
fun clearCopilot()  // called on navigate-away from InspectionFormScreen
```

Conversation history is stored in `_copilotMessages: MutableStateFlow<List<ChatMessage>>` — reset by `clearCopilot()`.

---

## Modified files

- `InspectionFormScreen.kt` — add floating `?` FAB, `ModalBottomSheet` with `CopilotBottomSheet` composable, wire to ViewModel
- `EmberViewModel.kt` — `copilotMessages`, `copilotLoading`, `sendCopilotMessage()`, `clearCopilot()`
- `AiRepository.kt` — add `chat()` method (or create fresh if Module 31 not yet shipped)

## New files

- `CopilotBottomSheet.kt` in `ca.taplog.app.ui.ember/` — the bottom sheet composable

---

## What does NOT change

- `InspectionFormScreen` form logic, result selector, deficiency section — untouched
- `ChecklistCard` from Module 32 — untouched
- VerticalConfig / VerticalRegistry — read-only inputs to prompt generation
- Room schema — no migrations
- Backend — no changes
- All other screens
