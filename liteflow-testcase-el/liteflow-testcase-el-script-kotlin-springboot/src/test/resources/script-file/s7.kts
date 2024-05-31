import com.yomahub.liteflow.slot.DefaultContext

var a = 2
var b = 3
val defaultContext = bindings["defaultContext"] as DefaultContext
defaultContext.setData("s7", a * b)