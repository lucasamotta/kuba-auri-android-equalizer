# Kuba Auri Equalizer — Aplicativo Android 🎧📱

> **Projeto Pessoal Open Source Não Oficial**  
> Um aplicativo simples para Android feito para trocar perfis e equalizar o cabo DSP USB-C do fone **Kuba Auri** diretamente pelo celular.

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-purple.svg)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-SDK%2026%2B-green.svg)](https://developer.android.com)
[![Developed with AI](https://img.shields.io/badge/Developed%20with-Generative%20AI-orange.svg)](#-como-o-projeto-foi-feito-100-com-ia)

---

## 💡 Por que este projeto existe? (A História do Projeto)

Comprei o **Kuba Auri** tendo como principal atrativo o cabo DSP USB-C programável. A possibilidade de carregar a assinatura de som gravada no próprio cabo é fantástica! Porém, no dia a dia, ter que ligar o computador e abrir o navegador apenas para alternar entre uma curva e outra era pouco prático.

A inspiração direta para este app veio de um post na comunidade [r/audiofiliabrasil](https://www.reddit.com/r/audiofiliabrasil/) no Reddit, onde o usuário compartilhou que havia criado um app desktop para Linux para uso próprio:  
👉 [Post no Reddit: App de equalização para o Kuba Auri](https://www.reddit.com/r/audiofiliabrasil/comments/1xedk23/app_de_equaliza%C3%A7%C3%A3o_para_o_kuba_auri/)

Ou seja: **a ideia não é nem de longe original!** Hahaha. 

Este projeto nasceu estritamente como um **projeto pessoal de pequeno porte**, feito para resolver a minha própria necessidade de alternar perfis e brincar com equalização direto pelo smartphone Android. **Não tenho intenção de manter ativamente este aplicativo no longo prazo** nem prestar suporte contínuo. Estou disponibilizando o código aberto aqui caso seja útil para mais alguém na comunidade que também queira usar ou evoluir por conta própria!

---

## 🤖 Como o projeto foi feito: 100% com IA Generativa

Eu **não escrevi nenhuma linha de código manualmente**. O projeto inteiro foi desenvolvido, depurado e colocado para rodar através de pair programming com **Inteligência Artificial Generativa** (Google DeepMind / Antigravity).

- A IA fez a engenharia reversa do protocolo de comunicação USB HID olhando o código do equalizador web.
- A IA escreveu todo o código em Kotlin e Jetpack Compose para Android.
- A IA foi conectada diretamente ao celular via depuração Wi-Fi (ADB) para analisar os logs em tempo real, identificar por que o cabo inicialmente não aplicava o som, corrigir o envio de pacotes para o chip DSP e calibrar a resposta visual do gráfico.

---

## ✨ O que o aplicativo faz

- ⚡ **Gravação Direta no Fone**: Qualquer equalização escolhida é gravada na memória flash do cabo DSP. Você pode desconectar o fone e usá-lo na TV, no videogame ou em outro aparelho que o som continuará calibrado.
- 🎯 **11 Curvas Oficiais Kuba Inclusas**:
  - *Preferência*: Auri (Padrão), Natural, Em V, Basshead, Enérgico, Musical e Analítico.
  - *Referência*: JM-1, Harman IE 2019, VDSF e Etymotic.
- 🎚️ **Equalizador Personalizado de 10 Bandas**:
  - Ajuste manual nas frequências de 31.5 Hz a 16 kHz (-12 dB a +12 dB).
  - Cálculo de ganho preventivo (*Safe Preamp*) para evitar distorções no som.
  - Opção para salvar suas próprias equalizações com nome.
- 📈 **Gráfico de Resposta em Frequência**: Visualização em tempo real baseada nas medições acústicas reais do fone.

---

## 📲 Como Usar no Celular (Instalação Fácil)

Você não precisa compilar nada se não quiser:

1. Acesse a aba **[Releases](https://github.com/lucasamotta/kuba-auri-android-equalizer/releases)** aqui no GitHub.
2. Baixe o arquivo **`.apk`** da versão mais recente direto no celular.
3. Toque no arquivo para instalar (autorize a instalação de fontes desconhecidas se o Android pedir).
4. Plugue o fone Kuba Auri na porta USB-C, aceite a permissão USB na tela e pronto!

---

## 🛠️ Para quem quiser compilar o código

Se quiser clonar o repositório e abrir no Android Studio:

```bash
git clone https://github.com/lucasamotta/kuba-auri-android-equalizer.git
cd kuba-auri-android-equalizer
```

- Abra a pasta no **Android Studio** (Hedgehog ou superior com JDK 17).
- Conecte o celular com depuração USB ativada e execute:
  ```bash
  ./gradlew assembleDebug
  adb install -r app/build/outputs/apk/debug/app-debug.apk
  ```

---

## ⚠️ Isenção de Responsabilidade & Créditos

- **Kuba Áudio Oficial**: Conheça a marca e os produtos em [kuba.audio](https://kuba.audio).
- **Equalizador Web Original**: Aplicação web oficial da fabricante em [equalizador.kuba.audio](https://equalizador.kuba.audio).
- "Kuba", "Kuba Áudio" e "Auri" são marcas registradas de **Kuba Áudio Comércio de Equipamentos de Áudio Ltda**.
- Este é um software comunitário independente, sem vínculo comercial ou homologação da Kuba Áudio. O código é livre sob licença [MIT](LICENSE).
