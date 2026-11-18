package com.pixstop.mobile.data.mapper

import com.pixstop.mobile.data.remote.dto.AssistantMessageDto
import com.pixstop.mobile.data.remote.dto.AssistantThreadDto
import com.pixstop.mobile.domain.model.CardRow
import com.pixstop.mobile.domain.model.ChatConversation
import com.pixstop.mobile.domain.model.ChatMessage
import com.pixstop.mobile.domain.model.ChatRole
import com.pixstop.mobile.domain.model.ChatThread
import com.pixstop.mobile.domain.model.ToolCard
import com.pixstop.mobile.ui.components.formatMoney
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull

/**
 * Do JSON do assistente para o que a tela desenha.
 *
 * Os rótulos e a ordem são os mesmos do card do painel
 * (`AssistantToolCard.vue`), para a pessoa ver o mesmo resumo nos dois.
 */
object AssistantMapper {

    private val toolLabels = mapOf(
        "top_customers" to "Quem mais compra",
        "inactive_customers" to "Quem parou de comprar",
        "never_purchased" to "Quem nunca comprou",
        "get_customer" to "Ficha da pessoa",
        "get_sales_summary" to "Vendas",
        "top_products" to "Mais vendidos",
        "search_products" to "Produtos",
        "get_stock_status" to "Estoque",
        "get_shopping_list" to "Lista de compras",
        "companies_overview" to "Suas geladeiras",
        "list_door_sessions" to "Aberturas",
        "get_door_session" to "Abertura",
        "door_audit_summary" to "Auditoria da câmera",
        "create_product" to "Cadastrar produto",
        "create_product_from_photo" to "Cadastrar produto pela foto",
        "update_product" to "Alterar produto",
        "draft_campaign" to "Rascunho de campanha",
        "review_door_session" to "Conferir abertura",
        "list_campaign_options" to "Opções de campanha",
        "get_campaign_results" to "Resultado da campanha",
    )

    private val fieldLabels = linkedMapOf(
        "name" to "Nome",
        "product" to "Produto",
        "price" to "Preço",
        "cost" to "Custo",
        "estimated_price" to "Preço de referência",
        "margin_percent" to "Margem (%)",
        "margin_percent_after" to "Margem depois (%)",
        "category" to "Categoria",
        "description" to "Descrição",
        "barcode" to "Código de barras",
        "initial_stock" to "Estoque inicial",
        "low_stock_threshold" to "Estoque mínimo",
        "audience" to "Público",
        "kind" to "Tipo",
        "subject" to "Assunto",
        "body" to "Mensagem",
        "eligible" to "Recebem",
        "skipped" to "Ficam de fora",
        "estimated_cost" to "Custo estimado",
        "wallet_balance" to "Saldo de mensagens",
        "opened_at" to "Abertura",
        "verdict" to "Veredito",
        "note" to "Nota",
    )

    private val moneyFields = setOf("price", "cost", "estimated_price", "estimated_cost", "wallet_balance", "spent", "revenue")

    private val listKeys = listOf(
        "similar_products", "catalog_matches", "customers", "appliances", "products", "top", "items", "sessions", "zeroed", "matches", "campaigns",
    )

    fun thread(dto: AssistantThreadDto): ChatThread = ChatThread(
        conversation = dto.conversation?.let { ChatConversation(it.id, it.title) },
        messages = dto.messages.map(::message),
    )

    fun message(dto: AssistantMessageDto): ChatMessage = when {
        dto.role == "user" -> ChatMessage(dto.id, ChatRole.User, dto.content.orEmpty(), photos = dto.attachments.size)
        dto.role == "tool" && dto.isConfirmation -> ChatMessage(dto.id, ChatRole.Confirmation, dto.content.orEmpty())
        dto.role == "tool" -> ChatMessage(dto.id, ChatRole.Tool, dto.content.orEmpty(), card = card(dto.toolName.orEmpty(), dto.result, dto.content.orEmpty()))
        else -> ChatMessage(dto.id, ChatRole.Assistant, dto.content.orEmpty())
    }

