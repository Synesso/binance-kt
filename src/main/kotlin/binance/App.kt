package binance

import com.binance.api.client.BinanceApiClientFactory
import com.binance.api.client.domain.event.DepthEvent



fun main(args: Array<String>) {
    val factory = BinanceApiClientFactory.newInstance(args[0], args[1])
    val restClient = factory.newRestClient()

    val xlmBtcOrderBook = restClient.getOrderBook("NEOBTC", 20)
    println(xlmBtcOrderBook)
    println(xlmBtcOrderBook.spread())
    println(xlmBtcOrderBook.bestAsk())
    val bestBid = xlmBtcOrderBook.bestBid()
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

    wsClient.onDepthEvent("ethbtc", { response: DepthEvent ->
        println(response)
//        println(restClient.getOrderBook("ethbtc", 5).asks)
//        println()
    })

}
