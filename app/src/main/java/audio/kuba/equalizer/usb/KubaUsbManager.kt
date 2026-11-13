package audio.kuba.equalizer.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import audio.kuba.equalizer.dsp.DspConstants
import audio.kuba.equalizer.dsp.DspProtocol
import audio.kuba.equalizer.dsp.Preset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Gerenciador responsável pela comunicação USB Host (OTG),
 * detecção de eventos de plug/unplug, controle de permissões
 * e transmissão de pacotes DSP para o cabo Kuba Auri.
 */
class KubaUsbManager(private val context: Context) {

    companion object {
        private const val TAG = "KubaUsbManager"
        private const val ACTION_USB_PERMISSION = "audio.kuba.equalizer.USB_PERMISSION"

        // Constantes da especificação USB HID
        private const val USB_REQUEST_TYPE_HID_SET_REPORT = 0x21 // HostToDevice | Class | Interface
        private const val HID_REQUEST_SET_REPORT = 0x09
        private const val HID_REPORT_TYPE_OUTPUT = 0x02
        private const val USB_TIMEOUT_MS = 3000

        // Classe USB HID (Human Interface Device)
        private const val USB_CLASS_HID = 3
        // Classe USB Audio
        private const val USB_CLASS_AUDIO = 1
    }

    private val usbManager: UsbManager =
        context.getSystemService(Context.USB_SERVICE) as UsbManager

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val usbMutex = kotlinx.coroutines.sync.Mutex()

    private val _connectionState = MutableStateFlow<UsbConnectionState>(UsbConnectionState.Disconnected)
    val connectionState: StateFlow<UsbConnectionState> = _connectionState.asStateFlow()

    private val _isLiveSyncing = MutableStateFlow(false)
    val isLiveSyncing: StateFlow<Boolean> = _isLiveSyncing.asStateFlow()

