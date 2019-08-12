package binance

import com.binance.api.client.BinanceApiClientFactory
import com.binance.api.client.domain.event.DepthEvent



fun main(args: Array<String>) {
    val factory = BinanceApiClientFactory.newInstance(args[0], args[1])
    val restClient = factory.newRestClient()

    val orderBook = restClient.getOrderBook("ETHBTC", 20)
    println(orderBook)
    println(orderBook.spread())
    println(orderBook.bestAsk())
    val bestBid = orderBook.bestBid()
    println(bestBid)
//    println("Pegged: ${xlmBtcOrderBook.pegBidPrice("0.0000003", "0.00094500")}")

//    client.newOrderTest(NewOrder.limitBuy("XLMBTC", TimeInForce.GTC, bestBid?.qty, bestBid?.price))
//    val orderResp = client.newOrder(NewOrder.limitBuy("XLMBTC", TimeInForce.GTC, bestBid?.qty, bestBid?.price))
//    orderResp.clientOrderId


//    client.getOrderStatus(OrderStatusRequest("XLMBTC"))

    val listenKey = restClient.startUserDataStream()
    restClient.keepAliveUserDataStream(listenKey)
    restClient.closeUserDataStream(listenKey)
//    System.exit(0)


    val wsClient = factory.newWebSocketClient()

    wsClient.onDepthEvent("ethbtc", { event: DepthEvent ->
        println("adding ${event.asks}")
        orderBook.update(event)
        println("orderbook: ${orderBook.asks}")
    })

}
