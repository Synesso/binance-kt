package binance

import io.kotlintest.matchers.collections.shouldContainExactly
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import org.junit.Test

class OrderBookExtTest {

    @Test
    fun `Pegged bid is inside spread`() {
        val book = OrderBook(
            bids = listOf(OrderBookEntry("25.0", "1")),
            asks = listOf(OrderBookEntry("35.0", "1"))
        )
        book.pegBidPrice("0.1", "30.0") shouldBe "25.1"
    }

    @Test
    fun `Pegged bid is above limit`() {
        val book = OrderBook(
            bids = listOf(OrderBookEntry("25.0", "1")),
            asks = listOf(OrderBookEntry("35.0", "1"))
        )
        shouldThrow<PeggedPriceExceedsLimit> { book.pegBidPrice("0.3", "25.2") }
    }

    @Test
    fun `Pegged bid is above spread`() {
        val book = OrderBook(
            bids = listOf(OrderBookEntry("25.0", "1")),
            asks = listOf(OrderBookEntry("26.0", "1"))
        )
        shouldThrow<PeggedPriceExceedsSpread> { book.pegBidPrice("1.3", "30.0") }
    }

    @Test
    fun `Pegged bid when there are no orders`() {
        val book = OrderBook(bids = emptyList(), asks = emptyList())
        shouldThrow<PeggedPriceNoOrder> { book.pegBidPrice("0.3", "25.2") }
    }

    @Test
    fun `Pegged ask is inside spread`() {
        val book = OrderBook(
            bids = listOf(OrderBookEntry("25.0", "1")),
            asks = listOf(OrderBookEntry("35.0", "1"))
        )
        book.pegAskPrice("0.1", "30.0") shouldBe "34.9"
    }

    @Test
    fun `Pegged ask is above limit`() {
        val book = OrderBook(
            bids = listOf(OrderBookEntry("25.0", "1")),
            asks = listOf(OrderBookEntry("35.0", "1"))
        )
        shouldThrow<PeggedPriceExceedsLimit> { book.pegAskPrice("0.3", "34.8") }
    }

    @Test
    fun `Pegged ask is above spread`() {
        val book = OrderBook(
            bids = listOf(OrderBookEntry("25.0", "1")),
            asks = listOf(OrderBookEntry("26.0", "1"))
        )
        shouldThrow<PeggedPriceExceedsSpread> { book.pegAskPrice("1.3", "20.0") }
    }

    @Test
    fun `Pegged ask when there are no orders`() {
        val book = OrderBook(bids = emptyList(), asks = emptyList())
        shouldThrow<PeggedPriceNoOrder> { book.pegAskPrice("0.3", "25.2") }
    }

    @Test
    fun `Merge entries from nothing to nothing`() {
        merge(listOf(), listOf(), inc = true) shouldBe emptyList()
        merge(listOf(), listOf(), inc = false) shouldBe emptyList()
    }

    @Test
    fun `Merge entries from something to nothing`() {
        val from = listOf(OrderBookEntry("25.0", "1"), OrderBookEntry("25.1", "2"))
        merge(from, listOf(), inc = true) shouldContainExactly from
        merge(from, listOf(), inc = false) shouldContainExactly from
    }

    @Test
    fun `Merge entries from nothing to something`() {
        val to = listOf(OrderBookEntry("25.0", "1"), OrderBookEntry("25.1", "2"))
        merge(listOf(), to, inc = true) shouldContainExactly to
        merge(listOf(), to, inc = false) shouldContainExactly to
    }

    @Test
    fun `Merge interleaving, incrementing values`() {
        val from = listOf(OrderBookEntry("25.0", "1"), OrderBookEntry("25.3", "1"), OrderBookEntry("25.9", "1"))
        val to = listOf(OrderBookEntry("25.2", "1"), OrderBookEntry("25.4", "1"), OrderBookEntry("25.6", "1"))
        merge(from, to, inc = true).map { it.price } shouldContainExactly
                listOf("25.0", "25.2", "25.3", "25.4", "25.6", "25.9")
    }

    @Test
    fun `Merge interleaving, decrementing values`() {
        val from = listOf(OrderBookEntry("25.9", "1"), OrderBookEntry("25.3", "1"), OrderBookEntry("25.0", "1"))
        val to = listOf(OrderBookEntry("25.6", "1"), OrderBookEntry("25.4", "1"), OrderBookEntry("25.2", "1"))
        merge(from, to, inc = false).map { it.price } shouldContainExactly
                listOf("25.9", "25.6", "25.4", "25.3", "25.2", "25.0")
    }

    @Test
    fun `Merge overwriting, incrementing values`() {
        val from = listOf(OrderBookEntry("25.0", "1"), OrderBookEntry("25.3", "1.1"), OrderBookEntry("25.9", "0.9"))
        val to = listOf(OrderBookEntry("25.2", "1"), OrderBookEntry("25.3", "1"), OrderBookEntry("25.9", "1"))
        merge(from, to, inc = true).map { it.price to it.qty } shouldContainExactly
                listOf("25.0" to "1", "25.2" to "1", "25.3" to "1.1", "25.9" to "0.9")
    }

    @Test
    fun `Merge overwriting, decrementing values`() {
        val from = listOf(OrderBookEntry("25.9", "1"), OrderBookEntry("25.3", "1.1"), OrderBookEntry("25.0", "0.9"))
        val to = listOf(OrderBookEntry("25.9", "0.4"), OrderBookEntry("25.3", "1"), OrderBookEntry("25.0", "1"))
        merge(from, to, inc = true).map { it.price to it.qty } shouldContainExactly
                listOf("25.9" to "1", "25.3" to "1.1", "25.0" to "0.9")
    }

    @Test
    fun `Merge deleting, incrementing values`() {
        val from = listOf(OrderBookEntry("25.0", "1"), OrderBookEntry("25.3", "0"), OrderBookEntry("25.9", "0.9"))
        val to = listOf(OrderBookEntry("25.2", "1"), OrderBookEntry("25.3", "1"), OrderBookEntry("25.9", "1"))
        merge(from, to, inc = true).map { it.price to it.qty } shouldContainExactly
                listOf("25.0" to "1", "25.2" to "1", "25.9" to "0.9")
    }

    @Test
    fun `Merge deleting, decrementing values`() {
        val from = listOf(OrderBookEntry("25.9", "0"), OrderBookEntry("25.3", "0"), OrderBookEntry("25.0", "0"))
        val to = listOf(OrderBookEntry("25.6", "0.4"), OrderBookEntry("25.3", "1"), OrderBookEntry("25.0", "1"))
        merge(from, to, inc = false).map { it.price to it.qty } shouldContainExactly listOf("25.6" to "0.4")
    }

    @Test
    fun `Merge deleting, values with trailing, not present zero`() {
        val from = listOf(OrderBookEntry("25.9", "0"), OrderBookEntry("25.3", "0"), OrderBookEntry("25.0", "0"))
        val to = listOf(OrderBookEntry("25.6", "0.4"), OrderBookEntry("25.3", "1"))
        merge(from, to, inc = false).map { it.price to it.qty } shouldContainExactly listOf("25.6" to "0.4")
    }

}