    private var receiverRegistered = false

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_USB_PERMISSION -> {
                    synchronized(this) {
                        val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                        }

                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            if (device != null && isKubaDevice(device)) {
                                Log.i(TAG, "Permissão USB concedida para: ${device.productName}")
                                logDeviceDescriptor(device)
                                onDeviceConnected(device)
                            }
                        } else {
                            Log.w(TAG, "Permissão USB negada pelo usuário.")
                            if (device != null) {
                                _connectionState.value = UsbConnectionState.PermissionDenied(device)
                            }
                        }
                    }
                }

                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }

                    if (device != null && isKubaDevice(device)) {
                        Log.i(TAG, "Cabo Kuba Auri detectado (plugado): ${device.productName}")
                        handleDeviceAttached(device)
                    }
                }

                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }

                    if (device != null && isKubaDevice(device)) {
                        Log.i(TAG, "Cabo Kuba Auri desconectado.")
                        _connectionState.value = UsbConnectionState.Disconnected
                    }
                }
            }
        }
    }

    /**
     * Inicia o monitoramento de conexão USB e verifica se o cabo já está plugado.
     */
    fun startMonitoring() {
        if (!receiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(ACTION_USB_PERMISSION)
                addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
                addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(usbReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(usbReceiver, filter)
            }
            receiverRegistered = true
        }

        // Verifica se o dispositivo já está conectado no momento da inicialização
        checkExistingConnections()
    }

    /**
     * Encerra o monitoramento e desregistra o receiver.
     */
    fun stopMonitoring() {
        if (receiverRegistered) {
            try {
                context.unregisterReceiver(usbReceiver)
            } catch (e: Exception) {
                Log.w(TAG, "Erro ao desregistrar receiver: ${e.message}")
            }
            receiverRegistered = false
        }
    }

    /**
     * Verifica na lista de dispositivos do UsbManager se o cabo Kuba está presente.
     */
    fun checkExistingConnections() {
        val device = findKubaDevice()
        if (device != null) {
            handleDeviceAttached(device)
        } else {
            _connectionState.value = UsbConnectionState.Disconnected
        }
    }

    fun requestPermission() {
        val device = findKubaDevice() ?: return
        requestPermission(device)
    }

    private fun handleDeviceAttached(device: UsbDevice) {
        if (usbManager.hasPermission(device)) {
            logDeviceDescriptor(device)
            onDeviceConnected(device)
        } else {
            _connectionState.value = UsbConnectionState.DeviceDetected(device)
            requestPermission(device)
        }
    }

    private fun requestPermission(device: UsbDevice) {
        _connectionState.value = UsbConnectionState.DeviceDetected(device)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val permissionIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(ACTION_USB_PERMISSION).setPackage(context.packageName),
            flags
        )
        usbManager.requestPermission(device, permissionIntent)
    }

    private fun onDeviceConnected(device: UsbDevice) {
        val name = device.productName ?: "Cabo Kuba Auri DSP"
        _connectionState.value = UsbConnectionState.Connected(device, name)
    }

    fun findKubaDevice(): UsbDevice? {
        return usbManager.deviceList.values.firstOrNull { isKubaDevice(it) }
    }

    private fun isKubaDevice(device: UsbDevice): Boolean {
        return device.vendorId == DspConstants.VENDOR_ID &&
                device.productId == DspConstants.PRODUCT_ID
    }

    /**
     * Imprime no logcat o descritor completo do dispositivo USB para diagnóstico.
     */
    private fun logDeviceDescriptor(device: UsbDevice) {
        Log.d(TAG, "=== DESCRITOR DO DISPOSITIVO USB ===")
        Log.d(TAG, "Nome: ${device.productName} | Fabricante: ${device.manufacturerName}")
        Log.d(TAG, "VID: 0x${device.vendorId.toString(16).uppercase()} | PID: 0x${device.productId.toString(16).uppercase()}")
        Log.d(TAG, "Classe do dispositivo: ${device.deviceClass} | Subclasse: ${device.deviceSubclass}")
        Log.d(TAG, "Total de interfaces: ${device.interfaceCount}")

        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            Log.d(TAG, "  Interface[$i]: id=${iface.id} name=${iface.name} class=${iface.interfaceClass}/${iface.interfaceSubclass} endpoints=${iface.endpointCount}")
            for (j in 0 until iface.endpointCount) {
                val ep = iface.getEndpoint(j)
                val dir = if (ep.direction == UsbConstants.USB_DIR_OUT) "OUT" else "IN"
                val type = when (ep.type) {
                    UsbConstants.USB_ENDPOINT_XFER_CONTROL -> "CTRL"
                    UsbConstants.USB_ENDPOINT_XFER_INT -> "INT"
                    UsbConstants.USB_ENDPOINT_XFER_BULK -> "BULK"
                    UsbConstants.USB_ENDPOINT_XFER_ISOC -> "ISOC"
                    else -> "UNKN"
                }
                Log.d(TAG, "    Endpoint[$j]: addr=0x${ep.address.toString(16)} dir=$dir type=$type maxPkt=${ep.maxPacketSize}")
            }
        }
        Log.d(TAG, "====================================")
    }

    /**
     * Grava o preset selecionado no hardware do cabo DSP.
     * @param preset Preset com as bandas e pré-amp calculados.
     * @param isSilent Se true, executa em segundo plano sem disparar pop-ups ou sobrescrever o estado de conexão com spinners globais (ideal para audição em tempo real).
     */
    suspend fun applyPreset(preset: Preset, isSilent: Boolean = false): Result<Unit> = withContext(Dispatchers.IO) {
        usbMutex.lock()
        try {
            val device = findKubaDevice()
                ?: return@withContext Result.failure(IllegalStateException("Cabo Kuba Auri não encontrado. Conecte o cabo USB-C."))

            if (!usbManager.hasPermission(device)) {
                requestPermission(device)
                return@withContext Result.failure(SecurityException("Permissão USB pendente. Autorize o acesso na janela do Android."))
            }

            val packets = try {
                DspProtocol.buildPackets(preset.bands, preset.preDb)
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao construir pacotes DSP: ${e.message}", e)
                return@withContext Result.failure(e)
            }

            if (isSilent) {
                _isLiveSyncing.value = true
            } else {
                Log.i(TAG, "=== INICIANDO GRAVAÇÃO: ${preset.name} (${packets.size} pacotes) ===")
            }

            var connection: UsbDeviceConnection? = null

            try {
                connection = usbManager.openDevice(device)
                    ?: return@withContext Result.failure(IllegalStateException("Não foi possível abrir comunicação com o cabo. Verifique a permissão USB."))

                val result = tryAllInterfaces(connection, device, packets, preset, isSilent)

                if (result.isSuccess) {
                    Log.i(TAG, "Preset '${preset.name}' gravado com sucesso no DSP!")
                    if (!isSilent) {
                        val connectedState = UsbConnectionState.Success(preset.name)
                        _connectionState.value = connectedState

                        scope.launch {
                            delay(2000)
                            if (_connectionState.value is UsbConnectionState.Success) {
                                _connectionState.value = UsbConnectionState.Connected(
                                    device,
                                    device.productName ?: "Cabo Kuba Auri DSP"
                                )
                            }
                        }
                    }
                } else {
                    val msg = result.exceptionOrNull()?.message ?: "Falha na gravação"
                    Log.e(TAG, "Falha em todas as interfaces: $msg")
                    if (!isSilent) {
                        _connectionState.value = UsbConnectionState.Error(msg)
                    }
                }

                result

            } catch (e: Exception) {
                Log.e(TAG, "Erro ao gravar preset: ${e.message}", e)
                if (!isSilent) {
                    _connectionState.value = UsbConnectionState.Error(
                        message = e.message ?: "Erro desconhecido durante gravação USB",
                        throwable = e
                    )
                }
                Result.failure(e)
            } finally {
                if (isSilent) {
                    _isLiveSyncing.value = false
                }
                try { connection?.close() } catch (ignored: Exception) {}
            }
        } finally {
            usbMutex.unlock()
        }
    }

    /**
     * Tenta todas as interfaces do dispositivo em ordem de prioridade:
     * 1. Interfaces HID (class=3)
     * 2. Outras interfaces com endpoint de interrupção
     * 3. Qualquer interface disponível
     */
    private suspend fun tryAllInterfaces(
        connection: UsbDeviceConnection,
        device: UsbDevice,
        packets: List<ByteArray>,
        preset: Preset,
        isSilent: Boolean = false
    ): Result<Unit> {
        // Monta lista de interfaces priorizando HID, depois interfaces com INT endpoints
        val interfaces = buildList {
            // Prioridade 1: interfaces HID (class 3)
            for (i in 0 until device.interfaceCount) {
                val iface = device.getInterface(i)
                if (iface.interfaceClass == USB_CLASS_HID) {
                    add(iface)
                }
            }
            // Prioridade 2: interfaces com endpoint de interrupção que não sejam áudio
            for (i in 0 until device.interfaceCount) {
                val iface = device.getInterface(i)
                if (iface.interfaceClass != USB_CLASS_HID &&
                    iface.interfaceClass != USB_CLASS_AUDIO &&
                    hasInterruptEndpoint(iface) &&
                    none { it.id == iface.id }
                ) {
                    add(iface)
                }
            }
            // Prioridade 3: qualquer interface não-áudio restante
            for (i in 0 until device.interfaceCount) {
                val iface = device.getInterface(i)
                if (iface.interfaceClass != USB_CLASS_AUDIO &&
                    none { it.id == iface.id }
                ) {
                    add(iface)
                }
            }
        }

        var lastError = "Nenhuma interface encontrada no dispositivo"

        for (iface in interfaces) {
            val claimed = connection.claimInterface(iface, true)
            if (!claimed) {
                lastError = "Falha ao reivindicar interface[${iface.id}]"
                continue
            }

            try {
                val outEndpoint = findInterruptOutEndpoint(iface)
                val success = transmitPackets(connection, iface, outEndpoint, packets, preset, isSilent)
                if (success) {
                    return Result.success(Unit)
                } else {
                    lastError = "Todas as estratégias de envio falharam na interface[${iface.id}]"
                }
            } finally {
                try { connection.releaseInterface(iface) } catch (ignored: Exception) {}
            }
        }

        return Result.failure(IllegalStateException(lastError))
    }

    /**
     * Transmite os 9 pacotes de dados + comando apply para uma interface específica.
     * Retorna true se TODOS os pacotes foram enviados com sucesso.
     */
    private suspend fun transmitPackets(
        connection: UsbDeviceConnection,
        iface: UsbInterface,
        outEndpoint: UsbEndpoint?,
        packets: List<ByteArray>,
        preset: Preset,
        isSilent: Boolean = false
    ): Boolean {
        // Envia os 9 pacotes de dados com intervalo de 16 ms
        for ((index, packet) in packets.withIndex()) {
            if (!isSilent) {
                _connectionState.value = UsbConnectionState.Writing(
                    presetName = preset.name,
                    currentPacket = index + 1,
                    totalPackets = packets.size
                )
            }

            val sent = sendHidReport(connection, iface, outEndpoint, DspConstants.REPORT_ID, packet)
            if (!sent) {
                Log.e(TAG, "Falha no pacote #$index — abortando transmissão.")
                return false
            }

            delay(DspConstants.DELAY_BETWEEN_PACKETS_MS)
        }

        // Intervalo de estabilização antes do apply
        delay(DspConstants.DELAY_BEFORE_APPLY_MS)

        // Envia o comando Apply (0x01, 0x65, 0x00, 0x01, 0xFF)
        val applyOk = sendHidReport(
            connection, iface, outEndpoint,
            DspConstants.REPORT_ID, DspConstants.APPLY_PAYLOAD
        )

        if (!applyOk) {
            Log.e(TAG, "Falha ao enviar comando APPLY.")
            return false
        }

        // Aguarda estabilização pós-gravação de flash (permite ao DSP recarregar os registradores de áudio)
        delay(250)

        return true
    }

    /**
     * Envia um HID Output Report.
     *
     * O WebHID usa o Interrupt OUT endpoint nativo (addr=0x05).
     * Tentamos na ordem correta:
     * 1. Interrupt OUT com Report ID prefixado (caminho nativo HID Output Report)
     * 2. Interrupt OUT sem prefixo
     * 3. Control Transfer SET_REPORT sem prefixo (EP0)
     * 4. Control Transfer SET_REPORT COM prefixo (EP0)
     */
    private fun sendHidReport(
        connection: UsbDeviceConnection,
        usbInterface: UsbInterface,
        outEndpoint: UsbEndpoint?,
        reportId: Byte,
        data: ByteArray
    ): Boolean {
        val reportIdInt = reportId.toInt() and 0xFF

        // Buffer com Report ID prefixado — padrão HID Output Report via Interrupt OUT
        val prefixedBuffer = ByteArray(data.size + 1).apply {
            this[0] = reportId
            System.arraycopy(data, 0, this, 1, data.size)
        }

        // === Estratégia 1: Interrupt OUT com Report ID prefixado (comportamento nativo WebHID) ===
        if (outEndpoint != null) {
            val int1 = connection.bulkTransfer(
                outEndpoint,
                prefixedBuffer,
                prefixedBuffer.size,
                USB_TIMEOUT_MS
            )
            Log.d(TAG, "Estratégia 1 (INT OUT + reportId): resultado=$int1")
            if (int1 >= 0) return true

            // === Estratégia 2: Interrupt OUT sem prefixo (raw data) ===
            val int2 = connection.bulkTransfer(
                outEndpoint,
                data,
                data.size,
                USB_TIMEOUT_MS
            )
            Log.d(TAG, "Estratégia 2 (INT OUT raw): resultado=$int2")
            if (int2 >= 0) return true
        }

        // wValue: high byte = report type (0x02 = Output), low byte = report ID
        val wValue = (HID_REPORT_TYPE_OUTPUT shl 8) or reportIdInt
        val wIndex = usbInterface.id

        // === Estratégia 3: Control Transfer SET_REPORT sem prefixo ===
        val ctrl3 = connection.controlTransfer(
            USB_REQUEST_TYPE_HID_SET_REPORT,
            HID_REQUEST_SET_REPORT,
            wValue,
            wIndex,
            data,
            data.size,
            USB_TIMEOUT_MS
        )
        Log.d(TAG, "Estratégia 3 (ctrl sem prefixo): resultado=$ctrl3")
        if (ctrl3 >= 0) return true

        // === Estratégia 4: Control Transfer SET_REPORT COM Report ID prefixado ===
        val ctrl4 = connection.controlTransfer(
            USB_REQUEST_TYPE_HID_SET_REPORT,
            HID_REQUEST_SET_REPORT,
            wValue,
            wIndex,
            prefixedBuffer,
            prefixedBuffer.size,
            USB_TIMEOUT_MS
        )
        Log.d(TAG, "Estratégia 4 (ctrl com prefixo): resultado=$ctrl4")
        if (ctrl4 >= 0) return true

        Log.e(TAG, "TODAS as estratégias falharam para Report ID 0x${reportIdInt.toString(16).uppercase()}")
        return false
    }

    private fun findInterruptOutEndpoint(usbInterface: UsbInterface): UsbEndpoint? {
        for (i in 0 until usbInterface.endpointCount) {
            val ep = usbInterface.getEndpoint(i)
            if (ep.type == UsbConstants.USB_ENDPOINT_XFER_INT &&
                ep.direction == UsbConstants.USB_DIR_OUT
            ) {
                return ep
            }
        }
        return null
    }

    private fun hasInterruptEndpoint(usbInterface: UsbInterface): Boolean {
        for (i in 0 until usbInterface.endpointCount) {
            if (usbInterface.getEndpoint(i).type == UsbConstants.USB_ENDPOINT_XFER_INT) {
                return true
            }
        }
        return false
    }
}
