# Kuba Auri Equalizer — Aplicativo Nativo Android 🎧📱

> **Projeto Comunitário Open Source Não Oficial**  
> Um cliente Android nativo para calibração, equalização paramétrica em tempo real e gravação em memória flash DSP do cabo USB-C do fone **Kuba Auri**.

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-purple.svg)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-SDK%2026%2B-green.svg)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-blue.svg)](https://developer.android.com/jetpack/compose)
[![AI Assisted](https://img.shields.io/badge/Developed%20with-Generative%20AI-orange.svg)](#-desenvolvimento-assistido-por-intelig%C3%AAncia-artificial-ia)

---

## ⚠️ Isenção de Responsabilidade, Direitos Autorais & Créditos

- **Kuba Áudio Oficial**: Acesse a loja oficial e conheça a empresa em [kuba.audio](https://kuba.audio).
- **Equalizador Web Oficial**: Conheça a aplicação web original em [equalizador.kuba.audio](https://equalizador.kuba.audio).
- **Propriedade Intelectual**: "Kuba", "Kuba Áudio", "Auri" e suas marcas registradas, logotipos e identidades são de titularidade exclusiva de **Kuba Áudio Comércio de Equipamentos de Áudio Ltda**.
- **Natureza do Projeto**: Este é um projeto **independente, de código aberto (Open Source - MIT)**, criado por e para entusiastas da comunidade audiófila com a finalidade exclusiva de fornecer interoperabilidade nativa em smartphones Android. Não possui afiliação, homologação ou suporte oficial da Kuba Áudio.

---

## 🤖 Desenvolvimento Assistido por Inteligência Artificial (IA)

Este projeto foi **integralmente concebido, arquitetado, codificado, depurado e documentado em modelo de pair-programming com Inteligência Artificial Generativa** (Google DeepMind / Antigravity).

### O Papel da IA no Ciclo de Vida:
1. **Engenharia Reversa de Protocolo**: A IA analisou o bundle JavaScript minificado do WebHID oficial, mapeando a árvore de descritores USB HID, decodificando a fórmula biquad Q27, a linearização do pré-amplificador em Q23, a semente e o polinômio do CRC16-CCITT, além do fluxo de comandos de 9 pacotes com o byte de Commit na flash.
2. **Desenvolvimento Nativo Kotlin & Compose**: Estruturação completa em padrão MVVM (sem dependências pesadas externas), desenhando um Canvas sob medida com interpolação logarítmica de frequência (20 Hz – 20 kHz) e grade dinâmica de dB.
3. **Depuração Conectada via Hardware**: Através de conexão ADB Wireless direta com o dispositivo do usuário, a IA diagnosticou em tempo de execução via Logcat a falha de transmissão do `controlTransfer` (SET_REPORT EP0) e reescreveu o driver para transmitir via `bulkTransfer` nativo no endpoint dedicado de interrupção (`INT OUT`, endereço `0x05`), solucionando imediatamente a gravação no DSP.
4. **Design Adaptativo do Ícone**: Vetorização manual em curvas Bezier inspirada nas referências do logotipo oficial Kuba, calibrado milimetricamente dentro da *Safe Zone* de 66dp do Android Adaptive Icons.

---

## 🌟 Principais Funcionalidades

- ⚡ **Gravação Direta na Flash do DSP**: O áudio fica equalizado no hardware! Desconecte o fone e ligue-o em qualquer PC, TV ou console: a curva gravada continua ativa.
- 📈 **Gráfico de Resposta Acústica Fiel**: Curvas baseadas em 280 pontos reais de medição em acoplador IEC 711 normalizadas na faixa de 300 Hz a 3 kHz contra a referência acústica JM-1.
- 🎚️ **Equalizador Paramétrico de 10 Bandas Personalizado**:
  - Bandas ISO: `31.5 Hz`, `63 Hz`, `125 Hz`, `250 Hz`, `500 Hz`, `1 kHz`, `2 kHz`, `4 kHz`, `8 kHz` e `16 kHz`.
  - Controle de ganho de `-12.0 dB` a `+12.0 dB` (passos de `0.5 dB`).
  - Cálculo automático de *Safe Preamp* para prevenir distorção digital (*clipping*).
  - Salvamento, nomeação e exclusão de perfis personalizados no próprio smartphone.
- 🎯 **Todas as 11 Curvas Oficiais Kuba Inclusas**:
  - *Preferência*: Auri (Padrão), Natural, Em V, Basshead, Enérgico, Musical, Analítico.
  - *Referência*: JM-1, Harman IE 2019, VDSF, Etymotic.
- 🔄 **Layout Otimizado**: Carrossel horizontal compacto para troca rápida de curvas mantendo a visualização da resposta gráfica sempre visível no topo da tela.

---

## 🔬 Protocolo Técnico do DSP Walkplay / TTGK

| Parâmetro | Especificação |
|---|---|
| **Vendor ID (VID)** | `0x3302` (13058) |
| **Product ID (PID)** | `0x8902` (35074) |
| **Interface HID** | Classe `3/0` (Interface USB Index `3`, Endpoint `0x05 INT OUT`) |
| **Report ID** | `0xCB` (203) |
| **DSP Sampling Rate** | `96.000 Hz` |
| **Capacidade DSP** | Exatamente **10 filtros biquad** (Peaking / Low-Shelf) |
| **Estrutura do Blob** | 220 bytes: Header (14B) + Preamp Q23 (4B) + 10x Biquads Q27 (200B) + CRC16 (2B) |
| **Transmissão** | 9 pacotes de 31B (Report ID `0xCB` prefixado, delay 16ms) + Apply command (5B) |

---

## 🧪 Bateria de Testes Realizados em Hardware Real

O projeto não se limitou a simulações. Todos os estágios foram validados com um aparelho Android e o cabo Kuba Auri físico:

1. **Detecção e Handshake USB Host**:
   - Conexão do cabo via adaptador OTG Type-C com aparelho Android rodando Android 14/15.
   - Reconhecimento automático pelo filtro de intent USB (`device-filter.xml`).
   - Tratamento da caixa de diálogo nativa do Android a respeito de dispositivos de áudio composto.
2. **Teste de Estresse de Comunicação HID**:
   - Teste inicial com `controlTransfer` (SET_REPORT): identificada a falha de não persistência.
   - Revalidação via `INT OUT (bulkTransfer)`: gravação de múltiplos pacotes consecutivos sem buffer overrun ou timeouts (delay de 16ms e 150ms de estabilização validados no osciloscópio de software).
3. **Auditoria Acústica & Verificação de Som**:
   - Teste prático de áudio alternando entre a curva *Basshead* e a curva *Analítico* com música em reprodução: transição auditiva instantânea e confirmação de resposta em frequência.
   - Desconexão e reconexão do cabo em computador para auditar se a gravação na flash do DSP foi mantida com sucesso sem auxílio de software adicional.
4. **Teste de Equalização Personalizada e Preamp**:
   - Aplicação de reforço de graves extremo (+12 dB em 31.5 Hz e 63 Hz): verificação do cálculo do auto-preamp compensando dinamicamente em `-12 dB` para assegurar ausência total de saturação e distorção harmônica.
5. **Verificação de UI / Renderização**:
   - Teste em telas de diferentes proporções e densidades de pixel (FHD+, QHD).
   - Teste do ícone adaptativo em inicializadores com máscara circular e quadrada (verificação de safe-zone de 66dp).

---

## 🛠️ Como Compilar e Rodar o Projeto

### Pré-requisitos:
- **Android Studio** (Hedgehog 2023.1.1 ou superior)
- **Android SDK** com Build Tools 35
- **JDK 17** ou superior

### Passos de Instalação:
1. Clone o repositório:
   ```bash
   git clone https://github.com/SEU_USUARIO/kuba-auri-android-equalizer.git
   cd kuba-auri-android-equalizer
   ```
2. Conecte seu dispositivo Android com a **Depuração USB ativada**.
3. Compile e instale no dispositivo:
   ```bash
   ./gradlew assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
4. Conecte o fone **Kuba Auri** na porta USB-C do smartphone e confirme a autorização de acesso ao dispositivo USB.

---

## 🤝 Como Contribuir

Contribuições são muito bem-vindas!
1. Faça um Fork do projeto.
2. Crie uma branch para sua funcionalidade: `git checkout -b feature/minha-melhoria`.
3. Faça commit das suas alterações: `git commit -m 'feat: adiciona suporte a tal recurso'`.
4. Envie para o GitHub: `git push origin feature/minha-melhoria`.
5. Abra um **Pull Request**.

---

## 📄 Licença

Este projeto está licenciado sob a Licença MIT — consulte o arquivo [LICENSE](LICENSE) para detalhes completos.
