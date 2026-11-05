# Guia de Manutenção e Arquitetura para Agentes de IA

Este documento foi elaborado especialmente para orientar **agentes de IA e desenvolvedores futuros** que forem manter, atualizar ou expandir a base de código do **Kuba Equalizador Android**.

---

## 🗺️ Mapa de Arquitetura & Diretórios

```
app/src/main/
├── AndroidManifest.xml                        # Configurações do app, intenções USB e filtros
├── java/audio/kuba/equalizer/
│   ├── MainActivity.kt                        # Ponto de entrada, configuração Edge-to-Edge e despacho de Intents
│   ├── dsp/                                   # Motor de processamento matemático e comunicação de baixo nível
│   │   ├── DspConstants.kt                    # VID, PID, Report ID, Header, Delays e tamanhos de buffers
│   │   ├── DspFilter.kt                       # Classes de filtro paramétrico (Peak, LowShelf) e BiquadCoeffs
│   │   ├── DspProtocol.kt                     # Funções biquad, CRC16, gerador de blob binário e pacotes
│   │   ├── CustomEqConstants.kt               # 10 frequências ISO (31.5Hz a 16kHz), Q padrão e cálculo de preDb automático
│   │   ├── KubaMeasurement.kt                 # Tabela acústica de 280 pontos (IEC 711), alvos e cálculo de YRange
│   │   └── PresetModels.kt                    # Lista completa dos presets (oficiais e categoria CUSTOM)
│   ├── usb/                                   # Camada USB Host e abstração do hardware
│   │   ├── KubaUsbManager.kt                  # Descoberta de dispositivo, solicitação de permissão e envio HID
│   │   └── UsbConnectionState.kt              # Sealed interface com estados de conexão reativos
│   └── ui/                                    # Interface visual Jetpack Compose
│       ├── MainViewModel.kt                   # ViewModel central (MVVM), estado reativo, persistência JSON SharedPreferences
│       ├── MainScreen.kt                      # Scaffold, abas (Oficiais vs Equalizador 10 bandas), BottomApplyBar e diálogos
│       ├── components/                        # Componentes reutilizáveis
│       │   ├── ConnectionStatusCard.kt        # Card e pill de status de conexão USB com indicador animado
│       │   ├── FrequencyGraph.kt              # Gráfico Canvas com curva de medição acústica + alvo de referência
│       │   ├── HorizontalPresetSelector.kt    # Carrossel horizontal de presets compactos com status e exclusão
│       │   ├── CustomBandSliders.kt           # Sliders táteis verticais de 10 bandas paramétricas (-12 a +12 dB)
│       │   └── PresetItemCard.kt              # Card legível de preset
│       └── theme/                             # Tema Kuba (cores âmbar/preto, tipografia e bordas)
│           ├── Color.kt
│           ├── Theme.kt
│           └── Type.kt
└── res/
    ├── drawable/                              # Ícones vetoriais adaptativos (ic_launcher_background / foreground)
    ├── mipmap-*/                              # Ícones em todas as resoluções (mdpi a xxxhdpi)
    └── xml/device_filter.xml                  # Filtro USB declarativo (VID=13058, PID=35074)
```

---

## ⚠️ Regras Críticas de Hardware e DSP

Ao modificar qualquer parte da camada de comunicação USB ou DSP, você **DEVE** respeitar estritamente as seguintes restrições:

1. **Capacidade Máxima do Hardware: 10 Bandas**:
   - O chip DSP Walkplay suporta exatamente **10 bandas de biquad filters**.
   - O equalizador personalizado do app utiliza as 10 frequências ISO padrão da indústria:
     `31.5 Hz, 63 Hz, 125 Hz, 250 Hz, 500 Hz, 1 kHz, 2 kHz, 4 kHz, 8 kHz, 16 kHz`.
   - A banda 1 (`31.5 Hz`) é modelada como Low-Shelf ($Q=0.707$) para extensão natural de sub-grave; as bandas 2 a 9 como Peaking ($Q=1.414$ para 1 oitava); e a banda 10 (`16 kHz`) como Peaking de agudos.
   - Qualquer preset submetido ao DSP deve conter **exatamente 10 bandas**.

2. **Endpoint INT OUT com Prefixo de Report ID**:
   - O cabo USB-C do Kuba Auri utiliza o Report ID `0xCB`.
   - No envio via `UsbDeviceConnection.bulkTransfer` pelo endpoint `INT OUT` (endereço `0x05`), **é mandatório que o primeiro byte do buffer seja o Report ID** (`0xCB`), totalizando **32 bytes** para cada pacote e **6 bytes** para o comando APPLY.

3. **Timings Obrigatórios**:
   - Intervalo entre pacotes: `16 ms` (`DspConstants.DELAY_BETWEEN_PACKETS_MS`).
   - Intervalo pré-apply: `150 ms` (`DspConstants.DELAY_BEFORE_APPLY_MS`).

4. **Tratamento de Dispositivos Compostos**:
   - O cabo expõe 7 interfaces (áudio USB streaming + HID).
   - Reivindique apenas a interface de classe `USB_CLASS_HID` (classe 3).

---

## 💾 Persistência de Equalizações Customizadas

- As curvas criadas pelo usuário são serializadas em formato JSON e salvas no `SharedPreferences` sob a chave `saved_custom_presets_json`.
- Cada preset customizado possui categoria `PresetCategory.CUSTOM`, alvo `"Custom"` e é interoperável com todo o pipeline de visualização gráfica e gravação USB Host.
