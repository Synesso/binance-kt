package binance

import com.binance.api.client.domain.market.OrderBookEntry

@Suppress("FunctionName")
fun OrderBookEntry(price: String, qty: String): OrderBookEntry = OrderBookEntry().apply {
    setPrice(price)
    setQty(qty)
}
