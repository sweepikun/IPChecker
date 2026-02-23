# IPChecker 构建说明

## 前置要求
- Java 16 或更高版本
- Gradle 8.x 或使用 Gradle Wrapper

## 构建方法

### 方法 1: 使用 Gradle Wrapper (推荐)
```bash
# Windows
gradlew.bat build

# Linux/Mac
./gradlew build
```

### 方法 2: 使用系统 Gradle
```bash
gradle build
```

## 输出文件
构建成功后，插件文件位于：
```
build/libs/IPChecker-1.0.0.jar
```

## 命令说明

| 命令 | 描述 | 权限 |
|------|------|------|
| `/ipchecker unban <IP>` | 解封指定 IP | ipchecker.admin |
| `/ipchecker info <玩家>` | 查看玩家 IP 信息 | ipchecker.admin |
| `/ipchecker reload` | 重载配置 | ipchecker.admin |
| `/ipchecker whitelist <add\|remove> <IP>` | 管理白名单 | ipchecker.admin |
| `/ipinfo <玩家>` | 查看玩家 IP 信息（快捷命令） | ipchecker.admin |
| `/ipreload` | 重载配置（快捷命令） | ipchecker.admin |
| `/ipwhitelist <add\|remove\|list> [IP]` | 管理白名单（快捷命令） | ipchecker.admin |

## 权限节点

- `ipchecker.admin` - 访问所有管理命令（默认 OP）
- `ipchecker.notify` - 接收封禁通知（默认 OP）
- `ipchecker.bypass` - 绕过 IP 检查（默认无）

## 配置文件

编辑 `plugins/IPChecker/config.yml`：

```yaml
# 玩家进入服务器后延迟检查时间（秒）
check-delay: 120

# 自动更新设置
auto-update:
  enabled: true
  interval-hours: 24

# 是否记录封禁日志
log-enabled: true

messages:
  kick: "&c检测到您的网络环境异常，请联系管理员"
  admin-notify: "&c[IPChecker] 玩家 {player} 使用 VPN/机房 IP 已被封禁"
```

## 数据存储

- **封禁数据**: `plugins/IPChecker/bans.yml`
- **IP 库文件**: `plugins/IPChecker/ips/`
  - `datacenter.txt` - 机房 IP 列表
  - `vpn.txt` - VPN IP 列表
  - `hash.txt` - 版本哈希值

## 注意事项

1. 首次运行时会自动下载 IP 库文件（约几 MB）
2. 确保服务器可以访问 `cdn.jsdmirror.cn`
3. 建议定期备份 `bans.yml` 文件
4. 使用 `/ipchecker whitelist add <IP>` 添加例外 IP
