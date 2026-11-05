# Relatório de Engenharia Reversa: Cabo DSP Kuba Auri

Este documento descreve detalhadamente o protocolo de comunicação, identificadores de hardware (VID/PID), fórmulas matemáticas de DSP e estrutura de pacotes binários utilizados pelo web app oficial [https://equalizador.kuba.audio](https://equalizador.kuba.audio) para programar o cabo DSP USB-C do fone **Kuba Auri**.

---

## 1. Identificação do Hardware

- **Dispositivo**: Cabo TTGK "USB DSP Audio" (Família de chipsets *Walkplay*)
- **Vendor ID (VID)**: `0x3302` (Decimal: `13058`)
- **Product ID (PID)**: `0x8902` (Decimal: `35074`)
- **Interface de Controle**: USB Human Interface Device (HID)
- **Taxa de Amostragem do DSP ($F_s$)**: `96.000 Hz` (96 kHz)
- **Report ID**: `0xCB` (Decimal: `203`)

---

## 2. Camada de Comunicação

O web app original utiliza a **WebHID API** do navegador (`navigator.hid`) para se comunicar com a interface HID do cabo de áudio.

No protocolo USB, cada transação para enviar comandos ao DSP é feita através de:
1. **HID Output Report** enviado para o **Report ID `0xCB`**.
2. No Android USB Host (`UsbManager`), isso é executado via:
   - **Control Transfer (Endpoint 0)**:
     - `bmRequestType = 0x21` (`USB_TYPE_CLASS | USB_DIR_OUT | USB_RECIP_INTERFACE`)
     - `bRequest = 0x09` (`SET_REPORT`)
     - `wValue = (0x02 << 8) | 0xCB` (`0x02CB` -> Report Type Output = 2, Report ID = 0xCB)
     - `wIndex = hidInterfaceNumber`
     - `buffer = payload`
   - **Interrupt OUT Transfer**: Caso a interface HID forneça um endpoint de interrupção de saída (`USB_ENDPOINT_XFER_INT` com direção OUT), o pacote pode ser transmitido como `[0xCB, ...payload]`.

---

## 3. Estrutura Matemática dos Filtros Biquad

O cabo DSP possui **10 bandas de equalização paramétrica**. Cada banda é implementada através de um filtro digital biquad de 2ª ordem calculado a $F_s = 96000\text{ Hz}$.

### 3.1. Filtro Peaking (PK)
Parâmetros: Frequência $f$ (Hz), Ganho $g$ (dB), Fator de Qualidade $Q$.
$$\begin{aligned}
A &= 10^{g / 40} \\
\omega &= \frac{2 \pi f}{F_s} \\
\alpha &= \frac{\sin(\omega)}{2 Q} \\
a_0 &= 1 + \frac{\alpha}{A} \\
b_0 &= \frac{1 + \alpha \cdot A}{a_0} \\
b_1 &= \frac{-2 \cos(\omega)}{a_0} \\
b_2 &= \frac{1 - \alpha \cdot A}{a_0} \\
a_1 &= b_1 \\
a_2 &= \frac{1 - \alpha / A}{a_0}
\end{aligned}$$

### 3.2. Filtro Low Shelf (LS)
$$\begin{aligned}
A &= 10^{g / 40} \\
\omega &= \frac{2 \pi f}{F_s} \\
\alpha &= \frac{\sin(\omega)}{2 Q} \\
c &= \cos(\omega) \\
s &= 2 \sqrt{A} \cdot \alpha \\
a_0 &= (A + 1) + (A - 1) c + s \\
b_0 &= A \cdot \frac{(A + 1) - (A - 1) c + s}{a_0} \\
b_1 &= 2 A \cdot \frac{(A - 1) - (A + 1) c}{a_0} \\
b_2 &= A \cdot \frac{(A + 1) - (A - 1) c - s}{a_0} \\
a_1 &= -2 \cdot \frac{(A - 1) + (A + 1) c}{a_0} \\
a_2 &= \frac{(A + 1) + (A - 1) c - s}{a_0}
\end{aligned}$$

---

## 4. Formato do Blob Binário do DSP (220 Bytes)

Cada preset é codificado em um buffer contínuo de **220 bytes**:

| Offset | Tamanho | Descrição | Formato |
|---|---|---|---|
| `0x00 - 0x0D` | 14 bytes | **Cabeçalho Fixo** | `0x45 0x51 0x31 0x23 0xD6 0x00 0x00 0x0A 0x04 0x05 0x01 0x0A 0x00 0x00` (`"EQ1#..."`) |
| `0x0E - 0x11` | 4 bytes | **Pré-amplificação (Gain)** | $\text{round}(10^{\text{preDb}/20} \cdot 2^{23})$ em `Int32 LE` (Q23) |
| `0x12 - 0xD9` | 200 bytes | **10 Bandas de Filtro** (20 bytes cada) | 5 coeficientes por banda: $[b_0, b_2, -a_1, b_1, -a_2]$ multiplicados por $2^{27}$ em `Int32 LE` |
| `0xDA - 0xDB` | 2 bytes | **Checksum CRC16** | CRC16 (polinômio `0x1021`, init `0xFFFF`) calculado sobre os 218 bytes anteriores |

---

## 5. Segmentação em Pacotes e Sequência de Transmissão

O blob de 220 bytes é fragmentado em **9 pacotes de 31 bytes** cada:
- **Byte 0**: Índice do pacote (`0x00` a `0x08`).
- **Bytes 1 a 30**: 30 bytes de dados fatiados do blob.
  - Para o pacote 7 (índice `0x07`), apenas 10 bytes pertencem ao blob (os últimos 2 bytes são o CRC16); os 20 bytes restantes são preenchidos com `0x00`.
  - O pacote 8 (índice `0x08`) é composto pelo byte `0x08` seguido de 30 bytes `0x00`.

### Sequência Temporal:
1. Envia os pacotes `0` a `8` sequencialmente para o Report ID `0xCB`, aguardando **16 ms** entre cada pacote.
2. Aguarda um intervalo de estabilização de **150 ms**.
3. Envia o pacote de gravação final (**Apply Command**) para o Report ID `0xCB`:
   ```
   [0x01, 0x65, 0x00, 0x01, 0xFF]  (5 bytes)
   ```

---

## 6. Lista Oficial de Presets do Kuba Auri

### Curvas de Preferência:
1. **Auri (Padrão)**: Assinatura padrão inspirada pela comunidade. Pré-amp: `-2.8 dB`.
2. **Natural**: Subgrave presente, médios limpos e agudos arejados. Pré-amp: `-3.0 dB`.
3. **Em V**: Alvo com graves e agudos levantados. Pré-amp: `-7.8 dB`.
4. **Basshead**: Para entusiastas de graves com reforço Low-Shelf acentuado. Pré-amp: `-5.7 dB`.
5. **Enérgico**: Sonoridade intensa, ideal para rock. Pré-amp: `-3.3 dB`.
6. **Musical**: Graves fortes, médios comedidos e agudos delicados. Pré-amp: `-2.3 dB`.
7. **Analítico**: Prioridade para detalhamento, microdinâmica e clareza. Pré-amp: `-0.8 dB`.

### Curvas de Referência:
8. **JM-1**: Curva de referência acústica de alta fidelidade. Pré-amp: `-4.0 dB`.
9. **Harman IE 2019**: Referência consagrada baseada nas pesquisas de Sean Olive da Harman. Pré-amp: `-0.5 dB`.
10. **VDSF**: Meio-termo entre campo difuso e Harman. Pré-amp: `-2.2 dB`.
11. **Etymotic**: Graves contidos com extrema definição em médios e agudos. Pré-amp: `-2.2 dB`.
