package binance

import com.binance.api.client.domain.event.DepthEvent
import com.binance.api.client.domain.market.OrderBook
import com.binance.api.client.domain.market.OrderBookEntry
import java.math.BigDecimal

fun OrderBook(bids: List<OrderBookEntry>, asks: List<OrderBookEntry>): OrderBook = OrderBook().apply {
    setAsks(asks)
    setBids(bids)
}

fun OrderBook.spread(): BigDecimal? {
    return if (this.asks.isEmpty() || this.bids.isEmpty()) null
    else BigDecimal(this.asks[0].price).minus(BigDecimal(this.bids[0].price))
}

fun OrderBook.bestAsk(): OrderBookEntry? = this.asks.getOrNull(0)
fun OrderBook.bestBid(): OrderBookEntry? = this.bids.getOrNull(0)


// Pegged prices
fun OrderBook.pegBidPrice(buffer: String, limit: String): String? {
    val limitBigDecimal = BigDecimal(limit)
    val bufferBigDecimal = BigDecimal(buffer)

    val bestPrice = this.bestBid()?.price ?: throw PeggedPriceNoOrder
    val bestPriceBigDecimal = BigDecimal(bestPrice)
    val pegPrice = bestPriceBigDecimal.plus(bufferBigDecimal)

    val bestAskBigDecimal = BigDecimal(bestAsk()?.price ?: return pegPrice.toPlainString())
    if (bestAskBigDecimal <= pegPrice) throw PeggedPriceExceedsSpread(bestPriceBigDecimal, bestAskBigDecimal, pegPrice)
    if (limitBigDecimal <= pegPrice) throw PeggedPriceExceedsLimit(bestPriceBigDecimal, bufferBigDecimal, limitBigDecimal, pegPrice)
    return pegPrice.toPlainString()
}

fun OrderBook.pegAskPrice(buffer: String, limit: String): String? {
    val limitBigDecimal = BigDecimal(limit)
    val bufferBigDecimal = BigDecimal(buffer)

    val bestPrice = this.bestAsk()?.price ?: throw PeggedPriceNoOrder
    val bestPriceBigDecimal = BigDecimal(bestPrice)
    val pegPrice = bestPriceBigDecimal.minus(bufferBigDecimal)

    val bestBidBigDecimal = BigDecimal(bestBid()?.price ?: return pegPrice.toPlainString())
    if (bestBidBigDecimal >= pegPrice) throw PeggedPriceExceedsSpread(bestPriceBigDecimal, bestBidBigDecimal, pegPrice)
    if (limitBigDecimal >= pegPrice) throw PeggedPriceExceedsLimit(bestPriceBigDecimal, bufferBigDecimal, limitBigDecimal, pegPrice)
    return pegPrice.toPlainString()
}

class PeggedPriceExceedsLimit(price: BigDecimal, buffer: BigDecimal, limit: BigDecimal, wouldBe: BigDecimal):
    Exception("Pegged price exceeds limit. [price=$price, buffer=$buffer, limit=$limit, wouldBe=$wouldBe]")

class PeggedPriceExceedsSpread(price: BigDecimal, otherPrice: BigDecimal, wouldBe: BigDecimal):
    Exception("Pegged price exceeds spread. [price=$price, otherPrice=$otherPrice, wouldBe=$wouldBe]")

object PeggedPriceNoOrder:
    Exception("Pegged price cannot be calculated as there's no other order in the book.")



// Updating
fun OrderBook.update(depthEvent: DepthEvent) {
    this.asks = merge(depthEvent.asks, this.asks, inc = true)
    this.bids = merge(depthEvent.bids, this.bids, inc = false)
}

fun merge(
    from: List<OrderBookEntry>,
    to: List<OrderBookEntry>,
    inc: Boolean
): List<OrderBookEntry> {

    if (to.isEmpty()) return from
    if (from.isEmpty()) return to

    var fi = 0
    var ti = 0
    val acc = mutableListOf<OrderBookEntry>()

    while (fi < from.size || ti < to.size) {

        if (fi == from.size) {
            acc.addAll(to.drop(ti))
            return acc.filterNot { it.qty.toDouble() == 0.0  }
        }

        if (ti == to.size) {
            acc.addAll(from.drop(fi))
            return acc.filterNot { it.qty.toDouble() == 0.0  }
        }

        val f = from[fi]
        val t = to[ti]

        val fp = f.price.toDouble()
        val tp = t.price.toDouble()

        if (fp == tp) {
            if (f.qty.toDouble() != 0.0) acc.add(f)
            fi++
            ti++
        } else if ((inc && fp < tp) || (!inc && fp > tp)) {
            acc.add(f)
            fi++
        } else {
            acc.add(t)
            ti++
        }
    }

    return acc.filterNot { it.qty.toDouble() == 0.0  }
}