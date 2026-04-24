# LibreIntel - Android App

Your intelligent reading & research companion — now available as a native Android application. Capture text, build exploration trees, and chat with AI to deepen your understanding.

## Features

- **Concept exploration tree** — Organize notes in a parent-child tree for chained exploration
- **AI-generated titles** — Each node gets a concise AI-generated title for quick scanning
- **Pin & branch** — Pin a node as parent, then add new notes as its children; or branch directly from AI responses
- **Ask AI** — Tap any node to chat with an LLM about it, with full conversation history
- **Context-aware AI** — The LLM receives the full ancestor chain so it understands your exploration path
- **Voice input** — Tap the microphone to speak your question
- **Markdown support** — AI responses with bold, lists, code blocks rendered correctly
- **JSON export/import** — Export your entire knowledge tree or branch as JSON; import to restore
- **Timestamps** — Every node and chat message is timestamped
- **Configurable LLM** — Bring your own API key; presets for DashScope (Qwen), OpenAI, and Ollama
- **Share intent** — Receive text from other apps via Android share sheet
- **No backend** — Everything runs locally on your device

## Screenshots

| Tree View | Chat | Settings |
|-----------|------|----------|
| Browse your knowledge tree with collapse/expand | Ask AI about any node with full context | Configure your LLM API |

## Install

### Pre-built APK
Download the latest APK from the [Releases](https://github.com/FrankS-IntelLab/libreintel-android/releases) page.

### Build from Source

1. **Prerequisites**
   - Android Studio (latest version recommended)
   - JDK 17+
   - Android SDK 34

2. **Clone and Build**
   ```bash
   git clone https://github.com/FrankS-IntelLab/libreintel-android.git
   cd libreintel-android
   # Open in Android Studio, or:
   ./gradlew assembleDebug
   ```

3. **Install**
   - Transfer `app/build/outputs/apk/debug/app-debug.apk` to your phone
   - Enable "Install from unknown sources" in Settings
   - Open the APK and install

## Configure LLM

Tap the **Settings** tab (⚙️) to configure your LLM:

| Preset | API Endpoint | Model |
|--------|--------------|-------|
| DashScope (Qwen) | `https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions` | `qwen-plus` |
| OpenAI | `https://api.openai.com/v1/chat/completions` | `gpt-4o-mini` |
| Ollama | `http://localhost:11434/api/chat` | `llama2` |
| Custom | Your API endpoint | Your model |

Any OpenAI-compatible API works (DashScope, OpenAI, OpenRouter, Ollama, LocalAI, etc.).

## Usage

1. **Add a note** — Tap the + button to add a new node with text and optional source URL
2. **Navigate** — Tap any node to open the chat view
3. **Branch deeper** — Select text in chat → tap branch to create a child node
4. **Pin a parent** — Tap the 🔗 icon to pin a node as parent for the next note
5. **Voice input** — Tap the 🎤 microphone button to speak your question
6. **Export** — Use Settings to export/import your knowledge tree as JSON

## Tech Stack

- **Language**: Kotlin 1.9.22
- **UI**: Jetpack Compose with Material Design 3
- **Architecture**: Clean Architecture (MVVM)
- **DI**: Hilt
- **Database**: Room
- **Preferences**: DataStore
- **Networking**: Retrofit + OkHttp
- **Async**: Kotlin Coroutines + Flow

## License

[MIT](LICENSE)

---

**Author**: Frank Sun — [GitHub](https://github.com/FrankS-IntelLab) · [Website](https://franks-intellab.github.io/)

A problem-solving strategist focused on business decision-making and real value creation. Working across data, AI, and economic logic.