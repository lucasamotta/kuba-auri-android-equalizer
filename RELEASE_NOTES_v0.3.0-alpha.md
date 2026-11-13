# Release Notes — v0.3.0-alpha

## 🎚️ Equalizador Paramétrico Avançado

Esta versão introduz o **Editor Paramétrico Avançado**, uma nova aba paralela ao equalizador padrão com controle completo sobre cada banda de frequência.

### Novas funcionalidades

#### Equalizador Paramétrico Avançado
- Nova aba **"Avançado"** com até 7 bandas de equalização independentes
- Controle individual de **Frequência**, **Ganho (dB)** e **Fator Q** por banda
- Seleção de **tipo de filtro** por banda: Pico, Low Shelf, High Shelf, Passa-Alta, Passa-Baixa
- Visualização da curva de resposta em frequência em tempo real
- Pré-sets personalizados para o editor avançado

#### Modo de Audição em Tempo Real ⚡
- Novo switch **"Tempo Real"** no editor avançado
- Aplica as alterações de EQ diretamente no fone enquanto você ajusta os controles
- Debounce inteligente: aguarda 800ms após a última alteração antes de gravar
- Mutex de USB para evitar conflitos em operações simultâneas
- Delay de estabilização de 250ms após cada gravação no DSP

---

> [!WARNING]
> ## ⚠️ Aviso de Durabilidade — Modo em Tempo Real
>
> O modo de audição em tempo real **grava repetidamente na memória flash do DSP** do fone a cada ajuste realizado.
>
> A memória flash possui um **número limitado de ciclos de escrita** (tipicamente entre 10.000 e 100.000 gravações). O uso intensivo e prolongado do modo em tempo real pode, ao longo do tempo, **reduzir a vida útil da memória do DSP**.
>
> **Recomendações:**
> - Use o modo em tempo real para **sessões curtas de ajuste fino**, não como modo padrão de operação
> - Prefira ajustar os parâmetros e então **gravar manualmente** com o botão "Gravar no Cabo"
> - Desative o modo em tempo real quando não estiver fazendo ajustes ativos
>
> O aplicativo exibe este aviso sempre que o modo em tempo real é ativado.

---

### Correções e melhorias

- **Correção de bug:** Preset não atualizava na interface após uso prolongado do modo em tempo real — corrigido com atualização correta do `_appliedPresetId` ao final de cada gravação silenciosa
- **Estabilidade USB:** Adicionado `Mutex` nas operações USB para evitar condições de corrida ao gravar presets
- **Layout landscape:** Gráfico de EQ na metade esquerda, controles na metade direita em telas horizontais
- **Responsividade:** Interface adaptada para tablets e telas grandes
- **Botão conectar:** Substituído por indicador de texto (cinza: desconectado / verde: conectado)
- **Descrição de presets:** Campo de descrição personalizada ao salvar curvas de equalização
- **Referência do EQ paramétrico:** Curva de referência fixada na JM-1 (não mais relativa à curva atual)
- **Interação com sliders:** Área de rolagem separada da área de toque dos sliders para evitar conflitos

### Detalhes técnicos

- **versionCode:** 3
- **versionName:** 0.3.0-alpha
- **minSdk:** 26 | **targetSdk:** 35
- Build: `assembleDebug` — debug APK para testes

---

*Kuba Auri Equalizer — Desenvolvido por Lucas Amotta*
