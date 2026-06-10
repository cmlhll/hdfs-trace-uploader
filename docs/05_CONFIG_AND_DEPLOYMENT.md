# 05 - 配置与部署

## 1. 配置文件

参考 `config/example-agent.yaml`。

主要配置块：

- app identity；
- local spool；
- file scanner；
- HDFS paths；
- upload concurrency；
- retry/backoff；
- manifest；
- GC；
- disk watermark；
- metrics。

## 2. 本地部署形态

推荐每台业务机器部署一个独立 Agent：

```text
systemd service / daemonset / sidecar
```

但 Agent 与业务进程应独立：

```text
Application JVM
  -> only writes local files
Uploader Agent process
  -> uploads HDFS
```

## 3. systemd 示例

```ini
[Unit]
Description=HDFS Trace Uploader Agent
After=network-online.target

[Service]
User=trace-uploader
ExecStart=/opt/trace-uploader/bin/trace-uploader --config /etc/trace-uploader/agent.yaml
Restart=always
RestartSec=10
LimitNOFILE=1048576

[Install]
WantedBy=multi-user.target
```

## 4. Kerberos

生产环境需要支持：

- principal；
- keytab；
- ticket refresh；
- Hadoop conf dir；
- NameService HA；
- Router-Based Federation。

MVP 可先跳过 Kerberos，但代码接口不能阻碍后续接入。

## 5. 权限原则

- 业务进程不需要 HDFS 写权限；
- Agent 只对目标 app raw 目录有写权限；
- final 文件禁止覆盖；
- staging 目录只允许 Agent 写。

## 6. 本地磁盘水位

默认策略：

| 水位 | 动作 |
|---|---|
| < 60% | 正常 |
| 60%~75% | warn |
| 75%~85% | 降低扫描频率，提升上传优先级 |
| 85%~90% | error，建议业务降采样 |
| > 90% | critical，保护业务，停止非必要写入或触发外部降级 |

Agent MVP 只告警不删除未提交文件。

