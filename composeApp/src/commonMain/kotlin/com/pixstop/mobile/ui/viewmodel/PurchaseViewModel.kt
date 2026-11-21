package com.pixstop.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixstop.mobile.core.image.downscaleJpeg
import com.pixstop.mobile.data.repository.PurchaseRepository
import com.pixstop.mobile.domain.model.CatalogProduct
import com.pixstop.mobile.domain.model.Destination
import com.pixstop.mobile.domain.model.FillPreview
import com.pixstop.mobile.domain.model.DomainError
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.model.PurchaseLine
import com.pixstop.mobile.domain.model.PurchaseSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Os passos da compra (docs/plans/COMPRAS_MOBILE.md): fotografar, esperar a
 * leitura, conferir, pronto.
 */
enum class PurchaseStep { Capture, Uploading, Reading, Review, Done }

/** Abaixo disto a sugestão da IA aparece em amarelo: vale olhar com mais cuidado. */
const val CONFIDENT_MATCH = 0.85

/**
 * O estado da compra. Toda regra é pura e mora aqui ou nas funções de
 * [PurchaseLine] abaixo: é o que deixa testar sem rede e sem Compose.
 */
data class PurchaseUiState(
    val step: PurchaseStep = PurchaseStep.Capture,
    val sheet: PurchaseSheet? = null,
    val lines: List<PurchaseLine> = emptyList(),
    val supplierName: String = "",
    val documentNumber: String = "",
    val issuedAt: String = "",
    val totalText: String = "",
    val applianceId: Long? = null,
    /** A foto que não subiu: fica aqui para tentar de novo sem fotografar outra vez. */
    val pendingPhoto: ByteArray? = null,
    /** A linha cujo produto está sendo escolhido na gaveta. */
    val pickingLine: Int? = null,
    val pickerQuery: String = "",
    val scannerOpen: Boolean = false,
    val allowDuplicate: Boolean = false,
    val receipt: ByteArray? = null,
    val receiptOpen: Boolean = false,
    val isWorking: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val confirmedUnits: Int = 0,
    val backOnShelf: List<String> = emptyList(),
    /** A folha "Para onde vai a mercadoria?" aberta, antes de confirmar. */
    val choosingDestination: Boolean = false,
    val destination: Destination = Destination.Warehouse,
    val destinationApplianceId: Long? = null,
    /** O que o servidor disse que entrou e onde. */
    val confirmSummary: String? = null,
) {
    val editable: Boolean get() = sheet?.editable == true
    val products: List<CatalogProduct> get() = sheet?.products.orEmpty()
    val total: Double? get() = parseMoney(totalText)

    /** Linhas que ainda pedem uma decisão: sem produto e não marcadas como "não é estoque". */
    val pending: Int get() = lines.count { !it.ignored && it.productId == null }
    val units: Int get() = lines.filterNot { it.ignored }.sumOf { it.quantityUnits }
    val itemsTotal: Double get() = lines.sumOf { it.lineTotal() }

    /** A soma das linhas contra o total da nota; só vale avisar acima de 1%. */
    val totalGap: Double?
        get() {
            val paid = total ?: return null
            if (paid <= 0.0) return null
            val gap = itemsTotal - paid
            return if (abs(gap) / paid > 0.01) gap else null
        }

    val needsDuplicateConsent: Boolean get() = sheet?.duplicate != null && !allowDuplicate

    val canConfirm: Boolean
        get() = editable && !isWorking && pending == 0 && units > 0 && !needsDuplicateConsent &&
            lines.none { !it.ignored && it.productId != null && (it.quantityUnits <= 0 || it.unitCost == null) }

    val pickerResults: List<CatalogProduct>
        get() = if (pickerQuery.isBlank()) products else products.filter { it.name.contains(pickerQuery, ignoreCase = true) }

    fun productOf(line: PurchaseLine): CatalogProduct? = line.productId?.let { id -> products.firstOrNull { it.id == id } }

    /** A linha pede validade só de produto que controla (VALIDADE.md, V5). */
    fun tracksExpiry(line: PurchaseLine): Boolean = !line.ignored && productOf(line)?.tracksExpiry == true

    /** A prévia do "encher" da geladeira escolhida, se ela tiver planograma. */
    val fillPreview: FillPreview?
        get() = sheet?.destinationOptions?.fill?.firstOrNull { it.applianceId == destinationApplianceId }

    /** "Pôr na geladeira" ou "encher" pedem geladeira; sem nenhuma, só o depósito. */
    val canChooseDestination: Boolean
        get() = when (destination) {
            Destination.Warehouse -> true
            Destination.Appliance -> destinationApplianceId != null
            Destination.Fill -> fillPreview != null
        }
}

