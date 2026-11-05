/**
 * Kuba Auri Equalizer - WebHID Packet Sniffer & Interceptor Hook
 * 
 * Como usar:
 * 1. Abra o Google Chrome ou Edge no PC.
 * 2. Acesse https://equalizador.kuba.audio
 * 3. Abra o Console do DevTools (F12 ou Ctrl+Shift+I -> Aba Console).
 * 4. Cole este script completo e pressione Enter.
 * 5. Conecte o cabo USB-C do Kuba Auri e clique nos perfis de equalização.
 * 6. Todos os pacotes binários, Report IDs, tempos e detalhes serão impressos no console em tempo real.
 */

(function () {
    console.clear();
    console.log(
        "%c[Kuba Audio Hook] 🎧 Interceptor WebHID Ativado com Sucesso!",
        "background: #0F0E0B; color: #F3C433; font-size: 14px; font-weight: bold; padding: 6px 12px; border-radius: 4px; border: 1px solid #F3C433;"
    );

    // 1. Interceptar sendReport no protótipo de HIDDevice
    if (typeof HIDDevice !== "undefined" && HIDDevice.prototype.sendReport) {
        const originalSendReport = HIDDevice.prototype.sendReport;
        let packetCounter = 0;
        let lastTimestamp = performance.now();

        HIDDevice.prototype.sendReport = async function (reportId, data) {
            const now = performance.now();
            const delta = (now - lastTimestamp).toFixed(2);
            lastTimestamp = now;

            const uint8 = data instanceof Uint8Array ? data : new Uint8Array(data.buffer || data);
            const hexArray = Array.from(uint8).map(b => b.toString(16).padStart(2, "0").toUpperCase());
            const hexString = hexArray.join(" ");

            packetCounter++;

            const isApply = uint8.length === 5 && uint8[0] === 0x01 && uint8[1] === 0x65;
            const titleColor = isApply ? "#00E676" : "#00B0FF";
            const typeLabel = isApply ? "🔥 COMANDO APLY (GRAVAR)" : `📦 PACOTE DSP #${uint8[0]} (${packetCounter})`;

            console.groupCollapsed(
                `%c[HID TX] ${typeLabel} | Report ID: 0x${reportId.toString(16).toUpperCase()} (${reportId}) | ${uint8.length} bytes (+${delta}ms)`,
                `color: ${titleColor}; font-weight: bold; font-family: monospace;`
            );

            console.log(`%cBytes Hex:`, "color: #AAA; font-weight: bold;", hexString);
            console.log(`%cArray Kotlin/Java:`, "color: #AAA; font-weight: bold;", `byteArrayOf(${hexArray.map(h => "0x" + h + ".toByte()").join(", ")})`);
            console.log(`%cArray JS/Int:`, "color: #AAA;", Array.from(uint8));
            console.log(`%cDispositivo Alvo:`, "color: #AAA;", this.productName || "Cabo USB DSP Audio", `(VID: 0x${this.vendorId.toString(16)}, PID: 0x${this.productId.toString(16)})`);
            console.groupEnd();

            // Continua a execução normal
            return await originalSendReport.call(this, reportId, data);
        };

        console.log("%c✓ HIDDevice.prototype.sendReport intercepted.", "color: #8FC79F;");
    } else {
        console.warn("[Kuba Audio Hook] WebHID não suportado ou HIDDevice indisponível neste navegador.");
    }

    // 2. Interceptar requestDevice para inspecionar filtros de conexão
    if (navigator.hid && navigator.hid.requestDevice) {
        const originalRequestDevice = navigator.hid.requestDevice;
        navigator.hid.requestDevice = async function (options) {
            console.log("%c[Kuba Audio Hook] navigator.hid.requestDevice solicitado com filtros:", "color: #F3C433;", options);
            const devices = await originalRequestDevice.call(this, options);
            devices.forEach(d => {
                console.log(
                    `%c[Dispositivo Selecionado] Nome: ${d.productName} | VID: 0x${d.vendorId.toString(16).toUpperCase()} | PID: 0x${d.productId.toString(16).toUpperCase()}`,
                    "background: #1B1913; color: #8FC79F; padding: 4px 8px;"
                );
            });
            return devices;
        };
        console.log("%c✓ navigator.hid.requestDevice intercepted.", "color: #8FC79F;");
    }

    // 3. Interceptar getDevices
    if (navigator.hid && navigator.hid.getDevices) {
        const originalGetDevices = navigator.hid.getDevices;
        navigator.hid.getDevices = async function () {
            const list = await originalGetDevices.call(this);
            console.log(`%c[Kuba Audio Hook] Dispositivos HID conectados autorizados: ${list.length}`, "color: #A8A092;");
            return list;
        };
    }

    console.log(
        "%cPronto! Conecte o cabo e clique em 'Aplicar no fone' no site para visualizar os pacotes.",
        "color: #FFF; font-style: italic;"
    );
})();
