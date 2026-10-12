# 图灵航空 MCP 天气服务

基于 Spring AI MCP（STDIO 协议）实现的天气工具服务器，作为主应用的外部 MCP 服务器被拉起。

## 提供工具

- `getWeatherForecastByLocation`：根据经纬度返回实时天气（OpenMeteo 免费 API，无需密钥）
- `getAirQuality`：返回空气质量数据

## 运行

主应用通过 `mcp-servers-config.json` 以子进程方式启动本服务（需 JDK 17）。