/**
 * A opção que a folha abre marcada: encher, se a geladeira para onde a
 * compra ia tem planograma; senão, o que o servidor sugeriu.
 */
fun PurchaseSheet.startingDestination(): Pair<Destination, Long?> {
    val options = destinationOptions ?: return Destination.Warehouse to null
    val applianceId = applianceId ?: options.applianceId ?: appliances.firstOrNull()?.id

    return when {
        appliances.isEmpty() -> Destination.Warehouse to null
        options.default == Destination.Fill && options.fill.any { it.applianceId == applianceId } -> Destination.Fill to applianceId
        options.default == Destination.Warehouse -> Destination.Warehouse to applianceId
        else -> Destination.Appliance to applianceId
    }
}

/**
 * A validade sugerida: a data da nota mais o prazo do produto. Só preenche o
 * que está vazio: a data digitada é a da embalagem e vale mais que o palpite.
 */
fun PurchaseLine.withSuggestedExpiry(product: CatalogProduct?, issuedAt: String): PurchaseLine {
    if (!expiresOn.isNullOrBlank() || product?.tracksExpiry != true) return this
    val days = product.shelfLifeDays ?: return this

    return copy(expiresOn = addDays(issuedAt, days) ?: return this)
}

/** "2026-11-20" + 30 = "2026-12-20". Data inválida devolve nulo. */
fun addDays(isoDate: String, days: Int): String? {
    val parts = isoDate.trim().take(10).split("-").mapNotNull { it.toIntOrNull() }
    if (parts.size != 3) return null
    val (year, month, day) = parts
    if (month !in 1..12 || day !in 1..31) return null

    return civilFromDays(daysFromCivil(year, month, day) + days)
}

/** Dias desde 1970-01-01 (algoritmo de Howard Hinnant). */
private fun daysFromCivil(year: Int, month: Int, day: Int): Long {
    val y = (if (month <= 2) year - 1 else year).toLong()
    val era = (if (y >= 0) y else y - 399) / 400
    val yoe = y - era * 400
    val mp = (month + 9) % 12
    val doy = (153 * mp + 2) / 5 + day - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy

    return era * 146097 + doe - 719468
}

private fun civilFromDays(days: Long): String {
    val z = days + 719468
    val era = (if (z >= 0) z else z - 146096) / 146097
    val doe = z - era * 146097
    val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val day = (doy - (153 * mp + 2) / 5 + 1).toInt()
    val month = (if (mp < 10) mp + 3 else mp - 9).toInt()
    val year = (yoe + era * 400 + if (month <= 2) 1 else 0).toInt()

    return "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
}

/** O que a linha custa na compra. "Não é estoque" não soma. */
fun PurchaseLine.lineTotal(): Double = if (ignored) 0.0 else quantityUnits * (unitCost ?: 0.0)

/**
 * Duas caixas de seis são doze latas: mudar a embalagem refaz as unidades e o
 * custo de uma lata a partir do que a nota cobrou. Linha digitada à mão, sem
 * quantidade da nota, só guarda a embalagem.
 */
fun PurchaseLine.withPack(pack: Int): PurchaseLine {
    val size = pack.coerceAtLeast(1)
    val quantity = rawQuantity ?: return copy(packSize = size)
    val units = (quantity * size).roundToInt()
    val cost = rawTotal?.takeIf { units > 0 }?.let { (it / units * 10000).roundToInt() / 10000.0 } ?: unitCost

    return copy(packSize = size, quantityUnits = units, unitCost = cost)
}

