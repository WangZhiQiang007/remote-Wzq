local voucherId = ARGV[1]
local userId = ARGV[2]
-- 库存key
local stockKey = "seckill:stock:" .. voucherId
-- 订单key
local orderKey = "seckill:order:" .. voucherId
--  判断库存是否充足
if (tonumber(redis.call("get", stockKey)) <= 0) then
    return 1
end
-- 判断用户是否重复下单
if (redis.call("sismember", orderKey, userId) == 1) then
    return 2
end
redis.call("incrby", stockKey, -1) -- 减少库存
redis.call("sadd", orderKey, userId) -- 记录用户
--redis.call("xadd", "stream.orders", "*", "userId", userId, "voucherId", voucherId,"id",orderId)
return 0