    fun card(toolName: String, result: JsonObject?, fallback: String): ToolCard {
        val data = (result?.get("data") as? JsonObject) ?: JsonObject(emptyMap())

        return ToolCard(
            toolName = toolName,
            title = toolLabels[toolName] ?: toolName,
            message = result.text("message") ?: fallback,
            success = result.bool("success") ?: true,
            fields = fieldLabels.mapNotNull { (key, label) ->
                (data[key] as? JsonPrimitive)?.let { value -> format(key, value)?.let { label to it } }
            },
            changes = (data["changes"] as? JsonArray).orEmpty().mapNotNull { change ->
                val item = change as? JsonObject ?: return@mapNotNull null
                val field = item.text("field") ?: return@mapNotNull null
                val label = fieldLabels[field] ?: field
                "$label: ${display(field, item["before"])} → ${display(field, item["after"])}"
            },
            notes = (data["review_notes"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonPrimitive)?.contentOrNull },
            rows = rows(data),
            confirmationToken = result.text("confirmation_token")
                ?.takeIf { result.bool("requires_user_confirmation") == true },
            decision = result.text("decision"),
        )
    }

    private fun rows(data: JsonObject): List<CardRow> {
        val lookalikes = listOf("similar_products", "catalog_matches").flatMap { (data[it] as? JsonArray).orEmpty() }
        val list = lookalikes.ifEmpty {
            listKeys.firstNotNullOfOrNull { key -> (data[key] as? JsonArray)?.takeIf { it.isNotEmpty() } }.orEmpty()
        }

        return list.take(8).mapNotNull { element ->
            val row = element as? JsonObject ?: return@mapNotNull null
            CardRow(
                title = row.text("name") ?: row.text("product") ?: row.text("appliance") ?: row.text("verdict_label") ?: "—",
                detail = detail(row),
            )
        }
    }

    private fun detail(row: JsonObject): String = buildList {
        row.text("company")?.let { add(it) }
        row.text("last_purchase_at")?.let { add("última compra ${brazilianDate(it)}") }
        row.text("opened_at")?.let { add(brazilianDate(it)) }
        row.number("orders")?.let { add("${it.toLong()} pedidos") }
        row.number("spent")?.let { add(formatMoney(it)) }
        row.number("quantity")?.let { add("${it.toLong()} un.") }
        row.number("units")?.let { add("${it.toLong()} un.") }
        row.number("revenue")?.let { add(formatMoney(it)) }
        row.number("total_stock")?.let { add("${it.toLong()} em estoque") }
        row.number("suggested")?.let { add("comprar ${it.toLong()}") }
        row.text("place")?.let { add(it) }
        if (row["product_id"] != null && row["price"] != null) row.number("price")?.let { add("já cadastrado, ${formatMoney(it)}") }
        if (row["catalog_product_id"] != null) add("no catálogo da plataforma" + if (row.bool("has_photo") == true) ", com foto" else "")
    }.joinToString(" · ")

    private fun format(key: String, value: JsonPrimitive): String? {
        if (value.contentOrNull.isNullOrBlank()) return null
        if (key.endsWith("_at")) return brazilianDate(value.content)
        if (key in moneyFields) return value.doubleOrNull?.let(::formatMoney) ?: value.content
        value.booleanOrNull?.let { return if (it) "Sim" else "Não" }

        return value.content
    }

    private fun display(field: String, element: JsonElement?): String {
        val primitive = element as? JsonPrimitive ?: return "—"

        return format(field, primitive) ?: "—"
    }

    /**
     * "2026-11-16" vira "16/11/2026", e "2026-11-16T14:05:00-03:00" vira
     * "16/11/2026 14:05": o servidor manda ISO, a tela fala português.
     */
    fun brazilianDate(iso: String): String {
        val match = Regex("""^(\d{4})-(\d{2})-(\d{2})(?:[T ](\d{2}):(\d{2}))?""").find(iso) ?: return iso
        val (year, month, day, hour, minute) = match.destructured

        return "$day/$month/$year" + if (hour.isNotEmpty()) " $hour:$minute" else ""
    }

    private fun JsonObject?.text(key: String): String? = (this?.get(key) as? JsonPrimitive)?.contentOrNull

    private fun JsonObject?.bool(key: String): Boolean? = (this?.get(key) as? JsonPrimitive)?.booleanOrNull

    private fun JsonObject.number(key: String): Double? = (this[key] as? JsonPrimitive)?.doubleOrNull
}