/** Escolher o produto traz o custo médio como palpite, se o custo estiver vazio. */
fun PurchaseLine.withProduct(product: CatalogProduct): PurchaseLine = copy(
    productId = product.id,
    productName = product.name,
    ignored = false,
    matchSource = "manual",
    confidence = null,
    unitCost = unitCost?.takeIf { it > 0 } ?: product.priceCost,
)

/** O custo médio que a linha vai deixar: é o número que o lucro vai usar. */
fun PurchaseLine.averageAfter(product: CatalogProduct?): Double? {
    product ?: return null
    val cost = unitCost ?: return null
    if (ignored || quantityUnits <= 0) return null
    if (product.stock <= 0 || product.priceCost <= 0.0) return cost

    return (product.stock * product.priceCost + quantityUnits * cost) / (product.stock + quantityUnits)
}

/** "566,12", "566.12" e "1.234,56" viram número; vazio vira nulo. */
fun parseMoney(text: String): Double? {
    val clean = text.trim().replace("R$", "").trim()
    if (clean.isEmpty()) return null
    val normalized = if (clean.contains(',')) clean.replace(".", "").replace(',', '.') else clean
    return normalized.toDoubleOrNull()
}

class PurchaseViewModel(
    private val repository: PurchaseRepository,
    private val shrink: (ByteArray) -> ByteArray = { downscaleJpeg(it) },
    private val pollMillis: Long = 3_000,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PurchaseUiState())
    val uiState: StateFlow<PurchaseUiState> = _uiState.asStateFlow()

    private var polling: Job? = null

    /** Abre uma compra que já existe: rascunho, lendo ou confirmada. */
    fun open(id: Long) {
        _uiState.update { it.copy(isWorking = true, error = null) }
        viewModelScope.launch { reload(id) }
    }

    /** A foto chegou (câmera ou galeria): reduz, sobe e começa a esperar a leitura. */
    fun onPhoto(photo: ByteArray) {
        _uiState.update { it.copy(step = PurchaseStep.Uploading, pendingPhoto = photo, error = null) }
        viewModelScope.launch {
            val small = withContext(Dispatchers.Default) { shrink(photo) }
            when (val outcome = repository.uploadReceipt(small)) {
                is Outcome.Success -> {
                    _uiState.update { it.copy(pendingPhoto = null, step = PurchaseStep.Reading) }
                    reload(outcome.value)
                }

                is Outcome.Failure -> _uiState.update {
                    it.copy(
                        error = if (outcome.error is DomainError.Offline) {
                            "Sem conexão. A foto está guardada aqui: tente de novo quando a rede voltar. Se fechar o app, ela se perde."
                        } else {
                            outcome.error.message
                        },
                    )
                }
            }
        }
    }

    fun retryUpload() {
        _uiState.value.pendingPhoto?.let(::onPhoto)
    }

    private suspend fun reload(id: Long) {
        when (val outcome = repository.show(id)) {
            is Outcome.Success -> apply(outcome.value)
            is Outcome.Failure -> _uiState.update { it.copy(isWorking = false, error = outcome.error.message) }
        }
    }

    private fun apply(sheet: PurchaseSheet) {
        val step = if (sheet.reading) PurchaseStep.Reading else PurchaseStep.Review

        _uiState.update {
            it.copy(
                step = step,
                sheet = sheet,
                lines = if (sheet.editable) {
                    sheet.lines.map { line ->
                        line.withSuggestedExpiry(sheet.products.firstOrNull { p -> p.id == line.productId }, sheet.issuedAt)
                    }
                } else {
                    sheet.lines
                },
                supplierName = sheet.supplierName,
                documentNumber = sheet.documentNumber,
                issuedAt = sheet.issuedAt,
                totalText = sheet.total?.let(::formatMoneyInput).orEmpty(),
                applianceId = sheet.applianceId,
                isWorking = false,
            )
        }

        if (sheet.reading) poll(sheet.id) else polling?.cancel()
    }

    /** Enquanto a IA lê, pergunta de novo a cada poucos segundos. */
    private fun poll(id: Long) {
        if (polling?.isActive == true) return
        polling = viewModelScope.launch {
            while (true) {
                delay(pollMillis)
                val outcome = repository.show(id)
                if (outcome is Outcome.Success && !outcome.value.reading) {
                    apply(outcome.value)
                    break
                }
            }
        }
    }

    fun onSupplierChange(value: String) = _uiState.update { it.copy(supplierName = value) }
    fun onDocumentChange(value: String) = _uiState.update { it.copy(documentNumber = value) }
    fun onDateChange(value: String) = _uiState.update { it.copy(issuedAt = value) }
    fun onTotalChange(value: String) = _uiState.update { it.copy(totalText = value) }
    fun onApplianceChange(id: Long?) = _uiState.update { it.copy(applianceId = id) }
    fun onAllowDuplicate(value: Boolean) = _uiState.update { it.copy(allowDuplicate = value) }

    private fun updateLine(index: Int, change: (PurchaseLine) -> PurchaseLine) = _uiState.update { state ->
        state.copy(lines = state.lines.mapIndexed { i, line -> if (i == index) change(line) else line })
    }

    fun setUnits(index: Int, value: Int) = updateLine(index) { it.copy(quantityUnits = value.coerceAtLeast(0)) }
    fun setPack(index: Int, value: Int) = updateLine(index) { it.withPack(value) }
    fun setCost(index: Int, text: String) = updateLine(index) { it.copy(unitCost = parseMoney(text)) }
    fun setIgnored(index: Int, value: Boolean) = updateLine(index) { it.copy(ignored = value) }
    fun setExpiry(index: Int, value: String) = updateLine(index) { it.copy(expiresOn = value.trim().ifBlank { null }) }

    fun addLine() = _uiState.update { it.copy(lines = it.lines + PurchaseLine(), pickingLine = it.lines.size) }
    fun removeLine(index: Int) = _uiState.update { state -> state.copy(lines = state.lines.filterIndexed { i, _ -> i != index }) }

    fun openPicker(index: Int) = _uiState.update { it.copy(pickingLine = index, pickerQuery = "") }
    fun closePicker() = _uiState.update { it.copy(pickingLine = null, pickerQuery = "", scannerOpen = false) }
    fun onPickerQuery(value: String) = _uiState.update { it.copy(pickerQuery = value) }

    fun choose(product: CatalogProduct) {
        val index = _uiState.value.pickingLine ?: return
        val issuedAt = _uiState.value.issuedAt
        updateLine(index) { it.withProduct(product).copy(expiresOn = null).withSuggestedExpiry(product, issuedAt) }
        closePicker()
    }

    fun openScanner() = _uiState.update { it.copy(scannerOpen = true) }
    fun closeScanner() = _uiState.update { it.copy(scannerOpen = false) }

    /** O código lido escolhe o produto; código desconhecido vira aviso, não erro. */
    fun onBarcode(code: String) {
        val product = _uiState.value.products.firstOrNull { it.barcode == code }
        if (product != null) {
            choose(product)
        } else {
            _uiState.update { it.copy(scannerOpen = false, message = "Este código não é de nenhum produto cadastrado. Busque pelo nome.") }
        }
    }

    fun dismissMessage() = _uiState.update { it.copy(message = null, error = null) }

    private fun currentSheet(): PurchaseSheet? {
        val state = _uiState.value
        return state.sheet?.copy(
            supplierName = state.supplierName.trim(),
            documentNumber = state.documentNumber.trim(),
            issuedAt = state.issuedAt.trim(),
            total = state.total,
            applianceId = state.applianceId,
            lines = state.lines,
        )
    }

    fun save() {
        val sheet = currentSheet() ?: return
        _uiState.update { it.copy(isWorking = true, error = null) }
        viewModelScope.launch {
            when (val outcome = repository.save(sheet)) {
                is Outcome.Success -> {
                    _uiState.update { it.copy(message = "Rascunho salvo.") }
                    reload(sheet.id)
                }

                is Outcome.Failure -> _uiState.update { it.copy(isWorking = false, error = outcome.error.message) }
            }
        }
    }

    /**
     * "Confirmar entrada" salva a folha e abre a pergunta "para onde vai".
     * Salvar antes é o que deixa a prévia do "encher a geladeira" certa.
     */
    fun askDestination() {
        val state = _uiState.value
        val sheet = currentSheet() ?: return
        if (!state.canConfirm) return

        _uiState.update { it.copy(isWorking = true, error = null) }
        viewModelScope.launch {
            val saved = repository.save(sheet)
            if (saved is Outcome.Failure) {
                _uiState.update { it.copy(isWorking = false, error = saved.error.message) }
                return@launch
            }

            when (val outcome = repository.show(sheet.id)) {
                is Outcome.Success -> {
                    val (destination, applianceId) = outcome.value.startingDestination()
                    apply(outcome.value)
                    _uiState.update {
                        it.copy(choosingDestination = true, destination = destination, destinationApplianceId = applianceId)
                    }
                }

                is Outcome.Failure -> _uiState.update { it.copy(isWorking = false, error = outcome.error.message) }
            }
        }
    }

    fun chooseDestination(destination: Destination) = _uiState.update { it.copy(destination = destination) }

    fun chooseDestinationAppliance(id: Long) = _uiState.update {
        // Trocou de geladeira e a nova não tem planograma: "encher" não existe lá.
        val next = it.copy(destinationApplianceId = id)
        if (next.destination == Destination.Fill && next.fillPreview == null) next.copy(destination = Destination.Appliance) else next
    }

    fun closeDestination() = _uiState.update { it.copy(choosingDestination = false) }

    /** Segurou para confirmar: a mercadoria entra onde a pessoa escolheu. */
    fun confirm() {
        val state = _uiState.value
        val id = state.sheet?.id ?: return
        if (!state.canChooseDestination || state.isWorking) return

        _uiState.update { it.copy(isWorking = true, error = null) }
        viewModelScope.launch {
            when (val outcome = repository.confirm(id, state.allowDuplicate, state.destination, state.destinationApplianceId)) {
                is Outcome.Success -> _uiState.update {
                    it.copy(
                        isWorking = false,
                        choosingDestination = false,
                        step = PurchaseStep.Done,
                        confirmSummary = outcome.value,
                        confirmedUnits = it.lines.filter { line -> !line.ignored && line.productId != null }.sumOf { line -> line.quantityUnits },
                        backOnShelf = it.lines
                            .filter { line -> !line.ignored && line.quantityUnits > 0 }
                            .mapNotNull { line -> it.productOf(line)?.takeIf { product -> product.stock <= 0 }?.name }
                            .distinct(),
                    )
                }

                is Outcome.Failure -> {
                    _uiState.update { it.copy(isWorking = false, choosingDestination = false, error = outcome.error.message) }
                    reload(id)
                }
            }
        }
    }

    fun discard(onDone: () -> Unit) {
        val id = _uiState.value.sheet?.id ?: return
        _uiState.update { it.copy(isWorking = true, error = null) }
        viewModelScope.launch {
            when (val outcome = repository.discard(id)) {
                is Outcome.Success -> {
                    _uiState.update { it.copy(isWorking = false) }
                    onDone()
                }

                is Outcome.Failure -> _uiState.update { it.copy(isWorking = false, error = outcome.error.message) }
            }
        }
    }

    /** A nota original em tela cheia, carregada só quando pedida. */
    fun openReceipt() {
        val id = _uiState.value.sheet?.id ?: return
        _uiState.update { it.copy(receiptOpen = true) }
        if (_uiState.value.receipt != null) return
        viewModelScope.launch {
            when (val outcome = repository.receipt(id)) {
                is Outcome.Success -> _uiState.update { it.copy(receipt = outcome.value) }
                is Outcome.Failure -> _uiState.update { it.copy(receiptOpen = false, error = outcome.error.message) }
            }
        }
    }

    fun closeReceipt() = _uiState.update { it.copy(receiptOpen = false) }

    override fun onCleared() {
        polling?.cancel()
        super.onCleared()
    }
}

/** 566.12 → "566,12", para o campo de total. */
fun formatMoneyInput(value: Double): String {
    val cents = (value * 100).roundToInt()
    val abs = abs(cents)
    return (if (cents < 0) "-" else "") + "${abs / 100},${(abs % 100).toString().padStart(2, '0')}"
}
